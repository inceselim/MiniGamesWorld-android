package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.background
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.football.engine.CompKind
import com.eomer91.minigamesworld.ui.games.football.engine.EvType
import com.eomer91.minigamesworld.ui.games.football.engine.FbClubLife
import com.eomer91.minigamesworld.ui.games.football.engine.FbCountries
import com.eomer91.minigamesworld.ui.games.football.engine.FbFixture
import com.eomer91.minigamesworld.ui.games.football.engine.FbNews
import com.eomer91.minigamesworld.ui.games.football.engine.FbSeason
import com.eomer91.minigamesworld.ui.games.football.engine.FbTransfer
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.Fin
import com.eomer91.minigamesworld.ui.games.football.engine.NewsKind
import com.eomer91.minigamesworld.ui.games.football.engine.OfferStatus
import com.eomer91.minigamesworld.ui.theme.AppColors

// ====================================================================== kabuk

private class NavItem(val tab: FbTab, val icon: ImageVector, val label: Int)

private val NAV = listOf(
    NavItem(FbTab.HOME, Icons.Rounded.Home, R.string.fb_tab_home),
    NavItem(FbTab.SQUAD, Icons.Rounded.Groups, R.string.fb_tab_squad),
    NavItem(FbTab.TACTICS, Icons.Rounded.Tune, R.string.fb_tab_tactics),
    NavItem(FbTab.FIXTURES, Icons.Rounded.CalendarMonth, R.string.fb_tab_fixtures),
    NavItem(FbTab.COMPETITIONS, Icons.Rounded.EmojiEvents, R.string.fb_tab_competitions),
    NavItem(FbTab.TRANSFERS, Icons.Rounded.SwapHoriz, R.string.fb_tab_transfers),
    NavItem(FbTab.CLUB, Icons.Rounded.AccountBalance, R.string.fb_tab_club),
    NavItem(FbTab.INBOX, Icons.Rounded.Mail, R.string.fb_tab_inbox),
    NavItem(FbTab.GUIDE, Icons.Rounded.School, R.string.fb_tab_guide),
)

