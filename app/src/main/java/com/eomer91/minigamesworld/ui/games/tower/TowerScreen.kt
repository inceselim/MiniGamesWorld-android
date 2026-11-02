package com.eomer91.minigamesworld.ui.games.tower

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.eomer91.minigamesworld.ui.games.bubble.Vec
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

private val SilverColor = Color(0xFFCBD5E1)
private val TowerColor = Color(0xFF38BDF8)

private fun enemyColor(type: EnemyType) = when (type) {
    EnemyType.NORMAL -> Color(0xFFEF4444)
    EnemyType.FAST -> Color(0xFFFACC15)
    EnemyType.TANK -> Color(0xFFA855F7)
    EnemyType.BOSS -> Color(0xFFF97316)
}

@Composable
fun TowerScreen(viewModel: TowerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val toWorkshopWithAd = { showInterstitial { viewModel.backToWorkshop() } }

    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    // Kare dongusu: sadece ekran on plandayken (reklam ve arka planda oyun durur).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val playing = state.phase == TowerPhase.PLAYING
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
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
            TowerPhase.WORKSHOP -> Workshop(state, viewModel)
            else -> RunContent(
                state = state,
                viewModel = viewModel,
                onExit = toWorkshopWithAd,
                onRevive = { showRewarded({ viewModel.revive() }, adNotReady) },
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------- Atolye (kalici yukseltmeler, altin) ----------------

@Composable
private fun Workshop(state: TowerUiState, viewModel: TowerViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.td_workshop), color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.td_workshop_info), color = AppColors.TextSecondary, fontSize = 13.sp)
            }
            GoldAmount(state.gold)
        }

        TowerStat.entries.forEach { stat ->
            val level = state.permanent[stat] ?: 0
            UpgradeRow(
                title = stringResource(stat.labelRes),
                levelText = stringResource(R.string.td_level, level),
                valueText = statValueText(stat, level) + if (level < TowerBalance.MAX_PERMANENT_LEVEL) "  →  " + statValueText(stat, level + 1) else "",
                cost = if (level >= TowerBalance.MAX_PERMANENT_LEVEL) null else viewModel.permanentCost(level),
                affordable = state.gold >= viewModel.permanentCost(level),
                isGold = true,
                onBuy = { viewModel.buyPermanent(stat.name, level) },
            )
        }
        EconomyPerk.entries.forEach { perk ->
            val level = state.perks[perk] ?: 0
            UpgradeRow(
                title = stringResource(perk.labelRes),
                levelText = stringResource(R.string.td_level, level),
                valueText = perkValueText(perk, level) + if (level < TowerBalance.MAX_PERMANENT_LEVEL) "  →  " + perkValueText(perk, level + 1) else "",
                cost = if (level >= TowerBalance.MAX_PERMANENT_LEVEL) null else viewModel.permanentCost(level),
                affordable = state.gold >= viewModel.permanentCost(level),
                isGold = true,
                onBuy = { viewModel.buyPermanent(perk.name, level) },
            )
        }

        if (state.best > 0) {
            Text(stringResource(R.string.best_score, state.best), color = AppColors.Gold, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Button(
            onClick = viewModel::startRun,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(56.dp),
        ) {
            Text(stringResource(R.string.td_start), fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun UpgradeRow(
    title: String,
    levelText: String,
    valueText: String,
    cost: Int?,
    affordable: Boolean,
    isGold: Boolean,
    onBuy: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(levelText, color = AppColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(valueText, color = AppColors.TextSecondary, fontSize = 12.sp)
        }
        if (cost == null) {
            Text(stringResource(R.string.td_max), color = AppColors.Gold, fontWeight = FontWeight.Black)
        } else {
            Button(
                onClick = onBuy,
                enabled = affordable,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGold) AppColors.Gold else SilverColor,
                    contentColor = AppColors.OnPrimary,
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.height(38.dp),
            ) {
                Text(cost.toString(), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun GoldAmount(gold: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.SurfaceHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = AppColors.Gold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(gold.toString(), color = AppColors.Gold, fontWeight = FontWeight.Black)
    }
}

// ---------------- Kosu ----------------

@Composable
private fun RunContent(state: TowerUiState, viewModel: TowerViewModel, onExit: () -> Unit, onRevive: () -> Unit) {
    val run = state.run ?: return
    val engine = viewModel.engine
    val maxHp = engine.maxHealth(run)

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        // Ust bilgi: dalga, puan, hiz, cikis
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.td_wave, run.wave), color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.td_score, run.score), color = AppColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = viewModel::toggleSpeed, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(38.dp)) {
                Text(stringResource(R.string.td_speed, state.speed), fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onExit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
                modifier = Modifier.height(38.dp),
            ) {
                Text(stringResource(R.string.td_back_workshop), fontWeight = FontWeight.Black)
            }
        }

        // Can cubugu + gumus
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(AppColors.SurfaceHigh),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((run.health / maxHp).coerceIn(0f, 1f))
                        .background(if (run.health / maxHp < 0.3f) AppColors.Danger else Color(0xFF22C55E)),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.td_silver, run.silver),
                color = SilverColor,
                fontWeight = FontWeight.Black,
            )
        }

        // Saha
        Box(
            Modifier
                .padding(top = 10.dp)
                .weight(1f, fill = false)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            Canvas(Modifier.fillMaxSize()) { drawField(run, engine) }

            if (run.breakTimer > 0f && !run.gameOver) {
                Text(
                    stringResource(R.string.td_next_wave, run.breakTimer.toInt() + 1),
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                )
            }
        }

        if (state.phase == TowerPhase.GAME_OVER) {
            if (!state.reviveUsed) {
                Button(
                    onClick = onRevive,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp),
                ) {
                    Icon(Icons.Rounded.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.td_revive), fontWeight = FontWeight.Black)
                }
            }
            GameOverPanel(
                score = run.score,
                isNewRecord = state.isNewRecord,
                onRestart = onExit,
                title = stringResource(R.string.td_tower_destroyed),
                detail = stringResource(R.string.td_result, run.wave, run.kills, run.score),
                goldEarned = state.goldEarned,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Text(
                stringResource(R.string.td_in_run_upgrades),
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
            )
            RunUpgrades(run, engine, onBuy = viewModel::buyRunUpgrade)
        }
    }
}

