package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.FbBanking
import com.eomer91.minigamesworld.ui.games.football.engine.Facility
import com.eomer91.minigamesworld.ui.games.football.engine.FbClub
import com.eomer91.minigamesworld.ui.games.football.engine.FbClubLife
import com.eomer91.minigamesworld.ui.games.football.engine.FbCountries
import com.eomer91.minigamesworld.ui.games.football.engine.FbGen
import com.eomer91.minigamesworld.ui.games.football.engine.FbSeason
import com.eomer91.minigamesworld.ui.games.football.engine.FbStaff
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.Fin
import com.eomer91.minigamesworld.ui.games.football.engine.StaffAttr
import com.eomer91.minigamesworld.ui.games.football.engine.StaffRole
import com.eomer91.minigamesworld.ui.theme.AppColors

private enum class ClubPage { FINANCE, BANK, FACILITIES, STAFF, SPONSOR, FRIENDLY, EDITOR, SETTINGS }

@Composable
fun FbClubScreen(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    var page by remember { mutableStateOf(ClubPage.FINANCE) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FbChip(stringResource(R.string.fb_finance), page == ClubPage.FINANCE, { page = ClubPage.FINANCE })
            FbChip(stringResource(R.string.fb_bank), page == ClubPage.BANK, { page = ClubPage.BANK })
            FbChip(stringResource(R.string.fb_facilities), page == ClubPage.FACILITIES, { page = ClubPage.FACILITIES })
            FbChip(stringResource(R.string.fb_staff), page == ClubPage.STAFF, { page = ClubPage.STAFF })
            FbChip(stringResource(R.string.fb_sponsor_tickets), page == ClubPage.SPONSOR, { page = ClubPage.SPONSOR })
            FbChip(stringResource(R.string.fb_friendlies), page == ClubPage.FRIENDLY, { page = ClubPage.FRIENDLY })
            FbChip(stringResource(R.string.fb_editor), page == ClubPage.EDITOR, { page = ClubPage.EDITOR })
            FbChip(stringResource(R.string.fb_settings), page == ClubPage.SETTINGS, { page = ClubPage.SETTINGS })
        }
        Box(Modifier.weight(1f)) {
            when (page) {
                ClubPage.FINANCE -> Finance(world)
                ClubPage.BANK -> Bank(world, vm)
                ClubPage.FACILITIES -> Facilities(world, vm)
                ClubPage.STAFF -> Staff(world, vm)
                ClubPage.SPONSOR -> SponsorTickets(world, vm)
                ClubPage.FRIENDLY -> Friendlies(world, vm)
                ClubPage.EDITOR -> Editor(world, ui, vm)
                ClubPage.SETTINGS -> Settings(world, vm)
            }
        }
    }
}

fun finName(f: Fin): Int = when (f) {
    Fin.TICKETS -> R.string.fb_fin_tickets; Fin.TV -> R.string.fb_fin_tv; Fin.MERCH -> R.string.fb_fin_merch; Fin.SPONSOR -> R.string.fb_fin_sponsor
    Fin.ADS -> R.string.fb_fin_ads; Fin.PRIZE -> R.string.fb_fin_prize; Fin.TRANSFER_IN -> R.string.fb_fin_transfer_in; Fin.OTHER_IN -> R.string.fb_fin_other_in
    Fin.WAGES -> R.string.fb_fin_wages; Fin.STAFF_WAGES -> R.string.fb_fin_staff_wages; Fin.FACILITIES -> R.string.fb_fin_facilities
    Fin.YOUTH -> R.string.fb_fin_youth; Fin.MATCHDAY -> R.string.fb_fin_matchday; Fin.TRANSFER_OUT -> R.string.fb_fin_transfer_out
    Fin.UPGRADES -> R.string.fb_fin_upgrades; Fin.INTEREST -> R.string.fb_fin_interest; Fin.OTHER_OUT -> R.string.fb_fin_other_out
}

// ====================================================================== finans

