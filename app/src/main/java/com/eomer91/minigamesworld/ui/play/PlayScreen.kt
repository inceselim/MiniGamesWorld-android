package com.eomer91.minigamesworld.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.data.GameCatalog
import com.eomer91.minigamesworld.ui.games.blockpuzzle.BlockPuzzleScreen
import com.eomer91.minigamesworld.ui.games.bubble.BubbleScreen
import com.eomer91.minigamesworld.ui.games.game2048.Game2048Screen
import com.eomer91.minigamesworld.ui.games.match3.Match3Screen
import com.eomer91.minigamesworld.ui.games.memory.MemoryScreen
import com.eomer91.minigamesworld.ui.games.snake.SnakeScreen
import com.eomer91.minigamesworld.ui.games.stack.StackScreen
import com.eomer91.minigamesworld.ui.games.sudoku.SudokuScreen
import com.eomer91.minigamesworld.ui.theme.AppColors

/** Oyun kabugu: ust bar + oyunun kendisi. Tasinan her oyun buradaki when'e eklenir. */
@Composable
fun PlayScreen(gameId: String, onBack: () -> Unit) {
    val game = GameCatalog.find(gameId)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(AppColors.Surface)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextPrimary)
            }
            Text(
                game?.let { stringResource(it.nameRes) } ?: gameId,
                color = AppColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        Box(Modifier.fillMaxSize().padding(16.dp)) {
            when (gameId) {
                "2048" -> Game2048Screen()
                "snake" -> SnakeScreen()
                "memory-card" -> MemoryScreen()
                "block-puzzle" -> BlockPuzzleScreen()
                "sudoku" -> SudokuScreen()
                "stack" -> StackScreen()
                "match-3" -> Match3Screen()
                "bubble-shooter" -> BubbleScreen()
                else -> ComingSoon()
            }
        }
    }
}

@Composable
private fun ComingSoon() {
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = AppColors.Primary)
        Text(
            stringResource(R.string.coming_soon_title),
            color = AppColors.Primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            stringResource(R.string.coming_soon_body),
            color = AppColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