@Composable
fun FbHub(world: FbWorld, ui: FbUiState, vm: FootballViewModel, onBack: () -> Unit) {
    Row(Modifier.fillMaxSize()) {
        // Sol menu
        Column(
            Modifier.width(74.dp).fillMaxHeight().background(FbColors.Panel).padding(vertical = 6.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(onClick = { vm.save(); onBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextSecondary)
            }
            val unread = world.market.news.count { newsUnread(it) }
            for (n in NAV) {
                val sel = ui.tab == n.tab
                Column(
                    Modifier.padding(vertical = 2.dp).width(66.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) AppColors.Primary.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { vm.setTab(n.tab) }.padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Icon(n.icon, contentDescription = null, tint = if (sel) AppColors.Primary else AppColors.TextSecondary, modifier = Modifier.size(22.dp))
                        if (n.tab == FbTab.INBOX && unread > 0) {
                            Box(Modifier.align(Alignment.TopEnd).size(9.dp).clip(CircleShape).background(FbColors.Red))
                        }
                    }
                    Text(stringResource(n.label), color = if (sel) AppColors.Primary else AppColors.TextSecondary, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            FbTopBar(world, vm)
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                when (ui.tab) {
                    FbTab.HOME -> FbHome(world, ui, vm)
                    FbTab.SQUAD -> FbSquad(world, ui, vm)
                    FbTab.INBOX -> FbInbox(world, ui, vm)
                    FbTab.TACTICS -> FbTactics(world, ui, vm)
                    FbTab.FIXTURES -> FbFixtures(world, ui, vm)
                    FbTab.COMPETITIONS -> FbCompetitions(world, ui, vm)
                    FbTab.TRANSFERS -> FbTransfers(world, ui, vm)
                    FbTab.CLUB -> FbClubScreen(world, ui, vm)
                    FbTab.GUIDE -> FbGuide()
                }
            }
        }
    }
    if (ui.negotiation != NO_NEGOTIATION) FbNegotiationDialog(world, ui, vm)
}

@Composable
private fun FbTopBar(world: FbWorld, vm: FootballViewModel) {
    val c = world.clubs[world.userClub]
    Row(
        Modifier.fillMaxWidth().height(54.dp).background(FbColors.Panel).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FbCrest(c.color1, c.color2, c.shortName, 36.dp)
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(c.name, color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
            Text(FbFormat.date(world) + "  ·  " + FbFormat.season(world.season), color = AppColors.TextSecondary, fontSize = 12.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 14.dp)) {
            Text(stringResource(R.string.fb_balance), color = AppColors.TextSecondary, fontSize = 11.sp)
            Text(FbFormat.money(c.balance), color = if (c.balance >= 0) FbColors.Green else FbColors.Red, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        val pending = FbSeason.pendingUserFixture(world) != null
        FbButton(stringResource(if (pending) R.string.fb_go_to_match else R.string.fb_continue), { vm.continueGame() }, color = FbColors.Green, textColor = Color.Black)
    }
}

// ====================================================================== ana sayfa

@Composable
private fun FbHome(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val c = world.clubs[world.userClub]
    val st = world.state!!
    val showRewarded = rememberRewardedLauncher()
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Sonraki maclar
            val upcoming = st.fixturesOf(world.userClub, false).filter { !it.played }.take(4)
            FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_next_matches)) {
                if (upcoming.isEmpty()) Text(stringResource(R.string.fb_no_matches), color = AppColors.TextSecondary, fontSize = 13.sp)
                for (f in upcoming) FixtureLine(world, f)
            }
            val recent = st.fixturesOf(world.userClub, false).filter { it.played }.takeLast(5).reversed()
            if (recent.isNotEmpty()) FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_recent_results)) {
                for (f in recent) FixtureLine(world, f)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Lig tablosu (kulubun cevresi)
            val comp = st.comps.firstOrNull { it.kind == CompKind.LEAGUE && it.league == c.league }
            if (comp != null) FbPanel(Modifier.fillMaxWidth(), title = leagueName(world, c.league)) {
                val t = st.table(comp.id, 0, 0)
                val me = t.indexOfFirst { it.team == world.userClub }
                val from = (me - 3).coerceIn(0, (t.size - 7).coerceAtLeast(0))
                MiniTableHeader()
                for (i in from until minOf(t.size, from + 7)) MiniTableRow(world, i + 1, t[i], t[i].team == world.userClub)
            }
            // Haberler
            val news = world.market.news.asReversed().filter { it.kind != NewsKind.TRANSFER_WORLD && it.kind != NewsKind.RUMOUR }.take(6)
            FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_latest_news)) {
                if (news.isEmpty()) Text(stringResource(R.string.fb_no_news), color = AppColors.TextSecondary, fontSize = 13.sp)
                for (n in news) Text(newsText(world, n), color = if (n.read) AppColors.TextSecondary else AppColors.TextPrimary, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 3.dp).clickable { vm.setTab(FbTab.INBOX) }, maxLines = 2)
            }
        }
        Column(Modifier.width(250.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_club_summary)) {
                FbKeyValue(stringResource(R.string.fb_balance), FbFormat.money(c.balance), if (c.balance >= 0) FbColors.Green else FbColors.Red)
                FbKeyValue(stringResource(R.string.fb_weekly_wages), FbFormat.money(c.players.sumOf { world.players[it].wage.toLong() }))
                val inc = c.ledger.withIndex().filter { Fin.entries[it.index].income }.sumOf { it.value }
                val exp = c.ledger.withIndex().filter { !Fin.entries[it.index].income }.sumOf { it.value }
                FbKeyValue(stringResource(R.string.fb_season_income), FbFormat.money(inc))
                FbKeyValue(stringResource(R.string.fb_season_expense), FbFormat.money(exp))
                FbKeyValue(stringResource(R.string.fb_reputation), "${c.reputation / 100}")
                FbKeyValue(stringResource(R.string.fb_squad_size), "${c.players.size}")
            }
            // Reklamla para
            FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_ad_bonus_title)) {
                val left = FbClubLife.adBonusLeft(world)
                val amount = FbClubLife.adBonusAmount(world, c)
                Text(stringResource(R.string.fb_ad_bonus_desc, FbFormat.money(amount)), color = AppColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FbButton(
                        stringResource(R.string.fb_watch_ad), {
                            showRewarded({ vm.claimAdBonus() }, { vm.toast(R.string.ad_not_ready) })
                        }, enabled = left > 0, color = FbColors.Yellow, textColor = Color.Black,
                    )
                    Icon(Icons.Rounded.OndemandVideo, contentDescription = null, tint = FbColors.Yellow, modifier = Modifier.padding(start = 8.dp))
                    Text(stringResource(R.string.fb_ad_bonus_left, left), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
fun FixtureLine(world: FbWorld, f: FbFixture) {
    val st = world.state!!
    val comp = st.comps[f.comp]
    val h = world.clubs[f.home]; val a = world.clubs[f.away]
    val user = world.userClub
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(FbFormat.shortDate(world, f.day), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(54.dp))
        Text(h.name, color = if (h.id == user) AppColors.Primary else AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
        val score = if (f.played) "${f.hg} - ${f.ag}" + (if (f.ph >= 0) " (${f.ph}-${f.pa})" else "") else "v"
        val resColor = if (!f.played) AppColors.TextSecondary else {
            val gf = if (f.home == user) f.hg else f.ag; val ga = if (f.home == user) f.ag else f.hg
            if (gf > ga) FbColors.Green else if (gf < ga) FbColors.Red else FbColors.Yellow
        }
        Text(score, color = resColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.width(78.dp), textAlign = TextAlign.Center)
        Text(a.name, color = if (a.id == user) AppColors.Primary else AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), maxLines = 1)
        Text(stringResource(compName(comp.kind)), color = AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(90.dp).padding(start = 6.dp), maxLines = 1)
    }
}

@Composable
fun MiniTableHeader() {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        FbCell("#", 24.dp, AppColors.TextSecondary)
        Text("", Modifier.weight(1f))
        for (h in stringResource(R.string.fb_table_cols).split(",")) FbCell(h, 30.dp, AppColors.TextSecondary, align = TextAlign.Center, size = 11.sp)
    }
}

@Composable
fun MiniTableRow(world: FbWorld, pos: Int, r: com.eomer91.minigamesworld.ui.games.football.engine.TableRow, highlight: Boolean) {
    val c = world.clubs[r.team]
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (highlight) AppColors.Primary.copy(alpha = 0.16f) else Color.Transparent).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FbCell("$pos", 24.dp, AppColors.TextSecondary)
        Text(c.name, Modifier.weight(1f), color = AppColors.TextPrimary, fontSize = 13.sp, maxLines = 1, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal)
        for (v in listOf(r.p, r.w, r.d, r.l, r.gd)) FbCell("$v", 30.dp, AppColors.TextMuted, align = TextAlign.Center)
        FbCell("${r.pts}", 30.dp, AppColors.TextPrimary, bold = true, align = TextAlign.Center)
    }
}

