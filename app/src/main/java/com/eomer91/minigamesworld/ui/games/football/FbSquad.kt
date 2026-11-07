package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.football.engine.Attr
import com.eomer91.minigamesworld.ui.games.football.engine.AttrGroup
import com.eomer91.minigamesworld.ui.games.football.engine.ContractReply
import com.eomer91.minigamesworld.ui.games.football.engine.FbCountries
import com.eomer91.minigamesworld.ui.games.football.engine.FbPlayer
import com.eomer91.minigamesworld.ui.games.football.engine.FbTransfer
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.Hidden
import com.eomer91.minigamesworld.ui.games.football.engine.OfferStatus
import com.eomer91.minigamesworld.ui.games.football.engine.Pos
import com.eomer91.minigamesworld.ui.games.football.engine.TrainArea
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.roundToInt

// ====================================================================== kadro

private enum class SquadSort { NUMBER, NAME, POS, AGE, CA, POT, COND, MORALE, FORM, VALUE, WAGE, CONTRACT }

@Composable
fun FbSquad(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    var sort by remember { mutableStateOf(SquadSort.POS) }
    var desc by remember { mutableStateOf(false) }
    var open by remember { mutableIntStateOf(-1) }
    val club = world.userClub
    val players = world.clubPlayers(club)
    val est = players.associate { it.id to FbTransfer.estimate(world, club, it) }
    val sorted = players.sortedWith(compareBy<FbPlayer> {
        when (sort) {
            SquadSort.NUMBER -> it.squadNumber; SquadSort.NAME -> 0; SquadSort.POS -> it.bestPos().ordinal * 1000 - it.currentAbility()
            SquadSort.AGE -> world.age(it); SquadSort.CA -> -it.currentAbility(); SquadSort.POT -> -(est[it.id]?.second ?: 0)
            SquadSort.COND -> -it.condition; SquadSort.MORALE -> -it.morale; SquadSort.FORM -> -it.form
            SquadSort.VALUE -> -(FbTransfer.value(world, it) / 1000).toInt(); SquadSort.WAGE -> -it.wage; SquadSort.CONTRACT -> it.contractEnd
        }
    }.thenBy { it.lastName }).let { if (desc) it.reversed() else it }

    fun header(s: SquadSort) { if (sort == s) desc = !desc else { sort = s; desc = false } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            SortHead("#", 30, SquadSort.NUMBER, sort) { header(it) }
            Box(Modifier.weight(1f)) { SortHead(stringResource(R.string.fb_col_name), 120, SquadSort.NAME, sort) { header(it) } }
            SortHead(stringResource(R.string.fb_col_pos), 46, SquadSort.POS, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_age), 36, SquadSort.AGE, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_ability), 78, SquadSort.CA, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_potential), 78, SquadSort.POT, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_condition), 52, SquadSort.COND, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_morale), 52, SquadSort.MORALE, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_form), 40, SquadSort.FORM, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_value), 66, SquadSort.VALUE, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_wage), 60, SquadSort.WAGE, sort) { header(it) }
            SortHead(stringResource(R.string.fb_col_contract), 48, SquadSort.CONTRACT, sort) { header(it) }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(sorted, key = { it.id }) { p ->
                val (eca, epa) = est[p.id] ?: (0 to 0)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp)).background(FbColors.Panel)
                        .clickable { open = p.id }.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FbCell("${p.squadNumber}", 30.dp, AppColors.TextSecondary)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.fullName, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        if (p.injuryDays > 0) Badge("+", FbColors.Red)
                        if (p.suspendedMatches > 0) Badge("C", FbColors.Orange)
                        if (p.askingPrice > 0) Badge("T", FbColors.Blue)
                        if (p.loanFrom >= 0) Badge("K", FbColors.Purple)
                        if (p.wantsAway) Badge("!", FbColors.Yellow)
                    }
                    FbCell(stringResource(posShort(p.bestPos())), 46.dp, AppColors.TextMuted, bold = true)
                    FbCell("${world.age(p)}", 36.dp, AppColors.TextMuted)
                    Box(Modifier.width(78.dp)) { FbStars(FbFormat.stars(eca), 12.dp) }
                    Box(Modifier.width(78.dp)) { FbStars(FbFormat.stars(epa), 12.dp, if (p.id in world.revealed) FbColors.Green else FbColors.Yellow) }
                    Box(Modifier.width(52.dp)) { FbBar(p.condition) }
                    Box(Modifier.width(52.dp)) { FbBar(p.morale) }
                    FbCell("%.1f".format(p.form / 10f), 40.dp, FbColors.pct(p.form + 15))
                    FbCell(FbFormat.money(FbTransfer.value(world, p)), 66.dp, AppColors.TextMuted)
                    FbCell(FbFormat.wage(p.wage), 60.dp, AppColors.TextMuted)
                    FbCell("${p.contractEnd + 1}", 48.dp, if (p.contractEnd <= world.season) FbColors.Orange else AppColors.TextMuted)
                }
            }
        }
    }
    if (open >= 0) FbPlayerDialog(world, ui, vm, world.players[open]) { open = -1 }
}

