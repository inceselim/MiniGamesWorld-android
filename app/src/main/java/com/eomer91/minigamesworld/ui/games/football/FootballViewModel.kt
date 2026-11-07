package com.eomer91.minigamesworld.ui.games.football

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.FbClubLife
import com.eomer91.minigamesworld.ui.games.football.engine.FbFixture
import com.eomer91.minigamesworld.ui.games.football.engine.FbGen
import com.eomer91.minigamesworld.ui.games.football.engine.FbLineup
import com.eomer91.minigamesworld.ui.games.football.engine.FbQuick
import com.eomer91.minigamesworld.ui.games.football.engine.FbSave
import com.eomer91.minigamesworld.ui.games.football.engine.FbSeason
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.MatchSim
import com.eomer91.minigamesworld.ui.games.football.engine.NewsKind
import com.eomer91.minigamesworld.ui.games.football.engine.SeasonSummary
import com.eomer91.minigamesworld.ui.games.football.engine.StageType
import com.eomer91.minigamesworld.ui.games.football.engine.Tactics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Dunyanin dosyaya kaydi (yedekli, atomik). */
@Singleton
class FootballRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dir = File(context.filesDir, "football").apply { mkdirs() }
    private val file = File(dir, "career.sav")
    private val backup = File(dir, "career.bak")

    fun exists() = file.exists() || backup.exists()

    fun load(): FbWorld? {
        for (f in listOf(file, backup)) {
            if (!f.exists()) continue
            val w = runCatching { FileInputStream(f).use { FbSave.read(it) } }.getOrNull()
            if (w != null) return w
        }
        return null
    }

    fun save(w: FbWorld) {
        val tmp = File(dir, "career.tmp")
        FileOutputStream(tmp).use { FbSave.write(w, it) }
        if (file.exists()) { backup.delete(); file.renameTo(backup) }
        tmp.renameTo(file)
    }

    fun delete() { file.delete(); backup.delete() }
}

enum class FbPhase { LOADING, START, SETUP, HUB, PREMATCH, LIVE, RESULT, SEASON_END }

enum class FbTab { HOME, SQUAD, TACTICS, FIXTURES, COMPETITIONS, TRANSFERS, CLUB, INBOX, GUIDE }

/** Kullanicinin oynadigi macin sonucu (sonuc ekrani). */
class FbResult(val fixture: Int, val sim: MatchSim)

data class FbToast(@StringRes val res: Int, val arg: String = "")

data class FbUiState(
    val phase: FbPhase = FbPhase.LOADING,
    val tab: FbTab = FbTab.HOME,
    /** Dunya degisti: yeniden ciz */
    val version: Int = 0,
    /** Arka planda simulasyon (dunyaya dokunma) */
    val busy: Boolean = false,
    val busyDate: String = "",
    val hasSave: Boolean = false,
    /** Kurulum: dunya hazir mi */
    val setupReady: Boolean = false,
    val pending: Int = -1,
    val result: FbResult? = null,
    val summary: SeasonSummary? = null,
    val toast: FbToast? = null,
    /** Sozlesme gorusmesi (bkz. openNegotiation), yoksa NO_NEGOTIATION */
    val negotiation: Int = NO_NEGOTIATION,
)

