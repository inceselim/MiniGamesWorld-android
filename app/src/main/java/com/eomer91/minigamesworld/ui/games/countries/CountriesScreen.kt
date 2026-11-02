package com.eomer91.minigamesworld.ui.games.countries

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val Good = Color(0xFF4ADE80)
private val Bad = Color(0xFFF87171)
private const val GEM = "💎"

/** 12345 -> 12.3K */
private fun short(v: Int): String = when {
    v >= 1_000_000 -> String.format(Locale.US, "%.1fM", v / 1_000_000f)
    v >= 10_000 -> String.format(Locale.US, "%.1fK", v / 1000f)
    else -> v.toString()
}

private fun signed(v: Int) = (if (v >= 0) "+" else "") + short(v)

private fun costText(r: Res): String = buildList {
    if (r.gold > 0) add("${short(r.gold)}💰")
    if (r.wood > 0) add("${short(r.wood)}🪵")
    if (r.clay > 0) add("${short(r.clay)}🧱")
    if (r.iron > 0) add("${short(r.iron)}⛏")
    if (r.gems > 0) add("${r.gems}$GEM")
}.joinToString(" ").ifEmpty { "–" }

@Composable
fun CountriesScreen(viewModel: CountriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    val noticeText = state.notice?.let {
        when (it) {
            CwNotice.NOT_ENOUGH_RESOURCES -> stringResource(R.string.cw_n_resources)
            CwNotice.NO_BARRACKS_SPACE -> stringResource(R.string.cw_n_barracks)
            CwNotice.NOT_ENOUGH_POPULATION -> stringResource(R.string.cw_n_population)
            CwNotice.MAX_LEVEL -> stringResource(R.string.cw_n_max)
            CwNotice.PEACE_TIME -> stringResource(R.string.cw_n_peace, CountriesEngine.PEACE_TURNS)
            CwNotice.COOLDOWN -> stringResource(R.string.cw_n_cooldown)
            CwNotice.NO_UNITS -> stringResource(R.string.cw_n_units)
            CwNotice.GEMS_ADDED -> stringResource(R.string.cw_n_gems, CountriesEngine.AD_GEMS)
        }
    }
    LaunchedEffect(state.notice) {
        if (noticeText != null) {
            viewModel.consumeNotice()
            snackbar.showSnackbar(noticeText)
        }
    }

    // Tur sayaci: sadece on planda ilerler.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (isActive) {
                withFrameNanos { now ->
                    if (last != 0L) viewModel.onFrame((now - last) / 1_000_000_000f)
                    last = now
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        when (state.phase) {
            CwPhase.LOADING -> Unit
            CwPhase.SETUP -> SetupContent(state, viewModel)
            CwPhase.PLAYING -> state.game?.let { g ->
                PlayingContent(
                    state = state,
                    g = g,
                    viewModel = viewModel,
                    onRestart = { showInterstitial { viewModel.restart() } },
                    onAdGems = { showRewarded({ viewModel.adGems() }, adNotReady) },
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    state.report?.let { r -> BattleDialog(r, playerName = state.game?.player?.name ?: "", onDismiss = viewModel::dismissReport) }
}

// ============================================================ Kurulum

@Composable
private fun SetupContent(state: CwUiState, viewModel: CountriesViewModel) {
    val defaultName = stringResource(R.string.cw_default_name)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.cw_setup_title), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text(stringResource(R.string.cw_setup_desc), color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

        SectionTitle(stringResource(R.string.cw_difficulty))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CwDifficulty.entries.forEach { d ->
                val sel = d == state.setupDifficulty
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) AppColors.Primary else AppColors.SurfaceHigh)
                        .clickable { viewModel.setDifficulty(d) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(d.nameRes), color = if (sel) AppColors.OnPrimary else AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text(stringResource(R.string.cw_bots, d.bots), color = if (sel) AppColors.OnPrimary else AppColors.TextSecondary, fontSize = 11.sp)
                }
            }
        }

        SectionTitle(stringResource(R.string.cw_country_name))
        OutlinedTextField(
            value = state.setupName,
            onValueChange = viewModel::setName,
            singleLine = true,
            placeholder = { Text(defaultName) },
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle(stringResource(R.string.cw_polity))
        Polity.entries.filter { it != Polity.PREMIUM }.forEach { p ->
            PolityRow(p, selected = p == state.setupPolity, onClick = { viewModel.setSetupPolity(p) })
        }

        Button(
            onClick = { viewModel.startGame(defaultName) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(54.dp),
        ) { Text(stringResource(R.string.cw_start), fontWeight = FontWeight.Black, fontSize = 17.sp) }
    }
}

@Composable
private fun polityEffects(p: Polity): String {
    // stringResource sadece @Composable icinde cagrilabilir: etiketleri once cozumle.
    val labels = listOf(
        p.attack to stringResource(R.string.cw_attack),
        p.defence to stringResource(R.string.cw_defence),
        p.income to stringResource(R.string.cw_income),
        p.population to stringResource(R.string.cw_population),
        p.buildPrice to stringResource(R.string.cw_build_discount),
    )
    return labels.filter { it.first != 0f }.joinToString(" · ") { (v, label) ->
        (if (v > 0) "+" else "") + (v * 100).roundToInt() + "% " + label
    }
}

@Composable
private fun PolityRow(p: Polity, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceHigh)
            .border(2.dp, if (selected) AppColors.Primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(stringResource(p.nameRes), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        Text(polityEffects(p), color = AppColors.TextSecondary, fontSize = 12.sp)
    }
}

// ============================================================ Oyun

@Composable
private fun PlayingContent(state: CwUiState, g: CwGame, viewModel: CountriesViewModel, onRestart: () -> Unit, onAdGems: () -> Unit) {
    val p = g.player
    val engine = viewModel.engine
    var confirmRestart by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
        // Baslik
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(
                    stringResource(R.string.cw_turn, g.turn) + "  ·  " +
                        if (g.peace) stringResource(R.string.cw_peace_left, CountriesEngine.PEACE_TURNS - g.turn + 1)
                        else stringResource(R.string.cw_enemies_left, g.bots.count { it.alive }),
                    color = if (g.peace) Good else AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!g.over) {
                IconButton(onClick = viewModel::togglePause) {
                    Icon(if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = AppColors.Primary, modifier = Modifier.size(30.dp))
                }
            }
            IconButton(onClick = { if (g.over) onRestart() else confirmRestart = true }) {
                Icon(Icons.Rounded.Refresh, null, tint = AppColors.TextSecondary)
            }
        }
        if (!g.over) {
            LinearProgressIndicator(
                progress = { state.turnProgress },
                color = if (state.paused) AppColors.TextSecondary else AppColors.Primary,
                trackColor = AppColors.SurfaceHigh,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
            )
        }

        // Kaynaklar
        val inc = engine.turnIncome(p)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ResBox("💰", p.gold, inc.gold)
            ResBox("🪵", p.wood, inc.wood)
            ResBox("🧱", p.clay, inc.clay)
            ResBox("⛏", p.iron, inc.iron)
            ResBox(GEM, p.gems, null)
            ResBox("👥", p.population, engine.nextPopulation(p) - p.population)
        }

        // Sekmeler
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CwTab.entries.forEach { t ->
                val sel = t == state.tab
                Text(
                    stringResource(
                        when (t) {
                            CwTab.OVERVIEW -> R.string.cw_tab_country
                            CwTab.BUILD -> R.string.cw_tab_build
                            CwTab.ARMY -> R.string.cw_tab_army
                            CwTab.WAR -> R.string.cw_tab_war
                            CwTab.MARKET -> R.string.cw_tab_market
                        },
                    ),
                    color = if (sel) AppColors.OnPrimary else AppColors.TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) AppColors.Primary else AppColors.SurfaceHigh)
                        .clickable { viewModel.setTab(t) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Column(Modifier.weight(1f).padding(top = 8.dp).verticalScroll(rememberScrollState())) {
            if (g.over) {
                GameOverPanel(
                    score = g.score,
                    isNewRecord = state.isNewRecord,
                    onRestart = onRestart,
                    title = stringResource(if (g.victory == true) R.string.cw_victory else R.string.cw_defeat),
                    titleColor = if (g.victory == true) Good else Bad,
                    detail = stringResource(R.string.cw_result, g.turn, g.defeatedBots, g.bots.size),
                    goldEarned = state.goldEarned,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            when (state.tab) {
                CwTab.OVERVIEW -> OverviewTab(g, engine, viewModel, onAdGems)
                CwTab.BUILD -> BuildTab(p, engine, viewModel)
                CwTab.ARMY -> ArmyTab(p, engine, viewModel)
                CwTab.WAR -> WarTab(state, g, engine, viewModel)
                CwTab.MARKET -> MarketTab(p, viewModel)
            }
            Spacer(Modifier.height(60.dp))
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text(stringResource(R.string.cw_new_game_q)) },
            text = { Text(stringResource(R.string.cw_new_game_desc)) },
            confirmButton = {
                TextButton(onClick = { confirmRestart = false; onRestart() }) { Text(stringResource(R.string.cw_yes)) }
            },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text(stringResource(R.string.cw_no)) } },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ResBox(icon: String, value: Int, delta: Int?) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceHigh)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 13.sp)
        Text(short(value), color = AppColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
        if (delta != null) {
            Text(signed(delta), color = if (delta >= 0) Good else Bad, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceHigh)
            .padding(12.dp),
        content = content,
    )
}