@Composable
private fun Badge(t: String, c: Color) {
    Box(Modifier.padding(start = 4.dp).clip(RoundedCornerShape(4.dp)).background(c).padding(horizontal = 4.dp)) {
        Text(t, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SortHead(text: String, w: Int, s: SquadSort, cur: SquadSort, onClick: (SquadSort) -> Unit) {
    Text(text, Modifier.width(w.dp).clickable { onClick(s) }, color = if (s == cur) AppColors.Primary else AppColors.TextSecondary,
        fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
}

// ====================================================================== oyuncu karti

@Composable
fun FbPlayerDialog(world: FbWorld, ui: FbUiState, vm: FootballViewModel, p: FbPlayer, onClose: () -> Unit) {
    val user = world.userClub
    val own = p.club == user
    val showRewarded = rememberRewardedLauncher()
    val (eca, epa) = FbTransfer.estimate(world, user, p)
    val revealed = p.id in world.revealed
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f).clip(RoundedCornerShape(18.dp)).background(AppColors.Background).padding(12.dp)) {
            // Baslik
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${p.squadNumber}. ${p.fullName}", color = AppColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    val clubName = world.clubs.getOrNull(p.club)?.name ?: stringResource(R.string.fb_free_agent)
                    Text("${stringResource(posShort(p.bestPos()))} · ${world.age(p)} · ${FbCountries.all[p.nation].englishName} · $clubName",
                        color = AppColors.TextSecondary, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.fb_col_ability) + " ", color = AppColors.TextSecondary, fontSize = 12.sp); FbStars(FbFormat.stars(eca), 14.dp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.fb_col_potential) + " ", color = AppColors.TextSecondary, fontSize = 12.sp)
                        FbStars(FbFormat.stars(epa), 14.dp, if (revealed) FbColors.Green else FbColors.Yellow)
                    }
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = null, tint = AppColors.TextSecondary) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Yetenekler
                Row(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val gk = p.bestPos() == Pos.GK
                    val groups = if (gk) listOf(AttrGroup.GOALKEEPING, AttrGroup.MENTAL, AttrGroup.PHYSICAL) else listOf(AttrGroup.TECHNICAL, AttrGroup.MENTAL, AttrGroup.PHYSICAL)
                    for (g in groups) {
                        FbPanel(Modifier.weight(1f), title = stringResource(when (g) {
                            AttrGroup.TECHNICAL -> R.string.fb_g_technical; AttrGroup.MENTAL -> R.string.fb_g_mental
                            AttrGroup.PHYSICAL -> R.string.fb_g_physical; AttrGroup.GOALKEEPING -> R.string.fb_g_goalkeeping
                        })) {
                            for (a in Attr.entries.filter { it.group == g }) AttrRow(stringResource(attrName(a)), FbFormat.attr(p.attr(a)))
                        }
                    }
                }
                // Sag sutun: bilgiler, gizli ozellikler, eylemler
                Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FbPanel(Modifier.fillMaxWidth()) {
                        FbKeyValue(stringResource(R.string.fb_col_value), FbFormat.money(FbTransfer.value(world, p)))
                        if (p.club >= 0) {
                            FbKeyValue(stringResource(R.string.fb_col_wage), FbFormat.wage(p.wage) + stringResource(R.string.fb_per_week))
                            FbKeyValue(stringResource(R.string.fb_col_contract), "${p.contractEnd + 1}")
                        }
                        if (p.releaseClause > 0) FbKeyValue(stringResource(R.string.fb_release_clause), FbFormat.money(p.releaseClause.toLong()))
                        FbKeyValue(stringResource(R.string.fb_col_condition), "${p.condition}%")
                        FbKeyValue(stringResource(R.string.fb_col_morale), "${p.morale}%")
                        if (p.injuryDays > 0) FbKeyValue(stringResource(R.string.fb_injured), stringResource(R.string.fb_days, p.injuryDays), FbColors.Red)
                        FbKeyValue(stringResource(R.string.fb_height_foot), "${p.heightCm} cm · " + stringResource(when (p.foot.ordinal) { 0 -> R.string.fb_foot_right; 1 -> R.string.fb_foot_left; else -> R.string.fb_foot_both }))
                        FbKeyValue(stringResource(R.string.fb_caps), "${p.caps} / ${p.internationalGoals}")
                        val stats = world.state?.stats?.filter { (it.key and 0xffffffffL).toInt() == p.id && world.state!!.comps[(it.key shr 32).toInt()].kind.official }?.values
                        if (!stats.isNullOrEmpty()) {
                            val apps = stats.sumOf { it.apps + it.subs }; val goals = stats.sumOf { it.goals }; val ast = stats.sumOf { it.assists }
                            val rat = stats.sumOf { it.ratingSum } / 10f / apps.coerceAtLeast(1)
                            FbKeyValue(stringResource(R.string.fb_season_stats), stringResource(R.string.fb_stats_line, apps, goals, ast, "%.2f".format(rat)))
                        }
                    }
                    // Mevki yatkinliklari
                    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_positions)) {
                        val pos = Pos.entries.filter { p.fam(it) >= 8 }.sortedByDescending { p.fam(it) }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (x in pos) FbPill("${stringResource(posShort(x))} ${p.fam(x)}", if (p.fam(x) >= 18) FbColors.Green.copy(alpha = 0.3f) else FbColors.PanelHigh)
                        }
                    }
                    // Gizli ozellikler (reklamla)
                    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_hidden_attributes)) {
                        if (revealed) {
                            FbKeyValue(stringResource(R.string.fb_exact_ability), "${p.currentAbility()} / 200")
                            FbKeyValue(stringResource(R.string.fb_exact_potential), "${p.potential} / 200", FbColors.Green)
                            for (h in Hidden.entries) AttrRow(stringResource(hiddenName(h)), p.hidden(h))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = AppColors.TextSecondary)
                                Text(stringResource(R.string.fb_hidden_locked), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FbButton(stringResource(R.string.fb_reveal_with_ad), { showRewarded({ vm.revealPlayer(p.id) }, { vm.toast(R.string.ad_not_ready) }) },
                                    color = FbColors.Yellow, textColor = Color.Black)
                                Icon(Icons.Rounded.OndemandVideo, contentDescription = null, tint = FbColors.Yellow, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    if (own) OwnActions(world, vm, p) else OtherActions(world, vm, p)
                }
            }
        }
    }
}

