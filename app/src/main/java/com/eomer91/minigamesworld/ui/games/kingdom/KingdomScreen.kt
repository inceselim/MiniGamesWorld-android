package com.eomer91.minigamesworld.ui.games.kingdom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val Green = Color(0xFF22C55E)
private val Royal = Color(0xFF7C3AED)
private val Crimson = Color(0xFFDC2626)

private fun num(d: Double): String = when {
    d >= 1e6 -> String.format(Locale.US, "%.2fM", d / 1e6)
    d >= 1e4 -> String.format(Locale.US, "%.1fK", d / 1e3)
    else -> d.toLong().toString()
}

private fun num(i: Int) = num(i.toDouble())

private fun formatDuration(s: Long): String = if (s >= 3600) "${s / 3600}h ${(s % 3600) / 60}m" else "${s / 60}m"

fun resName(r: Res): Int = when (r) {
    Res.GOLD -> R.string.ik_res_gold
    Res.WOOD -> R.string.ik_res_wood
    Res.STONE -> R.string.ik_res_stone
    Res.FOOD -> R.string.ik_res_food
}

fun buildingName(b: KBuilding): Int = when (b) {
    KBuilding.CASTLE -> R.string.ik_b_castle
    KBuilding.GOLD_MINE -> R.string.ik_b_gold_mine
    KBuilding.LUMBER_CAMP -> R.string.ik_b_lumber
    KBuilding.QUARRY -> R.string.ik_b_quarry
    KBuilding.FARM -> R.string.ik_b_farm
    KBuilding.WAREHOUSE -> R.string.ik_b_warehouse
    KBuilding.BARRACKS -> R.string.ik_b_barracks
    KBuilding.ARCHERY -> R.string.ik_b_archery
    KBuilding.STABLE -> R.string.ik_b_stable
}

private fun buildingDesc(b: KBuilding): Int = when (b) {
    KBuilding.CASTLE -> R.string.ik_b_castle_desc
    KBuilding.GOLD_MINE, KBuilding.LUMBER_CAMP, KBuilding.QUARRY, KBuilding.FARM -> R.string.ik_b_producer_desc
    KBuilding.WAREHOUSE -> R.string.ik_b_warehouse_desc
    KBuilding.BARRACKS -> R.string.ik_b_barracks_desc
    KBuilding.ARCHERY -> R.string.ik_b_archery_desc
    KBuilding.STABLE -> R.string.ik_b_stable_desc
}

fun unitName(u: UnitType): Int = when (u) {
    UnitType.SWORDSMAN -> R.string.ik_u_swordsman
    UnitType.SPEARMAN -> R.string.ik_u_spearman
    UnitType.ARCHER -> R.string.ik_u_archer
    UnitType.CAVALRY -> R.string.ik_u_cavalry
}

/** Bu birimin ustun oldugu birim. */
private fun strongVs(u: UnitType): UnitType = when (u) {
    UnitType.SWORDSMAN -> UnitType.SPEARMAN
    UnitType.SPEARMAN -> UnitType.CAVALRY
    UnitType.ARCHER -> UnitType.SWORDSMAN
    UnitType.CAVALRY -> UnitType.ARCHER
}

fun heroName(h: Hero): Int = when (h) {
    Hero.ARTHUR -> R.string.ik_h_arthur
    Hero.ROBIN -> R.string.ik_h_robin
    Hero.LEONIDAS -> R.string.ik_h_leonidas
}

private fun heroDesc(h: Hero): Int = when (h) {
    Hero.ARTHUR -> R.string.ik_h_arthur_desc
    Hero.ROBIN -> R.string.ik_h_robin_desc
    Hero.LEONIDAS -> R.string.ik_h_leonidas_desc
}

