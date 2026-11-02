package com.eomer91.minigamesworld.ui.games.stocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val Up = Color(0xFF4ADE80)
private val Down = Color(0xFFF87171)

private fun money(v: Float) = "$" + String.format(Locale.US, "%,.2f", v)
private fun pct(v: Float) = (if (v >= 0f) "+" else "") + String.format(Locale.US, "%.1f%%", v * 100f)
private fun changeColor(v: Float) = when {
    v > 0.0005f -> Up
    v < -0.0005f -> Down
    else -> AppColors.TextSecondary
}

@Composable
fun StockMarketScreen(viewModel: StockMarketViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val g = state.game ?: return
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    val noticeText = when (state.notice) {
        StockNotice.NOT_ENOUGH_CASH -> stringResource(R.string.stock_not_enough_cash)
        StockNotice.NOT_ENOUGH_SHARES -> stringResource(R.string.stock_not_enough_shares)
        null -> null
    }
    LaunchedEffect(state.notice) {
        if (noticeText != null) {
            viewModel.consumeNotice()
            snackbar.showSnackbar(noticeText)
        }
    }

    // Her ~3 dk aktif oyunda bir, gun degisiminde gecis reklami.
    LaunchedEffect(state.adDue) {
        if (state.adDue) {
            viewModel.adShown()
            showInterstitial {}
        }
    }

    // Gun sayaci: sadece on planda ilerler.
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
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
            ScoreHeader(
                score = g.score,
                best = state.best,
                onRestart = restartWithAd,
                scoreLabel = stringResource(R.string.stock_net_worth),
                showBestAsMax = false,
            )

            // Gun, nakit, kontroller
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.stock_day, g.day, StockEngine.DAYS),
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                    )
                    val total = g.netWorth / StockEngine.START_CASH - 1f
                    Text(
                        stringResource(R.string.stock_cash, money(g.cash)) + "  ·  " + pct(total),
                        color = changeColor(total),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!g.over) {
                    IconButton(onClick = viewModel::togglePlay) {
                        Icon(
                            if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        if (state.fast) "2×" else "1×",
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.SurfaceHigh)
                            .clickable(onClick = viewModel::toggleSpeed)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    IconButton(onClick = viewModel::nextDay, enabled = !state.playing) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = null,
                            tint = if (state.playing) AppColors.TextSecondary.copy(alpha = 0.4f) else AppColors.TextPrimary,
                        )
                    }
                }
            }
            if (!state.playing && !g.over && g.day == 1) {
                Text(
                    stringResource(R.string.stock_how_to),
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Hisse sekmeleri
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StockDef.entries.forEach { d ->
                    val s = g.stock(d)
                    val selected = d == state.selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) AppColors.SurfaceHigh else AppColors.Background)
                            .border(2.dp, if (selected) Color(d.color) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { viewModel.select(d) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            d.code + if (g.tipActive) " " + trendArrow(s.trend) else "",
                            color = Color(d.color),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                        )
                        Text(money(s.price), color = AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(pct(s.dayChange), color = changeColor(s.dayChange), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Grafik
            val sel = g.stock(state.selected)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Background)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.selected.company, color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(money(sel.price), color = Color(state.selected.color), fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Canvas(Modifier.fillMaxWidth().height(170.dp).padding(top = 8.dp)) {
                    drawChart(sel.history, Color(state.selected.color), if (sel.owned > 0) sel.avgCost else null)
                }
                if (sel.owned > 0) {
                    val pl = sel.price / sel.avgCost - 1f
                    Text(
                        stringResource(R.string.stock_position, sel.owned, money(sel.avgCost), money(sel.owned * sel.price)) + "  ·  " + pct(pl),
                        color = changeColor(pl),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                } else {
                    Text(stringResource(R.string.stock_no_position), color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }

            // Al / Sat
            if (!g.over) {
                TradeRow(
                    label = stringResource(R.string.stock_buy),
                    color = Up,
                    options = listOf(Qty.ONE to "1", Qty.TEN to "10", Qty.HALF to "%50", Qty.MAX to stringResource(R.string.stock_max)),
                    enabled = { viewModel.quantity(it, buying = true) > 0 },
                    onClick = viewModel::buy,
                )
                TradeRow(
                    label = stringResource(R.string.stock_sell),
                    color = Down,
                    options = listOf(Qty.ONE to "1", Qty.TEN to "10", Qty.HALF to "%50", Qty.MAX to stringResource(R.string.stock_all)),
                    enabled = { viewModel.quantity(it, buying = false) > 0 },
                    onClick = viewModel::sell,
                )
                Text(
                    stringResource(R.string.stock_fee),
                    color = AppColors.TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )

                if (g.tipDay < 0) {
                    Button(
                        onClick = { showRewarded({ viewModel.useTip() }, adNotReady) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceHigh, contentColor = AppColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(46.dp),
                    ) {
                        Icon(Icons.Rounded.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppColors.Gold)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.stock_tip, StockEngine.TIP_DAYS), fontWeight = FontWeight.Bold)
                    }
                } else if (g.tipActive) {
                    Text(
                        stringResource(R.string.stock_tip_active, StockEngine.TIP_DAYS - (g.day - g.tipDay)),
                        color = AppColors.Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                val total = g.netWorth / StockEngine.START_CASH - 1f
                GameOverPanel(
                    score = g.score,
                    isNewRecord = state.isNewRecord,
                    onRestart = restartWithAd,
                    title = stringResource(R.string.stock_market_closed),
                    titleColor = if (total >= 0f) Up else Down,
                    detail = stringResource(R.string.stock_result, money(g.netWorth), pct(total)),
                    goldEarned = state.goldEarned,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            // Haberler
            Text(
                stringResource(R.string.stock_news),
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
            )
            if (g.news.isEmpty()) {
                Text(stringResource(R.string.stock_no_news), color = AppColors.TextSecondary, fontSize = 13.sp)
            }
            g.news.forEach { n -> NewsRow(n, isToday = n.day == g.day) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun trendArrow(t: Int) = when (t) {
    1 -> "↑"
    -1 -> "↓"
    else -> "→"
}

@Composable
private fun TradeRow(
    label: String,
    color: Color,
    options: List<Pair<Qty, String>>,
    enabled: (Qty) -> Boolean,
    onClick: (Qty) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.width(44.dp))
        options.forEach { (qty, text) ->
            val on = enabled(qty)
            Text(
                text,
                color = if (on) AppColors.OnPrimary else AppColors.TextSecondary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) color else AppColors.SurfaceHigh)
                    .clickable(enabled = on) { onClick(qty) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun NewsRow(n: NewsItem, isToday: Boolean) {
    val code = n.stock?.let { StockDef.valueOf(it).code } ?: ""
    val (res, positive) = when (n.newsType) {
        NewsType.TREND_UP -> R.string.stock_news_trend_up to true
        NewsType.TREND_FLAT -> R.string.stock_news_trend_flat to null
        NewsType.TREND_DOWN -> R.string.stock_news_trend_down to false
        NewsType.RUMOR_UP -> R.string.stock_news_rumor_up to true
        NewsType.RUMOR_DOWN -> R.string.stock_news_rumor_down to false
        NewsType.EARNINGS_GOOD -> R.string.stock_news_earnings_good to true
        NewsType.EARNINGS_BAD -> R.string.stock_news_earnings_bad to false
        NewsType.MARKET_BULL -> R.string.stock_news_market_bull to true
        NewsType.MARKET_BEAR -> R.string.stock_news_market_bear to false
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isToday) AppColors.SurfaceHigh else AppColors.Background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when (positive) {
                        true -> Up
                        false -> Down
                        null -> AppColors.TextSecondary
                    },
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(res, code),
            color = if (isToday) AppColors.TextPrimary else AppColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.stock_day_short, n.day), color = AppColors.TextSecondary, fontSize = 11.sp)
    }
}

private fun DrawScope.drawChart(history: List<Float>, color: Color, avgCost: Float?) {
    if (history.isEmpty()) return
    val values = if (avgCost != null) history + avgCost else history
    var min = values.min()
    var max = values.max()
    if (abs(max - min) < 0.01f) {
        min -= 0.5f
        max += 0.5f
    }
    val pad = (max - min) * 0.1f
    min -= pad
    max += pad
    fun y(v: Float) = size.height - (v - min) / (max - min) * size.height
    val steps = (StockEngine.HISTORY - 1).coerceAtLeast(1)
    fun x(i: Int) = i * size.width / steps

    // Yatay kilavuz cizgileri
    for (k in 1..3) {
        val gy = size.height * k / 4f
        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx())
    }

    val line = Path()
    history.forEachIndexed { i, v -> if (i == 0) line.moveTo(x(i), y(v)) else line.lineTo(x(i), y(v)) }
    if (history.size > 1) {
        val fill = Path().apply {
            addPath(line)
            lineTo(x(history.size - 1), size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
    }
    drawPath(line, color, style = Stroke(2.5.dp.toPx()))
    drawCircle(color, 4.dp.toPx(), Offset(x(history.size - 1), y(history.last())))

    if (avgCost != null) {
        drawLine(
            Color.White.copy(alpha = 0.6f),
            Offset(0f, y(avgCost)),
            Offset(size.width, y(avgCost)),
            1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
    }
}