// ====================================================================== haberler

enum class NewsGroup { CLUB, MATCHES, TRANSFERS, WORLD }

fun newsGroup(k: NewsKind): NewsGroup = when (k) {
    NewsKind.MATCH_PREVIEW, NewsKind.MATCH_REPORT, NewsKind.CUP_DRAW, NewsKind.EURO_QUALIFIED, NewsKind.TROPHY -> NewsGroup.MATCHES
    NewsKind.OFFER_RECEIVED, NewsKind.OFFER_ACCEPTED, NewsKind.OFFER_REJECTED, NewsKind.OFFER_COUNTER, NewsKind.PLAYER_REFUSED,
    NewsKind.TRANSFER_DONE, NewsKind.RUMOUR, NewsKind.AUCTION_BID, NewsKind.AUCTION_RESULT, NewsKind.PRECONTRACT, NewsKind.LOAN_RETURN,
    NewsKind.WINDOW_OPEN, NewsKind.WINDOW_CLOSE -> NewsGroup.TRANSFERS
    NewsKind.TRANSFER_WORLD, NewsKind.LEAGUE_LEADER, NewsKind.PLAYER_OF_MONTH, NewsKind.RETIREMENT, NewsKind.AWARD_TOP_SCORER,
    NewsKind.AWARD_PLAYER_SEASON -> NewsGroup.WORLD
    else -> NewsGroup.CLUB
}

/** Okunmamis sayilan (vurgulu) haber: kucuk haberler (soylenti vb.) sayilmaz. */
fun newsUnread(n: FbNews) = !n.read && !newsMinor(n.kind)

/** Okunmamis sayacinda gosterilmeyen (kucuk) haberler */
fun newsMinor(k: NewsKind) = k == NewsKind.RUMOUR || k == NewsKind.TRANSFER_WORLD || k == NewsKind.MATCH_PREVIEW || k == NewsKind.CALLUP