private fun techName(t: Tech): Int = when (t) {
    Tech.ECON_GOLD -> R.string.ik_t_gold
    Tech.ECON_WOOD -> R.string.ik_t_wood
    Tech.ECON_STONE -> R.string.ik_t_stone
    Tech.ECON_FOOD -> R.string.ik_t_food
    Tech.MIL_INFANTRY -> R.string.ik_t_infantry
    Tech.MIL_ARCHERS -> R.string.ik_t_archers
    Tech.MIL_CAVALRY -> R.string.ik_t_cavalry
    Tech.DEF_ARMOR -> R.string.ik_t_armor
    Tech.LOG_CAPACITY -> R.string.ik_t_capacity
    Tech.LOG_OFFLINE -> R.string.ik_t_offline
}

private fun techDesc(t: Tech): Int = when (t) {
    Tech.ECON_GOLD, Tech.ECON_WOOD, Tech.ECON_STONE, Tech.ECON_FOOD -> R.string.ik_t_econ_desc
    Tech.MIL_INFANTRY, Tech.MIL_ARCHERS, Tech.MIL_CAVALRY -> R.string.ik_t_mil_desc
    Tech.DEF_ARMOR -> R.string.ik_t_armor_desc
    Tech.LOG_CAPACITY -> R.string.ik_t_capacity_desc
    Tech.LOG_OFFLINE -> R.string.ik_t_offline_desc
}

private val REGION_NAMES = intArrayOf(
    R.string.ik_r_0, R.string.ik_r_1, R.string.ik_r_2, R.string.ik_r_3, R.string.ik_r_4,
    R.string.ik_r_5, R.string.ik_r_6, R.string.ik_r_7, R.string.ik_r_8, R.string.ik_r_9,
    R.string.ik_r_10, R.string.ik_r_11, R.string.ik_r_12, R.string.ik_r_13, R.string.ik_r_14,
)

fun regionName(r: Region) = REGION_NAMES[r.index]