@Composable
private fun Finance(world: FbWorld) {
    val c = world.clubs[world.userClub]
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.weight(1.2f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("", Modifier.weight(1f))
                FbCell(stringResource(R.string.fb_this_season), 90.dp, AppColors.TextSecondary, align = TextAlign.End, size = 11.sp)
                FbCell(stringResource(R.string.fb_last_season), 90.dp, AppColors.TextSecondary, align = TextAlign.End, size = 11.sp)
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (income in listOf(true, false)) {
                    Text(stringResource(if (income) R.string.fb_income else R.string.fb_expenses), color = if (income) FbColors.Green else FbColors.Red,
                        fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                    for (f in Fin.entries.filter { it.income == income }) {
                        val a = c.ledger[f.ordinal]; val b = c.lastLedger[f.ordinal]
                        if (a == 0L && b == 0L) continue
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(stringResource(finName(f)), color = AppColors.TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            FbCell(FbFormat.money(a), 90.dp, AppColors.TextPrimary, align = TextAlign.End)
                            FbCell(FbFormat.money(b), 90.dp, AppColors.TextSecondary, align = TextAlign.End)
                        }
                    }
                }
                val net = Fin.entries.sumOf { if (it.income) c.ledger[it.ordinal] else -c.ledger[it.ordinal] }
                val netLast = Fin.entries.sumOf { if (it.income) c.lastLedger[it.ordinal] else -c.lastLedger[it.ordinal] }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(stringResource(R.string.fb_net), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    FbCell(FbFormat.money(net), 90.dp, if (net >= 0) FbColors.Green else FbColors.Red, bold = true, align = TextAlign.End)
                    FbCell(FbFormat.money(netLast), 90.dp, if (netLast >= 0) FbColors.Green else FbColors.Red, align = TextAlign.End)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FbPanel(Modifier.fillMaxWidth().weight(1f), title = stringResource(R.string.fb_balance_history)) {
                val h = c.balanceHistory
                if (h.size >= 2) Canvas(Modifier.fillMaxSize()) {
                    val mn = minOf(0, h.min()).toFloat(); val mx = maxOf(1, h.max()).toFloat()
                    fun y(v: Int) = size.height - (v - mn) / (mx - mn) * size.height
                    val zero = y(0)
                    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, zero), Offset(size.width, zero), 1.5f)
                    val path = Path()
                    h.forEachIndexed { i, v -> val x = i * size.width / (h.size - 1); if (i == 0) path.moveTo(x, y(v)) else path.lineTo(x, y(v)) }
                    drawPath(path, if (h.last() >= 0) FbColors.Green else FbColors.Red, style = Stroke(width = 4f))
                }
            }
            // Aylik toplamlar
            FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_monthly)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                    Text("", Modifier.weight(1f))
                    FbCell(stringResource(R.string.fb_this_month), 86.dp, AppColors.TextSecondary, align = TextAlign.End, size = 11.sp)
                    FbCell(stringResource(R.string.fb_last_month), 86.dp, AppColors.TextSecondary, align = TextAlign.End, size = 11.sp)
                }
                MonthRow(stringResource(R.string.fb_month_income), c.monthIn, c.lastMonthIn, FbColors.Green)
                MonthRow(stringResource(R.string.fb_month_expense), c.monthOut, c.lastMonthOut, FbColors.Red)
                val n1 = c.monthIn - c.monthOut; val n2 = c.lastMonthIn - c.lastMonthOut
                Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
                    Text(stringResource(R.string.fb_net), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    FbCell(FbFormat.money(n1), 86.dp, if (n1 >= 0) FbColors.Green else FbColors.Red, bold = true, align = TextAlign.End)
                    FbCell(FbFormat.money(n2), 86.dp, if (n2 >= 0) FbColors.Green else FbColors.Red, bold = true, align = TextAlign.End)
                }
            }
            FbPanel(Modifier.fillMaxWidth()) {
                FbKeyValue(stringResource(R.string.fb_balance), FbFormat.money(c.balance), if (c.balance >= 0) FbColors.Green else FbColors.Red)
                if (world.bank.deposit > 0) FbKeyValue(stringResource(R.string.fb_deposit_balance), FbFormat.money(world.bank.deposit), FbColors.Blue)
                if (world.bank.hasLoan) FbKeyValue(stringResource(R.string.fb_loan_left), FbFormat.money(world.bank.loanLeft), FbColors.Red)
                FbKeyValue(stringResource(R.string.fb_weekly_wages), FbFormat.money(c.players.sumOf { world.players[it].wage.toLong() }))
                FbKeyValue(stringResource(R.string.fb_staff_wages_week), FbFormat.money(c.staff.sumOf { world.staff[it].wage.toLong() }))
                FbKeyValue(stringResource(R.string.fb_fan_base), "${c.fanBase}")
            }
        }
    }
}