@Composable
private fun SmallButton(text: String, enabled: Boolean = true, color: Color = AppColors.Primary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        color = if (enabled) AppColors.OnPrimary else AppColors.TextSecondary,
        fontWeight = FontWeight.Black,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) color else AppColors.Surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ------------------------------------------------------------ Ulke sekmesi

@Composable
private fun OverviewTab(g: CwGame, engine: CountriesEngine, viewModel: CountriesViewModel, onAdGems: () -> Unit) {
    val p = g.player
    Card {
        StatLine(stringResource(R.string.cw_attack_power), short(engine.attackPower(p)))
        StatLine(stringResource(R.string.cw_defence_power), short(engine.defencePower(p)))
        StatLine(stringResource(R.string.cw_population), "${short(p.population)} / ${short(p.farmCapacity)}")
        StatLine(stringResource(R.string.cw_parliament_eff), "${(engine.parliamentEfficiency(p) * 100).roundToInt()}%")
        StatLine(stringResource(R.string.cw_barracks_use), "${p.soldiersPop} / ${p.barracksCapacity}")
    }

    if (!g.over) {
        SectionTitle(stringResource(R.string.cw_government))
        Card {
            Text(stringResource(R.string.cw_tax, p.taxRate), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.cw_tax_desc), color = AppColors.TextSecondary, fontSize = 12.sp)
            Slider(
                value = p.taxRate.toFloat(),
                onValueChange = { viewModel.setTax((it / 5f).roundToInt() * 5) },
                valueRange = CountriesEngine.MIN_TAX.toFloat()..CountriesEngine.MAX_TAX.toFloat(),
                steps = (CountriesEngine.MAX_TAX - CountriesEngine.MIN_TAX) / 5 - 1,
            )
        }
        Card {
            Text(stringResource(R.string.cw_focus), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.cw_focus_desc), color = AppColors.TextSecondary, fontSize = 12.sp)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Focus.entries.forEach { f ->
                    SmallButton(
                        stringResource(f.nameRes),
                        color = if (p.foc == f) AppColors.Primary else AppColors.Background,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.setFocus(f) }
                }
            }
        }
        Card {
            Text(stringResource(R.string.cw_polity_now, stringResource(p.pol.nameRes)), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text(polityEffects(p.pol), color = AppColors.TextSecondary, fontSize = 12.sp)
            if (p.premiumTurns > 0) {
                Text(stringResource(R.string.cw_premium_left, p.premiumTurns), color = AppColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            val wait = CountriesEngine.POLITY_COOLDOWN - (g.turn - p.polityChangedTurn)
            Text(
                if (wait > 0) stringResource(R.string.cw_polity_wait, wait) else stringResource(R.string.cw_polity_change, CountriesEngine.POLITY_CHANGE_GOLD),
                color = AppColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )
            Polity.entries.filter { it != Polity.PREMIUM && it != p.pol }.forEach { pol ->
                PolityRow(pol, selected = false, onClick = { viewModel.setPolity(pol) }, enabled = wait <= 0 && p.premiumTurns == 0)
            }
            Button(
                onClick = viewModel::activatePremium,
                enabled = p.premiumTurns == 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.cw_premium, CountriesEngine.PREMIUM_GEMS, CountriesEngine.PREMIUM_TURNS), fontWeight = FontWeight.Black)
            }
        }
        Button(
            onClick = onAdGems,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceHigh, contentColor = AppColors.TextPrimary),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(18.dp), tint = AppColors.Gold)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.cw_ad_gems, CountriesEngine.AD_GEMS), fontWeight = FontWeight.Bold)
        }
    }

    SectionTitle(stringResource(R.string.cw_log))
    if (g.log.isEmpty()) Text(stringResource(R.string.cw_log_empty), color = AppColors.TextSecondary, fontSize = 13.sp)
    g.log.forEach { LogRow(it) }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = AppColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LogRow(e: LogEntry) {
    val (text, color) = when (e.logType) {
        LogType.BOT_ATTACK_WON -> stringResource(R.string.cw_log_bot_won, e.who) to Bad
        LogType.BOT_ATTACK_LOST -> stringResource(R.string.cw_log_bot_lost, e.who) to Good
        LogType.YOU_WON -> stringResource(R.string.cw_log_you_won, e.who) to Good
        LogType.YOU_LOST -> stringResource(R.string.cw_log_you_lost, e.who) to Bad
        LogType.SHORTAGE -> stringResource(R.string.cw_log_shortage, e.a) to Bad
        LogType.PREMIUM_ENDED -> stringResource(R.string.cw_log_premium_end) to AppColors.Gold
        LogType.PEACE_OVER -> stringResource(R.string.cw_log_peace_over) to AppColors.Gold
        LogType.BOT_BUILT_CASTLE -> e.who to AppColors.TextSecondary
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(stringResource(R.string.cw_turn, e.turn), color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(56.dp))
        Text(text, color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

// ------------------------------------------------------------ Insaat

@Composable
private fun BuildTab(p: Country, engine: CountriesEngine, viewModel: CountriesViewModel) {
    Building.entries.forEach { b ->
        val level = p.level(b)
        val cost = engine.buildCost(p, b)
        val maxed = level >= b.maxLevel
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.icon, fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(b.nameRes) + "  ·  " + stringResource(R.string.cw_level, level), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                    Text(stringResource(b.descRes), color = AppColors.TextSecondary, fontSize = 12.sp)
                }
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cw_cost, costText(cost)), color = AppColors.TextMuted, fontSize = 12.sp)
                    if (b.upkeep != Res()) Text(stringResource(R.string.cw_upkeep, costText(b.upkeep)), color = AppColors.TextSecondary, fontSize = 11.sp)
                }
                SmallButton(
                    if (maxed) stringResource(R.string.cw_max) else stringResource(R.string.cw_build),
                    enabled = !maxed && p.has(cost),
                ) { viewModel.build(b) }
            }
        }
    }
}