@Composable
fun KingdomScreen(viewModel: KingdomViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_PAUSE -> viewModel.save()
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose {
            owner.lifecycle.removeObserver(obs)
            viewModel.save()
        }
    }
    LaunchedEffect(Unit) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(TICK_MS)
                viewModel.tick(TICK_MS / 1000.0)
            }
        }
    }
    val questMsg = stringResource(R.string.ik_quest_reward)
    LaunchedEffect(state.toast) {
        val t = state.toast ?: return@LaunchedEffect
        snackbar.showSnackbar(questMsg + "  " + t.entries.joinToString("  ") { "${it.key.emoji} +${num(it.value)}" })
        viewModel.clearToast()
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        if (!state.loading) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Header(viewModel)
                Tabs(state.tab, viewModel)
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
                    when (state.tab) {
                        KingdomTab.KINGDOM -> Buildings(viewModel)
                        KingdomTab.ARMY -> Army(viewModel)
                        KingdomTab.MAP -> WorldMap(viewModel)
                        KingdomTab.RESEARCH -> Research(viewModel)
                        KingdomTab.QUESTS -> Quests(viewModel)
                    }
                }
            }
        }
        if (state.offline.isNotEmpty()) {
            Overlay {
                Text("🏰", fontSize = 44.sp)
                Text(stringResource(R.string.ik_offline_title), color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.ik_offline_body, formatDuration(state.offlineSeconds), viewModel.engine.offlineCapHours),
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                ResourceLine(state.offline.mapValues { it.value.toInt() }, Modifier.padding(top = 10.dp))
                Button(
                    onClick = {
                        showRewarded({ viewModel.closeOffline(doubled = true) }) {
                            scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ik_double), fontWeight = FontWeight.Black)
                }
                TextButton(onClick = { viewModel.closeOffline(doubled = false) }) {
                    Text(stringResource(R.string.ik_collect), color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
        state.report?.let { BattleReportView(it, viewModel) }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private const val TICK_MS = 250L

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).clickable { }, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(20.dp).clip(RoundedCornerShape(22.dp)).background(AppColors.Surface).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Composable
private fun ResourceLine(amounts: Map<Res, Int>, modifier: Modifier = Modifier, color: Color = AppColors.TextPrimary) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (r in Res.entries) {
            val v = amounts[r] ?: continue
            if (v <= 0) continue
            Text("${r.emoji} ${num(v)}", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/** Maliyet satiri: yetmeyen kaynak kirmizi. */
@Composable
private fun CostLine(cost: Map<Res, Int>, e: KingdomEngine) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((r, v) in cost) {
            val enough = (e.res[r] ?: 0.0) >= v
            Text("${r.emoji}${num(v)}", color = if (enough) AppColors.TextMuted else AppColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Header(vm: KingdomViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    val ageName = when (e.age) {
        3 -> R.string.ik_age_3
        2 -> R.string.ik_age_2
        else -> R.string.ik_age_1
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF312E81), Color(0xFF4C1D95))))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(KBuilding.CASTLE.emoji, fontSize = 26.sp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(stringResource(ageName), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    stringResource(R.string.ik_castle_level, e.castleLevel) + " · " + stringResource(R.string.ik_army_size, e.armySize(), e.armyCapacity()),
                    color = Color(0xFFDDD6FE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("📦 " + num(e.capacity()), color = Color(0xFFDDD6FE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            for (r in Res.entries) {
                val full = (e.res[r] ?: 0.0) >= e.capacity()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${r.emoji} ${num(e.res[r] ?: 0.0)}", color = if (full) AppColors.Gold else Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("+" + num(e.perMinute(r)) + "/" + stringResource(R.string.ik_min), color = Color(0xFFC4B5FD), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun Tabs(current: KingdomTab, vm: KingdomViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val questReady = vm.engine.questDone()
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (t in KingdomTab.entries) {
            val (emoji, label) = when (t) {
                KingdomTab.KINGDOM -> "🏰" to R.string.ik_tab_kingdom
                KingdomTab.ARMY -> "⚔️" to R.string.ik_tab_army
                KingdomTab.MAP -> "🗺️" to R.string.ik_tab_map
                KingdomTab.RESEARCH -> "📜" to R.string.ik_tab_research
                KingdomTab.QUESTS -> "🎯" to R.string.ik_tab_quests
            }
            val sel = t == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) Royal else AppColors.SurfaceHigh)
                    .clickable { vm.selectTab(t) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(emoji + if (t == KingdomTab.QUESTS && questReady) "❗" else "", fontSize = 16.sp)
                Text(stringResource(label), color = if (sel) Color.White else AppColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, highlight: Boolean = false, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceHigh)
            .then(if (highlight) Modifier.border(2.dp, AppColors.Gold, RoundedCornerShape(14.dp)) else Modifier)
            .padding(10.dp),
    ) { content() }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, color: Color = Green, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp),
    ) { Text(text, fontWeight = FontWeight.Black, fontSize = 13.sp) }
}

// ---------------- Binalar ----------------

@Composable
private fun Buildings(vm: KingdomViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(KBuilding.entries) { b ->
            val lv = e.level(b)
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.emoji, fontSize = 28.sp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(buildingName(b)), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                            Text(
                                "  " + if (lv == 0) "—" else stringResource(R.string.ik_level, lv),
                                color = AppColors.Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                        val desc = if (b.produces != null) {
                            stringResource(R.string.ik_b_producer_desc, stringResource(resName(b.produces)))
                        } else {
                            stringResource(buildingDesc(b))
                        }
                        Text(desc, color = AppColors.TextSecondary, fontSize = 11.sp)
                        if (b.produces != null && lv > 0) {
                            Text("+" + num(e.perMinute(b.produces)) + " ${b.produces.emoji}/" + stringResource(R.string.ik_min), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        when {
                            !e.isUnlocked(b) -> Text(
                                stringResource(R.string.ik_needs_castle, if (b == KBuilding.ARCHERY) 2 else 4),
                                color = AppColors.Danger,
                                fontSize = 11.sp,
                            )
                            lv >= KingdomEngine.MAX_LEVEL -> {}
                            lv >= e.maxLevel(b) -> Text(stringResource(R.string.ik_needs_castle, lv), color = AppColors.TextSecondary, fontSize = 11.sp)
                            else -> CostLine(e.upgradeCost(b), e)
                        }
                    }
                    if (lv >= KingdomEngine.MAX_LEVEL) {
                        Text(stringResource(R.string.ik_max), color = AppColors.Gold, fontWeight = FontWeight.Black)
                    } else {
                        ActionButton(
                            stringResource(if (lv == 0) R.string.ik_build else R.string.ik_upgrade),
                            enabled = e.canUpgrade(b),
                        ) { vm.upgrade(b) }
                    }
                }
            }
        }
    }
}

// ---------------- Ordu ----------------

@Composable
private fun Army(vm: KingdomViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text(stringResource(R.string.ik_army_size, e.armySize(), e.armyCapacity()), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            LinearProgressIndicator(
                progress = { (e.armySize().toFloat() / e.armyCapacity()).coerceIn(0f, 1f) },
                color = Royal,
                trackColor = AppColors.SurfaceHigh,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
        }
        items(UnitType.entries) { u ->
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(u.emoji, fontSize = 28.sp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(unitName(u)), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                            Text("  ×" + (e.army[u] ?: 0), color = AppColors.Gold, fontWeight = FontWeight.Black)
                        }
                        Text(
                            "⚔${u.atk.toInt()} 🛡${u.def.toInt()} ❤${u.hp.toInt()} · " + stringResource(R.string.ik_strong_vs, stringResource(unitName(strongVs(u)))),
                            color = AppColors.TextSecondary,
                            fontSize = 11.sp,
                        )
                        if (e.canTrainType(u)) {
                            CostLine(e.unitCost(u), e)
                        } else {
                            val need = if (u == UnitType.SPEARMAN && e.level(KBuilding.BARRACKS) >= 1) {
                                stringResource(R.string.ik_needs_building_level, stringResource(buildingName(KBuilding.BARRACKS)), 2)
                            } else {
                                stringResource(R.string.ik_needs_building, stringResource(buildingName(e.trainBuilding(u))))
                            }
                            Text(need, color = AppColors.Danger, fontSize = 11.sp)
                        }
                    }
                }
                if (e.canTrainType(u)) {
                    val max = e.maxTrainable(u)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                        ActionButton("+1", enabled = max >= 1) { vm.train(u, 1) }
                        ActionButton("+10", enabled = max >= 10) { vm.train(u, 10) }
                        ActionButton(stringResource(R.string.ik_train_max, max), enabled = max >= 1, color = Royal) { vm.train(u, max) }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.ik_heroes), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        }
        items(Hero.entries) { h -> HeroCard(h, vm) }
    }
}

@Composable
private fun HeroCard(h: Hero, vm: KingdomViewModel) {
    val e = vm.engine
    val owned = h in e.heroes
    val active = owned && e.activeHero == h
    Card(highlight = active) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (owned) h.emoji else "🔒", fontSize = 28.sp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(heroName(h)), color = if (owned) AppColors.TextPrimary else AppColors.TextSecondary, fontWeight = FontWeight.Black)
                    if (owned) Text("  " + stringResource(R.string.ik_level, e.heroLevel(h)), color = AppColors.Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(stringResource(heroDesc(h), e.heroBonusPct(h)), color = AppColors.TextSecondary, fontSize = 11.sp)
                if (!owned) {
                    KingdomEngine.REGIONS.firstOrNull { it.unlocksHero == h }?.let { region ->
                        Text(stringResource(R.string.ik_hero_locked, stringResource(regionName(region))), color = AppColors.Danger, fontSize = 11.sp)
                    }
                } else if (e.heroLevel(h) < KingdomEngine.HERO_MAX) {
                    CostLine(e.heroUpgradeCost(h), e)
                }
            }
        }
        if (owned) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                if (active) {
                    Text(stringResource(R.string.ik_hero_active), color = AppColors.Gold, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 6.dp).align(Alignment.CenterVertically))
                } else {
                    ActionButton(stringResource(R.string.ik_hero_select), enabled = true, color = Royal) { vm.selectHero(h) }
                }
                if (e.heroLevel(h) < KingdomEngine.HERO_MAX) {
                    ActionButton(stringResource(R.string.ik_upgrade), enabled = e.canAfford(e.heroUpgradeCost(h))) { vm.upgradeHero(h) }
                }
            }
        }
    }
}

