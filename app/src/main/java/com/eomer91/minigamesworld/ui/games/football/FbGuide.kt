package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.theme.AppColors

/** Rehber bolumleri: (baslik, metin) */
private val GUIDE = listOf(
    R.string.fb_gt_basics to R.string.fb_gb_basics,
    R.string.fb_tac_mentality to R.string.fb_gb_mentality,
    R.string.fb_tac_passing to R.string.fb_gb_passing,
    R.string.fb_tac_tempo to R.string.fb_gb_tempo,
    R.string.fb_tac_width to R.string.fb_gb_width,
    R.string.fb_tac_focus to R.string.fb_gb_focus,
    R.string.fb_tac_pressing to R.string.fb_gb_pressing,
    R.string.fb_tac_defline to R.string.fb_gb_defline,
    R.string.fb_tac_marking to R.string.fb_gb_marking,
    R.string.fb_tac_counter to R.string.fb_gb_counter,
    R.string.fb_tac_offside to R.string.fb_gb_offside,
    R.string.fb_tac_time_wasting to R.string.fb_gb_time_wasting,
    R.string.fb_tac_hard_tackling to R.string.fb_gb_hard_tackling,
    R.string.fb_gt_formations to R.string.fb_gb_formations,
    R.string.fb_gt_training to R.string.fb_gb_training,
    R.string.fb_gt_coaches to R.string.fb_gb_coaches,
    R.string.fb_gt_morale to R.string.fb_gb_morale,
    R.string.fb_gt_youth to R.string.fb_gb_youth,
    R.string.fb_gt_finance to R.string.fb_gb_finance,
    R.string.fb_gt_transfers to R.string.fb_gb_transfers,
    R.string.fb_gt_contracts to R.string.fb_gb_contracts,
    R.string.fb_gt_competitions to R.string.fb_gb_competitions,
    R.string.fb_gt_hidden to R.string.fb_gb_hidden,
)

@Composable
fun FbGuide() {
    var sel by remember { mutableIntStateOf(0) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.width(220.dp).fillMaxHeight()) {
            LazyColumn {
                itemsIndexed(GUIDE) { i, (title, _) ->
                    Text(
                        stringResource(title), color = if (i == sel) AppColors.OnPrimary else AppColors.TextPrimary, fontSize = 13.sp,
                        fontWeight = if (i == sel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (i == sel) AppColors.Primary else Color.Transparent)
                            .clickable { sel = i }.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(GUIDE[sel].first)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(GUIDE[sel].second), color = AppColors.TextMuted, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}
