package com.eomer91.minigamesworld.ui.games.kingdom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
private val Panel = Color(0xFF262B44)
private val PanelLight = Color(0xFF333C57)
private val Frame = Color(0xFF3A4466)

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
                Header(viewModel, showScene = state.tab == KingdomTab.KINGDOM)
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
                Sprite(buildingSprite(KBuilding.CASTLE, viewModel.engine.castleLevel), 72.dp)
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
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        for (r in Res.entries) {
            val v = amounts[r] ?: continue
            if (v <= 0) continue
            Row(verticalAlignment = Alignment.CenterVertically) {
                Sprite(resSprite(r), 18.dp)
                Text(" " + num(v), color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

/** Maliyet satiri: yetmeyen kaynak kirmizi. */
@Composable
private fun CostLine(cost: Map<Res, Int>, e: KingdomEngine) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        for ((r, v) in cost) {
            val enough = (e.res[r] ?: 0.0) >= v
            Row(verticalAlignment = Alignment.CenterVertically) {
                Sprite(resSprite(r), 14.dp)
                Text(num(v), color = if (enough) AppColors.TextMuted else AppColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Header(vm: KingdomViewModel, showScene: Boolean) {
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
            .border(2.dp, Frame, RoundedCornerShape(16.dp))
            .background(Panel),
    ) {
        if (showScene) Box {
            KingdomScene(e.levels)
            Row(
                Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC1A1C2C))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(ageName), color = AppColors.Gold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(
                    "  " + stringResource(R.string.ik_castle_level, e.castleLevel),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC1A1C2C))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Sprite(R.drawable.ik_unit_sword, 16.dp)
                Text(" ${e.armySize()}/${e.armyCapacity()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            for (r in Res.entries) {
                val full = (e.res[r] ?: 0.0) >= e.capacity()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sprite(resSprite(r), 22.dp)
                    Column(Modifier.padding(start = 3.dp)) {
                        Text(num(e.res[r] ?: 0.0), color = if (full) AppColors.Danger else Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("+" + num(e.perMinute(r)) + "/" + stringResource(R.string.ik_min), color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(
            stringResource(R.string.ik_storage, num(e.capacity())),
            color = AppColors.TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 10.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun Tabs(current: KingdomTab, vm: KingdomViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val questReady = vm.engine.questDone()
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (t in KingdomTab.entries) {
            val (icon, label) = when (t) {
                KingdomTab.KINGDOM -> R.drawable.ik_castle_1 to R.string.ik_tab_kingdom
                KingdomTab.ARMY -> R.drawable.ik_unit_sword to R.string.ik_tab_army
                KingdomTab.MAP -> R.drawable.ik_icon_map to R.string.ik_tab_map
                KingdomTab.RESEARCH -> R.drawable.ik_icon_book to R.string.ik_tab_research
                KingdomTab.QUESTS -> R.drawable.ik_res_gold to R.string.ik_tab_quests
            }
            val sel = t == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) Royal else Panel)
                    .border(2.dp, if (sel) AppColors.Gold else Frame, RoundedCornerShape(12.dp))
                    .clickable { vm.selectTab(t) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    Sprite(icon, 26.dp)
                    if (t == KingdomTab.QUESTS && questReady) {
                        Box(Modifier.align(Alignment.TopEnd).size(9.dp).clip(RoundedCornerShape(5.dp)).background(Crimson))
                    }
                }
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
            .background(Brush.verticalGradient(listOf(PanelLight, Panel)))
            .border(2.dp, if (highlight) AppColors.Gold else Frame, RoundedCornerShape(14.dp))
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
                    Sprite(buildingSprite(b, lv.coerceAtLeast(1)), 56.dp, locked = lv == 0)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+" + num(e.perMinute(b.produces)) + " ", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Sprite(resSprite(b.produces), 12.dp)
                                Text("/" + stringResource(R.string.ik_min), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
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
                    Sprite(unitSprite(u), 48.dp, locked = !e.canTrainType(u))
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
            Sprite(heroSprite(h), 52.dp, modifier = Modifier.clip(RoundedCornerShape(8.dp)), locked = !owned)
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
    val last = KingdomEngine.REGIONS.lastIndex
    var selected by remember { mutableIntStateOf(e.conquered.coerceAtMost(last)) }
    LaunchedEffect(e.conquered) { selected = e.conquered.coerceAtMost(last) }
    Column(Modifier.fillMaxSize()) {
        RegionCard(KingdomEngine.REGIONS[selected], vm)
        MapView(e, selected, onSelect = { selected = it }, modifier = Modifier.weight(1f).padding(top = 6.dp))
    }
}

@Composable
private fun MapView(e: KingdomEngine, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val mapImg = ImageBitmap.imageResource(R.drawable.ik_world_map)
    val pulse = rememberClash()
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val paint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
    }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, Frame, RoundedCornerShape(14.dp)),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val viewH = constraints.maxHeight.toFloat()
        val scale = widthPx / MAP_W
        val mapHeight = with(density) { (MAP_H * scale).toDp() }
        LaunchedEffect(e.conquered, widthPx) {
            val node = MAP_NODES[e.conquered.coerceAtMost(MAP_NODES.lastIndex)]
            scroll.animateScrollTo((node.second * scale - viewH / 2f).toInt().coerceAtLeast(0))
        }
        Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .pointerInput(scale) {
                        detectTapGestures { o ->
                            val mx = o.x / scale
                            val my = o.y / scale
                            val best = MAP_NODES.indices.minByOrNull { i ->
                                val dx = MAP_NODES[i].first - mx
                                val dy = MAP_NODES[i].second - my
                                dx * dx + dy * dy
                            } ?: return@detectTapGestures
                            val dx = MAP_NODES[best].first - mx
                            val dy = MAP_NODES[best].second - my
                            if (dx * dx + dy * dy < 16 * 16) onSelect(best)
                        }
                    },
            ) {
                drawImage(
                    image = mapImg,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(mapImg.width, mapImg.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.None,
                )
                paint.textSize = 6f * scale
                for (i in MAP_NODES.indices) {
                    val (nx, ny) = MAP_NODES[i]
                    val c = Offset(nx * scale, ny * scale)
                    val r = 6f * scale
                    val region = KingdomEngine.REGIONS[i]
                    val done = i < e.conquered
                    val next = i == e.conquered
                    if (next) {
                        drawCircle(Crimson.copy(alpha = 0.35f * (1f - pulse)), r * (1.3f + 0.7f * pulse), c)
                    }
                    drawCircle(Color(0xFF1A1C2C), r + scale, c)
                    val fill = when {
                        done -> Color(0xFFFFCD75)
                        next -> if (region.isBoss) Crimson else Color(0xFFEF7D57)
                        region.isBoss -> Color(0xFF6E263E)
                        else -> Color(0xFF566C86)
                    }
                    drawCircle(fill, r, c)
                    drawCircle(Color.White.copy(alpha = 0.35f), r * 0.55f, Offset(c.x - r * 0.25f, c.y - r * 0.3f))
                    if (done) {
                        // sancak
                        val px = c.x + r * 0.2f
                        drawRect(Color(0xFF603828), Offset(px, c.y - r * 2.1f), Size(scale, r * 2f))
                        drawRect(Color(0xFF6E3CA0), Offset(px + scale, c.y - r * 2.1f), Size(r * 1.1f, r * 0.7f))
                        drawRect(Color(0xFFFFCD75), Offset(px + scale * 2, c.y - r * 1.9f), Size(scale, scale))
                    }
                    if (i == selected) {
                        drawCircle(Color.White, r + scale * 2.5f, c, style = Stroke(width = scale))
                    }
                    paint.color = if (done) android.graphics.Color.rgb(26, 28, 44) else android.graphics.Color.WHITE
                    drawIntoCanvas { cv -> cv.nativeCanvas.drawText((i + 1).toString(), c.x, c.y + paint.textSize * 0.35f, paint) }
                }
            }
        }
    }
}

@Composable
private fun RegionCard(r: Region, vm: KingdomViewModel) {
    val e = vm.engine
    val done = r.index < e.conquered
    val isNext = r.index == e.conquered
    Card(highlight = isNext) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (r.index + 1).toString() + ". " + if (done || isNext) stringResource(regionName(r)) else "???",
                        color = if (done || isNext) AppColors.TextPrimary else AppColors.TextSecondary,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                    )
                    if (r.isBoss) Text("  " + stringResource(R.string.ik_boss), color = Crimson, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                if (done) Text(stringResource(R.string.ik_conquered), color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (done || isNext) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (r.bonusRes != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+${r.bonusPct}% ", color = AppColors.TextMuted, fontSize = 11.sp)
                                Sprite(resSprite(r.bonusRes), 13.dp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("+" + num(r.taxPerMin) + " ", color = AppColors.TextMuted, fontSize = 11.sp)
                            Sprite(R.drawable.ik_res_gold, 13.dp)
                            Text("/" + stringResource(R.string.ik_min), color = AppColors.TextMuted, fontSize = 11.sp)
                        }
                        if (r.unlocksHero != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Sprite(heroSprite(r.unlocksHero), 16.dp, modifier = Modifier.clip(RoundedCornerShape(3.dp)))
                                Text(" " + stringResource(heroName(r.unlocksHero)), color = AppColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (isNext) {
                ActionButton(stringResource(R.string.ik_attack), enabled = e.armySize() > 0, color = Crimson) { vm.attack() }
            }
        }
        if (isNext) {
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                for ((u, n) in r.enemy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Sprite(unitSprite(u), 22.dp, flipX = true)
                        Text("×$n", color = AppColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            val mine = e.myPowerVs(r.enemy)
            val theirs = e.enemyPower(r)
            Text(
                stringResource(R.string.ik_your_power, num(mine)) + "  ·  " + stringResource(R.string.ik_enemy_power, num(theirs)),
                color = if (mine > theirs * 1.1) Green else if (mine > theirs * 0.9) AppColors.Gold else AppColors.Danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ik_reward) + ": ", color = AppColors.TextSecondary, fontSize = 11.sp)
                ResourceLine(r.reward, color = AppColors.TextMuted)
            }
            if (e.armySize() == 0) Text(stringResource(R.string.ik_no_army), color = AppColors.Danger, fontSize = 11.sp)
        } else if (!done) {
            Text(stringResource(R.string.ik_map_locked), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (e.nextRegion == null) {
            Text(stringResource(R.string.ik_all_conquered), color = AppColors.Gold, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun BattleReportView(r: BattleReport, vm: KingdomViewModel) {
    val clash = rememberClash()
    Overlay {
        // Iki ordu karsilasiyor: bizimkiler soldan, dusman sagdan (aynalanmis).
        val mine = vm.engine.army.filterValues { it > 0 }.keys.ifEmpty { r.myLosses.keys }.take(3)
        val theirs = r.region.enemy.keys.take(3)
        Box(
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF41A6F6), Color(0xFF38B764), Color(0xFF257179)))),
        ) {
            Row(Modifier.align(Alignment.CenterStart).padding(start = (8 + 14 * clash).dp)) {
                for (u in mine) Sprite(unitSprite(u), 40.dp)
            }
            Row(Modifier.align(Alignment.CenterEnd).padding(end = (8 + 14 * clash).dp)) {
                for (u in theirs) Sprite(unitSprite(u), 40.dp, flipX = true, locked = r.won && clash > 0.8f)
            }
        }
        Text(
            stringResource(if (r.won) R.string.ik_victory else R.string.ik_defeat),
            color = if (r.won) AppColors.Gold else AppColors.Danger,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 8.dp),
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
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Sprite(heroSprite(it), 40.dp, modifier = Modifier.clip(RoundedCornerShape(6.dp)))
                    Text(" " + stringResource(R.string.ik_hero_unlocked, stringResource(heroName(it))), color = AppColors.Gold, fontWeight = FontWeight.Black)
                }
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
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = AppColors.TextMuted, fontSize = 13.sp)
        if (losses.isEmpty()) {
            Text(stringResource(R.string.ik_none), color = AppColors.TextMuted, fontSize = 13.sp)
        } else {
            for ((u, n) in losses) {
                Sprite(unitSprite(u), 18.dp)
                Text("−$n ", color = AppColors.Danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
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