@Composable
fun newsTitle(k: NewsKind): String = stringResource(when (k) {
    NewsKind.OFFER_RECEIVED -> R.string.fb_nt_offer_received; NewsKind.OFFER_ACCEPTED -> R.string.fb_nt_offer_accepted
    NewsKind.OFFER_REJECTED -> R.string.fb_nt_offer_rejected; NewsKind.OFFER_COUNTER -> R.string.fb_nt_offer_counter
    NewsKind.PLAYER_REFUSED -> R.string.fb_nt_player_refused; NewsKind.TRANSFER_DONE -> R.string.fb_nt_transfer_done
    NewsKind.TRANSFER_WORLD -> R.string.fb_nt_transfer_world; NewsKind.RUMOUR -> R.string.fb_nt_rumour
    NewsKind.CONTRACT_EXPIRING -> R.string.fb_nt_contract; NewsKind.PLAYER_UNHAPPY -> R.string.fb_nt_unhappy
    NewsKind.AUCTION_BID -> R.string.fb_nt_auction; NewsKind.AUCTION_RESULT -> R.string.fb_nt_auction
    NewsKind.LOAN_RETURN -> R.string.fb_nt_loan_return; NewsKind.PRECONTRACT -> R.string.fb_nt_precontract
    NewsKind.YOUTH_INTAKE -> R.string.fb_nt_youth; NewsKind.RELEASED -> R.string.fb_nt_released
    NewsKind.MATCH_PREVIEW -> R.string.fb_nt_preview; NewsKind.MATCH_REPORT -> R.string.fb_nt_report
    NewsKind.INJURY -> R.string.fb_nt_injury; NewsKind.SUSPENSION -> R.string.fb_nt_suspension
    NewsKind.LEAGUE_LEADER -> R.string.fb_nt_leader; NewsKind.PLAYER_OF_MONTH -> R.string.fb_nt_potm
    NewsKind.WINDOW_OPEN -> R.string.fb_nt_window_open; NewsKind.WINDOW_CLOSE -> R.string.fb_nt_window_close
    NewsKind.CUP_DRAW -> R.string.fb_nt_draw; NewsKind.EURO_QUALIFIED -> R.string.fb_nt_europe
    NewsKind.FACILITY_DONE -> R.string.fb_nt_facility; NewsKind.FINANCE_WARNING -> R.string.fb_nt_finance
    NewsKind.RETIREMENT -> R.string.fb_nt_retirement; NewsKind.AWARD_TOP_SCORER -> R.string.fb_nt_top_scorer
    NewsKind.AWARD_PLAYER_SEASON -> R.string.fb_nt_player_season; NewsKind.FANS_HAPPY -> R.string.fb_nt_fans_happy
    NewsKind.FANS_ANGRY -> R.string.fb_nt_fans_angry; NewsKind.TROPHY -> R.string.fb_nt_trophy
    NewsKind.CALLUP -> R.string.fb_nt_callup; NewsKind.YOUTH_GRADUATE -> R.string.fb_nt_graduate
})