@Composable
private fun AttrRow(name: String, v: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, color = AppColors.TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
        FbAttrValue(v)
    }
}

@Composable
private fun OwnActions(world: FbWorld, vm: FootballViewModel, p: FbPlayer) {
    var confirmRelease by remember { mutableStateOf(false) }
    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_training)) {
        Text(stringResource(R.string.fb_individual_focus), color = AppColors.TextSecondary, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FbChip(stringResource(R.string.fb_none), p.trainArea < 0, { vm.edit { p.trainArea = -1 } })
            for (a in TrainArea.entries) FbChip(stringResource(trainAreaName(a)), p.trainArea == a.ordinal, { vm.edit { p.trainArea = a.ordinal } })
        }
        Text(stringResource(R.string.fb_position_training), color = AppColors.TextSecondary, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FbChip(stringResource(R.string.fb_none), p.trainPos < 0, { vm.edit { p.trainPos = -1 } })
            for (x in Pos.entries) FbChip(stringResource(posShort(x)), p.trainPos == x.ordinal, { vm.edit { p.trainPos = x.ordinal } })
        }
    }
    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_contract_transfer)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FbButton(stringResource(R.string.fb_renew_contract), { vm.openRenewal(p.id) }, Modifier.fillMaxWidth())
            if (p.loanFrom < 0) {
                val v = FbTransfer.value(world, p)
                if (p.askingPrice > 0) FbButton(stringResource(R.string.fb_unlist, FbFormat.money(p.askingPrice)), { vm.edit { FbTransfer.setListed(p, 0) } }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                else FbButton(stringResource(R.string.fb_list_for_transfer, FbFormat.money(v)), { vm.edit { FbTransfer.setListed(p, v) } }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                FbButton(stringResource(if (p.loanListed) R.string.fb_unlist_loan else R.string.fb_list_for_loan), { vm.edit { FbTransfer.setLoanListed(p, !p.loanListed) } },
                    Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                if (FbTransfer.windowOpen(world)) FbButton(stringResource(R.string.fb_auction), {
                    vm.edit { w -> if (FbTransfer.startAuction(w, p, (v * 0.6).toLong(), 3) != null) vm.toast(R.string.fb_auction_started) }
                }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                if (!confirmRelease) FbButton(stringResource(R.string.fb_release), { confirmRelease = true }, Modifier.fillMaxWidth(), color = FbColors.Red.copy(alpha = 0.3f), textColor = FbColors.Red)
                else FbButton(stringResource(R.string.fb_release_confirm, FbFormat.money(FbTransfer.releaseCost(world, p))), { vm.edit { FbTransfer.release(it, p) }; confirmRelease = false },
                    Modifier.fillMaxWidth(), color = FbColors.Red, textColor = Color.Black)
            }
        }
    }
}

@Composable
private fun OtherActions(world: FbWorld, vm: FootballViewModel, p: FbPlayer) {
    val user = world.userClub
    val m = world.market
    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_transfer)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val listed = p.id in m.shortlist
            FbButton(stringResource(if (listed) R.string.fb_shortlist_remove else R.string.fb_shortlist_add), {
                vm.edit { if (listed) m.shortlist.remove(p.id) else m.shortlist.add(p.id) }
            }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            if (p.club >= 0 && p.loanFrom < 0) {
                val v = FbTransfer.value(world, p)
                var fee by remember(p.id) { mutableFloatStateOf(v.toFloat()) }
                val maxFee = (v * 3f).coerceAtLeast(100_000f)
                Text(stringResource(R.string.fb_offer_fee, FbFormat.money(fee.toLong())), color = AppColors.TextPrimary, fontSize = 13.sp)
                Slider(value = fee, onValueChange = { fee = it }, valueRange = 0f..maxFee)
                if (p.askingPrice > 0) Text(stringResource(R.string.fb_asking_price, FbFormat.money(p.askingPrice)), color = FbColors.Blue, fontSize = 12.sp)
                if (!FbTransfer.willing(world, p, user)) Text(stringResource(R.string.fb_not_interested), color = FbColors.Orange, fontSize = 12.sp)
                FbButton(stringResource(R.string.fb_make_offer), {
                    vm.edit { w ->
                        val (_, err) = FbTransfer.makeOffer(w, user, p.id, FbEconomyRound(fee.toLong()))
                        vm.toast(offerError(err))
                    }
                }, Modifier.fillMaxWidth())
                FbButton(stringResource(R.string.fb_request_loan), {
                    vm.edit { w -> val (_, err) = FbTransfer.makeOffer(w, user, p.id, 0, loan = true, loanWagePct = 70); vm.toast(offerError(err)) }
                }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            } else if (p.club < 0) {
                FbButton(stringResource(R.string.fb_negotiate_contract), { vm.openNegotiation(-1_000_000 - p.id) }, Modifier.fillMaxWidth())
            }
        }
    }
}