// ------------------------------------------------------------ Ordu

@Composable
private fun ArmyTab(p: Country, engine: CountriesEngine, viewModel: CountriesViewModel) {
    Text(
        stringResource(R.string.cw_barracks_info, p.soldiersPop, p.barracksCapacity),
        color = AppColors.TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    UnitType.entries.forEach { u ->
        val max = engine.maxRecruit(p, u)
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(u.icon, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(u.nameRes) + "  ×" + p.count(u), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.cw_unit_stats, u.attack, u.defence, u.pop), color = AppColors.TextSecondary, fontSize = 12.sp)
                }
            }
            Text(
                stringResource(R.string.cw_cost, costText(u.cost)) + "  ·  " + stringResource(R.string.cw_upkeep, costText(u.upkeep)),
                color = AppColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallButton("+1", enabled = max >= 1, modifier = Modifier.weight(1f)) { viewModel.recruit(u, 1) }
                SmallButton("+10", enabled = max >= 10, modifier = Modifier.weight(1f)) { viewModel.recruit(u, 10) }
                SmallButton(stringResource(R.string.cw_max_n, max), enabled = max >= 1, modifier = Modifier.weight(1.4f)) { viewModel.recruitMax(u) }
            }
        }
    }
}

// ------------------------------------------------------------ Savas

