package com.eomer91.minigamesworld.ui.games.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.FbButton
import com.eomer91.minigamesworld.ui.games.football.FbChip
import com.eomer91.minigamesworld.ui.games.football.FbColors
import com.eomer91.minigamesworld.ui.games.island.engine.BType
import com.eomer91.minigamesworld.ui.games.island.engine.Cat
import com.eomer91.minigamesworld.ui.games.island.engine.Company
import com.eomer91.minigamesworld.ui.games.island.engine.Dirty
import com.eomer91.minigamesworld.ui.games.island.engine.Fin
import com.eomer91.minigamesworld.ui.games.island.engine.IsCatalog
import com.eomer91.minigamesworld.ui.games.island.engine.IsRivals
import com.eomer91.minigamesworld.ui.games.island.engine.IsSim
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import com.eomer91.minigamesworld.ui.games.island.engine.Terrain
import com.eomer91.minigamesworld.ui.games.island.render.AndroidIsoPainter
import com.eomer91.minigamesworld.ui.games.island.render.IsoArt
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.roundToInt

// ====================================================================== ortak

@Composable
internal fun IsPanelFrame(title: String, onClose: () -> Unit, header: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color(0xF2111827))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(30.dp).clip(CircleShape).background(FbColors.PanelHigh).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Text("✕", color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        header?.invoke()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { content() }
    }
}

@Composable
internal fun IsSection(text: String) {
    Text(text, color = AppColors.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
}

/** Binanin kucuk resmi: haritadaki cizimin aynisi, kutuya sigdirilmis */
@Composable
fun BuildingIcon(t: BType, color: Int, size: Dp) {
    val painter = remember { AndroidIsoPainter(true) }
    Canvas(Modifier.size(size)) {
        val box = IsoArt.spriteBox(t, 64f, 32f)
        val k = minOf(this.size.width / box.w, this.size.height / box.h)
        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            nc.save()
            nc.translate((this.size.width - box.w * k) / 2f, (this.size.height - box.h * k) / 2f)
            nc.scale(k, k)
            painter.canvas = nc
            IsoArt.building(painter, t, box.ox, box.oy, 64f, 32f, color)
            painter.canvas = null
            nc.restore()
        }
    }
}

/** Sinif basina cekicilik cubugu */
@Composable
private fun AppealBars(t: BType) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (c in 0..2) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(clsName(c)), color = AppColors.TextSecondary, fontSize = 9.sp, maxLines = 1)
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(FbColors.Line)) {
                    Box(Modifier.fillMaxWidth(t.appeal[c].coerceIn(0f, 1f)).height(5.dp).background(CLASS_COLORS[c]))
                }
            }
        }
    }
}

internal val CLASS_COLORS = listOf(Color(0xFF4ADE80), Color(0xFF38BDF8), Color(0xFFFACC15))

// ====================================================================== insa menusu

