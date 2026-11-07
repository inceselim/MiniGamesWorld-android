package com.eomer91.minigamesworld.ui.games.football

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.FbCountries
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.theme.AppColors

/** Kulup Patronu: futbol menajerlik oyunu (yatay). */
@Composable
fun FootballScreen(onBack: () -> Unit, viewModel: FootballViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LandscapeImmersive()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    ui.toast?.let { t ->
        LaunchedEffect(t) {
            snackbar.showSnackbar(if (t.arg.isEmpty()) context.getString(t.res) else context.getString(t.res, t.arg))
            viewModel.clearToast()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) viewModel.save() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    BackHandler {
        when (ui.phase) {
            FbPhase.HUB -> if (ui.tab != FbTab.HOME) viewModel.setTab(FbTab.HOME) else { viewModel.save(); onBack() }
            FbPhase.SETUP -> viewModel.abandonSetup()
            FbPhase.RESULT -> viewModel.closeResult()
            FbPhase.LIVE -> Unit
            FbPhase.SEASON_END -> viewModel.closeSeasonSummary()
            else -> if (!ui.busy) onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(AppColors.Background)) {
        val world = viewModel.world
        if (!ui.busy || ui.phase == FbPhase.SETUP) {
            when (ui.phase) {
                FbPhase.LOADING -> Unit
                FbPhase.START -> FbStart(ui, viewModel, onBack)
                FbPhase.SETUP -> if (ui.setupReady && world != null) FbSetup(world, ui, viewModel) else FbBusy(stringResource(R.string.fb_generating))
                FbPhase.HUB -> if (world != null) FbHub(world, ui, viewModel, onBack)
                FbPhase.PREMATCH -> if (world != null) FbPreMatch(world, ui, viewModel)
                FbPhase.LIVE -> { val sim = viewModel.live; if (world != null && sim != null) FbLiveMatch(world, sim, viewModel) }
                FbPhase.RESULT -> if (world != null) FbResultScreen(world, ui, viewModel)
                FbPhase.SEASON_END -> if (world != null) FbSeasonEnd(world, ui, viewModel)
            }
        }
        if (ui.busy && ui.phase != FbPhase.SETUP) FbBusy(ui.busyDate.ifEmpty { stringResource(R.string.fb_please_wait) })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun LandscapeImmersive() {
    val activity = LocalActivity.current
    val view = LocalView.current
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

@Composable
fun FbBusy(text: String) {
    Box(Modifier.fillMaxSize().background(AppColors.Background.copy(alpha = 0.92f)).clickable(enabled = true) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppColors.Primary)
            Spacer(Modifier.height(14.dp))
            Text(text, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ====================================================================== baslangic

@Composable
private fun FbStart(ui: FbUiState, vm: FootballViewModel, onBack: () -> Unit) {
    var confirmNew by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(AppColors.Surface)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextPrimary)
            }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.game_football_manager_name), color = AppColors.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.fb_tagline), color = AppColors.TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (ui.hasSave) FbButton(stringResource(R.string.fb_continue_career), { vm.loadCareer() }, Modifier.fillMaxWidth())
            FbButton(stringResource(R.string.fb_new_career), { if (ui.hasSave) confirmNew = true else vm.newCareer() }, Modifier.fillMaxWidth(),
                color = if (ui.hasSave) FbColors.PanelHigh else AppColors.Primary, textColor = if (ui.hasSave) AppColors.TextPrimary else AppColors.OnPrimary)
            if (confirmNew) {
                FbPanel(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.fb_new_career_warning), color = FbColors.Orange, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FbButton(stringResource(R.string.fb_yes), { confirmNew = false; vm.newCareer() }, color = FbColors.Red)
                        FbButton(stringResource(R.string.fb_no), { confirmNew = false }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                    }
                }
            }
        }
    }
}

// ====================================================================== kulup secimi

@Composable
private fun FbSetup(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val countries = remember(ui.version) {
        FbCountries.all.indices.filter { world.leaguesOf(it).isNotEmpty() }.sortedByDescending { FbCountries.all[it].level }
    }
    var country by remember { mutableIntStateOf(countries.firstOrNull { FbCountries.all[it].code == "TUR" } ?: countries.first()) }
    var tier by remember { mutableIntStateOf(0) }
    var club by remember { mutableIntStateOf(-1) }
    var name by remember { mutableStateOf("") }

    Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Ulkeler
        FbPanel(Modifier.width(190.dp).fillMaxHeight(), title = stringResource(R.string.fb_choose_country)) {
            LazyColumn {
                items(countries) { ci ->
                    val sel = ci == country
                    Text(
                        FbCountries.all[ci].englishName, color = if (sel) AppColors.OnPrimary else AppColors.TextPrimary, fontSize = 14.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (sel) AppColors.Primary else Color.Transparent)
                            .clickable { country = ci; tier = 0; club = -1 }.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
        // Lig ve kulupler
        val leagues = world.leaguesOf(country)
        val lg = leagues.getOrNull(tier) ?: leagues.first()
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_choose_club)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                leagues.forEachIndexed { i, l -> FbChip(stringResource(R.string.fb_tier_n, l.tier), i == tier, { tier = i; club = -1 }) }
            }
            val clubs = lg.clubs.map { world.clubs[it] }.sortedByDescending { it.reputation }
            LazyColumn {
                items(clubs) { c ->
                    val sel = c.id == club
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (sel) FbColors.PanelHigh else Color.Transparent)
                            .clickable { club = c.id }.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FbCrest(c.color1, c.color2, c.shortName, 30.dp)
                        Text(c.name, color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1)
                        FbStars(c.reputation / 2000f, 12.dp)
                        Text(FbFormat.money(c.balance), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(80.dp).padding(start = 8.dp))
                    }
                }
            }
        }
        // Secilen kulup
        FbPanel(Modifier.width(280.dp).fillMaxHeight()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (club >= 0) {
                    val c = world.clubs[club]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FbCrest(c.color1, c.color2, c.shortName, 46.dp)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(c.name, color = AppColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            Text(leagueName(world, c.league), color = AppColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FbKeyValue(stringResource(R.string.fb_reputation), "")
                    FbStars(c.reputation / 2000f, 16.dp)
                    FbKeyValue(stringResource(R.string.fb_balance), FbFormat.money(c.balance))
                    FbKeyValue(stringResource(R.string.fb_transfer_budget), FbFormat.money(c.transferBudget))
                    FbKeyValue(stringResource(R.string.fb_stadium), "${c.stadiumName} (${c.capacity})")
                    FbKeyValue(stringResource(R.string.fb_squad_size), "${c.players.size}")
                    val best = world.clubPlayers(c.id).maxByOrNull { it.currentAbility() }
                    if (best != null) FbKeyValue(stringResource(R.string.fb_best_player), best.fullName)
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(28) }, singleLine = true,
                    label = { Text(stringResource(R.string.fb_manager_name)) }, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                FbButton(stringResource(R.string.fb_start_career), { vm.startCareer(club, name.ifBlank { "Manager" }) }, Modifier.fillMaxWidth(), enabled = club >= 0)
                Spacer(Modifier.height(8.dp))
                FbButton(stringResource(R.string.fb_cancel), { vm.abandonSetup() }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                if (ui.busy) Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), color = AppColors.Primary) }
            }
        }
    }
}