private fun FbEconomyRound(v: Long) = com.eomer91.minigamesworld.ui.games.football.engine.FbEconomy.roundValue(v.coerceAtLeast(0))

private fun offerError(err: String?): Int = when (err) {
    null -> R.string.fb_offer_sent
    "WINDOW" -> R.string.fb_err_window
    "FUNDS" -> R.string.fb_no_funds
    "DUPLICATE" -> R.string.fb_err_duplicate
    "ON_LOAN" -> R.string.fb_err_on_loan
    else -> R.string.fb_err_generic
}

fun trainAreaName(a: TrainArea): Int = when (a) {
    TrainArea.ATTACKING -> R.string.fb_t_attacking; TrainArea.DEFENDING -> R.string.fb_t_defending; TrainArea.TECHNICAL -> R.string.fb_t_technical
    TrainArea.TACTICAL -> R.string.fb_t_tactical; TrainArea.PHYSICAL -> R.string.fb_t_physical; TrainArea.MENTAL -> R.string.fb_t_mental
    TrainArea.SET_PIECES -> R.string.fb_t_set_pieces; TrainArea.GOALKEEPING -> R.string.fb_t_goalkeeping
}

// ====================================================================== sozlesme gorusmesi

/**
 * Gorusme kodlari: >= 0 transfer teklifi id'si; -1 - oyuncu = yenileme; -1_000_000 - oyuncu = serbest oyuncu.
 */