@Composable
private fun MonthRow(label: String, now: Long, last: Long, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = AppColors.TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        FbCell(FbFormat.money(now), 86.dp, color, bold = true, align = TextAlign.End)
        FbCell(FbFormat.money(last), 86.dp, AppColors.TextSecondary, align = TextAlign.End)
    }
}

// ====================================================================== banka

private fun pct(v: Float) = (if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)) + "%"

@Composable
private fun Bank(world: FbWorld, vm: FootballViewModel) {
    val c = world.clubs[world.userClub]
    val b = world.bank
    var share by remember { mutableIntStateOf(50) }
    var years by remember { mutableIntStateOf(2) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Kredi
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_bank_loan)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (b.hasLoan) {
                    FbKeyValue(stringResource(R.string.fb_loan_original), FbFormat.money(b.loanTaken))
                    FbKeyValue(stringResource(R.string.fb_loan_left), FbFormat.money(b.loanLeft), FbColors.Red)
                    FbKeyValue(stringResource(R.string.fb_interest_rate), stringResource(R.string.fb_rate_year, pct(b.loanRate)))
                    FbKeyValue(stringResource(R.string.fb_weekly_payment), FbFormat.money(b.loanWeekly), FbColors.Yellow)
                    FbKeyValue(stringResource(R.string.fb_weeks_left), "${b.loanWeeksLeft}")
                    FbBar(((b.loanTaken - b.loanLeft) * 100 / b.loanTaken.coerceAtLeast(1)).toInt(), FbColors.Green, 220.dp, 8.dp)
                    Spacer(Modifier.height(10.dp))
                    val cost = FbBanking.earlyRepayCost(b)
                    FbButton(stringResource(R.string.fb_repay_loan, FbFormat.money(cost)), {
                        vm.edit { w -> if (FbBanking.repayLoan(w)) vm.toast(R.string.fb_loan_repaid) else vm.toast(R.string.fb_no_funds) }
                    }, enabled = c.balance >= cost, color = FbColors.Green, textColor = Color.Black)
                } else {
                    val limit = FbBanking.creditLimit(world, c)
                    FbKeyValue(stringResource(R.string.fb_credit_limit), FbFormat.money(limit), FbColors.Green)
                    Text(stringResource(R.string.fb_loan_amount), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (s in listOf(25, 50, 75, 100)) FbChip(FbFormat.money(FbEconomyRound(limit * s / 100)), share == s, { share = s })
                    }
                    Text(stringResource(R.string.fb_loan_term), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (y in FbBanking.TERMS_YEARS) FbChip(stringResource(R.string.fb_years_short, y), years == y, { years = y })
                    }
                    val amount = FbEconomyRound(limit * share / 100)
                    val rate = FbBanking.loanRate(c, years)
                    val weekly = FbBanking.weeklyPayment(amount, rate, years * 52)
                    FbKeyValue(stringResource(R.string.fb_interest_rate), stringResource(R.string.fb_rate_year, pct(rate)))
                    FbKeyValue(stringResource(R.string.fb_weekly_payment), FbFormat.money(weekly), FbColors.Yellow)
                    FbKeyValue(stringResource(R.string.fb_total_repay), FbFormat.money(weekly * years * 52))
                    Spacer(Modifier.height(8.dp))
                    FbButton(stringResource(R.string.fb_take_loan), {
                        vm.edit { w -> if (FbBanking.takeLoan(w, amount, years)) vm.toast(R.string.fb_loan_taken_ok, FbFormat.money(amount)) }
                    }, enabled = amount > 0, color = AppColors.Primary)
                }
                Text(stringResource(R.string.fb_bank_tip), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
        // Vadeli mevduat
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_deposit)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FbKeyValue(stringResource(R.string.fb_deposit_balance), FbFormat.money(b.deposit), FbColors.Blue)
                FbKeyValue(stringResource(R.string.fb_interest_rate), stringResource(R.string.fb_rate_year, pct(FbBanking.DEPOSIT_RATE)))
                FbKeyValue(stringResource(R.string.fb_weekly_return), FbFormat.money((b.deposit * FbBanking.DEPOSIT_RATE / 100 / 52).toLong()), FbColors.Green)
                FbKeyValue(stringResource(R.string.fb_deposit_earned), FbFormat.money(b.depositEarned))
                FbKeyValue(stringResource(R.string.fb_balance), FbFormat.money(c.balance), if (c.balance >= 0) FbColors.Green else FbColors.Red)
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (s in listOf(10, 25, 50)) {
                        val a = FbEconomyRound(c.balance * s / 100)
                        FbButton(stringResource(R.string.fb_deposit_x, FbFormat.money(a)), { vm.edit { w -> FbBanking.depositMoney(w, a) } },
                            enabled = a > 0, color = FbColors.Blue, textColor = Color.Black)
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val half = b.deposit / 2
                    FbButton(stringResource(R.string.fb_withdraw_x, FbFormat.money(half)), { vm.edit { w -> FbBanking.withdraw(w, half) } },
                        enabled = half > 0, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                    FbButton(stringResource(R.string.fb_withdraw_x, FbFormat.money(b.deposit)), { vm.edit { w -> FbBanking.withdraw(w, Long.MAX_VALUE) } },
                        enabled = b.deposit > 0, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                }
                Text(stringResource(R.string.fb_deposit_tip), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

private fun FbEconomyRound(v: Long): Long = if (v <= 0) 0 else com.eomer91.minigamesworld.ui.games.football.engine.FbEconomy.roundValue(v)

// ====================================================================== tesisler

private fun facilityName(f: Facility): Int = when (f) {
    Facility.TRAINING -> R.string.fb_f_training; Facility.YOUTH -> R.string.fb_f_youth; Facility.RECRUITMENT -> R.string.fb_f_recruitment
    Facility.STADIUM -> R.string.fb_f_stadium; Facility.SOCIAL -> R.string.fb_f_social; Facility.CAPACITY -> R.string.fb_f_capacity
}

private fun facilityDesc(f: Facility): Int = when (f) {
    Facility.TRAINING -> R.string.fb_fd_training; Facility.YOUTH -> R.string.fb_fd_youth; Facility.RECRUITMENT -> R.string.fb_fd_recruitment
    Facility.STADIUM -> R.string.fb_fd_stadium; Facility.SOCIAL -> R.string.fb_fd_social; Facility.CAPACITY -> R.string.fb_fd_capacity
}

@Composable
private fun Facilities(world: FbWorld, vm: FootballViewModel) {
    val c = world.clubs[world.userClub]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (c.upgradeType >= 0) {
            val f = Facility.entries[c.upgradeType]
            FbPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.fb_under_construction, stringResource(facilityName(f)), FbFormat.shortDate(world, c.upgradeDay) +
                    (if (c.upgradeSeason > world.season) " ${FbFormat.season(c.upgradeSeason)}" else "")), color = FbColors.Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        for (f in Facility.entries) {
            FbPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(facilityName(f)), color = AppColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(facilityDesc(f)), color = AppColors.TextSecondary, fontSize = 12.sp)
                    }
                    val lvl = FbClubLife.facilityLevel(c, f)
                    if (f == Facility.CAPACITY) Text(stringResource(R.string.fb_seats, lvl), color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
                    else { FbBar(lvl * 5, FbColors.attr(lvl), 100.dp, 8.dp); Text(" $lvl/20", color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 10.dp)) }
                    val cost = FbClubLife.upgradeCost(c, f)
                    val can = c.upgradeType < 0 && (f == Facility.CAPACITY || lvl < 20)
                    FbButton(stringResource(R.string.fb_upgrade_x, FbFormat.money(cost), FbClubLife.upgradeDays(c, f)), {
                        vm.edit { w ->
                            when (FbClubLife.startUpgrade(w, c, f)) {
                                null -> vm.toast(R.string.fb_upgrade_started)
                                "FUNDS" -> vm.toast(R.string.fb_no_funds)
                                else -> vm.toast(R.string.fb_err_generic)
                            }
                        }
                    }, enabled = can)
                }
            }
        }
    }
}