@Composable
fun BuildPanel(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    val me = w.companies[w.userId]
    IsPanelFrame(stringResource(R.string.is_build), { vm.closePanel() }, header = {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (c in BUILD_CATS) FbChip(catIcon(c) + " " + stringResource(catName(c)), ui.buildCat == c, { vm.setBuildCat(c) })
        }
    }) {
        for (t in IsCatalog.types.filter { it.buildable && it.cat == ui.buildCat }) {
            val affordable = me.balance >= t.cost
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp)).background(FbColors.PanelHigh)
                    .clickable { vm.startPlacing(t.id) }.padding(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BuildingIcon(t, me.color, 64.dp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(stringResource(bName(t)), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text(IsFmt.money(t.cost), color = if (affordable) FbColors.Green else FbColors.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${t.w}×${t.h} · " + stringResource(R.string.is_days, t.days) + " · " + stringResource(R.string.is_upkeep) + " " + IsFmt.money(t.upkeep),
                            color = AppColors.TextSecondary, fontSize = 11.sp)
                        if (t.cap > 0) {
                            val capLabel = stringResource(if (t.cat == Cat.STAY) R.string.is_capacity_beds else R.string.is_capacity_visits)
                            Text("$capLabel ${t.cap} · " + IsFmt.price(t.price), color = AppColors.TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
                if (t.cap > 0) { Spacer(Modifier.height(6.dp)); AppealBars(t) }
                if (t.noise >= 0.4f) Text("🔊 " + stringResource(R.string.is_noisy), color = FbColors.Orange, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                if (t.beauty >= 0.5f) Text("🌸 " + stringResource(R.string.is_beautifies), color = FbColors.Green, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                if (t.needsBeach) Text("🏖️ " + stringResource(R.string.is_needs_beach), color = FbColors.Blue, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ====================================================================== bina

@Composable
fun BuildingPanel(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    val b = w.buildings.getOrNull(ui.selBuilding)
    if (b == null) { LaunchedEffect(Unit) { vm.closePanel() }; return }
    val t = b.type
    val mine = b.owner == w.userId
    val ownerColor = if (b.owner >= 0) w.companies[b.owner].color else 0xFF90A4AE.toInt()
    var confirmDemolish by remember(b.id) { mutableStateOf(false) }

    IsPanelFrame(stringResource(bName(t)), { vm.closePanel() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BuildingIcon(t, ownerColor, 84.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(ownerColor)))
                    val ownerName = if (b.owner < 0) stringResource(R.string.is_public)
                        else if (mine) companyName(w, b.owner) + " (" + stringResource(R.string.is_you) + ")" else companyName(w, b.owner)
                    Text(ownerName,
                        color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
                }
                val status = when {
                    b.buildLeft > 0 && b.level > 0 -> stringResource(R.string.is_upgrading, b.buildLeft)
                    b.buildLeft > 0 -> stringResource(R.string.is_under_construction, b.buildLeft)
                    b.closedDays > 0 -> stringResource(R.string.is_closed_days, b.closedDays)
                    else -> stringResource(R.string.is_open)
                }
                Text(status, color = if (b.open) FbColors.Green else FbColors.Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (t.cap > 0) Text(stringResource(R.string.is_level) + " ${b.level + 1}/3 · " + stringResource(R.string.is_quality) + " " + "★".repeat(b.quality.roundToInt().coerceIn(1, 5)),
                    color = FbColors.Yellow, fontSize = 12.sp)
            }
        }
        if (b.owner < 0) return@IsPanelFrame
        if (!mine) {
            val auction = w.auctions.firstOrNull { !it.done && it.building == b.id }
            if (auction != null) {
                IsSection(stringResource(R.string.is_in_auction))
                AuctionCard(w, auction, vm, showMapButton = false)
                Text(stringResource(R.string.is_auction_tip), color = AppColors.TextSecondary, fontSize = 11.sp)
            } else if (w.companies[b.owner].alive && b.open && t.cap > 0 && w.companies[w.userId].alive) {
                DirtyCard(w, Dirty.SABOTAGE, vm, b.id)
            }
        }

        if (t.cap > 0) {
            IsSection(stringResource(R.string.is_by_class))
            val used = b.today.sum()
            val cap = b.capacity.toFloat()
            IsKV(stringResource(if (t.cat == Cat.STAY) R.string.is_guests_today else R.string.is_visits_today), IsFmt.count(used) + " / " + b.capacity)
            OccupancyBar(if (cap > 0) used / cap else 0f)
            for (c in 0..2) {
                val loc = ((b.loc[c] + 0.2f) / 1.9f * 5f).coerceIn(0f, 5f)
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(CLASS_COLORS[c]))
                    Text(stringResource(clsName(c)), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 6.dp))
                    Text(IsFmt.count(b.today[c]), color = AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                    Text("📍" + "%.1f".format(loc), color = AppColors.TextSecondary, fontSize = 11.sp)
                }
            }
            IsSection(stringResource(R.string.is_finance))
            IsKV(stringResource(R.string.is_rev_this_month), IsFmt.money(b.monthRevenue), FbColors.Green)
            IsKV(stringResource(R.string.is_rev_last_month), IsFmt.money(b.lastMonthRevenue))
        }
        IsKV(stringResource(R.string.is_upkeep), IsFmt.money((t.upkeep * (1 + 0.2 * b.level)).toLong()), FbColors.Red)

        if (mine && t.cap > 0) {
            IsSection(stringResource(if (t.cat == Cat.STAY) R.string.is_price_night else R.string.is_price_visit))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepButton("−") { vm.setPrice(b.id, step(b.price, -1)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(IsFmt.price(b.price), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    val rel = b.price / t.price
                    Text("%+.0f%%".format((rel - 1f) * 100f), color = when { rel < 0.97f -> FbColors.Green; rel > 1.03f -> FbColors.Orange; else -> AppColors.TextSecondary }, fontSize = 11.sp)
                }
                StepButton("+") { vm.setPrice(b.id, step(b.price, 1)) }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (f in listOf(0.7f, 0.85f, 1f, 1.2f, 1.5f)) {
                    FbChip(if (f == 1f) stringResource(R.string.is_default_price) else "%.0f%%".format(f * 100), kotlin.math.abs(b.price / t.price - f) < 0.01f,
                        { vm.setPrice(b.id, t.price * f) })
                }
            }
            Text(stringResource(R.string.is_price_tip), color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))

            Spacer(Modifier.height(10.dp))
            if (b.level >= 2) {
                Text(stringResource(R.string.is_max_level), color = AppColors.TextSecondary, fontSize = 12.sp)
            } else {
                FbButton(stringResource(R.string.is_upgrade, IsFmt.money(IsSim.upgradeCost(b))), { vm.upgrade(b.id) }, Modifier.fillMaxWidth(),
                    enabled = b.buildLeft <= 0 && w.companies[w.userId].balance >= IsSim.upgradeCost(b))
                Text(stringResource(R.string.is_upgrade_tip), color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (mine) {
            Spacer(Modifier.height(10.dp))
            if (!confirmDemolish) {
                FbButton(stringResource(R.string.is_demolish), { confirmDemolish = true }, Modifier.fillMaxWidth(), color = FbColors.PanelHigh, textColor = FbColors.Red)
            } else {
                Text(stringResource(R.string.is_demolish_confirm, IsFmt.money(if (b.buildLeft <= 0) t.cost / 5 else 0)), color = FbColors.Orange, fontSize = 12.sp)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FbButton(stringResource(R.string.is_yes), { vm.demolish(b.id) }, color = FbColors.Red)
                    FbButton(stringResource(R.string.is_no), { confirmDemolish = false }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                }
            }
        }
    }
}

/** Fiyat adimi: %5, guzel sayilara yuvarlanir */
private fun step(p: Float, dir: Int): Float {
    val n = p * (1f + 0.05f * dir)
    val unit = when { n >= 200f -> 5f; n >= 40f -> 1f; else -> 0.5f }
    val r = (n / unit).roundToInt() * unit
    return if (r == p) p + dir * unit else r
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(FbColors.PanelHigh).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = AppColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun OccupancyBar(f: Float) {
    val c = when { f >= 0.85f -> FbColors.Green; f >= 0.5f -> FbColors.Yellow; else -> FbColors.Orange }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(FbColors.Line)) {
            Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(8.dp).background(c))
        }
        Text(stringResource(R.string.is_occupancy) + " " + IsFmt.pct(f), color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

// ====================================================================== arsa

@Composable
fun LandPanel(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    if (ui.selX < 0 || !w.inside(ui.selX, ui.selY)) { LaunchedEffect(Unit) { vm.closePanel() }; return }
    val x = ui.selX; val y = ui.selY
    val t = w.ter(x, y)
    val owner = w.owner[w.idx(x, y)]
    IsPanelFrame(stringResource(R.string.is_land), { vm.closePanel() }) {
        when {
            !t.isLand -> Text(stringResource(R.string.is_land_water), color = AppColors.TextSecondary, fontSize = 13.sp)
            owner == -2 -> Text(stringResource(R.string.is_land_public), color = AppColors.TextSecondary, fontSize = 13.sp)
            owner == w.userId -> Text(stringResource(R.string.is_land_owned_you), color = FbColors.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            owner >= 0 -> Text(stringResource(R.string.is_land_owned_other, companyName(w, owner)), color = FbColors.Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            else -> {
                val price = IsSim.landPrice(w, x, y)
                IsKV(stringResource(R.string.is_land_price), IsFmt.money(price))
                Spacer(Modifier.height(8.dp))
                FbButton(stringResource(R.string.is_buy_land), { vm.buyLand() }, Modifier.fillMaxWidth(), enabled = w.companies[w.userId].balance >= price)
            }
        }
        if (t.isLand && owner >= -1) {
            Text(stringResource(R.string.is_land_hint), color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(8.dp))
            FbButton(catIcon(Cat.STAY) + " " + stringResource(R.string.is_build), { vm.openPanel(IsPanel.BUILD) }, Modifier.fillMaxWidth(),
                color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
        }
        val terrain = when (t) { Terrain.SAND -> "🏖️"; Terrain.FOREST -> "🌲"; Terrain.HILL -> "⛰️"; Terrain.GRASS -> "🌿"; else -> "🌊" }
        Text("$terrain  ($x, $y)", color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

// ====================================================================== finans

@Composable
fun FinancePanel(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    val c = w.userId
    val co = w.companies[c]
    IsPanelFrame(stringResource(R.string.is_finance), { vm.closePanel() }) {
        IsKV(stringResource(R.string.is_balance), IsFmt.money(co.balance), if (co.balance < 0) FbColors.Red else FbColors.Green)
        IsKV(stringResource(R.string.is_assets), IsFmt.money(IsSim.assets(w, c)))
        IsKV(stringResource(R.string.is_net_worth), IsFmt.money(IsSim.netWorth(w, c)), FbColors.Yellow)
        IsKV(stringResource(R.string.is_reputation), "%.0f / 100".format(co.reputation))
        if (co.redMonths > 0) {
            Text(stringResource(R.string.is_red_warning, co.redMonths, IsFmt.money(-IsRivals.overdraft(w, c)), IsRivals.RED_LIMIT),
                color = FbColors.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
        }

        IsSection(stringResource(R.string.is_this_month) + " / " + stringResource(R.string.is_last_month))
        MonthRow(stringResource(R.string.is_income), co.monthIn, co.lastMonthIn, FbColors.Green)
        MonthRow(stringResource(R.string.is_expense), co.monthOut, co.lastMonthOut, FbColors.Red)
        MonthRow(stringResource(R.string.is_net), co.monthIn - co.monthOut, co.lastMonthIn - co.lastMonthOut, null)

        IsSection(stringResource(R.string.is_this_year) + " / " + stringResource(R.string.is_last_year))
        for (f in Fin.entries) {
            val a = co.ledger[f.ordinal]; val b = co.lastLedger[f.ordinal]
            if (a == 0L && b == 0L) continue
            MonthRow(stringResource(finName(f)), a, b, if (f.income) FbColors.Green else FbColors.Red)
        }

        IsSection(stringResource(R.string.is_bank))
        if (co.loanMonthsLeft > 0) {
            IsKV(stringResource(R.string.is_loan_left), IsFmt.money(co.loanLeft), FbColors.Orange)
            IsKV(stringResource(R.string.is_interest_rate), stringResource(R.string.is_rate_year, "%.1f%%".format(co.loanRate)))
            IsKV(stringResource(R.string.is_monthly_payment), IsFmt.money(co.loanPayment))
            IsKV(stringResource(R.string.is_months_left), "${co.loanMonthsLeft}")
            Spacer(Modifier.height(6.dp))
            val cost = IsSim.repayCost(co)
            FbButton(stringResource(R.string.is_repay, IsFmt.money(cost)), { vm.repayLoan() }, Modifier.fillMaxWidth(), enabled = co.balance >= cost)
        } else {
            val limit = IsSim.creditLimit(w, c)
            IsKV(stringResource(R.string.is_credit_limit), IsFmt.money(limit))
            if (limit <= 0) {
                Text(stringResource(R.string.is_no_credit), color = FbColors.Orange, fontSize = 12.sp)
            } else {
                var share by remember { mutableIntStateOf(2) }
                var years by remember { mutableIntStateOf(3) }
                val amount = (limit * (share + 1) / 4) / 10_000 * 10_000
                Text(stringResource(R.string.is_loan_amount), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0..3) FbChip(IsFmt.money(limit * (i + 1) / 4 / 10_000 * 10_000), share == i, { share = i })
                }
                Text(stringResource(R.string.is_loan_term), color = AppColors.TextSecondary, fontSize = 12.sp)
                Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (yy in 1..5) FbChip(stringResource(R.string.is_years_short, yy), years == yy, { years = yy })
                }
                val rate = IsSim.loanRate(w, c, years)
                val monthly = IsSim.monthlyPayment(amount, rate, years * 12)
                IsKV(stringResource(R.string.is_interest_rate), stringResource(R.string.is_rate_year, "%.1f%%".format(rate)))
                IsKV(stringResource(R.string.is_monthly_payment), IsFmt.money(monthly))
                IsKV(stringResource(R.string.is_total_repay), IsFmt.money(monthly * years * 12))
                Spacer(Modifier.height(6.dp))
                FbButton(stringResource(R.string.is_take_loan), { vm.takeLoan(amount, years) }, Modifier.fillMaxWidth())
            }
        }
        Text(stringResource(R.string.is_bank_tip), color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MonthRow(label: String, a: Long, b: Long, color: Color?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(IsFmt.money(a), color = color ?: if (a < 0) FbColors.Red else FbColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(78.dp))
        Text(IsFmt.money(b), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(70.dp))
    }
}

// ====================================================================== rakipler

@Composable
fun RivalsPanel(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    // Bugunku konaklama misafirinin sirketlere dagilimi
    val guests = DoubleArray(w.companies.size)
    for (b in w.buildings) if (b != null && b.owner >= 0 && b.type.cat == Cat.STAY) guests[b.owner] += b.today.sum().toDouble()
    val total = guests.sum().coerceAtLeast(1.0)
    val counts = IntArray(w.companies.size)
    for (b in w.buildings) if (b != null && b.owner >= 0 && b.type.cat != Cat.ROAD && b.type.cat != Cat.NATURE) counts[b.owner]++
    val ranked = w.companies.sortedWith(compareBy<Company>({ !it.alive }, { -IsSim.netWorth(w, it.id) }))

    val open = w.auctions.filter { !it.done && w.buildings.getOrNull(it.building) != null }
    IsPanelFrame(stringResource(R.string.is_rivals), { vm.closePanel() }) {
        if (open.isNotEmpty()) {
            IsSection("🔨 " + stringResource(R.string.is_auctions) + " (${open.size})")
            for (a in open) AuctionCard(w, a, vm, showMapButton = true)
            Text(stringResource(R.string.is_auction_tip), color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
            IsSection(stringResource(R.string.is_rivals))
        }
        for ((i, co) in ranked.withIndex()) {
            val me = co.id == w.userId
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (me) AppColors.Primary.copy(alpha = 0.18f) else FbColors.PanelHigh).padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = AppColors.TextSecondary, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.width(22.dp))
                    Box(Modifier.size(14.dp).clip(CircleShape).background(Color(co.color)))
                    Text(co.name + if (me) " (" + stringResource(R.string.is_you) + ")" else "", color = if (co.alive) AppColors.TextPrimary else AppColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!co.alive) Text(stringResource(R.string.is_bankrupt_tag), color = FbColors.Red, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                if (co.alive) {
                    IsKV(stringResource(R.string.is_net_worth), IsFmt.money(IsSim.netWorth(w, co.id)), FbColors.Yellow)
                    IsKV(stringResource(R.string.is_buildings), "${counts[co.id]}")
                    IsKV(stringResource(R.string.is_reputation), "%.0f".format(co.reputation))
                    IsKV(stringResource(R.string.is_guests_share), IsFmt.pct((guests[co.id] / total).toFloat()))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(FbColors.Line)) {
                        Box(Modifier.fillMaxWidth((guests[co.id] / total).toFloat().coerceIn(0f, 1f)).height(6.dp).background(Color(co.color)))
                    }
                    if (!me && w.companies[w.userId].alive) {
                        var showDirty by remember(co.id) { mutableStateOf(false) }
                        Spacer(Modifier.height(6.dp))
                        FbChip("🕶️ " + stringResource(R.string.is_dirty), showDirty, { showDirty = !showDirty })
                        if (showDirty) {
                            DirtyCard(w, Dirty.PROPAGANDA, vm, co.id)
                            DirtyCard(w, Dirty.STRIKE, vm, co.id)
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================== haberler

@Composable
fun NewsPanel(w: IsWorld, ui: IsUiState, vm: IslandViewModel) {
    IsPanelFrame(stringResource(R.string.is_news), { vm.closePanel() }) {
        if (w.news.isEmpty()) Text(stringResource(R.string.is_no_news), color = AppColors.TextSecondary, fontSize = 13.sp)
        val start = (w.news.size - 80).coerceAtLeast(0)
        for (i in w.news.size - 1 downTo start) {
            val n = w.news[i]
            val txt = newsText(w, n) ?: continue
            val col = w.companies.getOrNull(n.company)?.color ?: 0xFF90A4AE.toInt()
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(Color(col)))
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(newsIcon(n.kind) + " " + txt, color = if (n.company == w.userId) AppColors.TextPrimary else AppColors.TextMuted, fontSize = 13.sp)
                    Text(IsFmt.date(w, n.day), color = AppColors.TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}
