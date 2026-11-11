package com.eomer91.minigamesworld.ui.games.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.theme.AppColors

/** Oyunlarin ust kismi: skor, en iyi skor ve yeniden baslat. */
@Composable
fun ScoreHeader(
    score: Int,
    best: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    scoreLabel: String = stringResource(R.string.game_score),
    showBestAsMax: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ScoreBox(scoreLabel, score)
        Spacer(Modifier.width(10.dp))
        ScoreBox(stringResource(R.string.game_best), if (showBestAsMax) maxOf(score, best) else best)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onRestart,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
            modifier = Modifier.height(44.dp),
        ) {
            Text(stringResource(R.string.game_restart), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ScoreBox(label: String, value: Int) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value.toString(), color = AppColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

/** Oyun bitti paneli. */
@Composable
fun GameOverPanel(
    score: Int,
    isNewRecord: Boolean,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.game_over),
    titleColor: Color = AppColors.Danger,
    detail: String = stringResource(R.string.game_final_score, score),
    goldEarned: Int = 0,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.SurfaceHigh)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = titleColor, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(detail, color = AppColors.TextMuted, modifier = Modifier.padding(top = 6.dp))
        if (isNewRecord) {
            Text(stringResource(R.string.game_new_record), color = AppColors.Gold, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
        }
        Button(
            onClick = onRestart,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.game_play_again), fontWeight = FontWeight.Black)
        }
    }
}