@Composable
private fun WarTab(state: CwUiState, g: CwGame, engine: CountriesEngine, viewModel: CountriesViewModel) {
    val p = g.player
    val spy = p.level(Building.ESPIONAGE) > 0
    if (g.peace) {
        Text(stringResource(R.string.cw_n_peace, CountriesEngine.PEACE_TURNS), color = Good, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
    }
    if (!spy) {
        Text(stringResource(R.string.cw_no_spy), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
    }
    SectionTitle(stringResource(R.string.cw_enemies))
    g.bots.forEach { b ->
        val sel = state.targetId == b.id
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceHigh)
                .border(2.dp, if (sel) Bad else Color.Transparent, RoundedCornerShape(12.dp))
                .clickable(enabled = b.alive && !g.over) { viewModel.selectTarget(b.id) }
                .padding(12.dp),
        ) {
            Text(
                b.name,
                color = if (b.alive) AppColors.TextPrimary else AppColors.TextSecondary,
                fontWeight = FontWeight.Black,
                textDecoration = if (b.alive) null else TextDecoration.LineThrough,
            )
            if (b.alive) {
                Text(
                    stringResource(b.pol.nameRes) + " · " + stringResource(R.string.cw_castles, b.level(Building.CASTLE)),
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    stringResource(R.string.cw_enemy_power, if (spy) short(engine.defencePower(b)) else "?", if (spy) short(engine.attackPower(b)) else "?"),
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                )
            } else {
                Text(stringResource(R.string.cw_defeated), color = Good, fontSize = 12.sp)
            }
        }
    }

    val target = g.bots.firstOrNull { it.id == state.targetId && it.alive }
    if (target != null && !g.over) {
        SectionTitle(stringResource(R.string.cw_send_army, target.name))
        UnitType.entries.forEach { u ->
            val have = p.count(u)
            val n = state.sent[u.name] ?: 0
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(u.icon + " " + stringResource(u.nameRes), color = AppColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                SmallButton("-10", enabled = n > 0, color = AppColors.Background) { viewModel.changeSent(u, -10) }
                Spacer(Modifier.width(4.dp))
                Text("$n/$have", color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
                Spacer(Modifier.width(4.dp))
                SmallButton("+10", enabled = n < have, color = AppColors.Background) { viewModel.changeSent(u, 10) }
            }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton(stringResource(R.string.cw_send_all), modifier = Modifier.weight(1f)) { viewModel.sendAll() }
            SmallButton(stringResource(R.string.cw_send_clear), color = AppColors.Background, modifier = Modifier.weight(1f)) { viewModel.clearSent() }
        }
        val power = engine.attackPower(p, state.sent)
        Text(
            stringResource(R.string.cw_attack_vs, short(power), if (spy) short(engine.defencePower(target)) else "?"),
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = viewModel::attack,
            enabled = power > 0 && !g.peace,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Bad, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp),
        ) { Text(stringResource(R.string.cw_attack_btn), fontWeight = FontWeight.Black, fontSize = 16.sp) }
    }
}

