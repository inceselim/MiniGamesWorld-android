package com.eomer91.minigamesworld.ui.games.island

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.eomer91.minigamesworld.ui.games.football.FbButton
import com.eomer91.minigamesworld.ui.games.football.FbColors
import com.eomer91.minigamesworld.ui.games.island.engine.Dirty
import com.eomer91.minigamesworld.ui.games.island.engine.IsAuction
import com.eomer91.minigamesworld.ui.games.island.engine.IsRivals
import com.eomer91.minigamesworld.ui.games.island.engine.IsSim
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.max
import kotlin.math.min

// ====================================================================== pazarlama

@Composable
fun MarketingPanel(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    val me = w.companies[w.userId]
    IsPanelFrame(stringResource(R.string.is_marketing), { vm.closePanel() }) {
        for (c in 0..2) {
            val left = me.adUntil[c] - w.day
            val cost = IsRivals.adCost(w, c)
            val cls = stringResource(clsName(c))
            IsCard(highlight = left > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(CLASS_COLORS[c]))
                    Text(stringResource(R.string.is_ad_title, cls), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp))
                }
                Text(stringResource(R.string.is_ad_desc, cls), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                if (left > 0) Text(stringResource(R.string.is_ad_active, left), color = FbColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                FbButton(stringResource(if (left > 0) R.string.is_ad_extend else R.string.is_ad_start, IsFmt.money(cost)), { vm.startAd(c) },
                    Modifier.fillMaxWidth(), enabled = me.balance >= cost)
            }
        }
        IsSection(stringResource(R.string.is_rival_ads))
        var any = false
        for (co in w.companies) {
            if (co.id == w.userId || !co.alive) continue
            for (c in 0..2) {
                val left = co.adUntil[c] - w.day
                if (left <= 0) continue
                any = true
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(co.color)))
                    Text(co.name, color = AppColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 6.dp), maxLines = 1)
                    Text(stringResource(clsName(c)) + " · " + stringResource(R.string.is_days, left), color = AppColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
        if (!any) Text(stringResource(R.string.is_no_rival_ads), color = AppColors.TextSecondary, fontSize = 12.sp)

        IsSection(stringResource(R.string.is_rep_title) + ": %.0f / 100".format(me.reputation))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(FbColors.Line)) {
            Box(Modifier.fillMaxWidth((me.reputation / 100f).coerceIn(0f, 1f)).height(8.dp).background(FbColors.Yellow))
        }
        Text(stringResource(R.string.is_rep_tip), color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

// ====================================================================== kirli isler

@Composable
fun DirtyCard(w: IsWorld, d: Dirty, vm: IslandViewModel, target: Int) {
    val cost = IsRivals.dirtyCost(w, d)
    val wait = IsRivals.dirtyReadyIn(w, w.userId)
    val (title, desc) = when (d) {
        Dirty.SABOTAGE -> R.string.is_d_sabotage to R.string.is_d_sabotage_desc
        Dirty.PROPAGANDA -> R.string.is_d_propaganda to R.string.is_d_propaganda_desc
        Dirty.STRIKE -> R.string.is_d_strike to R.string.is_d_strike_desc
    }
    // Basarili olsa da olmasa da yakalanabilir (basarisizlikta risk 1.5 kat)
    val risk = d.success * d.caught + (1 - d.success) * min(1f, d.caught * 1.5f)
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x33F87171))
            .border(1.dp, FbColors.Red.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).padding(8.dp),
    ) {
        Text("🕶️ " + stringResource(title), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text(stringResource(desc), color = AppColors.TextMuted, fontSize = 12.sp)
        Text(stringResource(R.string.is_d_odds, IsFmt.pct(d.success), IsFmt.pct(risk)), color = FbColors.Orange, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        Text(stringResource(R.string.is_d_penalty, IsFmt.money(cost * 3)), color = AppColors.TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        if (wait > 0) {
            Text(stringResource(R.string.is_d_cooldown, wait), color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            FbButton(stringResource(R.string.is_d_go, IsFmt.money(cost)), { vm.dirty(d, target) }, Modifier.fillMaxWidth(),
                color = FbColors.Red, textColor = Color.White, enabled = IsRivals.canDirty(w, w.userId, d))
        }
    }
}

// ====================================================================== acik artirma

@Composable
fun AuctionCard(w: IsWorld, a: IsAuction, vm: IslandViewModel, showMapButton: Boolean) {
    val b = w.buildings.getOrNull(a.building) ?: return
    val lead = a.topBidder == w.userId
    val next = IsRivals.nextBid(a)
    IsCard(highlight = lead) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BuildingIcon(b.type, w.companies.getOrNull(b.owner)?.color ?: 0xFF90A4AE.toInt(), 52.dp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("🔨 " + stringResource(bName(b.type)), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(stringResource(R.string.is_auction_ends, max(0, a.endDay - w.day)), color = FbColors.Orange, fontSize = 12.sp)
            }
        }
        IsKV(stringResource(R.string.is_auction_value), IsFmt.money(IsSim.buildingValue(b)))
        if (a.topBidder < 0) {
            Text(stringResource(R.string.is_auction_none, IsFmt.money(a.minBid)), color = AppColors.TextSecondary, fontSize = 12.sp)
        } else {
            IsKV(stringResource(R.string.is_auction_top), stringResource(R.string.is_auction_by, IsFmt.money(a.topBid), companyName(w, a.topBidder)),
                if (lead) FbColors.Green else FbColors.Yellow)
        }
        Spacer(Modifier.height(6.dp))
        if (lead) {
            Text("✓ " + stringResource(R.string.is_auction_lead), color = FbColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            FbButton(stringResource(R.string.is_bid, IsFmt.money(next)), { vm.bid(a.id) }, Modifier.fillMaxWidth(),
                enabled = w.companies[w.userId].balance >= next)
        }
        if (showMapButton) {
            Spacer(Modifier.height(6.dp))
            FbButton("📍 " + stringResource(R.string.is_show_on_map), { vm.showBuilding(b.id) }, Modifier.fillMaxWidth(),
                color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
        }
    }
}

// ====================================================================== rehber

@Composable
fun GuidePanel(vm: IslandViewModel) {
    val sections = listOf(
        R.string.is_g_goal_t to R.string.is_g_goal,
        R.string.is_g_tips_t to R.string.is_g_tips,
        R.string.is_g_tourists_t to R.string.is_g_tourists,
        R.string.is_g_season_t to R.string.is_g_season,
        R.string.is_g_stay_t to R.string.is_g_stay,
        R.string.is_g_loc_t to R.string.is_g_loc,
        R.string.is_g_price_t to R.string.is_g_price,
        R.string.is_g_land_t to R.string.is_g_land,
        R.string.is_g_money_t to R.string.is_g_money,
        R.string.is_g_rivals_t to R.string.is_g_rivals,
        R.string.is_g_ads_t to R.string.is_g_ads,
    )
    IsPanelFrame("❓ " + stringResource(R.string.is_guide), { vm.closePanel() }) {
        for ((t, body) in sections) {
            IsSection(stringResource(t))
            Text(stringResource(body), color = AppColors.TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

// ====================================================================== ortak kart

@Composable
fun IsCard(highlight: Boolean = false, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp))
            .background(if (highlight) AppColors.Primary.copy(alpha = 0.16f) else FbColors.PanelHigh)
            .border(1.dp, if (highlight) AppColors.Primary.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(12.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(10.dp),
    ) { content() }
}
