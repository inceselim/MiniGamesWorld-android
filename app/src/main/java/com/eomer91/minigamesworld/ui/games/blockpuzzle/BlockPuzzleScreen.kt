package com.eomer91.minigamesworld.ui.games.blockpuzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.roundToInt

private val BoardColor = Color(0xFF020617)
private val EmptyCellColor = Color(0xFF1E293B)
private val ValidPreviewColor = Color(0x8CFACC15)
private val InvalidPreviewColor = Color(0x8CEF4444)
private val PieceColors = listOf(
    Color(0xFF38BDF8),
    Color(0xFFFACC15),
    Color(0xFFF472B6),
    Color(0xFF34D399),
    Color(0xFFA78BFA),
    Color(0xFFFB923C),
)
private val BoardGap = 3.dp
private val TrayCell = 20.dp

/** Surukleme sirasinda parca parmagin bu kadar ustunde durur, parmak parcayi kapatmaz. */
private val DragLift = 64.dp

private fun colorOf(index: Int): Color = PieceColors[(index - 1).mod(PieceColors.size)]

/** Tahtanin ic alaninin ekrandaki (root) konumu ve hucre olculeri, piksel. */
private data class BoardGeometry(val origin: Offset, val cellPx: Float, val gapPx: Float) {
    val step: Float get() = cellPx + gapPx
}

private data class DragState(val slot: Int, val piece: BlockPiece, val pointer: Offset)

private data class Placement(val piece: BlockPiece, val row: Int, val col: Int, val valid: Boolean)