// ------------------------------------------------------------ Pazar

@Composable
private fun MarketTab(p: Country, viewModel: CountriesViewModel) {
    Text(
        stringResource(R.string.cw_market_desc, CountriesEngine.SELL_LOT, CountriesEngine.SELL_PRICE, CountriesEngine.BUY_LOT, CountriesEngine.BUY_LOT),
        color = AppColors.TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    listOf(Resource.WOOD to p.wood, Resource.CLAY to p.clay, Resource.IRON to p.iron).forEach { (r, have) ->
        Card {
            Text(r.icon + " " + stringResource(r.nameRes) + ": " + short(have), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cw_sell), color = Bad, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(40.dp))
                listOf(1, 10, 100).forEach { lots ->
                    SmallButton(
                        short(CountriesEngine.SELL_LOT * lots),
                        enabled = have >= CountriesEngine.SELL_LOT * lots,
                        color = Bad,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.trade(r, lots, selling = true) }
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cw_buy), color = Good, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(40.dp))
                listOf(1, 10, 100).forEach { lots ->
                    SmallButton(
                        short(CountriesEngine.BUY_LOT * lots),
                        enabled = p.gold >= CountriesEngine.BUY_LOT * lots,
                        color = Good,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.trade(r, lots, selling = false) }
                }
            }
        }
    }
}

// ------------------------------------------------------------ Savas raporu

@Composable
private fun BattleDialog(r: BattleReport, playerName: String, onDismiss: () -> Unit) {
    val playerAttacked = r.attacker == playerName
    val good = if (playerAttacked) r.won else !r.won
    fun losses(m: Map<String, Int>): String =
        if (m.isEmpty()) "–" else m.entries.joinToString(", ") { (u, n) -> UnitType.valueOf(u).icon + " " + n }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when {
                        playerAttacked && r.won -> R.string.cw_rep_you_won
                        playerAttacked -> R.string.cw_rep_you_lost
                        r.won -> R.string.cw_rep_enemy_won
                        else -> R.string.cw_rep_defended
                    },
                    if (playerAttacked) r.defender else r.attacker,
                ),
                color = if (good) Good else Bad,
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.cw_rep_powers, short(r.attackPower), short(r.defencePower)))
                Text(stringResource(R.string.cw_rep_att_losses, r.attacker, losses(r.attackerLosses)), modifier = Modifier.padding(top = 6.dp))
                Text(stringResource(R.string.cw_rep_def_losses, r.defender, losses(r.defenderLosses)), modifier = Modifier.padding(top = 4.dp))
                if (r.won) Text(stringResource(R.string.cw_rep_loot, short(r.goldTaken), short(r.popTaken)), modifier = Modifier.padding(top = 6.dp), fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cw_ok)) } },
    )
}
