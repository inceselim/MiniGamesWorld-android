package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.CLUB_ABROAD
import com.eomer91.minigamesworld.ui.games.football.engine.CLUB_NONE
import com.eomer91.minigamesworld.ui.games.football.engine.FbCountries
import com.eomer91.minigamesworld.ui.games.football.engine.FbAuction
import com.eomer91.minigamesworld.ui.games.football.engine.FbNews
import com.eomer91.minigamesworld.ui.games.football.engine.FbOffer
import com.eomer91.minigamesworld.ui.games.football.engine.FbPlayer
import com.eomer91.minigamesworld.ui.games.football.engine.FbTransfer
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.NewsKind
import com.eomer91.minigamesworld.ui.games.football.engine.OfferStatus
import com.eomer91.minigamesworld.ui.games.football.engine.Pos
import com.eomer91.minigamesworld.ui.theme.AppColors

private enum class TrPage { SEARCH, SHORTLIST, SCOUT, OFFERS, AUCTIONS, HISTORY }

@Composable
fun FbTransfers(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    var page by remember { mutableStateOf(TrPage.SEARCH) }
    var open by remember { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FbChip(stringResource(R.string.fb_search), page == TrPage.SEARCH, { page = TrPage.SEARCH })
                FbChip(stringResource(R.string.fb_shortlist) + " (${world.market.shortlist.size})", page == TrPage.SHORTLIST, { page = TrPage.SHORTLIST })
                FbChip(stringResource(R.string.fb_scout_reports), page == TrPage.SCOUT, { page = TrPage.SCOUT })
                FbChip(stringResource(R.string.fb_offers) + " (${world.market.offers.count { it.open && (it.from == world.userClub || it.to == world.userClub) }})", page == TrPage.OFFERS, { page = TrPage.OFFERS })
                FbChip(stringResource(R.string.fb_auctions), page == TrPage.AUCTIONS, { page = TrPage.AUCTIONS })
                FbChip(stringResource(R.string.fb_history), page == TrPage.HISTORY, { page = TrPage.HISTORY })
            }
            val open2 = FbTransfer.windowOpen(world)
            FbPill(stringResource(if (open2) R.string.fb_window_open else R.string.fb_window_closed), if (open2) FbColors.Green.copy(alpha = 0.25f) else FbColors.Red.copy(alpha = 0.25f))
        }
        Box(Modifier.weight(1f)) {
            when (page) {
                TrPage.SEARCH -> Search(world, ui) { open = it }
                TrPage.SHORTLIST -> PlayerList(world, world.market.shortlist.mapNotNull { world.players.getOrNull(it) }.filter { !it.retired }) { open = it }
                TrPage.SCOUT -> Scout(world, ui) { open = it }
                TrPage.OFFERS -> Offers(world, ui, vm) { open = it }
                TrPage.AUCTIONS -> Auctions(world, ui, vm) { open = it }
                TrPage.HISTORY -> History(world) { open = it }
            }
        }
    }
    if (open >= 0) world.players.getOrNull(open)?.let { FbPlayerDialog(world, ui, vm, it) { open = -1 } }
}

// ====================================================================== arama

private class SearchFilter(val line: Int, val age: Int, val maxValue: Long, val listedOnly: Boolean, val freeOnly: Boolean, val name: String)