@Composable
fun FbNegotiationDialog(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val code = ui.negotiation
    val user = world.userClub
    val offer = if (code >= 0) world.market.offers.firstOrNull { it.id == code } else null
    val playerId = when {
        offer != null -> offer.player
        code <= -1_000_000 -> -1_000_000 - code
        else -> -1 - code
    }
    val p = world.players.getOrNull(playerId) ?: return
    val mode = when { offer != null -> 0; code <= -1_000_000 -> 2; else -> 1 }
    val demand = remember(code, ui.version) { if (mode == 1) FbTransfer.renewalDemand(world, p) else FbTransfer.demand(world, p, user) }
    var wage by remember(code) { mutableFloatStateOf(demand.wage.toFloat()) }
    var years by remember(code) { mutableIntStateOf(demand.minYears.coerceAtLeast(1)) }
    var clause by remember(code) { mutableIntStateOf(0) }
    var reply by remember(code) { mutableStateOf<ContractReply?>(null) }

    Dialog(onDismissRequest = { vm.closeNegotiation() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.width(520.dp).clip(RoundedCornerShape(18.dp)).background(AppColors.Background).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.fb_contract_talks, p.fullName), color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
            if (offer != null) Text(stringResource(R.string.fb_fee_agreed, FbFormat.money(offer.fee)), color = AppColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            FbKeyValue(stringResource(R.string.fb_wage_demand), FbFormat.wage(demand.wage) + stringResource(R.string.fb_per_week), FbColors.Yellow)
            FbKeyValue(stringResource(R.string.fb_years_wanted), "${demand.minYears} - ${demand.maxYears}")
            if (demand.signingFee > 0) FbKeyValue(stringResource(R.string.fb_signing_fee), FbFormat.money(demand.signingFee))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.fb_offered_wage, FbFormat.wage(wage.roundToInt())), color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Slider(value = wage, onValueChange = { wage = it }, valueRange = (demand.wage * 0.5f)..(demand.wage * 1.6f))
            Text(stringResource(R.string.fb_contract_years), color = AppColors.TextSecondary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                for (y in 1..5) FbChip("$y", years == y, { years = y })
            }
            Text(stringResource(R.string.fb_release_clause), color = AppColors.TextSecondary, fontSize = 13.sp)
            val v = FbTransfer.value(world, p)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                FbChip(stringResource(R.string.fb_none), clause == 0, { clause = 0 })
                for (k in listOf(2, 3, 5)) {
                    val c = (v * k).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    FbChip(FbFormat.money(c.toLong()), clause == c, { clause = c })
                }
            }
            reply?.let { r ->
                Text(stringResource(when (r) {
                    ContractReply.ACCEPT -> R.string.fb_reply_accept; ContractReply.WAGE_LOW -> R.string.fb_reply_wage_low
                    ContractReply.YEARS -> R.string.fb_reply_years; ContractReply.NOT_INTERESTED -> R.string.fb_reply_not_interested
                    ContractReply.WALKED_AWAY -> R.string.fb_reply_walked
                }), color = if (r == ContractReply.ACCEPT) FbColors.Green else FbColors.Orange, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                val done = reply == ContractReply.ACCEPT || reply == ContractReply.WALKED_AWAY || reply == ContractReply.NOT_INTERESTED
                if (!done) FbButton(stringResource(R.string.fb_propose), {
                    val w = FbTransfer.roundWage(wage.toDouble())
                    vm.edit { world2 ->
                        reply = when (mode) {
                            0 -> FbTransfer.offerContract(world2, offer!!, w, years, clause)
                            1 -> FbTransfer.offerRenewal(world2, p, w, years, clause)
                            else -> FbTransfer.signFreeAgent(world2, user, p.id, w, years)
                        }
                    }
                }, color = FbColors.Green, textColor = Color.Black)
                FbButton(stringResource(if (done) R.string.fb_close else R.string.fb_cancel), { vm.closeNegotiation() }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            }
            if (offer != null && offer.status != OfferStatus.AGREED && reply == null) Text(stringResource(R.string.fb_err_generic), color = FbColors.Red, fontSize = 12.sp)
        }
    }
}