@Composable
fun newsText(world: FbWorld, n: FbNews): String {
    val p = n.text.takeIf { it.isNotEmpty() && n.kind != NewsKind.INJURY } ?: world.players.getOrNull(n.player)?.fullName ?: "?"
    val c1 = world.clubs.getOrNull(n.club)?.name ?: ""
    val c2 = world.clubs.getOrNull(n.club2)?.name ?: ""
    val v = FbFormat.money(n.value)
    return when (n.kind) {
        NewsKind.OFFER_RECEIVED -> stringResource(R.string.fb_n_offer_received, c1, v, p)
        NewsKind.OFFER_ACCEPTED -> stringResource(R.string.fb_n_offer_accepted, c1, p, v)
        NewsKind.OFFER_REJECTED -> stringResource(R.string.fb_n_offer_rejected, c1, p)
        NewsKind.OFFER_COUNTER -> stringResource(R.string.fb_n_offer_counter, c1, p, v)
        NewsKind.PLAYER_REFUSED -> stringResource(R.string.fb_n_player_refused, p, c1)
        NewsKind.TRANSFER_DONE, NewsKind.TRANSFER_WORLD ->
            if (n.club < 0) stringResource(R.string.fb_n_signed_free, p, c2)
            else stringResource(R.string.fb_n_transfer, p, c1, c2, if (n.value > 0) v else stringResource(R.string.fb_free))
        NewsKind.RUMOUR -> stringResource(R.string.fb_n_rumour, c1, p, c2, n.credibility)
        NewsKind.CONTRACT_EXPIRING -> stringResource(R.string.fb_n_contract_expiring, p)
        NewsKind.PLAYER_UNHAPPY -> stringResource(R.string.fb_n_unhappy, p)
        NewsKind.AUCTION_BID -> stringResource(R.string.fb_n_auction_bid, c1, v, p)
        NewsKind.AUCTION_RESULT -> if (n.club >= 0) stringResource(R.string.fb_n_auction_sold, p, c1, v) else stringResource(R.string.fb_n_auction_unsold, p)
        NewsKind.LOAN_RETURN -> stringResource(R.string.fb_n_loan_return, p, c1)
        NewsKind.PRECONTRACT -> stringResource(R.string.fb_n_precontract, p, c1)
        NewsKind.YOUTH_INTAKE -> if (n.player >= 0) stringResource(R.string.fb_n_youth_best, n.value.toInt(), p) else stringResource(R.string.fb_n_youth, n.value.toInt())
        NewsKind.RELEASED -> stringResource(R.string.fb_n_released, p)
        NewsKind.MATCH_PREVIEW -> {
            val st = world.state!!
            val comp = st.comps.getOrNull(n.club2)
            val fw = stringResource(R.string.fb_form_w); val fd = stringResource(R.string.fb_form_d); val fl = stringResource(R.string.fb_form_l)
            val form = st.recentResults(n.club, world.day, 5).joinToString(" ") { when (it) { 3 -> fw; 1 -> fd; else -> fl } }
            stringResource(R.string.fb_n_preview, c1, if (comp != null) compTitle(world, comp.id) else "", form.ifEmpty { "-" })
        }
        NewsKind.MATCH_REPORT -> {
            val gf = (n.value / 100).toInt(); val ga = (n.value % 100).toInt()
            val res = stringResource(if (gf > ga) R.string.fb_n_report_win else if (gf < ga) R.string.fb_n_report_loss else R.string.fb_n_report_draw, c1, "$gf-$ga")
            res + " " + stringResource(R.string.fb_n_report_motm, p)
        }
        NewsKind.INJURY -> stringResource(if (n.text == "T") R.string.fb_n_injury_training else R.string.fb_n_injury, p, n.value.toInt())
        NewsKind.SUSPENSION -> stringResource(R.string.fb_n_suspension, p, n.value.toInt())
        NewsKind.LEAGUE_LEADER -> stringResource(R.string.fb_n_leader, c1, leagueName(world, n.club2), n.value.toInt())
        NewsKind.PLAYER_OF_MONTH -> stringResource(R.string.fb_n_potm, p, c1)
        NewsKind.WINDOW_OPEN -> stringResource(R.string.fb_n_window_open)
        NewsKind.WINDOW_CLOSE -> stringResource(R.string.fb_n_window_close)
        NewsKind.CUP_DRAW -> stringResource(R.string.fb_n_draw, compTitle(world, n.club2), stringResource(stageName(com.eomer91.minigamesworld.ui.games.football.engine.StageLabel.entries[n.value.toInt()])), c1)
        NewsKind.EURO_QUALIFIED -> stringResource(if (n.value == 1L) R.string.fb_n_europe_direct else R.string.fb_n_europe_qual, stringResource(compName(CompKind.entries[n.club2])))
        NewsKind.FACILITY_DONE -> stringResource(R.string.fb_n_facility, stringResource(facilityTitle(n.value.toInt())))
        NewsKind.FINANCE_WARNING -> stringResource(R.string.fb_n_finance, v)
        NewsKind.RETIREMENT -> stringResource(R.string.fb_n_retirement, p, n.value.toInt())
        NewsKind.AWARD_TOP_SCORER -> stringResource(R.string.fb_n_top_scorer, p, n.value.toInt(), leagueName(world, n.club2))
        NewsKind.AWARD_PLAYER_SEASON -> stringResource(R.string.fb_n_player_season, p, leagueName(world, n.club2), "%.2f".format(n.value / 100f))
        NewsKind.FANS_HAPPY -> stringResource(R.string.fb_n_fans_happy)
        NewsKind.FANS_ANGRY -> stringResource(R.string.fb_n_fans_angry)
        NewsKind.TROPHY -> {
            val kind = CompKind.entries[n.club2]
            val title = if (kind == CompKind.LEAGUE && n.value >= 0) leagueName(world, n.value.toInt()) else stringResource(compName(kind))
            val winner = if (kind.national) FbCountries.all.getOrNull(n.club)?.englishName ?: "" else c1
            stringResource(if (!kind.national && n.club == world.userClub) R.string.fb_n_trophy_us else R.string.fb_n_trophy, winner, title)
        }
        NewsKind.CALLUP -> stringResource(R.string.fb_n_callup, p, FbCountries.all.getOrNull(n.club2)?.englishName ?: "")
        NewsKind.YOUTH_GRADUATE -> stringResource(R.string.fb_n_graduate, p)
    }
}

private fun facilityTitle(i: Int): Int = when (com.eomer91.minigamesworld.ui.games.football.engine.Facility.entries[i]) {
    com.eomer91.minigamesworld.ui.games.football.engine.Facility.TRAINING -> R.string.fb_f_training
    com.eomer91.minigamesworld.ui.games.football.engine.Facility.YOUTH -> R.string.fb_f_youth
    com.eomer91.minigamesworld.ui.games.football.engine.Facility.RECRUITMENT -> R.string.fb_f_recruitment
    com.eomer91.minigamesworld.ui.games.football.engine.Facility.STADIUM -> R.string.fb_f_stadium
    com.eomer91.minigamesworld.ui.games.football.engine.Facility.SOCIAL -> R.string.fb_f_social
    com.eomer91.minigamesworld.ui.games.football.engine.Facility.CAPACITY -> R.string.fb_f_capacity
}

private fun groupColor(g: NewsGroup): Color = when (g) {
    NewsGroup.CLUB -> FbColors.Blue; NewsGroup.MATCHES -> FbColors.Green; NewsGroup.TRANSFERS -> FbColors.Purple; NewsGroup.WORLD -> FbColors.Yellow
}