@Composable
private fun RunUpgrades(run: TowerRun, engine: TowerEngine, onBuy: (TowerStat) -> Unit) {
    val rows = TowerStat.entries.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { stat ->
                    val cost = engine.upgradeCost(run, stat)
                    val affordable = run.silver >= cost
                    Column(
                        Modifier
                            .weight(1f)
                            .alpha(if (affordable) 1f else 0.45f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.SurfaceHigh)
                            .clickable(enabled = affordable) { onBuy(stat) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(stat.labelRes), color = AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                        Text(statValueText(stat, engine.level(run, stat)), color = AppColors.TextSecondary, fontSize = 11.sp)
                        Text(stringResource(R.string.td_silver, cost), color = SilverColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                // Son satir 2 elemanliysa hizayi korumak icin bosluk
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun statValueText(stat: TowerStat, level: Int): String = when (stat) {
    TowerStat.DAMAGE -> fmt(TowerBalance.damage(level))
    TowerStat.ATTACK_SPEED -> fmt(TowerBalance.attacksPerSecond(level)) + "/s"
    TowerStat.RANGE -> fmt(TowerBalance.range(level))
    TowerStat.HEALTH -> TowerBalance.maxHealth(level).toInt().toString()
    TowerStat.REGEN -> fmt(TowerBalance.regenPerSecond(level)) + "/s"
}

private fun perkValueText(perk: EconomyPerk, level: Int): String = when (perk) {
    EconomyPerk.SILVER_BONUS -> "×" + fmt(TowerBalance.silverMultiplier(level))
    EconomyPerk.START_SILVER -> "+" + TowerBalance.startSilver(level)
}

private fun fmt(v: Float) = String.format(Locale.US, "%.1f", v)

// ---------------- Cizim ----------------

private fun DrawScope.drawField(run: TowerRun, engine: TowerEngine) {
    val unit = size.width / TowerEngine.FIELD
    fun pt(v: Vec) = Offset(v.x * unit, v.y * unit)
    val c = pt(engine.center)

    drawRect(
        Brush.radialGradient(
            listOf(Color(0xFF1E293B), Color(0xFF020617)),
            center = c,
            radius = size.width * 0.7f,
        ),
    )

    // Menzil
    val range = engine.range(run) * unit
    drawCircle(TowerColor.copy(alpha = 0.07f), range, c)
    drawCircle(TowerColor.copy(alpha = 0.35f), range, c, style = Stroke(1.5.dp.toPx()))

    // Kule
    val tr = TowerEngine.TOWER_RADIUS * unit
    drawCircle(TowerColor.copy(alpha = 0.25f), tr * 1.5f, c)
    drawCircle(TowerColor, tr, c)
    drawCircle(Color.White, tr * 0.35f, c)

    // Dusmanlar (+ can cubugu)
    run.enemies.forEach { e ->
        val p = pt(e.pos)
        val r = e.type.radius * unit
        drawCircle(enemyColor(e.type), r, p)
        drawCircle(Color.Black.copy(alpha = 0.25f), r, p, style = Stroke(r * 0.15f))
        if (e.hp < e.maxHp) {
            val w = r * 2f
            val top = Offset(p.x - r, p.y - r - 5.dp.toPx())
            drawRect(Color.Black.copy(alpha = 0.6f), top, Size(w, 3.dp.toPx()))
            drawRect(Color(0xFF22C55E), top, Size(w * (e.hp / e.maxHp).coerceIn(0f, 1f), 3.dp.toPx()))
        }
    }

    // Mermiler
    run.projectiles.forEach { p -> drawCircle(Color(0xFFFDE68A), 0.1f * unit, pt(p.pos)) }

    // Olum patlamalari
    run.bursts.forEach { b ->
        val t = (b.age / TowerEngine.BURST_LIFETIME).coerceIn(0f, 1f)
        drawCircle(Color.White.copy(alpha = 0.6f * (1f - t)), b.radius * unit * (1f + t * 1.5f), pt(b.pos), style = Stroke(2.dp.toPx()))
    }
}
