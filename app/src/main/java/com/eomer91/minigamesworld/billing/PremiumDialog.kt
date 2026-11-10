package com.eomer91.minigamesworld.billing

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.roundToInt

/** MainActivity saglar; onizlemede null */
val LocalBilling = staticCompositionLocalOf<BillingManager?> { null }

/** Reklamsiz Premium abonelik penceresi */
@Composable
fun PremiumDialog(onDismiss: () -> Unit) {
    val billing = LocalBilling.current ?: return
    val premium by billing.isPremium.collectAsStateWithLifecycle()
    val plans by billing.plans.collectAsStateWithLifecycle()
    val status by billing.status.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current

    LaunchedEffect(billing) {
        billing.refresh()
        billing.events.collect { e ->
            val msg = when (e) {
                BillingManager.Event.PURCHASED -> R.string.premium_thanks
                BillingManager.Event.PENDING -> R.string.premium_pending
                BillingManager.Event.RESTORED -> R.string.premium_restored
                BillingManager.Event.ERROR -> R.string.premium_error
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.widthIn(max = 460.dp).fillMaxWidth(0.92f).clip(RoundedCornerShape(24.dp)).background(AppColors.Background)
                .border(1.dp, AppColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("👑", fontSize = 44.sp)
            Text(stringResource(R.string.premium_title), color = AppColors.Gold, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Benefit("🚫", stringResource(R.string.premium_b1))
                Benefit("📵", stringResource(R.string.premium_b2))
                Benefit("🎁", stringResource(R.string.premium_b3))
                Benefit("❤️", stringResource(R.string.premium_b4))
            }
            Spacer(Modifier.height(16.dp))

            if (premium) {
                Text("✓ " + stringResource(R.string.premium_active), color = Color(0xFF4ADE80), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(10.dp))
                PremiumButton(stringResource(R.string.premium_manage), AppColors.Surface, AppColors.TextPrimary) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(billing.manageUrl())))
                }
            } else if (plans.isEmpty()) {
                Text(
                    stringResource(if (status == BillingManager.Status.CONNECTING) R.string.premium_loading else R.string.premium_unavailable),
                    color = AppColors.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                )
            } else {
                // Yillik planin aylik esdegerine gore tasarruf
                val monthly = plans.firstOrNull { BillingManager.periodDays(it.period) in 28..31 }
                var selected by remember(plans) { mutableStateOf(plans.lastOrNull { BillingManager.periodDays(it.period) >= 360 } ?: plans.first()) }
                for (p in plans) {
                    val days = BillingManager.periodDays(p.period)
                    val yearly = days >= 360
                    val save = if (yearly && monthly != null && monthly.priceMicros > 0)
                        (100 - p.priceMicros * 100.0 / (monthly.priceMicros * 12.0)).roundToInt() else 0
                    PlanCard(
                        title = stringResource(if (yearly) R.string.premium_yearly else R.string.premium_monthly),
                        price = stringResource(if (yearly) R.string.premium_per_year else R.string.premium_per_month, p.price),
                        badge = if (save >= 5) stringResource(R.string.premium_save, "$save%") else null,
                        trial = if (p.trialDays > 0) stringResource(R.string.premium_trial, p.trialDays) else null,
                        selected = selected === p,
                    ) { selected = p }
                }
                Spacer(Modifier.height(12.dp))
                val label = if (selected.trialDays > 0) stringResource(R.string.premium_start_trial) else stringResource(R.string.premium_subscribe)
                PremiumButton(label, AppColors.Gold, Color.Black) {
                    if (activity == null || !billing.launch(activity, selected)) {
                        Toast.makeText(context, R.string.premium_unavailable, Toast.LENGTH_LONG).show()
                    }
                }
                if (selected.trialDays > 0) {
                    val per = stringResource(if (BillingManager.periodDays(selected.period) >= 360) R.string.premium_per_year else R.string.premium_per_month, selected.price)
                    Text(stringResource(R.string.premium_then, selected.trialDays, per), color = AppColors.TextSecondary, fontSize = 12.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.premium_restore), color = AppColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { billing.refresh(userInitiated = true) }.padding(8.dp))
            Text(stringResource(R.string.premium_terms), color = AppColors.TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.premium_close), color = AppColors.TextSecondary, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDismiss).padding(10.dp))
        }
    }
}

@Composable
private fun Benefit(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp)
        Text(text, color = AppColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun PlanCard(title: String, price: String, badge: String?, trial: String?, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) AppColors.Gold.copy(alpha = 0.14f) else AppColors.Surface)
            .border(2.dp, if (selected) AppColors.Gold else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = AppColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (badge != null) {
                    Text(badge, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.Gold).padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Text(price, color = AppColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
            if (trial != null) Text("🎁 $trial", color = Color(0xFF4ADE80), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun PremiumButton(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
}