// ---------------- Harita ----------------

@Composable
private fun WorldMap(vm: KingdomViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val next = e.nextRegion
        if (next == null) {
            item {
                Card(highlight = true) {
                    Text("👑 " + stringResource(R.string.ik_all_conquered), color = AppColors.Gold, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        items(KingdomEngine.REGIONS) { r ->
            val done = r.index < e.conquered
            val isNext = r.index == e.conquered
            Card(highlight = isNext) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (done || isNext) r.emoji else "❔", fontSize = 28.sp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (r.index + 1).toString() + ". " + if (done || isNext) stringResource(regionName(r)) else "???",
                                color = if (done || isNext) AppColors.TextPrimary else AppColors.TextSecondary,
                                fontWeight = FontWeight.Black,
                            )
                            if (r.isBoss) Text("  " + stringResource(R.string.ik_boss), color = Crimson, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                        if (done) {
                            Text("✅ " + stringResource(R.string.ik_conquered), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (done || isNext) {
                            val minLabel = stringResource(R.string.ik_min)
                            val heroLabel = r.unlocksHero?.let { it.emoji + " " + stringResource(heroName(it)) }
                            val perks = buildList {
                                if (r.bonusRes != null) add("+${r.bonusPct}% " + r.bonusRes.emoji)
                                add("+" + num(r.taxPerMin) + " 🪙/" + minLabel)
                                if (heroLabel != null) add(heroLabel)
                            }
                            Text(perks.joinToString(" · "), color = AppColors.TextMuted, fontSize = 11.sp)
                        }
                        if (isNext) {
                            Text(
                                r.enemy.entries.joinToString("  ") { "${it.key.emoji}${it.value}" },
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp,
                            )
                            val mine = e.myPowerVs(r.enemy)
                            val theirs = e.enemyPower(r)
                            Text(
                                stringResource(R.string.ik_your_power, num(mine)) + "  ·  " + stringResource(R.string.ik_enemy_power, num(theirs)),
                                color = if (mine > theirs * 1.1) Green else if (mine > theirs * 0.9) AppColors.Gold else AppColors.Danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(stringResource(R.string.ik_reward) + ":", color = AppColors.TextSecondary, fontSize = 11.sp)
                            ResourceLine(r.reward, color = AppColors.TextMuted)
                        }
                    }
                    if (isNext) {
                        ActionButton(stringResource(R.string.ik_attack), enabled = e.armySize() > 0, color = Crimson) { vm.attack() }
                    }
                }
                if (isNext && e.armySize() == 0) {
                    Text(stringResource(R.string.ik_no_army), color = AppColors.Danger, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun BattleReportView(r: BattleReport, vm: KingdomViewModel) {
    Overlay {
        Text(if (r.won) "🏆" else "💀", fontSize = 44.sp)
        Text(
            stringResource(if (r.won) R.string.ik_victory else R.string.ik_defeat),
            color = if (r.won) AppColors.Gold else AppColors.Danger,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
        )
        Text(stringResource(regionName(r.region)), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.ik_your_power, num(r.myPower)) + "  ·  " + stringResource(R.string.ik_enemy_power, num(r.enemyPower)),
            color = AppColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        LossLine(stringResource(R.string.ik_losses_you), r.myLosses)
        LossLine(stringResource(R.string.ik_losses_enemy), r.enemyLosses)
        if (r.won) {
            Text(stringResource(R.string.ik_reward), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            ResourceLine(r.reward, color = AppColors.Gold)
            r.heroUnlocked?.let {
                Text(stringResource(R.string.ik_hero_unlocked, stringResource(heroName(it))), color = AppColors.Gold, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            Text(stringResource(R.string.ik_defeat_tip), color = AppColors.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            onClick = { vm.closeReport() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Royal, contentColor = Color.White),
            modifier = Modifier.padding(top = 14.dp),
        ) { Text(stringResource(R.string.ik_continue), fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun LossLine(label: String, losses: Map<UnitType, Int>) {
    Text(
        label + ": " + if (losses.isEmpty()) stringResource(R.string.ik_none) else losses.entries.joinToString("  ") { "${it.key.emoji}−${it.value}" },
        color = AppColors.TextMuted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

// ---------------- Arastirma ----------------

@Composable
private fun Research(vm: KingdomViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Tech.entries) { t ->
            val done = t in e.techs
            val unlocked = e.techUnlocked(t)
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unlocked || done) t.emoji else "🔒", fontSize = 26.sp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(stringResource(techName(t)), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                        Text(stringResource(techDesc(t)), color = AppColors.TextSecondary, fontSize = 11.sp)
                        when {
                            done -> Text("✅ " + stringResource(R.string.ik_researched), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            !unlocked -> Text(
                                stringResource(R.string.ik_needs_castle, when (t) {
                                    Tech.LOG_CAPACITY, Tech.LOG_OFFLINE -> 5
                                    Tech.MIL_INFANTRY, Tech.MIL_ARCHERS, Tech.MIL_CAVALRY, Tech.DEF_ARMOR -> 3
                                    else -> 2
                                }),
                                color = AppColors.Danger,
                                fontSize = 11.sp,
                            )
                            else -> CostLine(e.techCost(t), e)
                        }
                    }
                    if (!done && unlocked) {
                        ActionButton(stringResource(R.string.ik_research), enabled = e.canAfford(e.techCost(t))) { vm.research(t) }
                    }
                }
            }
        }
    }
}

// ---------------- Gorevler ----------------

@Composable
private fun Quests(vm: KingdomViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    val q = e.quest
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (q == null) {
            Card(highlight = true) {
                Text("🏆 " + stringResource(R.string.ik_quests_done), color = AppColors.Gold, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            return@Column
        }
        val progress = e.questProgress(q).coerceAtMost(q.target)
        val text = when (q.kind) {
            QuestKind.BUILD -> stringResource(R.string.ik_q_build, stringResource(buildingName(q.building!!)), q.target)
            QuestKind.TRAIN -> stringResource(R.string.ik_q_train, q.target)
            QuestKind.CONQUER -> stringResource(R.string.ik_q_conquer, q.target)
            QuestKind.RESEARCH -> stringResource(R.string.ik_q_research, q.target)
            QuestKind.HERO_LEVEL -> stringResource(R.string.ik_q_hero, q.target)
            QuestKind.WIN_BATTLES -> stringResource(R.string.ik_q_win, q.target)
        }
        Card(highlight = e.questDone()) {
            Text(stringResource(R.string.ik_quest_n, e.questIndex + 1, KingdomEngine.QUESTS.size), color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("🎯 $text", color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            LinearProgressIndicator(
                progress = { progress.toFloat() / q.target },
                color = Green,
                trackColor = AppColors.Surface,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("$progress / ${q.target}", color = AppColors.TextMuted, fontWeight = FontWeight.Bold)
                    ResourceLine(q.reward, color = AppColors.Gold)
                }
                ActionButton(stringResource(R.string.ik_claim), enabled = e.questDone()) { vm.claimQuest() }
            }
        }
        Card {
            Text("💡 " + stringResource(R.string.ik_tip), color = AppColors.TextSecondary, fontSize = 12.sp)
        }
    }
}
