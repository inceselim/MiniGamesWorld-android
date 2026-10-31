package com.eomer91.minigamesworld.ui.games.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/**
 * Kaydirma hareketlerini yon olarak bildirir.
 * [continuous] = true ise parmak kalkmadan da esik asildikca yeni yon gonderilir
 * (Snake gibi hizli donus gereken oyunlar icin).
 */
fun Modifier.swipeGestures(
    threshold: Dp = 24.dp,
    continuous: Boolean = false,
    onSwipe: (SwipeDirection) -> Unit,
): Modifier = pointerInput(continuous) {
    val thresholdPx = threshold.toPx()
    var total = Offset.Zero

    fun directionOf(offset: Offset): SwipeDirection? {
        val (dx, dy) = offset
        if (maxOf(abs(dx), abs(dy)) < thresholdPx) return null
        return if (abs(dx) > abs(dy)) {
            if (dx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
        } else {
            if (dy > 0) SwipeDirection.DOWN else SwipeDirection.UP
        }
    }

    detectDragGestures(
        onDragStart = { total = Offset.Zero },
        onDrag = { change, amount ->
            change.consume()
            total += amount
            if (continuous) {
                directionOf(total)?.let {
                    onSwipe(it)
                    total = Offset.Zero
                }
            }
        },
        onDragEnd = {
            if (!continuous) directionOf(total)?.let(onSwipe)
            total = Offset.Zero
        },
    )
}
