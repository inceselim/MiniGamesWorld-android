package com.eomer91.minigamesworld.ui.games.island

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.feedback.GameMusicEffect
import com.eomer91.minigamesworld.ui.games.football.FbBusy
import com.eomer91.minigamesworld.ui.games.football.FbButton
import com.eomer91.minigamesworld.ui.games.football.FbChip
import com.eomer91.minigamesworld.ui.games.football.FbColors
import com.eomer91.minigamesworld.ui.games.football.FbPanel
import com.eomer91.minigamesworld.ui.games.football.LandscapeImmersive
import com.eomer91.minigamesworld.ui.games.island.engine.BuildError
import com.eomer91.minigamesworld.ui.games.island.engine.Cat
import com.eomer91.minigamesworld.ui.games.island.engine.Difficulty
import com.eomer91.minigamesworld.ui.games.island.engine.Goal
import com.eomer91.minigamesworld.ui.games.island.engine.IsCatalog
import com.eomer91.minigamesworld.ui.games.island.engine.IsSettings
import com.eomer91.minigamesworld.ui.games.island.engine.IsSim
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import com.eomer91.minigamesworld.ui.games.island.engine.MapSize
import com.eomer91.minigamesworld.ui.games.island.engine.MapType
import com.eomer91.minigamesworld.ui.games.island.engine.NewsKind
import com.eomer91.minigamesworld.ui.games.island.render.IsCamera
import com.eomer91.minigamesworld.ui.games.island.render.IsGhost
import com.eomer91.minigamesworld.ui.games.island.render.IsSelection
import com.eomer91.minigamesworld.ui.games.island.render.IslandMapView
import com.eomer91.minigamesworld.ui.theme.AppColors

/** Oyuncunun secebilecegi sirket renkleri (bot renklerinden ayri) */
val IS_USER_COLORS = intArrayOf(0xFF2E86DE.toInt(), 0xFFE84393.toInt(), 0xFF00B8D4.toInt(), 0xFF34495E.toInt(), 0xFFA0522D.toInt())

