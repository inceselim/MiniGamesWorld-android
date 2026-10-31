package com.eomer91.minigamesworld.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Background = Color(0xFF0B1020)
    val Surface = Color(0xFF111827)
    val SurfaceHigh = Color(0xFF1F2937)
    val Primary = Color(0xFF38BDF8)
    val OnPrimary = Color(0xFF020617)
    val Gold = Color(0xFFFACC15)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFFCBD5E1)
    val Danger = Color(0xFFF87171)
}

private val DarkScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    secondary = AppColors.Gold,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceHigh,
    onSurfaceVariant = AppColors.TextSecondary,
    surfaceContainer = AppColors.Surface,
    error = AppColors.Danger,
)

@Composable
fun MiniGamesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