// ====================================================================== personel

fun roleName(r: StaffRole): Int = when (r) {
    StaffRole.ASSISTANT -> R.string.fb_r_assistant; StaffRole.COACH -> R.string.fb_r_coach; StaffRole.GK_COACH -> R.string.fb_r_gk_coach
    StaffRole.FITNESS_COACH -> R.string.fb_r_fitness; StaffRole.SCOUT -> R.string.fb_r_scout; StaffRole.PHYSIO -> R.string.fb_r_physio
    StaffRole.YOUTH_COACH -> R.string.fb_r_youth
}

fun staffAttrName(a: StaffAttr): Int = when (a) {
    StaffAttr.COACH_ATTACK -> R.string.fb_sa_attack; StaffAttr.COACH_DEFENCE -> R.string.fb_sa_defence; StaffAttr.COACH_TECHNICAL -> R.string.fb_sa_technical
    StaffAttr.COACH_TACTICAL -> R.string.fb_sa_tactical; StaffAttr.COACH_MENTAL -> R.string.fb_sa_mental; StaffAttr.COACH_FITNESS -> R.string.fb_sa_fitness
    StaffAttr.COACH_GK -> R.string.fb_sa_gk; StaffAttr.WORKING_WITH_YOUTH -> R.string.fb_sa_youth; StaffAttr.JUDGING_ABILITY -> R.string.fb_sa_judge_ability
    StaffAttr.JUDGING_POTENTIAL -> R.string.fb_sa_judge_potential; StaffAttr.PHYSIOTHERAPY -> R.string.fb_sa_physio; StaffAttr.MOTIVATING -> R.string.fb_sa_motivating
    StaffAttr.DISCIPLINE -> R.string.fb_sa_discipline; StaffAttr.MAN_MANAGEMENT -> R.string.fb_sa_man_management
}

