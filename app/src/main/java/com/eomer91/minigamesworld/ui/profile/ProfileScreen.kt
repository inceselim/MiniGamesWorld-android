package com.eomer91.minigamesworld.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.data.GameCatalog
import com.eomer91.minigamesworld.ui.components.ScreenTitle
import com.eomer91.minigamesworld.ui.theme.AppColors

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val scores by viewModel.highScores.collectAsStateWithLifecycle()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.tab_profile))

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(AppColors.SurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = AppColors.Primary)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.profile_guest),
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                stringResource(R.string.profile_guest_body),
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 12.dp),
            )
            // Google ile giris: online/multiplayer asamasinda Credential Manager ile eklenecek.
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text("${stringResource(R.string.profile_sign_in)} · ${stringResource(R.string.soon)}")
            }
        }

        Card {
            Text(stringResource(R.string.profile_best_scores), color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            val rows = GameCatalog.games.mapNotNull { game -> scores[game.id]?.let { game to it } }
            if (rows.isEmpty()) {
                Text(stringResource(R.string.profile_no_scores), color = AppColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))
            } else {
                rows.forEach { (game, score) ->
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Text(stringResource(game.nameRes), color = AppColors.TextMuted, modifier = Modifier.weight(1f))
                        Text(score.toString(), color = AppColors.Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .padding(16.dp),
    ) { content() }
}