// ====================================================================== gelen kutusu

@Composable
private fun FbInbox(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    var group by remember { mutableStateOf<NewsGroup?>(null) }
    var unreadOnly by remember { mutableStateOf(false) }
    var open by remember { mutableIntStateOf(-1) }
    val all = world.market.news
    val unreadCount = all.count { newsUnread(it) }
    val news = all.asReversed().filter { (group == null || newsGroup(it.kind) == group) && (!unreadOnly || newsUnread(it)) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FbChip(stringResource(R.string.fb_unread_n, unreadCount), unreadOnly, { unreadOnly = !unreadOnly })
                FbChip(stringResource(R.string.fb_all), group == null, { group = null })
                FbChip(stringResource(R.string.fb_ng_club), group == NewsGroup.CLUB, { group = NewsGroup.CLUB })
                FbChip(stringResource(R.string.fb_ng_matches), group == NewsGroup.MATCHES, { group = NewsGroup.MATCHES })
                FbChip(stringResource(R.string.fb_ng_transfers), group == NewsGroup.TRANSFERS, { group = NewsGroup.TRANSFERS })
                FbChip(stringResource(R.string.fb_ng_world), group == NewsGroup.WORLD, { group = NewsGroup.WORLD })
            }
            FbButton(stringResource(R.string.fb_mark_all_read), { vm.edit { w -> w.market.news.forEach { it.read = true } } },
                enabled = unreadCount > 0, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(news) { n -> NewsCard(world, n, vm) { open = it } }
        }
    }
    if (open >= 0) world.players.getOrNull(open)?.let { FbPlayerDialog(world, ui, vm, it) { open = -1 } }
}

@Composable
private fun NewsCard(world: FbWorld, n: FbNews, vm: FootballViewModel, onOpen: (Int) -> Unit) {
    val g = newsGroup(n.kind)
    val accent = groupColor(g)
    val unread = newsUnread(n)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (unread) FbColors.PanelHigh else FbColors.Panel)
            .then(if (unread) Modifier.border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp)) else Modifier)
            .clickable { if (!n.read) vm.edit { n.read = true } }
            .height(IntrinsicSize.Min),
    ) {
        Box(Modifier.width(if (unread) 6.dp else 3.dp).fillMaxHeight().background(if (unread) accent else accent.copy(alpha = 0.3f)))
        Column(Modifier.weight(1f).padding(10.dp).alpha(if (unread) 1f else 0.62f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (unread) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AppColors.Primary))
                    Spacer(Modifier.width(6.dp))
                }
                Text(newsTitle(n.kind), color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (unread) FbPill(stringResource(R.string.fb_new), AppColors.Primary, AppColors.OnPrimary)
                else Text(stringResource(R.string.fb_read_mark), color = AppColors.TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Text(if (n.season == world.season) FbFormat.shortDate(world, n.day) else FbFormat.season(n.season), color = AppColors.TextSecondary, fontSize = 11.sp)
            }
            if (n.kind == NewsKind.RUMOUR) RumourBody(world, n, onOpen)
            else Text(newsText(world, n), color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 2.dp))
            NewsActions(world, n, vm, onOpen)
        }
    }
}

