package com.eomer91.minigamesworld.ui.games.solitaire

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TableColor = Color(0xFF0B3B2E)
private val SlotColor = Color(0x33FFFFFF)
private val CardRed = Color(0xFFDC2626)
private val CardBlack = Color(0xFF111827)
private val HintColor = Color(0xFFFACC15)
private val SUITS = listOf("♠", "♥", "♦", "♣")

private fun rankText(rank: Int) = when (rank) {
    1 -> "A"
    11 -> "J"
    12 -> "Q"
    13 -> "K"
    else -> rank.toString()
}

/** Ekrandaki bir kartin yeri (piksel). */
private data class Placed(val card: Card, val pile: Pile, val index: Int, val x: Float, val y: Float, val z: Int)

/** Surukleme: [pile]/[index]'ten baslayan dizi [delta] kadar kaydirildi. */
private data class Drag(val pile: Pile, val index: Int, val delta: Offset)

@Composable
fun SolitaireScreen(viewModel: SolitaireViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val newGameWithAd = { showInterstitial { viewModel.backToChoosing() } }

    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val playing = state.phase == SolitairePhase.PLAYING
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(1_000)
                viewModel.tick()
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
            SolitairePhase.LOADING -> Unit
            SolitairePhase.CHOOSING -> DrawPicker(onPick = viewModel::start)
            else -> Column(Modifier.fillMaxSize().padding(10.dp)) {
                TopBar(
                    state = state,
                    onNewGame = newGameWithAd,
                    onUndo = viewModel::undo,
                    onHint = { showRewarded({ viewModel.showHint() }, adNotReady) },
                )
                val game = state.game
                if (game != null) {
                    Board(
                        state = state,
                        game = game,
                        viewModel = viewModel,
                        modifier = Modifier.padding(top = 8.dp).weight(1f),
                    )
                }
                if (state.phase == SolitairePhase.WON) {
                    GameOverPanel(
                        score = state.finalScore,
                        isNewRecord = state.isNewRecord,
                        onRestart = newGameWithAd,
                        title = stringResource(R.string.solitaire_won),
                        titleColor = Color(0xFF22C55E),
                        detail = stringResource(R.string.mines_result, "%d:%02d".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60), state.finalScore),
                        goldEarned = state.goldEarned,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else if (game != null && viewModel.engine.canAutoComplete(game) && !state.autoCompleting) {
                    Button(
                        onClick = viewModel::autoComplete,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(46.dp),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.solitaire_auto_finish), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DrawPicker(onPick: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.solitaire_choose), color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
        listOf(1 to Color(0xFF22C55E), 3 to AppColors.Primary).forEach { (n, accent) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.SurfaceHigh)
                    .border(2.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { onPick(n) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    stringResource(if (n == 1) R.string.solitaire_draw1 else R.string.solitaire_draw3),
                    color = accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    stringResource(if (n == 1) R.string.solitaire_draw1_info else R.string.solitaire_draw3_info),
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TopBar(state: SolitaireUiState, onNewGame: () -> Unit, onUndo: () -> Unit, onHint: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.td_score, state.game?.score ?: 0), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
            Text(
                "%d:%02d · %s".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60, stringResource(R.string.solitaire_moves, state.game?.moves ?: 0)),
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
        if (state.phase == SolitairePhase.PLAYING) {
            SmallAction(Icons.AutoMirrored.Rounded.Undo, enabled = state.canUndo, onClick = onUndo)
            Spacer(Modifier.width(6.dp))
            SmallAction(Icons.Rounded.Lightbulb, enabled = true, onClick = onHint, adBadge = true)
            Spacer(Modifier.width(6.dp))
        }
        SmallAction(Icons.Rounded.Refresh, enabled = true, onClick = onNewGame, highlighted = true)
    }
}

@Composable
private fun SmallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    adBadge: Boolean = false,
    highlighted: Boolean = false,
) {
    Box(
        Modifier
            .size(42.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) AppColors.Primary else AppColors.SurfaceHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (highlighted) AppColors.OnPrimary else AppColors.TextPrimary, modifier = Modifier.size(22.dp))
        if (adBadge) {
            Icon(
                Icons.Rounded.OndemandVideo,
                contentDescription = null,
                tint = AppColors.Gold,
                modifier = Modifier.size(13.dp).align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun Board(state: SolitaireUiState, game: SolitaireGame, viewModel: SolitaireViewModel, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    var drag by remember { mutableStateOf<Drag?>(null) }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TableColor),
    ) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val gap = with(density) { 4.dp.toPx() }
        val cardW = (w - gap * 8) / 7f
        val cardH = cardW * 1.42f
        val topY = gap
        val tableauY = topY + cardH + with(density) { 12.dp.toPx() }
        fun colX(i: Int) = gap + i * (cardW + gap)

        // Tableau kart araliklari: sutun ekrana sigmiyorsa sikistir.
        val available = h - tableauY - gap - cardH
        fun steps(col: List<Card>): Pair<Float, Float> {
            var down = cardH * 0.13f
            var up = cardH * 0.27f
            val downs = col.count { !it.faceUp }
            val ups = (col.size - downs - 1).coerceAtLeast(0)
            val need = downs * down + ups * up
            if (need > available && need > 0f) {
                val k = (available / need).coerceAtLeast(0.35f)
                down *= k
                up *= k
            }
            return down to up
        }

        // Tum kartlarin yerleri
        val placed = ArrayList<Placed>(52)
        var z = 0
        game.stock.forEachIndexed { i, c -> placed += Placed(c, Pile.Stock, i, colX(0), topY, z++) }
        // 3 kart modunda acik kartlarin son 3'u yelpaze gibi gosterilir.
        val wasteShown = minOf(if (game.drawCount == 3) 3 else 1, game.waste.size)
        game.waste.forEachIndexed { i, c ->
            val fromTop = game.waste.lastIndex - i
            val fan = if (fromTop < wasteShown) wasteShown - 1 - fromTop else 0
            placed += Placed(c, Pile.Waste, i, colX(1) + fan * cardW * 0.28f, topY, z++)
        }
        game.foundations.forEachIndexed { f, pile -> pile.forEachIndexed { i, c -> placed += Placed(c, Pile.Foundation(f), i, colX(3 + f), topY, z++) } }
        game.tableau.forEachIndexed { t, col ->
            val (down, up) = steps(col)
            var y = tableauY
            col.forEachIndexed { i, c ->
                placed += Placed(c, Pile.Tableau(t), i, colX(t), y, z++)
                y += if (c.faceUp) up else down
            }
        }

        fun rectOf(p: Placed) = Rect(p.x, p.y, p.x + cardW, p.y + cardH)
        fun hitTest(point: Offset): Placed? = placed.lastOrNull { rectOf(it).contains(point) }

        fun dropTarget(center: Offset): Pile? {
            for (f in 0 until 4) if (Rect(colX(3 + f), topY, colX(3 + f) + cardW, topY + cardH).inflate(gap * 2).contains(center)) return Pile.Foundation(f)
            for (t in 0 until 7) if (center.x in colX(t) - gap..colX(t) + cardW + gap && center.y >= tableauY - gap) return Pile.Tableau(t)
            return null
        }

        // Bos yiginlarin yerleri (deste, hedefler, bos sutunlar)
        val cardWDp: Dp = with(density) { cardW.toDp() }
        val cardHDp: Dp = with(density) { cardH.toDp() }
        EmptySlot(colX(0), topY, cardWDp, cardHDp, label = if (game.stock.isEmpty() && game.waste.isNotEmpty()) "↻" else "", highlight = state.hintStock)
        (0 until 4).forEach { f -> EmptySlot(colX(3 + f), topY, cardWDp, cardHDp, label = SUITS[f]) }
        (0 until 7).forEach { t -> if (game.tableau[t].isEmpty()) EmptySlot(colX(t), tableauY, cardWDp, cardHDp, label = "K") }

        // Dokunus + surukleme katmani
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(game) {
                    detectTapGestures { point ->
                        val hit = hitTest(point)
                        when {
                            hit == null && Rect(colX(0), topY, colX(0) + cardW, topY + cardH).contains(point) -> viewModel.onStockTap()
                            hit == null -> Unit
                            hit.pile == Pile.Stock -> viewModel.onStockTap()
                            !hit.card.faceUp -> Unit
                            hit.pile == Pile.Waste && hit.index != game.waste.lastIndex -> Unit
                            else -> viewModel.onCardTap(hit.pile, hit.index)
                        }
                    }
                }
                .pointerInput(game) {
                    var startCard: Placed? = null
                    detectDragGestures(
                        onDragStart = { point ->
                            val hit = hitTest(point)
                            val draggable = hit != null && hit.card.faceUp && hit.pile != Pile.Stock &&
                                (hit.pile is Pile.Tableau || hit.index == game.pile(hit.pile).lastIndex)
                            startCard = if (draggable) hit else null
                            drag = startCard?.let { Drag(it.pile, it.index, Offset.Zero) }
                        },
                        onDrag = { change, amount ->
                            if (startCard != null) {
                                change.consume()
                                drag = drag?.let { it.copy(delta = it.delta + amount) }
                            }
                        },
                        onDragEnd = {
                            val d = drag
                            val s = startCard
                            if (d != null && s != null) {
                                val center = Offset(s.x + cardW / 2 + d.delta.x, s.y + cardH / 2 + d.delta.y)
                                dropTarget(center)?.let { target -> viewModel.onDrop(d.pile, d.index, target) }
                            }
                            drag = null
                            startCard = null
                        },
                        onDragCancel = {
                            drag = null
                            startCard = null
                        },
                    )
                },
        )

        // Kartlar
        val hint = state.hint
        placed.forEach { p ->
            val dragging = drag?.let { d -> d.pile == p.pile && p.index >= d.index } == true
            val isHinted = hint != null && hint.from == p.pile && p.index >= hint.fromIndex
            key(p.card.id) {
                CardView(
                    placed = p,
                    width = cardWDp,
                    height = cardHDp,
                    dragDelta = if (dragging) drag!!.delta else Offset.Zero,
                    dragging = dragging,
                    highlighted = isHinted,
                )
            }
        }
    }
}

@Composable
private fun EmptySlot(x: Float, y: Float, width: Dp, height: Dp, label: String, highlight: Boolean = false) {
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(width, height)
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, if (highlight) HintColor else SlotColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = SlotColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CardView(placed: Placed, width: Dp, height: Dp, dragDelta: Offset, dragging: Boolean, highlighted: Boolean) {
    val target = IntOffset((placed.x + dragDelta.x).roundToInt(), (placed.y + dragDelta.y).roundToInt())
    // Surukleme sirasinda parmagi aninda takip et; birakinca yerine yumusakca gitsin.
    val animated by animateIntOffsetAsState(target, animationSpec = tween(if (dragging) 0 else 180), label = "card")
    val card = placed.card
    val shape = RoundedCornerShape(width * 0.12f)

    Box(
        Modifier
            .zIndex(if (dragging) 1000f + placed.z else placed.z.toFloat())
            .offset { animated }
            .size(width, height)
            .clip(shape)
            .then(
                if (card.faceUp) Modifier.background(Color.White)
                else Modifier.background(Brush.linearGradient(listOf(Color(0xFF1D4ED8), Color(0xFF1E3A8A)))),
            )
            .border(if (highlighted) 2.5.dp else 1.dp, if (highlighted) HintColor else Color.Black.copy(alpha = 0.25f), shape),
    ) {
        if (card.faceUp) {
            val color = if (card.isRed) CardRed else CardBlack
            val density = LocalDensity.current
            val small = with(density) { (width * 0.30f).toSp() }
            val big = with(density) { (width * 0.55f).toSp() }
            Column(Modifier.padding(start = width * 0.06f, top = width * 0.02f)) {
                Text(rankText(card.rank), color = color, fontSize = small, fontWeight = FontWeight.Black, lineHeight = small)
                Text(SUITS[card.suit], color = color, fontSize = small, lineHeight = small)
            }
            Text(SUITS[card.suit], color = color, fontSize = big, modifier = Modifier.align(Alignment.BottomEnd).padding(end = width * 0.06f))
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(width * 0.1f)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(width * 0.08f)),
            )
        }
    }
}