@Composable
private fun Search(world: FbWorld, ui: FbUiState, onOpen: (Int) -> Unit) {
    var line by remember { mutableIntStateOf(-1) }
    var age by remember { mutableIntStateOf(0) }
    var maxValue by remember { mutableLongStateOf(0L) }
    var listed by remember { mutableStateOf(false) }
    var free by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FbPlayer>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }

    fun run() {
        val f = SearchFilter(line, age, maxValue, listed, free, name.trim().lowercase())
        val user = world.userClub
        results = world.players.asSequence().filter { p ->
            !p.retired && p.club != user && p.club != CLUB_ABROAD &&
                (f.line < 0 || p.bestPos().line.ordinal == f.line) &&
                (f.age == 0 || world.age(p).let { a -> when (f.age) { 1 -> a <= 21; 2 -> a in 22..29; else -> a >= 30 } }) &&
                (!f.listedOnly || p.askingPrice > 0 || p.loanListed) && (!f.freeOnly || p.club == CLUB_NONE) &&
                (f.name.isEmpty() || p.fullName.lowercase().contains(f.name)) &&
                (f.maxValue == 0L || FbTransfer.value(world, p) <= f.maxValue)
        }.sortedByDescending { FbTransfer.estimate(world, user, it).first }.take(120).toList()
        searched = true
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FbChip(stringResource(R.string.fb_all), line < 0, { line = -1 })
            for (l in Pos.Line.entries) FbChip(stringResource(when (l) { Pos.Line.GK -> R.string.fb_line_gk; Pos.Line.DEF -> R.string.fb_line_def; Pos.Line.MID -> R.string.fb_line_mid; Pos.Line.ATT -> R.string.fb_line_att }), line == l.ordinal, { line = l.ordinal })
            Spacer(Modifier.width(8.dp))
            for ((i, t) in listOf(R.string.fb_any_age, R.string.fb_age_young, R.string.fb_age_prime, R.string.fb_age_veteran).withIndex()) FbChip(stringResource(t), age == i, { age = i })
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for (v in listOf(0L, 1_000_000L, 5_000_000L, 20_000_000L, 50_000_000L)) FbChip(if (v == 0L) stringResource(R.string.fb_any_value) else "≤ " + FbFormat.money(v), maxValue == v, { maxValue = v })
            Spacer(Modifier.width(8.dp))
            FbChip(stringResource(R.string.fb_listed_only), listed, { listed = !listed })
            FbChip(stringResource(R.string.fb_free_agents), free, { free = !free })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(30) }, singleLine = true, label = { Text(stringResource(R.string.fb_player_name)) }, modifier = Modifier.width(260.dp))
            Spacer(Modifier.width(10.dp))
            FbButton(stringResource(R.string.fb_search), { run() })
        }
        Spacer(Modifier.width(4.dp))
        if (searched && results.isEmpty()) Text(stringResource(R.string.fb_no_results), color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
        Box(Modifier.weight(1f)) { PlayerList(world, results, onOpen) }
    }
}

/** Ortak oyuncu listesi satiri (tahmini yetenek ve potansiyel) */
@Composable
private fun PlayerList(world: FbWorld, list: List<FbPlayer>, onOpen: (Int) -> Unit) {
    val user = world.userClub
    LazyColumn {
        items(list, key = { it.id }) { p ->
            val (eca, epa) = FbTransfer.estimate(world, user, p)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp)).background(FbColors.Panel)
                    .clickable { onOpen(p.id) }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FbCell(stringResource(posShort(p.bestPos())), 42.dp, AppColors.TextSecondary, bold = true)
                Column(Modifier.weight(1f)) {
                    Text(p.fullName, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text((world.clubs.getOrNull(p.club)?.name ?: stringResource(R.string.fb_free_agent)) + " · " + FbCountries.all[p.nation].englishName,
                        color = AppColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                }
                FbCell("${world.age(p)}", 32.dp, AppColors.TextMuted)
                Box(Modifier.width(78.dp)) { FbStars(FbFormat.stars(eca), 12.dp) }
                Box(Modifier.width(78.dp)) { FbStars(FbFormat.stars(epa), 12.dp, if (p.id in world.revealed) FbColors.Green else FbColors.Yellow) }
                FbCell(FbFormat.money(FbTransfer.value(world, p)), 66.dp, AppColors.TextMuted)
                FbCell(when { p.askingPrice > 0 -> FbFormat.money(p.askingPrice); p.loanListed -> stringResource(R.string.fb_loan); else -> "" }, 66.dp, FbColors.Blue)
            }
        }
    }
}

// ====================================================================== gozlemci

@Composable
private fun Scout(world: FbWorld, ui: FbUiState, onOpen: (Int) -> Unit) {
    val c = world.clubs[world.userClub]
    var budget by remember { mutableLongStateOf(maxOf(500_000L, c.balance / 3)) }
    var recs by remember { mutableStateOf<List<FbPlayer>>(emptyList()) }
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.fb_scout_desc), color = AppColors.TextSecondary, fontSize = 13.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for (v in listOf(500_000L, 2_000_000L, 10_000_000L, 30_000_000L, 100_000_000L)) FbChip("≤ " + FbFormat.money(v), budget == v, { budget = v })
            FbButton(stringResource(R.string.fb_get_reports), { recs = FbTransfer.recommend(world, world.userClub, budget, 30).map { world.players[it.player] } })
        }
        Box(Modifier.weight(1f)) { PlayerList(world, recs, onOpen) }
    }
}