/** Ada Patronu: tatil adasi tycoon oyunu (yatay) */
@Composable
fun IslandScreen(onBack: () -> Unit, viewModel: IslandViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LandscapeImmersive()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    ui.toast?.let { t ->
        LaunchedEffect(t) {
            snackbar.showSnackbar(t.text ?: if (t.arg.isEmpty()) context.getString(t.res) else context.getString(t.res, t.arg))
            viewModel.clearToast()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        // Reklam ya da arka plan: kaydet ve simulasyonu durdur; geri gelince devam
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_PAUSE -> { viewModel.setActive(false); viewModel.save() }
                Lifecycle.Event.ON_RESUME -> viewModel.setActive(true)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    BackHandler {
        when (ui.phase) {
            IsPhase.GAME -> when {
                ui.placing >= 0 -> viewModel.cancelPlacing()
                ui.panel != IsPanel.NONE -> viewModel.closePanel()
                else -> viewModel.openPanel(IsPanel.MENU)
            }
            IsPhase.SETUP -> viewModel.backToStart()
            else -> onBack()
        }
    }

    GameMusicEffect(R.raw.music_island, playing = ui.phase != IsPhase.LOADING, gain = 0.45f)

    Box(Modifier.fillMaxSize().background(AppColors.Background)) {
        val world = viewModel.world
        when (ui.phase) {
            IsPhase.LOADING -> FbBusy(stringResource(R.string.fb_please_wait))
            IsPhase.START -> IsStart(ui, viewModel, onBack)
            IsPhase.SETUP -> IsSetup(ui, viewModel)
            IsPhase.GAME -> if (world != null) IsGame(world, ui, viewModel, onBack)
        }
        if (ui.generating) FbBusy(stringResource(R.string.is_generating))
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

// ====================================================================== baslangic

@Composable
private fun IsStart(ui: IsUiState, vm: IslandViewModel, onBack: () -> Unit) {
    var confirmNew by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.game_island), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xE60B1020), Color(0x660B1020), Color(0xCC0B1020)))))
        Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(AppColors.Surface)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextPrimary)
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.game_island_name), color = AppColors.TextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.is_tagline), color = AppColors.TextMuted, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp).widthIn(max = 420.dp))
            }
            Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (ui.hasSave) FbButton(stringResource(R.string.is_continue), { vm.load() }, Modifier.fillMaxWidth())
                FbButton(stringResource(R.string.is_new_game), { if (ui.hasSave) confirmNew = true else vm.openSetup() }, Modifier.fillMaxWidth(),
                    color = if (ui.hasSave) FbColors.PanelHigh else AppColors.Primary, textColor = if (ui.hasSave) AppColors.TextPrimary else AppColors.OnPrimary)
                if (confirmNew) {
                    FbPanel(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.is_new_warning), color = FbColors.Orange, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FbButton(stringResource(R.string.is_yes), { confirmNew = false; vm.openSetup() }, color = FbColors.Red)
                            FbButton(stringResource(R.string.is_no), { confirmNew = false }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================== kurulum

@Composable
private fun IsSetup(ui: IsUiState, vm: IslandViewModel) {
    val defaultName = stringResource(R.string.is_company_default)
    var name by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(IS_USER_COLORS[0]) }
    var bots by remember { mutableIntStateOf(3) }
    var diff by remember { mutableStateOf(Difficulty.NORMAL) }
    var mapType by remember { mutableStateOf(MapType.ISLAND) }
    var mapSize by remember { mutableStateOf(MapSize.MEDIUM) }
    var goal by remember { mutableStateOf(Goal.LAST_STANDING) }

    Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.is_setup_title)) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(20) }, label = { Text(stringResource(R.string.is_company_name)) },
                    placeholder = { Text(defaultName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SetupRow(stringResource(R.string.is_color)) {
                    for (c in IS_USER_COLORS) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                            .border(if (c == color) 3.dp else 1.dp, if (c == color) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { color = c }, contentAlignment = Alignment.Center) {
                            if (c == color) Text("✓", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }
                SetupRow(stringResource(R.string.is_bots)) { for (b in 1..5) FbChip("$b", bots == b, { bots = b }) }
                SetupRow(stringResource(R.string.is_difficulty)) { for (d in Difficulty.entries) FbChip(stringResource(diffName(d)), diff == d, { diff = d }) }
                SetupRow(stringResource(R.string.is_map_type)) { for (m in MapType.entries) FbChip(stringResource(mapTypeName(m)), mapType == m, { mapType = m }) }
                SetupRow(stringResource(R.string.is_map_size)) { for (m in MapSize.entries) FbChip(stringResource(mapSizeName(m)), mapSize == m, { mapSize = m }) }
                SetupRow(stringResource(R.string.is_goal)) { for (g in Goal.entries) FbChip(stringResource(goalName(g)), goal == g, { goal = g }) }
            }
        }
        FbPanel(Modifier.width(300.dp).fillMaxHeight()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(color)))
                    Text(name.ifBlank { defaultName }, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(12.dp))
                IsKV(stringResource(R.string.is_start_money), IsFmt.money(diff.startMoney), FbColors.Green)
                IsKV(stringResource(R.string.is_rival_money), IsFmt.money(diff.botMoney) + " × $bots", FbColors.Orange)
                Text(stringResource(R.string.is_tourists_year, IsFmt.count((mapSize.tourists.sum() * IsSim.SEASON.max()))),
                    color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                Text(stringResource(goalDesc(goal)), color = AppColors.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FbButton(stringResource(R.string.is_cancel), { vm.backToStart() }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                FbButton(stringResource(R.string.is_start), {
                    vm.newGame(IsSettings(companyName = name.trim().ifBlank { defaultName }, color = color, bots = bots,
                        difficulty = diff, mapType = mapType, mapSize = mapSize, goal = goal))
                }, Modifier.weight(1f), enabled = !ui.generating)
            }
        }
    }
}

@Composable
private fun SetupRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

// ====================================================================== oyun