const val NO_NEGOTIATION = Int.MIN_VALUE

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FootballViewModel @Inject constructor(
    private val repo: FootballRepository,
) : ViewModel() {

    /** Tum motor islemleri tek is parcaciginda */
    private val engine = Dispatchers.Default.limitedParallelism(1)

    /** Oyun dunyasi. Arayuz yalnizca [FbUiState.busy] false iken okur. */
    var world: FbWorld? = null
        private set

    private val _state = MutableStateFlow(FbUiState())
    val state: StateFlow<FbUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val has = withContext(engine) { repo.exists() }
            _state.update { it.copy(phase = FbPhase.START, hasSave = has) }
        }
    }

    private fun bump() = _state.update { it.copy(version = it.version + 1) }

    fun toast(@StringRes res: Int, arg: String = "") = _state.update { it.copy(toast = FbToast(res, arg)) }
    fun clearToast() = _state.update { it.copy(toast = null) }
    fun setTab(t: FbTab) = _state.update { it.copy(tab = t) }

    /** Kucuk degisiklikler (arayuzden, motor bostayken). */
    fun edit(block: (FbWorld) -> Unit) {
        val w = world ?: return
        if (_state.value.busy) return
        block(w)
        bump()
    }

    // ================================================================== kariyer

    fun loadCareer() {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val w = withContext(engine) { repo.load() }
            if (w == null) {
                _state.update { it.copy(busy = false, hasSave = false, toast = FbToast(R.string.fb_load_failed)) }
                return@launch
            }
            world = w
            _state.update { it.copy(busy = false, phase = FbPhase.HUB, tab = FbTab.HOME) }
            checkPending()
        }
    }

    /** Yeni kariyer: dunyayi uret, kulup secimine gec. */
    fun newCareer() {
        _state.update { it.copy(phase = FbPhase.SETUP, setupReady = false) }
        viewModelScope.launch {
            val w = withContext(engine) { FbGen(System.currentTimeMillis()).generate() }
            world = w
            _state.update { it.copy(setupReady = true, version = it.version + 1) }
        }
    }

    fun startCareer(club: Int, managerName: String) {
        val w = world ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            withContext(engine) {
                w.userClub = club
                w.managerName = managerName
                w.clubs[club].managerName = managerName
                FbSeason.start(w)
                repo.save(w)
            }
            _state.update { it.copy(busy = false, phase = FbPhase.HUB, tab = FbTab.HOME, hasSave = true, version = it.version + 1) }
        }
    }

    /** Kulup secimini iptal et (dunya henuz baslatilmadi). */
    fun abandonSetup() {
        if (_state.value.busy) return
        world = null
        _state.update { it.copy(phase = FbPhase.START, setupReady = false) }
    }

    fun abandonCareer() {
        viewModelScope.launch {
            withContext(engine) { repo.delete() }
            world = null
            _state.update { FbUiState(phase = FbPhase.START, hasSave = false) }
        }
    }

    fun save() {
        val w = world ?: return
        if (_state.value.busy) return
        viewModelScope.launch { withContext(engine) { runCatching { repo.save(w) } } }
    }

    // ================================================================== ilerleme

    private fun checkPending() {
        val w = world ?: return
        val f = FbSeason.pendingUserFixture(w)
        if (f != null) _state.update { it.copy(phase = FbPhase.PREMATCH, pending = f.id) }
    }

    /**
     * "Devam": kullanicinin maci, onemli bir haber ya da sezon sonu gelene kadar gunleri ilerletir
     * (en fazla 14 gun).
     */
    fun continueGame() {
        val w = world ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, busyDate = FbFormat.date(w)) }
        viewModelScope.launch {
            var summary: SeasonSummary? = null
            var pending = -1
            withContext(engine) {
                val newsBefore = w.market.news.size
                var days = 0
                while (true) {
                    val r = FbSeason.advanceDay(w)
                    if (r.pending != null) { pending = r.pending.id; break }
                    days++
                    _state.update { it.copy(busyDate = FbFormat.date(w)) }
                    if (r.seasonEnded) { summary = FbSeason.lastSummary; break }
                    if (FbSeason.pendingUserFixture(w) != null) { pending = FbSeason.pendingUserFixture(w)!!.id; break }
                    if (w.market.news.drop(newsBefore).any { it.kind in STOP_NEWS }) break
                    if (days >= 14) break
                }
                runCatching { repo.save(w) }
            }
            _state.update {
                it.copy(
                    busy = false, version = it.version + 1,
                    phase = when { summary != null -> FbPhase.SEASON_END; pending >= 0 -> FbPhase.PREMATCH; else -> FbPhase.HUB },
                    pending = pending, summary = summary,
                )
            }
        }
    }

    fun closeSeasonSummary() = _state.update { it.copy(phase = FbPhase.HUB, summary = null) }

    // ================================================================== mac

    /** Kullanici takiminin mac taktigi (kayitli yoksa YZ onerisi). */
    fun userTactics(w: FbWorld): Tactics {
        w.userTactics?.let { return it }
        val players = w.clubPlayers(w.userClub)
        val f = FbLineup.bestFormation(players)
        return Tactics(formation = f)
    }

    /** Mac kurulumu (onizleme ve 2D mac ekrani icin). */
    fun buildUserMatch(w: FbWorld, f: FbFixture, recordAll: Boolean, userControlled: Boolean = recordAll): MatchSim {
        val st = w.state!!
        val comp = st.comps[f.comp]
        val stage = if (f.stage >= 0) comp.stages[f.stage] else null
        val needWinner = stage != null && stage.type == StageType.KO && (stage.legs == 1 || f.leg == 2)
        val neutral = FbSeason.isNeutral(w, f)
        val agg = FbSeason.aggregateOffset(st, f)
        val userHome = f.home == w.userClub
        val ut = userTactics(w)
        val us = FbSeason.clubSheet(w, w.userClub)
        val opp = if (userHome) f.away else f.home
        val os = FbSeason.clubSheet(w, opp)
        val ot = FbLineup.aiTactics(os.strength, us.strength, !userHome && !neutral, os.formation, f.id)
        val userSide = FbSeason.sideOf(us, w.clubs[w.userClub].name, ut, userHome && !neutral)
        val oppSide = FbSeason.sideOf(os, w.clubs[opp].name, ot, !userHome && !neutral)
        val seed = w.seed xor (w.season.toLong() shl 20) xor (f.id * 104729L)
        val us0 = if (userControlled) (if (userHome) 0 else 1) else -1
        return if (userHome) MatchSim(userSide, oppSide, seed, needWinner, agg, recordAll, userSide = us0)
        else MatchSim(oppSide, userSide, seed, needWinner, agg, recordAll, userSide = us0)
    }

    /** Hizli sonuc: maci arka planda tamamla. */
    fun quickResult() {
        val w = world ?: return
        val fid = _state.value.pending
        if (fid < 0 || _state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val sim = withContext(engine) {
                val f = w.state!!.fixtures[fid]
                val sim = buildUserMatch(w, f, recordAll = false).playToEnd()
                FbSeason.submit(w, f, FbQuick.fromSim(sim))
                sim
            }
            _state.update { it.copy(busy = false, phase = FbPhase.RESULT, result = FbResult(fid, sim), pending = -1, version = it.version + 1) }
        }
    }

    /** 2D mac ekrani bitince sonucu isle. */
    fun submitMatch(sim: MatchSim) {
        val w = world ?: return
        val fid = _state.value.pending
        if (fid < 0) return
        val f = w.state!!.fixtures[fid]
        FbSeason.submit(w, f, FbQuick.fromSim(sim))
        _state.update { it.copy(phase = FbPhase.RESULT, result = FbResult(fid, sim), pending = -1, version = it.version + 1) }
    }

    fun closeResult() {
        _state.update { it.copy(phase = FbPhase.HUB, result = null) }
        save()
    }

    /** Mac oncesi ekrandan hub sekmesine (bekleyen mac korunur). */
    fun toHubTab(t: FbTab) = _state.update { it.copy(phase = FbPhase.HUB, tab = t) }

    /** Canli 2D mac (ekranda adim adim oynatilir). */
    var live: MatchSim? = null
        private set

    fun playLive() {
        val w = world ?: return
        val fid = _state.value.pending
        if (fid < 0 || _state.value.busy) return
        live = buildUserMatch(w, w.state!!.fixtures[fid], recordAll = true)
        _state.update { it.copy(phase = FbPhase.LIVE) }
    }

    /** Canli mac bitti: sonucu isle. */
    fun finishLive() {
        val sim = live ?: return
        live = null
        submitMatch(sim)
    }

    // ================================================================== sozlesme gorusmesi

    /** Acik gorusme: transfer teklifi id'si ya da (negatif - 1 - oyuncu) sozlesme yenileme. */
    fun openNegotiation(offerId: Int) = _state.update { it.copy(negotiation = offerId) }
    fun openRenewal(playerId: Int) = _state.update { it.copy(negotiation = -1 - playerId) }
    fun closeNegotiation() = _state.update { it.copy(negotiation = NO_NEGOTIATION) }

    // ================================================================== reklam odulleri

    fun revealPlayer(id: Int) = edit { it.revealed.add(id) }

    fun claimAdBonus() {
        val w = world ?: return
        val v = FbClubLife.claimAdBonus(w)
        bump()
        if (v > 0) toast(R.string.fb_ad_bonus_got, FbFormat.money(v))
    }

    companion object {
        val STOP_NEWS = setOf(
            NewsKind.OFFER_RECEIVED, NewsKind.OFFER_ACCEPTED, NewsKind.OFFER_REJECTED, NewsKind.OFFER_COUNTER, NewsKind.PLAYER_REFUSED,
            NewsKind.TRANSFER_DONE, NewsKind.AUCTION_RESULT, NewsKind.PRECONTRACT, NewsKind.PLAYER_UNHAPPY, NewsKind.CONTRACT_EXPIRING,
            NewsKind.YOUTH_INTAKE,
        )
    }
}