/** Rolune gore personelin ana gucu (1..20) */
private fun staffScore(s: FbStaff): Int = when (s.role) {
    StaffRole.COACH -> listOf(StaffAttr.COACH_ATTACK, StaffAttr.COACH_DEFENCE, StaffAttr.COACH_TECHNICAL, StaffAttr.COACH_TACTICAL, StaffAttr.COACH_MENTAL).maxOf { s.attr(it) }
    StaffRole.ASSISTANT -> (s.attr(StaffAttr.MAN_MANAGEMENT) + s.attr(StaffAttr.COACH_TACTICAL)) / 2
    StaffRole.GK_COACH -> s.attr(StaffAttr.COACH_GK); StaffRole.FITNESS_COACH -> s.attr(StaffAttr.COACH_FITNESS)
    StaffRole.SCOUT -> (s.attr(StaffAttr.JUDGING_ABILITY) + s.attr(StaffAttr.JUDGING_POTENTIAL)) / 2
    StaffRole.PHYSIO -> s.attr(StaffAttr.PHYSIOTHERAPY); StaffRole.YOUTH_COACH -> s.attr(StaffAttr.WORKING_WITH_YOUTH)
}

@Composable
private fun Staff(world: FbWorld, vm: FootballViewModel) {
    val c = world.clubs[world.userClub]
    var role by remember { mutableStateOf<StaffRole?>(null) }
    var detail by remember { mutableIntStateOf(-1) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_your_staff, c.staff.size)) {
            LazyColumn {
                items(c.staff.map { world.staff[it] }.sortedBy { it.role.ordinal }, key = { it.id }) { s -> StaffRow(world, s, detail == s.id, { detail = if (detail == s.id) -1 else s.id }) {
                    var confirm by remember { mutableStateOf(false) }
                    if (!confirm) FbButton(stringResource(R.string.fb_fire), { confirm = true }, color = FbColors.Red.copy(alpha = 0.3f), textColor = FbColors.Red)
                    else FbButton(stringResource(R.string.fb_fire_confirm, FbFormat.money(FbClubLife.severance(world, s))), { vm.edit { FbClubLife.fireStaff(it, c, s) } }, color = FbColors.Red, textColor = Color.Black)
                } }
            }
        }
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_staff_market)) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FbChip(stringResource(R.string.fb_all), role == null, { role = null })
                for (r in StaffRole.entries) FbChip(stringResource(roleName(r)), role == r, { role = r })
            }
            val free = FbClubLife.freeStaff(world).filter { role == null || it.role == role }.sortedByDescending { staffScore(it) }.take(60)
            LazyColumn {
                items(free, key = { it.id }) { s -> StaffRow(world, s, detail == s.id, { detail = if (detail == s.id) -1 else s.id }) {
                    Text(FbFormat.wage(FbClubLife.staffAskingWage(c, s)) + stringResource(R.string.fb_per_week), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                    FbButton(stringResource(R.string.fb_hire), {
                        vm.edit { w ->
                            when (FbClubLife.hireStaff(w, c, s)) {
                                null -> vm.toast(R.string.fb_hired)
                                "FULL" -> vm.toast(R.string.fb_staff_full)
                                "REJECT" -> vm.toast(R.string.fb_staff_reject)
                                else -> vm.toast(R.string.fb_err_generic)
                            }
                        }
                    }, color = FbColors.Green, textColor = Color.Black)
                } }
            }
        }
    }
}