@Composable
private fun IsGame(w: IsWorld, ui: IsUiState, vm: IslandViewModel, onBack: () -> Unit) {
    val camera = remember(w) { IsCamera() }
    val me = w.companies[w.userId]
    // Ilk acilista kamerayi havalimanina ya da oyuncunun binalarina getir
    LaunchedEffect(w, camera.ready) {
        if (!camera.ready) return@LaunchedEffect
        val focus = w.buildings.firstOrNull { it != null && it.owner == w.userId } ?: w.buildings.firstOrNull { it?.type == IsCatalog.AIRPORT }
        if (focus != null) camera.centerOn(com.eomer91.minigamesworld.ui.games.island.render.isLayout(w.n), focus.cx, focus.cy, maxOf(camera.scale, 1.1f))
    }
    LaunchedEffect(ui.focusSeq) {
        if (ui.focusSeq > 0 && camera.ready) camera.centerOn(com.eomer91.minigamesworld.ui.games.island.render.isLayout(w.n), ui.focusX, ui.focusY, maxOf(camera.scale, 1.2f))
    }
    // Seni ilgilendiren onemli gelismeler kisa bildirim olarak da gosterilir
    val lastNews = w.news.lastOrNull()
    val alert = lastNews?.takeIf { n ->
        when (n.kind) {
            NewsKind.SABOTAGE, NewsKind.PROPAGANDA, NewsKind.STRIKE, NewsKind.WARNING, NewsKind.AUCTION_WON -> n.company == w.userId
            NewsKind.BANKRUPT, NewsKind.AUCTION -> true
            else -> false
        }
    }?.let { n -> newsText(w, n)?.let { newsIcon(n.kind) + " " + it } }
    val newsAtOpen = remember(w) { w.newsCount }
    LaunchedEffect(w.newsCount) { if (alert != null && w.newsCount > newsAtOpen) vm.toastText(alert) }
    val ghost = if (ui.placing >= 0 && ui.ghostX >= 0) {
        val t = IsCatalog[ui.placing]
        IsGhost(t, ui.ghostX, ui.ghostY, vm.canPlace() == BuildError.OK, me.color, tiles = if (t.cat == Cat.ROAD) vm.roadLinePlan().first else null)
    } else null
    val selection = when {
        ui.panel == IsPanel.BUILDING && ui.selBuilding >= 0 -> w.buildings.getOrNull(ui.selBuilding)?.let { IsSelection(it.x, it.y, it.type.w, it.type.h) }
        ui.panel == IsPanel.LAND && ui.selX >= 0 -> IsSim.parcelOrigin(ui.selX, ui.selY).let { (px, py) ->
            IsSelection(px, py, minOf(IsSim.PARCEL, w.n - px), minOf(IsSim.PARCEL, w.n - py))
        }
        else -> null
    }

    Box(Modifier.fillMaxSize()) {
        IslandMapView(w, ui.version, camera, Modifier.fillMaxSize(), selection = selection, ghost = ghost,
            pickBuildings = ui.placing < 0, onTap = { x, y, id -> vm.tapMap(x, y, id) })

        Column(Modifier.fillMaxSize()) {
            IsTopBar(w, ui, vm)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                IsToolbar(w, ui, vm)
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(ui.panel != IsPanel.NONE && ui.panel != IsPanel.MENU,
                    enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
                    Box(Modifier.width(340.dp).fillMaxHeight().padding(end = 8.dp, bottom = 8.dp)) {
                        when (ui.panel) {
                            IsPanel.BUILD -> BuildPanel(w, ui, vm)
                            IsPanel.BUILDING -> BuildingPanel(w, ui, vm)
                            IsPanel.LAND -> LandPanel(w, ui, vm)
                            IsPanel.FINANCE -> FinancePanel(w, ui, vm)
                            IsPanel.MARKETING -> MarketingPanel(w, ui, vm)
                            IsPanel.GUIDE -> GuidePanel(vm)
                            IsPanel.RIVALS -> RivalsPanel(w, ui, vm)
                            IsPanel.NEWS -> NewsPanel(w, ui, vm)
                            else -> Unit
                        }
                    }
                }
            }
        }
        if (ui.placing >= 0) PlacementBar(w, ui, vm, Modifier.align(Alignment.BottomCenter))
        if (ui.panel == IsPanel.MENU) GameMenu(vm, onBack)
        if (w.gameOver && !ui.gameOverSeen) GameOverDialog(w, vm, onBack)
    }
}