// ====================================================================== teklifler

@Composable
private fun Offers(world: FbWorld, ui: FbUiState, vm: FootballViewModel, onOpen: (Int) -> Unit) {
    val user = world.userClub
    val list = world.market.offers.filter { (it.from == user || it.to == user) && it.season == world.season }.asReversed()
    if (list.isEmpty()) { Text(stringResource(R.string.fb_no_offers), color = AppColors.TextSecondary, fontSize = 14.sp); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(list, key = { it.id }) { o -> OfferRow(world, vm, o, onOpen) }
    }
}

@Composable
private fun OfferRow(world: FbWorld, vm: FootballViewModel, o: FbOffer, onOpen: (Int) -> Unit) {
    val user = world.userClub
    val p = world.players.getOrNull(o.player) ?: return
    val incoming = o.to == user
    val other = world.clubs[if (incoming) o.from else o.to]
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (o.open) FbColors.PanelHigh else FbColors.Panel).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FbPill(stringResource(if (incoming) R.string.fb_incoming else R.string.fb_outgoing), if (incoming) FbColors.Purple.copy(alpha = 0.3f) else FbColors.Blue.copy(alpha = 0.3f))
            Text(p.fullName, color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).clickable { onOpen(p.id) })
            Text("  ·  " + other.name, color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Text(if (o.loan) stringResource(R.string.fb_loan) else FbFormat.money(o.fee), color = FbColors.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(offerStatusText(o, incoming), color = offerStatusColor(o.status), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                incoming && o.status == OfferStatus.PENDING -> {
                    FbButton(stringResource(R.string.fb_accept), { vm.edit { FbTransfer.respondIncoming(it, o, true) } }, color = FbColors.Green, textColor = Color.Black)
                    FbButton(stringResource(R.string.fb_counter_x, FbFormat.money(FbTransferRound(o.fee * 13 / 10))), { vm.edit { FbTransfer.respondIncoming(it, o, false, FbTransferRound(o.fee * 13 / 10)) } })
                    FbButton(stringResource(R.string.fb_reject), { vm.edit { FbTransfer.respondIncoming(it, o, false) } }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                }
                !incoming && o.status == OfferStatus.COUNTER -> {
                    FbButton(stringResource(R.string.fb_accept_counter, FbFormat.money(o.counterFee)), { vm.edit { if (!FbTransfer.acceptCounter(it, o)) vm.toast(R.string.fb_no_funds) } }, color = FbColors.Green, textColor = Color.Black)
                    val mid = FbTransferRound((o.fee + o.counterFee) / 2)
                    FbButton(stringResource(R.string.fb_offer_x, FbFormat.money(mid)), { vm.edit { FbTransfer.raiseOffer(it, o, mid) } })
                    FbButton(stringResource(R.string.fb_withdraw), { vm.edit { FbTransfer.withdraw(it, o) } }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                }
                !incoming && o.status == OfferStatus.AGREED -> FbButton(stringResource(R.string.fb_negotiate_contract), { vm.openNegotiation(o.id) })
                !incoming && o.status == OfferStatus.PENDING -> FbButton(stringResource(R.string.fb_withdraw), { vm.edit { FbTransfer.withdraw(it, o) } }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            }
        }
    }
}

private fun FbTransferRound(v: Long) = com.eomer91.minigamesworld.ui.games.football.engine.FbEconomy.roundValue(v)

// ====================================================================== acik artirmalar

@Composable
private fun Auctions(world: FbWorld, ui: FbUiState, vm: FootballViewModel, onOpen: (Int) -> Unit) {
    val user = world.userClub
    val list = world.market.auctions.filter { !it.done || it.endSeason == world.season }.asReversed().sortedBy { it.done }
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.fb_auction_desc), color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        if (list.isEmpty()) Text(stringResource(R.string.fb_no_auctions), color = AppColors.TextSecondary, fontSize = 14.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.id }) { a ->
                val p = world.players.getOrNull(a.player)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(FbColors.Panel).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { p?.let { onOpen(it.id) } }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p?.fullName ?: "?", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (a.seller == user) FbPill(stringResource(R.string.fb_au_yours), FbColors.Purple.copy(alpha = 0.3f), modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(world.clubs[a.seller].name + " · " + stringResource(R.string.fb_reserve, FbFormat.money(a.reserve)) + " · " + stringResource(R.string.fb_au_bids, a.bids),
                            color = AppColors.TextSecondary, fontSize = 12.sp)
                        val (msg, col) = auctionStatus(world, a)
                        Text(msg, color = col, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 3.dp))
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 10.dp)) {
                        Text(if (a.topBid > 0) FbFormat.money(a.topBid) else "-", color = FbColors.Yellow, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(world.clubs.getOrNull(a.topBidder)?.shortName ?: "", color = if (a.topBidder == user) AppColors.Primary else AppColors.TextSecondary, fontSize = 11.sp)
                    }
                    val won = if (a.done && a.topBidder == user) world.market.offers.lastOrNull { it.player == a.player && it.from == user && it.fee == a.topBid && it.status == OfferStatus.AGREED } else null
                    if (won != null) FbButton(stringResource(R.string.fb_negotiate_contract), { vm.openNegotiation(won.id) }, color = AppColors.Primary)
                    if (!a.done && a.seller != user && a.topBidder != user) {
                        val next = if (a.topBid == 0L) a.reserve else FbTransfer.nextBid(a.topBid)
                        FbButton(stringResource(R.string.fb_bid_x, FbFormat.money(next)), {
                            vm.edit { w -> if (!FbTransfer.bid(w, a, user, next)) vm.toast(R.string.fb_no_funds) }
                        }, color = FbColors.Green, textColor = Color.Black)
                    }
                }
            }
        }
    }
}