@Composable
private fun StaffRow(world: FbWorld, s: FbStaff, expanded: Boolean, onToggle: () -> Unit, actions: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(FbColors.PanelHigh).clickable(onClick = onToggle).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.fullName, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(stringResource(roleName(s.role)) + " · " + FbCountries.all[s.nation].englishName, color = AppColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
            }
            FbAttrValue(staffScore(s))
            Spacer(Modifier.width(8.dp))
            actions()
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            for (a in StaffAttr.entries) Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(staffAttrName(a)), color = AppColors.TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                FbAttrValue(s.attr(a), 26.dp)
            }
            if (s.club >= 0) FbKeyValue(stringResource(R.string.fb_col_contract), "${s.contractEnd + 1} · " + FbFormat.wage(s.wage) + stringResource(R.string.fb_per_week))
        }
    }
}

// ====================================================================== sponsor ve bilet

@Composable
private fun SponsorTickets(world: FbWorld, vm: FootballViewModel) {
    val c = world.clubs[world.userClub]
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_shirt_sponsor)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FbKeyValue(stringResource(R.string.fb_current_sponsor), c.sponsorName)
                FbKeyValue(stringResource(R.string.fb_per_year), FbFormat.money(c.sponsorIncome))
                FbKeyValue(stringResource(R.string.fb_until), FbFormat.season(c.sponsorEnd))
                Spacer(Modifier.height(8.dp))
                if (c.sponsorEnd < world.season) {
                    Text(stringResource(R.string.fb_sponsor_offers), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    for (o in FbClubLife.sponsorOffers(world, c)) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(o.name, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.fb_sponsor_line, FbFormat.money(o.yearly), o.years), color = AppColors.TextSecondary, fontSize = 12.sp)
                            }
                            FbButton(stringResource(R.string.fb_sign), { vm.edit { FbClubLife.acceptSponsor(it, c, o) } }, color = FbColors.Green, textColor = Color.Black)
                        }
                    }
                } else Text(stringResource(R.string.fb_sponsor_locked), color = AppColors.TextSecondary, fontSize = 12.sp)
            }
        }
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_ticket_prices)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (l in -2..2) FbChip(stringResource(when (l) { -2 -> R.string.fb_tp_very_low; -1 -> R.string.fb_tp_low; 0 -> R.string.fb_o_normal; 1 -> R.string.fb_tp_high; else -> R.string.fb_tp_very_high }),
                        c.ticketLevel == l, { vm.edit { c.ticketLevel = l } })
                }
                Spacer(Modifier.height(8.dp))
                FbKeyValue(stringResource(R.string.fb_ticket_price), FbFormat.money((FbClubLife.ticketPrice(world, c) * FbClubLife.revenueMul(world, c)).toLong()))
                val att = FbClubLife.attendance(world, c, -1, com.eomer91.minigamesworld.ui.games.football.engine.CompKind.LEAGUE)
                FbKeyValue(stringResource(R.string.fb_expected_attendance), "$att / ${c.capacity}")
                FbKeyValue(stringResource(R.string.fb_matchday_income), FbFormat.money((att * FbClubLife.ticketPrice(world, c) * FbClubLife.revenueMul(world, c)).toLong()))
                Text(stringResource(R.string.fb_tip_tickets), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

// ====================================================================== dostluk maclari

@Composable
private fun Friendlies(world: FbWorld, vm: FootballViewModel) {
    val c = world.clubs[world.userClub]
    var opp by remember { mutableIntStateOf(-1) }
    val freeDays = remember(world.day, world.state!!.fixtures.size) { (world.day + 2..minOf(363, world.day + 70)).filter { !FbSeason.clubBusy(world, c.id, it) }.take(40) }
    val opponents = remember(world.season) {
        (world.clubs.filter { it.country == c.country && it.league >= 0 && it.id != c.id } +
            world.clubs.filter { it.league >= 0 && it.country != c.country }.sortedByDescending { it.reputation }.take(40)).sortedByDescending { it.reputation }
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_choose_opponent)) {
            LazyColumn {
                items(opponents, key = { it.id }) { o ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (o.id == opp) FbColors.PanelHigh else Color.Transparent)
                        .clickable { opp = o.id }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        FbCrest(o.color1, o.color2, o.shortName, 26.dp)
                        Text(o.name, color = AppColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
                        FbStars(o.reputation / 2000f, 11.dp)
                    }
                }
            }
        }
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_choose_date)) {
            Text(stringResource(R.string.fb_friendly_desc), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            LazyColumn {
                items(freeDays) { d ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(FbFormat.date(world, d), color = AppColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        FbButton(stringResource(R.string.fb_arrange), {
                            vm.edit { w ->
                                vm.toast(when (FbSeason.arrangeFriendly(w, c.id, opp, d)) {
                                    FbSeason.FriendlyResult.OK -> R.string.fb_friendly_ok
                                    FbSeason.FriendlyResult.DECLINED -> R.string.fb_friendly_declined
                                    FbSeason.FriendlyResult.BUSY_THEM -> R.string.fb_friendly_busy
                                    else -> R.string.fb_err_generic
                                })
                            }
                        }, enabled = opp >= 0)
                    }
                }
            }
        }
    }
}