@Composable
private fun IsTopBar(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    val me = w.companies[w.userId]
    Row(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xF00B1020), Color(0xC00B1020))))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HudButton("☰") { vm.openPanel(IsPanel.MENU) }
        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(me.color)))
        Text(me.name, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 110.dp))
        HudStat("💰", IsFmt.money(me.balance), if (me.balance < 0) FbColors.Red else FbColors.Green) { vm.openPanel(IsPanel.FINANCE) }
        HudStat("📅", IsFmt.date(w) + " · " + stringResource(seasonName(w.month)), AppColors.TextPrimary)
        HudStat("🧳", IsFmt.count(w.present.sum()), AppColors.TextPrimary)
        HudStat("⭐", "%.0f".format(me.reputation), FbColors.Yellow)
        // Son haber: kalan bosluga sigar (dar ekranda kisalir, hiz dugmeleri hep gorunur)
        val last = w.news.lastOrNull()
        val txt = last?.let { newsText(w, it) }
        if (last != null && txt != null) {
            Text(newsIcon(last.kind) + " " + txt, color = AppColors.TextMuted, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).clickable { vm.openPanel(IsPanel.NEWS) })
        } else {
            Spacer(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (s in 0..3) SpeedButton(s, ui.speed == s) { vm.setSpeed(s) }
        }
    }
}

@Composable
private fun HudButton(icon: String, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(FbColors.PanelHigh).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(icon, color = AppColors.TextPrimary, fontSize = 16.sp)
    }
}

@Composable
private fun HudStat(icon: String, value: String, color: Color, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 13.sp)
        Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp), maxLines = 1)
    }
}

@Composable
private fun SpeedButton(s: Int, selected: Boolean, onClick: () -> Unit) {
    val label = when (s) { 0 -> "❚❚"; 1 -> "▶"; 2 -> "▶▶"; else -> "▶▶▶" }
    Box(
        Modifier.height(30.dp).widthIn(min = 30.dp).clip(RoundedCornerShape(8.dp))
            .background(if (selected) AppColors.Primary else FbColors.PanelHigh)
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.8f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) AppColors.OnPrimary else AppColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1) }
}

@Composable
private fun IsToolbar(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    val unread = (w.newsCount - ui.newsSeen).coerceAtLeast(0)
    val auctions = w.auctions.count { !it.done }
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolButton("🏗️", stringResource(R.string.is_build), ui.panel == IsPanel.BUILD || ui.placing >= 0) { vm.openPanel(IsPanel.BUILD) }
        ToolButton("💰", stringResource(R.string.is_finance), ui.panel == IsPanel.FINANCE) { vm.openPanel(IsPanel.FINANCE) }
        ToolButton("📣", stringResource(R.string.is_marketing), ui.panel == IsPanel.MARKETING) { vm.openPanel(IsPanel.MARKETING) }
        ToolButton("🏆", stringResource(R.string.is_rivals), ui.panel == IsPanel.RIVALS, badge = auctions) { vm.openPanel(IsPanel.RIVALS) }
        ToolButton("📰", stringResource(R.string.is_news), ui.panel == IsPanel.NEWS, badge = if (ui.panel == IsPanel.NEWS) 0 else unread) { vm.openPanel(IsPanel.NEWS) }
    }
}