// ====================================================================== gecmis ve soylentiler

@Composable
private fun History(world: FbWorld, onOpen: (Int) -> Unit) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.weight(1.3f), title = stringResource(R.string.fb_latest_transfers)) {
            val list = world.market.history.asReversed().take(250)
            LazyColumn {
                items(list) { h ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(h.playerName, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp), maxLines = 1)
                        Text((world.clubs.getOrNull(h.from)?.shortName ?: stringResource(R.string.fb_free_agent)) + " → " + (world.clubs.getOrNull(h.to)?.name ?: ""),
                            color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(if (h.loan) stringResource(R.string.fb_loan) else if (h.fee > 0) FbFormat.money(h.fee) else stringResource(R.string.fb_free),
                            color = if (h.fee >= 10_000_000) FbColors.Yellow else AppColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        FbPanel(Modifier.weight(1f), title = stringResource(R.string.fb_rumours)) {
            val rumours = world.market.news.asReversed().filter { it.kind == NewsKind.RUMOUR }.take(60)
            LazyColumn {
                items(rumours) { n ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(FbColors.PanelHigh).padding(8.dp)) {
                        Text(if (n.season == world.season) FbFormat.shortDate(world, n.day) else FbFormat.season(n.season), color = AppColors.TextSecondary, fontSize = 11.sp)
                        RumourBody(world, n, onOpen)
                    }
                }
            }
        }
    }
}

/** Teklifin durum metni (teklifler ekrani ve gelen kutusu). */
@Composable
fun offerStatusText(o: FbOffer, incoming: Boolean): String = stringResource(when (o.status) {
    OfferStatus.PENDING -> if (incoming) R.string.fb_os_waiting_you else R.string.fb_os_pending
    OfferStatus.COUNTER -> R.string.fb_os_counter
    OfferStatus.AGREED -> if (incoming) R.string.fb_os_agreed_in else R.string.fb_os_agreed
    OfferStatus.COMPLETED -> R.string.fb_os_completed; OfferStatus.REJECTED -> R.string.fb_os_rejected
    OfferStatus.WITHDRAWN -> R.string.fb_os_withdrawn; OfferStatus.PLAYER_REFUSED -> R.string.fb_os_refused
    OfferStatus.EXPIRED -> R.string.fb_os_expired
}, FbFormat.money(o.counterFee))

fun offerStatusColor(s: OfferStatus): Color = when (s) {
    OfferStatus.PENDING, OfferStatus.COUNTER, OfferStatus.AGREED -> FbColors.Yellow
    OfferStatus.COMPLETED -> FbColors.Green
    else -> FbColors.Red
}

/** Acik artirmanin anlasilir sonucu: devam / en yuksek sende / satildi / satilmadi / oyuncu reddetti. */
@Composable
fun auctionStatus(world: FbWorld, a: FbAuction): Pair<String, Color> {
    val user = world.userClub
    val bidder = world.clubs.getOrNull(a.topBidder)
    if (!a.done) {
        return if (a.topBidder == user) stringResource(R.string.fb_au_leading) to FbColors.Green
        else stringResource(R.string.fb_au_running, FbFormat.shortDate(world, a.endDay)) to FbColors.Yellow
    }
    if (bidder == null || a.topBid < a.reserve) return stringResource(R.string.fb_au_unsold) to FbColors.Red
    val p = world.players.getOrNull(a.player)
    if (a.topBidder == user) {
        val o = world.market.offers.lastOrNull { it.player == a.player && it.from == user && it.fee == a.topBid }
        return when {
            p?.club == user -> stringResource(R.string.fb_au_bought) to FbColors.Green
            o?.status == OfferStatus.AGREED -> stringResource(R.string.fb_au_won) to AppColors.Primary
            else -> stringResource(R.string.fb_au_refused, bidder.name) to FbColors.Red
        }
    }
    val moved = p?.club == a.topBidder || world.market.history.any { it.player == a.player && it.to == a.topBidder && it.season >= a.endSeason }
    return if (moved) stringResource(R.string.fb_au_sold, bidder.name, FbFormat.money(a.topBid)) to FbColors.Green
    else stringResource(R.string.fb_au_refused, bidder.name) to FbColors.Red
}

/** Soylenti: kim, kimi istiyor, kaynagin guvenilirligi ve sonucu (gerceklesti mi). */
@Composable
fun RumourBody(world: FbWorld, n: FbNews, onOpen: (Int) -> Unit) {
    val p = world.players.getOrNull(n.player)
    val suitor = world.clubs.getOrNull(n.club)
    val current = world.clubs.getOrNull(n.club2)
    val (credRes, credCol) = when {
        n.credibility >= 60 -> R.string.fb_cred_strong to FbColors.Green
        n.credibility >= 40 -> R.string.fb_cred_medium to FbColors.Yellow
        else -> R.string.fb_cred_weak to AppColors.TextSecondary
    }
    Column(Modifier.padding(top = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p?.fullName ?: n.text.ifEmpty { "?" }, color = AppColors.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                modifier = Modifier.clickable(enabled = p != null) { p?.let { onOpen(it.id) } })
            if (current != null && current.id == world.userClub) FbPill(stringResource(R.string.fb_rumour_yours), FbColors.Purple.copy(alpha = 0.3f), modifier = Modifier.padding(start = 8.dp))
        }
        Text((current?.name ?: stringResource(R.string.fb_free_agent)) + "  →  " + (suitor?.name ?: "?"), color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.fb_rumour_interest, suitor?.name ?: "?") + " · " + FbFormat.money(n.value), color = AppColors.TextSecondary, fontSize = 12.sp)
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            FbPill(stringResource(credRes) + " · " + n.credibility + "%", credCol.copy(alpha = 0.22f), credCol)
            Spacer(Modifier.width(6.dp))
            FbBar(n.credibility, credCol, 70.dp, 6.dp)
            Spacer(Modifier.width(8.dp))
            val rec = world.market.history.lastOrNull { it.player == n.player && (it.season > n.season || (it.season == n.season && it.day >= n.day)) }
            when {
                rec == null -> Text(stringResource(R.string.fb_rumour_open), color = AppColors.TextSecondary, fontSize = 12.sp)
                rec.to == n.club -> Text(stringResource(R.string.fb_rumour_true), color = FbColors.Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                else -> Text(stringResource(R.string.fb_rumour_elsewhere, world.clubs.getOrNull(rec.to)?.name ?: "?"), color = FbColors.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