/** Haberin turune gore isleme gecis/eylem butonlari; teklifte guncel durum. */
@Composable
private fun NewsActions(world: FbWorld, n: FbNews, vm: FootballViewModel, onOpen: (Int) -> Unit) {
    val user = world.userClub
    val p = world.players.getOrNull(n.player)
    fun done() { n.read = true }
    val offerKinds = setOf(NewsKind.OFFER_RECEIVED, NewsKind.OFFER_COUNTER, NewsKind.OFFER_ACCEPTED, NewsKind.OFFER_REJECTED, NewsKind.PLAYER_REFUSED)
    // Teklif haberleri: en son ilgili teklif (acik ya da kapanmis)
    val offer = when {
        n.kind in offerKinds -> world.market.offers.lastOrNull { it.player == n.player && (it.from == n.club || it.to == n.club) && (it.from == user || it.to == user) }
        n.kind == NewsKind.AUCTION_RESULT && n.club == user -> world.market.offers.lastOrNull { it.player == n.player && it.from == user && it.fee == n.value }
        else -> null
    }
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (offer != null) {
            val incoming = offer.to == user
            when {
                incoming && offer.status == OfferStatus.PENDING -> {
                    FbButton(stringResource(R.string.fb_accept), { vm.edit { FbTransfer.respondIncoming(it, offer, true); done() } }, color = FbColors.Green, textColor = Color.Black)
                    FbButton(stringResource(R.string.fb_reject), { vm.edit { FbTransfer.respondIncoming(it, offer, false); done() } }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                }
                !incoming && offer.status == OfferStatus.COUNTER -> {
                    FbButton(stringResource(R.string.fb_accept_counter, FbFormat.money(offer.counterFee)), {
                        vm.edit { if (!FbTransfer.acceptCounter(it, offer)) vm.toast(R.string.fb_no_funds); done() }
                    }, color = FbColors.Green, textColor = Color.Black)
                    FbButton(stringResource(R.string.fb_withdraw), { vm.edit { FbTransfer.withdraw(it, offer); done() } }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                }
                !incoming && offer.status == OfferStatus.AGREED ->
                    FbButton(stringResource(R.string.fb_negotiate_contract), { vm.edit { done() }; vm.openNegotiation(offer.id) }, color = AppColors.Primary)
            }
            FbPill(offerStatusText(offer, incoming), offerStatusColor(offer.status).copy(alpha = 0.22f), offerStatusColor(offer.status))
        }
        val own = p != null && p.club == user && user >= 0
        when (n.kind) {
            NewsKind.CONTRACT_EXPIRING, NewsKind.PLAYER_UNHAPPY -> if (own) FbButton(stringResource(R.string.fb_renew_contract), { vm.edit { done() }; vm.openRenewal(p!!.id) }, color = AppColors.Primary)
            NewsKind.MATCH_PREVIEW, NewsKind.INJURY, NewsKind.SUSPENSION ->
                FbButton(stringResource(R.string.fb_edit_tactics), { vm.edit { done() }; vm.setTab(FbTab.TACTICS) }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            NewsKind.YOUTH_INTAKE, NewsKind.YOUTH_GRADUATE ->
                FbButton(stringResource(R.string.fb_youth_team), { vm.edit { done() }; vm.setTab(FbTab.SQUAD) }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            NewsKind.FINANCE_WARNING, NewsKind.FACILITY_DONE, NewsKind.FANS_ANGRY, NewsKind.FANS_HAPPY ->
                FbButton(stringResource(R.string.fb_tab_club), { vm.edit { done() }; vm.setTab(FbTab.CLUB) }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            NewsKind.AUCTION_BID ->
                FbButton(stringResource(R.string.fb_auctions), { vm.edit { done() }; vm.setTab(FbTab.TRANSFERS) }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            else -> {}
        }
        if (p != null && !p.retired && n.kind != NewsKind.MATCH_PREVIEW && n.kind != NewsKind.YOUTH_INTAKE)
            FbButton(stringResource(R.string.fb_player_card), { vm.edit { done() }; onOpen(p.id) }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
    }
}

// ====================================================================== mac oncesi

@Composable
fun FbPreMatch(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val st = world.state!!
    val f = st.fixtures.getOrNull(ui.pending) ?: return
    val h = world.clubs[f.home]; val a = world.clubs[f.away]
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(compTitle(world, f.comp) + (if (f.stage >= 0) "  ·  " + stringResource(stageName(st.comps[f.comp].stages[f.stage].label)) else ""),
            color = AppColors.TextSecondary, fontSize = 14.sp)
        Text(FbFormat.date(world, f.day), color = AppColors.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TeamBlock(world, f.home, Modifier.weight(1f))
            Text(stringResource(R.string.fb_vs), color = AppColors.TextSecondary, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 18.dp))
            TeamBlock(world, f.away, Modifier.weight(1f))
        }
        if (f.leg == 2) {
            val first = st.compFixtures(f.comp).firstOrNull { it.stage == f.stage && it.group == f.group && it.leg == 1 }
            if (first != null) Text(stringResource(R.string.fb_first_leg, "${world.clubs[first.home].shortName} ${first.hg}-${first.ag} ${world.clubs[first.away].shortName}"),
                color = FbColors.Yellow, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FbButton(stringResource(R.string.fb_play_match), { vm.playLive() }, color = FbColors.Green, textColor = Color.Black)
            FbButton(stringResource(R.string.fb_quick_result), { vm.quickResult() })
            FbButton(stringResource(R.string.fb_edit_tactics), { vm.toHubTab(FbTab.TACTICS) }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            FbButton(stringResource(R.string.fb_squad), { vm.toHubTab(FbTab.SQUAD) }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
        }
    }
}

@Composable
private fun TeamBlock(world: FbWorld, club: Int, modifier: Modifier) {
    val c = world.clubs[club]
    val sheet = FbSeason.clubSheet(world, club)
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(FbColors.Panel).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FbCrest(c.color1, c.color2, c.shortName, 52.dp)
        Text(c.name, color = if (club == world.userClub) AppColors.Primary else AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp))
        Text(sheet.formation.label, color = AppColors.TextSecondary, fontSize = 12.sp)
        FbStars(sheet.strength / 3.2f, 14.dp)
        val recent = world.state!!.recentResults(club, world.day, 5)
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (r in recent) Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(if (r == 3) FbColors.Green else if (r == 1) FbColors.Yellow else FbColors.Red))
        }
    }
}

// ====================================================================== mac sonucu

@Composable
fun FbResultScreen(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val r = ui.result ?: return
    val sim = r.sim
    val f = world.state!!.fixtures[r.fixture]
    Row(Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Text(compTitle(world, f.comp), color = AppColors.TextSecondary, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(world.clubs[f.home].name, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("${sim.homeGoals} - ${sim.awayGoals}", color = FbColors.Yellow, fontSize = 30.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp))
                Text(world.clubs[f.away].name, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            }
            sim.shootout?.let { Text(stringResource(R.string.fb_penalties, "${it.first}-${it.second}"), color = FbColors.Yellow, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            // Goller
            for (e in sim.events.filter { it.type == EvType.GOAL || it.type == EvType.RED }) {
                val p = world.players.getOrNull(e.player)?.fullName ?: ""
                val icon = if (e.type == EvType.GOAL) "⚽" else "🟥"
                Text("${e.minute}'  $icon  $p", color = if (e.side == 0) AppColors.TextPrimary else AppColors.TextMuted, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), textAlign = if (e.side == 0) TextAlign.Start else TextAlign.End)
            }
            Spacer(Modifier.height(10.dp))
            FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_match_stats)) {
                val hs = sim.home.stats; val as_ = sim.away.stats
                val total = (hs.possessionSec + as_.possessionSec).coerceAtLeast(1)
                StatLine(stringResource(R.string.fb_possession), "${hs.possessionSec * 100 / total}%", "${as_.possessionSec * 100 / total}%")
                StatLine(stringResource(R.string.fb_shots), "${hs.shots} (${hs.onTarget})", "${as_.shots} (${as_.onTarget})")
                StatLine("xG", "%.2f".format(hs.xg), "%.2f".format(as_.xg))
                StatLine(stringResource(R.string.fb_corners), "${hs.corners}", "${as_.corners}")
                StatLine(stringResource(R.string.fb_fouls), "${hs.fouls}", "${as_.fouls}")
                StatLine(stringResource(R.string.fb_cards), "${hs.yellows}/${hs.reds}", "${as_.yellows}/${as_.reds}")
            }
        }
        FbPanel(Modifier.width(300.dp).fillMaxHeight(), title = stringResource(R.string.fb_player_ratings)) {
            val mine = if (f.home == world.userClub) sim.home else sim.away
            LazyColumn(Modifier.weight(1f)) {
                items(mine.all().filter { it.minutes > 0 }.sortedByDescending { it.rating }) { mp ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        FbCell(stringResource(posShort(mp.pos)), 38.dp, AppColors.TextSecondary)
                        Text(mp.p.fullName, color = AppColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("%.1f".format(mp.rating), color = if (mp.rating >= 7.5f) FbColors.Green else if (mp.rating >= 6.5f) FbColors.Yellow else FbColors.Red,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            FbButton(stringResource(R.string.fb_continue), { vm.closeResult() }, Modifier.fillMaxWidth().padding(top = 8.dp), color = FbColors.Green, textColor = Color.Black)
        }
    }
}

@Composable
private fun StatLine(label: String, h: String, a: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(h, color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Text(label, color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(a, color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
    }
}

// ====================================================================== sezon sonu

@Composable
fun FbSeasonEnd(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val s = ui.summary ?: return
    val st = world.state!!
    val user = world.userClub
    val c = world.clubs[user]
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.fb_season_over, FbFormat.season(s.season)), color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.fb_final_position, c.lastPosition), color = FbColors.Yellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        when {
            s.promoted.any { it.first == user } -> Text(stringResource(R.string.fb_promoted), color = FbColors.Green, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            s.relegated.any { it.first == user } -> Text(stringResource(R.string.fb_relegated), color = FbColors.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_honours_this_season)) {
            val hs = st.honours.filter { it.season == s.season && (it.kind.national || it.kind in setOf(CompKind.CHAMPIONS, CompKind.EUROPA, CompKind.CONFERENCE) || it.country == c.country) }
            for (h in hs) {
                val win = if (h.kind.national) FbCountries.all[h.winner].englishName else world.clubs[h.winner].name
                val title = if (h.kind == CompKind.LEAGUE) stringResource(R.string.fb_league_name, FbCountries.all[h.country].englishName, h.tier) else stringResource(compName(h.kind))
                Text("$title: $win", color = if (h.winner == user && !h.kind.national) FbColors.Yellow else AppColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        FbButton(stringResource(R.string.fb_new_season, FbFormat.season(world.season)), { vm.closeSeasonSummary() }, color = FbColors.Green, textColor = Color.Black)
    }
}