@Composable
private fun ToolButton(icon: String, label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    Box {
        Column(
            Modifier.width(62.dp).clip(RoundedCornerShape(14.dp))
                .background(if (selected) AppColors.Primary else Color(0xE0111827))
                .border(1.dp, if (selected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, fontSize = 18.sp)
            Text(label, color = if (selected) AppColors.OnPrimary else AppColors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        if (badge > 0) {
            Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(FbColors.Red), contentAlignment = Alignment.Center) {
                Text(if (badge > 9) "9+" else "$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PlacementBar(w: IsWorld, ui: IsUiState, vm: IslandViewModel, modifier: Modifier) {
    val t = IsCatalog[ui.placing]
    Row(
        modifier.padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xF0111827))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.widthIn(min = 160.dp, max = 300.dp)) {
            Text(stringResource(bName(t)), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            if (t.cat == Cat.ROAD) {
                // Yol: iki dokunusla hat
                val (tiles, cost) = vm.roadLinePlan()
                Text(stringResource(if (ui.ghostX < 0) R.string.is_road_hint_start else if (ui.endX < 0) R.string.is_road_hint_end else R.string.is_road_hint_again),
                    color = AppColors.TextSecondary, fontSize = 12.sp)
                if (ui.ghostX >= 0) Text(stringResource(R.string.is_road_tiles, tiles.size, IsFmt.money(cost)),
                    color = if (cost <= w.companies[w.userId].balance) FbColors.Green else FbColors.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else if (ui.ghostX < 0) {
                Text(stringResource(R.string.is_place_hint), color = AppColors.TextSecondary, fontSize = 12.sp)
            } else {
                val err = vm.canPlace()
                val cost = IsSim.buildCost(w, w.userId, t, ui.ghostX, ui.ghostY)
                Text(stringResource(R.string.is_place_total, IsFmt.money(cost)), color = if (err == BuildError.OK) FbColors.Green else AppColors.TextSecondary, fontSize = 12.sp)
                if (err != BuildError.OK) Text(stringResource(IslandViewModel.buildErrorRes(err)), color = FbColors.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        val canPlace = ui.ghostX >= 0 && vm.canPlace() == BuildError.OK
        if (t.adOnly) {
            val showRewarded = rememberRewardedLauncher()
            FbButton(stringResource(R.string.is_ad_build_btn), { showRewarded({ vm.confirmPlace(adWatched = true) }, { vm.toast(R.string.ad_not_ready) }) },
                enabled = canPlace, color = FbColors.Yellow, textColor = Color.Black)
        } else {
            FbButton(stringResource(R.string.is_confirm_build), { vm.confirmPlace() }, enabled = canPlace, color = FbColors.Green)
        }
        FbButton(stringResource(R.string.is_cancel), { vm.cancelPlacing() }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
    }
}

@Composable
private fun GameMenu(vm: IslandViewModel, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { vm.closePanel() }, contentAlignment = Alignment.Center) {
        FbPanel(Modifier.width(300.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
            Text(stringResource(R.string.game_island_name), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            FbButton(stringResource(R.string.is_continue), { vm.closePanel() }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FbButton("❓ " + stringResource(R.string.is_guide), { vm.openPanel(IsPanel.GUIDE) }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            FbButton(stringResource(R.string.is_save_exit), { vm.exitGame(); onBack() }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
        }
    }
}

@Composable
private fun GameOverDialog(w: IsWorld, vm: IslandViewModel, onBack: () -> Unit) {
    val me = w.companies[w.userId]
    val won = w.winner == w.userId
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = true) {}, contentAlignment = Alignment.Center) {
        FbPanel(Modifier.width(360.dp)) {
            Text(if (won) "🏆" else "💥", fontSize = 44.sp)
            Text(stringResource(if (won) R.string.is_you_won else if (!me.alive) R.string.is_you_lost else R.string.is_game_over),
                color = if (won) FbColors.Yellow else FbColors.Red, fontWeight = FontWeight.Black, fontSize = 22.sp)
            if (w.winner >= 0 && !won) Text(stringResource(R.string.is_winner, companyName(w, w.winner)), color = AppColors.TextPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            IsKV(stringResource(R.string.is_net_worth), IsFmt.money(IsSim.netWorth(w, w.userId)))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FbButton(stringResource(R.string.is_back_menu), { vm.dismissGameOver(); vm.exitGame(); onBack() }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                FbButton(stringResource(R.string.is_close), { vm.dismissGameOver() })
            }
        }
    }
}

@Composable
fun IsKV(key: String, value: String, valueColor: Color = AppColors.TextPrimary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(key, color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}