@Composable
fun BlockPuzzleScreen(viewModel: BlockPuzzleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }

    val density = LocalDensity.current
    val gapPx = with(density) { BoardGap.toPx() }
    val liftPx = with(density) { DragLift.toPx() }

    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var geometry by remember { mutableStateOf<BoardGeometry?>(null) }
    var drag by remember { mutableStateOf<DragState?>(null) }

    fun pieceSizePx(shape: Shape, g: BoardGeometry) = Size(
        shape.cols * g.cellPx + (shape.cols - 1) * g.gapPx,
        shape.rows * g.cellPx + (shape.rows - 1) * g.gapPx,
    )

    /** Surukelenen parcanin sol ust kosesi (root koordinatlari). */
    fun pieceTopLeft(d: DragState, g: BoardGeometry): Offset {
        val size = pieceSizePx(d.piece.shape, g)
        return d.pointer - Offset(size.width / 2f, size.height + liftPx)
    }

    /** Parca su an birakilirsa hangi hucreye oturur? Tahtanin disindaysa null. */
    fun placementFor(d: DragState, g: BoardGeometry): Placement? {
        val local = pieceTopLeft(d, g) - g.origin
        val col = (local.x / g.step).roundToInt()
        val row = (local.y / g.step).roundToInt()
        val shape = d.piece.shape
        val n = viewModel.boardSize
        if (row < 0 || col < 0 || row + shape.rows > n || col + shape.cols > n) return null
        return Placement(d.piece, row, col, viewModel.canPlace(shape, row, col))
    }

    val currentDrag = drag
    val currentGeometry = geometry
    val placement = if (currentDrag != null && currentGeometry != null) placementFor(currentDrag, currentGeometry) else null

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .onGloballyPositioned { rootOrigin = it.positionInRoot() },
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScoreHeader(score = state.score, best = state.best, onRestart = restartWithAd)

            Box(
                Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BoardColor)
                    .padding(6.dp),
            ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            val n = viewModel.boardSize
                            val cell = (coords.size.width - gapPx * (n - 1)) / n
                            geometry = BoardGeometry(coords.positionInRoot(), cell, gapPx)
                        },
                ) {
                    val n = viewModel.boardSize
                    val cell = (size.width - gapPx * (n - 1)) / n
                    val step = cell + gapPx
                    val radius = CornerRadius(cell * 0.22f)

                    state.board.forEachIndexed { r, line ->
                        line.forEachIndexed { c, v ->
                            drawRoundRect(
                                color = if (v == 0) EmptyCellColor else colorOf(v),
                                topLeft = Offset(c * step, r * step),
                                size = Size(cell, cell),
                                cornerRadius = radius,
                            )
                        }
                    }

                    placement?.let { p ->
                        val color = if (p.valid) ValidPreviewColor else InvalidPreviewColor
                        p.piece.shape.cells.forEach { (r, c) ->
                            drawRoundRect(
                                color = color,
                                topLeft = Offset((p.col + c) * step, (p.row + r) * step),
                                size = Size(cell, cell),
                                cornerRadius = radius,
                            )
                        }
                    }
                }
            }

            if (state.gameOver) {
                GameOverPanel(
                    score = state.score,
                    isNewRecord = state.isNewRecord,
                    onRestart = restartWithAd,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.pieces.forEachIndexed { slot, piece ->
                        var slotOrigin by remember(slot) { mutableStateOf(Offset.Zero) }
                        val isDragged = drag?.slot == slot
                        val canPlaceSomewhere = state.placeable.getOrElse(slot) { false }

                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppColors.SurfaceHigh.copy(alpha = 0.4f))
                                .onGloballyPositioned { slotOrigin = it.positionInRoot() }
                                .pointerInput(slot, piece) {
                                    if (piece == null) return@pointerInput
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            drag = DragState(slot, piece, slotOrigin + offset)
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            drag = drag?.let { it.copy(pointer = it.pointer + amount) }
                                        },
                                        onDragEnd = {
                                            val d = drag
                                            val g = geometry
                                            if (d != null && g != null) {
                                                val target = placementFor(d, g)
                                                if (target != null && target.valid) {
                                                    viewModel.place(d.slot, target.row, target.col)
                                                }
                                            }
                                            drag = null
                                        },
                                        onDragCancel = { drag = null },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (piece != null && !isDragged) {
                                TrayPiece(piece, dimmed = !canPlaceSomewhere)
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.block_hint),
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }

        // Surukelenen parca: tum ekranin ustunde, parmagin biraz yukarisinda tam boyutta cizilir.
        if (currentDrag != null && currentGeometry != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawPiece(
                    piece = currentDrag.piece,
                    topLeft = pieceTopLeft(currentDrag, currentGeometry) - rootOrigin,
                    cellPx = currentGeometry.cellPx,
                    gapPx = currentGeometry.gapPx,
                    alpha = 0.95f,
                )
            }
        }
    }
}

@Composable
private fun TrayPiece(piece: BlockPiece, dimmed: Boolean) {
    val density = LocalDensity.current
    val cellPx = with(density) { TrayCell.toPx() }
    val gapPx = with(density) { 2.dp.toPx() }
    val widthDp = with(density) { (piece.shape.cols * cellPx + (piece.shape.cols - 1) * gapPx).toDp() }
    val heightDp = with(density) { (piece.shape.rows * cellPx + (piece.shape.rows - 1) * gapPx).toDp() }

    Canvas(Modifier.padding(4.dp).fillMaxWidth().height(heightDp)) {
        val left = (size.width - widthDp.toPx()) / 2f
        drawPiece(piece, Offset(left, 0f), cellPx, gapPx, alpha = if (dimmed) 0.3f else 1f)
    }
}

private fun DrawScope.drawPiece(piece: BlockPiece, topLeft: Offset, cellPx: Float, gapPx: Float, alpha: Float) {
    val color = colorOf(piece.color).copy(alpha = alpha)
    val radius = CornerRadius(cellPx * 0.22f)
    piece.shape.cells.forEach { (r, c) ->
        drawRoundRect(
            color = color,
            topLeft = topLeft + Offset(c * (cellPx + gapPx), r * (cellPx + gapPx)),
            size = Size(cellPx, cellPx),
            cornerRadius = radius,
        )
    }
}