// ====================================================================== ayarlar

@Composable
private fun Settings(world: FbWorld, vm: FootballViewModel) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_currency)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (cur in FbCurrency.entries) FbChip(cur.code, world.currency == cur.ordinal, { vm.setCurrency(cur) })
            }
            Text(stringResource(R.string.fb_currency_desc, FbFormat.money(1_000_000)), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_career)) {
            Text(stringResource(R.string.fb_manager_line, world.managerName, FbFormat.season(world.season)), color = AppColors.TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            if (!confirm) FbButton(stringResource(R.string.fb_quit_career), { confirm = true }, color = FbColors.Red.copy(alpha = 0.3f), textColor = FbColors.Red)
            else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.fb_new_career_warning), color = FbColors.Orange, fontSize = 13.sp, modifier = Modifier.weight(1f))
                FbButton(stringResource(R.string.fb_yes), { vm.abandonCareer() }, color = FbColors.Red, textColor = Color.Black)
                FbButton(stringResource(R.string.fb_no), { confirm = false }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            }
        }
    }
}

// ====================================================================== duzenleyici

@Composable
private fun Editor(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    var country by remember { mutableIntStateOf(world.clubs[world.userClub].country) }
    var clubId by remember { mutableIntStateOf(world.userClub) }
    val countries = remember { FbCountries.all.indices.filter { world.leaguesOf(it).isNotEmpty() }.sortedByDescending { FbCountries.all[it].level } }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.width(180.dp).fillMaxHeight()) {
            LazyColumn {
                items(countries) { ci ->
                    Text(FbCountries.all[ci].englishName, color = if (ci == country) AppColors.Primary else AppColors.TextPrimary, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable { country = ci; clubId = world.leaguesOf(ci)[0].clubs[0] }.padding(6.dp))
                }
            }
        }
        FbPanel(Modifier.width(240.dp).fillMaxHeight()) {
            LazyColumn {
                for (lg in world.leaguesOf(country)) {
                    item(key = "lg${lg.id}") { LeagueNameEdit(world, vm, lg.id) }
                    items(lg.clubs.map { world.clubs[it] }.sortedByDescending { it.reputation }, key = { "c${it.id}" }) { cl ->
                        Text(cl.name, color = if (cl.id == clubId) AppColors.Primary else AppColors.TextPrimary, fontSize = 13.sp, maxLines = 1,
                            modifier = Modifier.fillMaxWidth().clickable { clubId = cl.id }.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }
        ClubEdit(world, vm, world.clubs[clubId], Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun LeagueNameEdit(world: FbWorld, vm: FootballViewModel, leagueId: Int) {
    val lg = world.leagues[leagueId]
    var name by remember(leagueId) { mutableStateOf(lg.customName) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it.take(32) }, singleLine = true, modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.fb_tier_n, lg.tier)) }, placeholder = { Text(leagueName(world, leagueId)) })
        FbButton("✓", { vm.edit { lg.customName = name.trim() } }, Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ClubEdit(world: FbWorld, vm: FootballViewModel, c: FbClub, modifier: Modifier) {
    var name by remember(c.id) { mutableStateOf(c.name) }
    var short by remember(c.id) { mutableStateOf(c.shortName) }
    var stadium by remember(c.id) { mutableStateOf(c.stadiumName) }
    var playerId by remember(c.id) { mutableIntStateOf(-1) }
    FbPanel(modifier) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FbCrest(c.color1, c.color2, short, 48.dp)
                Text(stringResource(R.string.fb_edit_club), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
            }
            OutlinedTextField(value = name, onValueChange = { name = it.take(32) }, singleLine = true, label = { Text(stringResource(R.string.fb_club_name)) }, modifier = Modifier.fillMaxWidth())
            Row {
                OutlinedTextField(value = short, onValueChange = { short = it.take(3).uppercase() }, singleLine = true, label = { Text(stringResource(R.string.fb_short_name)) }, modifier = Modifier.width(110.dp))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = stadium, onValueChange = { stadium = it.take(32) }, singleLine = true, label = { Text(stringResource(R.string.fb_stadium)) }, modifier = Modifier.weight(1f))
            }
            for (k in 0..1) {
                Text(stringResource(if (k == 0) R.string.fb_color_main else R.string.fb_color_second), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in FbGen.PALETTE) {
                        val sel = (if (k == 0) c.color1 else c.color2) == col
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(col)).border(if (sel) 3.dp else 1.dp, if (sel) FbColors.Yellow else Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable { vm.edit { if (k == 0) c.color1 = col else c.color2 = col } })
                    }
                }
            }
            FbButton(stringResource(R.string.fb_save), {
                vm.edit { c.name = name.trim().ifEmpty { c.name }; c.shortName = short.trim().ifEmpty { c.shortName }; c.stadiumName = stadium.trim() }
                vm.toast(R.string.fb_saved)
            }, Modifier.padding(top = 10.dp))
            // Oyuncu adlari
            Text(stringResource(R.string.fb_edit_players), color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            for (p in world.clubPlayers(c.id).sortedBy { it.squadNumber }) {
                if (playerId == p.id) {
                    var first by remember(p.id) { mutableStateOf(p.firstName) }
                    var last by remember(p.id) { mutableStateOf(p.lastName) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = first, onValueChange = { first = it.take(20) }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(value = last, onValueChange = { last = it.take(24) }, singleLine = true, modifier = Modifier.weight(1f))
                        FbButton("✓", { vm.edit { p.firstName = first.trim().ifEmpty { p.firstName }; p.lastName = last.trim().ifEmpty { p.lastName } }; playerId = -1 }, Modifier.padding(start = 4.dp))
                    }
                } else Text("${p.squadNumber}. ${p.fullName}", color = AppColors.TextMuted, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().clickable { playerId = p.id }.padding(vertical = 4.dp))
            }
        }
    }
}
