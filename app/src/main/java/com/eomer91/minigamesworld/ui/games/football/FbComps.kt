package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.CompKind
import com.eomer91.minigamesworld.ui.games.football.engine.FbComp
import com.eomer91.minigamesworld.ui.games.football.engine.FbCountries
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.StageType
import com.eomer91.minigamesworld.ui.games.football.engine.TableRow
import com.eomer91.minigamesworld.ui.theme.AppColors

// ====================================================================== fikstur

private enum class FixFilter { ALL, LEAGUE, CUPS, EUROPE, FRIENDLY }

@Composable
fun FbFixtures(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    var filter by remember { mutableStateOf(FixFilter.ALL) }
    val st = world.state!!
    val all = st.fixturesOf(world.userClub, false)
    val list = all.filter { f ->
        when (filter) {
            FixFilter.ALL -> true
            FixFilter.LEAGUE -> st.comps[f.comp].kind == CompKind.LEAGUE || st.comps[f.comp].kind == CompKind.PLAYOFF
            FixFilter.CUPS -> st.comps[f.comp].kind == CompKind.CUP || st.comps[f.comp].kind == CompKind.SUPER_CUP
            FixFilter.EUROPE -> st.comps[f.comp].kind in EURO_KINDS
            FixFilter.FRIENDLY -> st.comps[f.comp].kind == CompKind.FRIENDLY
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FbChip(stringResource(R.string.fb_all), filter == FixFilter.ALL, { filter = FixFilter.ALL })
            FbChip(stringResource(R.string.fb_c_league), filter == FixFilter.LEAGUE, { filter = FixFilter.LEAGUE })
            FbChip(stringResource(R.string.fb_cups), filter == FixFilter.CUPS, { filter = FixFilter.CUPS })
            FbChip(stringResource(R.string.fb_europe), filter == FixFilter.EUROPE, { filter = FixFilter.EUROPE })
            FbChip(stringResource(R.string.fb_c_friendly), filter == FixFilter.FRIENDLY, { filter = FixFilter.FRIENDLY })
        }
        val next = list.indexOfFirst { !it.played }.coerceAtLeast(0)
        LazyColumn(Modifier.weight(1f), state = androidx.compose.foundation.lazy.rememberLazyListState((next - 3).coerceAtLeast(0))) {
            items(list, key = { it.id }) { f ->
                Column(Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp)).background(if (f.played) FbColors.Panel else FbColors.PanelHigh).padding(horizontal = 8.dp)) {
                    FixtureLine(world, f)
                }
            }
        }
    }
}

private val EURO_KINDS = setOf(CompKind.CHAMPIONS, CompKind.EUROPA, CompKind.CONFERENCE, CompKind.UEFA_SUPER)

// ====================================================================== ligler ve kupalar

/** Gruplar: ulke indeksi, -1 Avrupa, -2 milli takimlar */
private const val GROUP_EUROPE = -1
private const val GROUP_NATIONAL = -2

@Composable
fun FbCompetitions(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val st = world.state!!
    val userCountry = world.clubs[world.userClub].country
    var group by remember { mutableIntStateOf(userCountry) }
    val countries = remember { FbCountries.all.indices.filter { world.leaguesOf(it).isNotEmpty() }.sortedByDescending { FbCountries.all[it].level } }
    val comps = when (group) {
        GROUP_EUROPE -> st.comps.filter { it.kind in EURO_KINDS }
        GROUP_NATIONAL -> st.comps.filter { it.national && it.kind != CompKind.NAT_FRIENDLY }
        else -> st.comps.filter { it.country == group && it.kind != CompKind.FRIENDLY }
            .sortedBy { c -> when (c.kind) { CompKind.LEAGUE -> world.leagues[c.league].tier; CompKind.PLAYOFF -> 10 + world.leagues[c.league].tier; CompKind.CUP -> 20; else -> 30 } }
    }
    var compId by remember(group) { mutableIntStateOf(comps.firstOrNull { it.kind == CompKind.LEAGUE && world.clubs[world.userClub].league == it.league }?.id ?: comps.firstOrNull()?.id ?: -1) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FbPanel(Modifier.width(210.dp).fillMaxHeight()) {
            LazyColumn {
                item { GroupItem(stringResource(R.string.fb_europe), group == GROUP_EUROPE) { group = GROUP_EUROPE } }
                item { GroupItem(stringResource(R.string.fb_national_teams), group == GROUP_NATIONAL) { group = GROUP_NATIONAL } }
                items(countries) { ci -> GroupItem(FbCountries.all[ci].englishName, group == ci) { group = ci } }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in comps) FbChip(compChipName(world, c), c.id == compId, { compId = c.id })
            }
            val comp = st.comps.getOrNull(compId)
            if (comp == null) Text(stringResource(R.string.fb_no_competitions), color = AppColors.TextSecondary, fontSize = 14.sp)
            else CompView(world, comp)
        }
    }
}

@Composable
private fun GroupItem(text: String, sel: Boolean, onClick: () -> Unit) {
    Text(
        text, color = if (sel) AppColors.OnPrimary else AppColors.TextPrimary, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (sel) AppColors.Primary else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun compChipName(world: FbWorld, c: FbComp): String = when (c.kind) {
    CompKind.LEAGUE -> stringResource(R.string.fb_tier_n, world.leagues[c.league].tier)
    CompKind.PLAYOFF -> stringResource(R.string.fb_c_playoff) + " " + world.leagues[c.league].tier
    CompKind.QUALIFIERS -> stringResource(R.string.fb_c_qualifiers) + " " + com.eomer91.minigamesworld.ui.games.football.engine.Confed.entries[c.confed].name
    else -> stringResource(compName(c.kind))
}

private fun teamName(world: FbWorld, national: Boolean, t: Int): String =
    if (national) FbCountries.all.getOrNull(t)?.englishName ?: "?" else world.clubs.getOrNull(t)?.name ?: "?"

@Composable
private fun CompView(world: FbWorld, comp: FbComp) {
    val st = world.state!!
    val built = comp.stages.withIndex().filter { it.value.built }
    var stage by remember(comp.id) { mutableIntStateOf(built.lastOrNull { !it.value.complete }?.index ?: built.lastOrNull()?.index ?: 0) }
    Column(Modifier.fillMaxSize()) {
        if (comp.stages.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((i, s) in comp.stages.withIndex()) if (s.built) FbChip(stringResource(stageName(s.label)), i == stage, { stage = i })
        }
        if (comp.done && comp.winner >= 0) Text(stringResource(R.string.fb_winner, teamName(world, comp.national, comp.winner)), color = FbColors.Yellow, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        val s = comp.stages.getOrNull(stage)
        if (s == null || !s.built) Text(stringResource(R.string.fb_not_started), color = AppColors.TextSecondary, fontSize = 14.sp)
        else Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1.4f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (s.type) {
                    StageType.TABLE, StageType.SWISS -> {
                        for (g in s.groups.indices) {
                            val rows = st.table(comp.id, stage, g)
                            val title = if (s.groups.size > 1) stringResource(R.string.fb_group_n, ('A' + g).toString()) else null
                            FbPanel(Modifier.fillMaxWidth(), title = title) {
                                TableHeader()
                                rows.forEachIndexed { i, r -> TableLine(world, comp, i + 1, r, rows.size) }
                            }
                        }
                    }
                    StageType.KO -> FbPanel(Modifier.fillMaxWidth()) {
                        val fx = st.compFixtures(comp.id).filter { it.stage == stage }
                        for ((ti, tie) in s.ties.withIndex()) {
                            val legs = fx.filter { it.group == ti }.sortedBy { it.leg }
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(teamName(world, comp.national, tie.a), color = tieColor(world, comp, tie.a, tie.winner), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
                                val score = legs.joinToString("  ") { f ->
                                    if (!f.played) FbFormat.shortDate(world, f.day)
                                    else { val a = if (f.home == tie.a) f.hg else f.ag; val b = if (f.home == tie.a) f.ag else f.hg; "$a-$b" + (if (f.ph >= 0) " p" else "") }
                                }
                                Text(score, color = AppColors.TextMuted, fontSize = 13.sp, modifier = Modifier.width(130.dp), textAlign = TextAlign.Center)
                                Text(teamName(world, comp.national, tie.b), color = tieColor(world, comp, tie.b, tie.winner), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f), maxLines = 1)
                            }
                        }
                        if (s.byes.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.fb_byes, s.byes.size), color = AppColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            // Gol kralligi
            FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_top_scorers)) {
                val top = remember(comp.id, st.fixtures.count { it.played && it.comp == comp.id }) {
                    st.stats.entries.filter { (it.key shr 32).toInt() == comp.id && it.value.goals > 0 }
                        .sortedWith(compareByDescending<Map.Entry<Long, com.eomer91.minigamesworld.ui.games.football.engine.PlayerCompStats>> { it.value.goals }.thenByDescending { it.value.assists })
                        .take(20).map { (it.key and 0xffffffffL).toInt() to it.value }
                }
                LazyColumn {
                    items(top) { (pid, s2) ->
                        val p = world.players.getOrNull(pid)
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p?.fullName ?: "?", color = if (p?.club == world.userClub) AppColors.Primary else AppColors.TextPrimary, fontSize = 13.sp, maxLines = 1)
                                Text(if (comp.national) FbCountries.all[p?.nation ?: 0].englishName else world.clubs.getOrNull(p?.club ?: -1)?.name ?: "",
                                    color = AppColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                            }
                            FbCell("${s2.goals}", 28.dp, FbColors.Yellow, bold = true, align = TextAlign.End)
                            FbCell("${s2.assists}", 28.dp, AppColors.TextSecondary, align = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

private fun tieColor(world: FbWorld, comp: FbComp, team: Int, winner: Int): Color = when {
    !comp.national && team == world.userClub -> AppColors.Primary
    winner == team -> FbColors.Green
    winner >= 0 -> AppColors.TextSecondary
    else -> AppColors.TextPrimary
}

@Composable
private fun TableHeader() {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        FbCell("#", 26.dp, AppColors.TextSecondary)
        Text("", Modifier.weight(1f))
        for (h in stringResource(R.string.fb_table_cols).split(",")) FbCell(h, 32.dp, AppColors.TextSecondary, align = TextAlign.Center, size = 11.sp)
        FbCell("", 60.dp)
    }
}

@Composable
private fun TableLine(world: FbWorld, comp: FbComp, pos: Int, r: TableRow, size: Int) {
    val user = !comp.national && r.team == world.userClub
    // Bolge rengi: sampiyon/yukselme yesil, dusme kirmizi
    val zone = when {
        comp.kind == CompKind.LEAGUE -> {
            val lg = world.leagues[comp.league]
            when {
                lg.tier > 1 && pos <= lg.promoted -> FbColors.Green
                lg.tier == 1 && pos == 1 -> FbColors.Yellow
                lg.relegated > 0 && pos > size - lg.relegated -> FbColors.Red
                else -> Color.Transparent
            }
        }
        comp.kind in EURO_KINDS -> if (pos <= 8) FbColors.Green else if (pos <= 24) FbColors.Yellow else FbColors.Red
        else -> if (pos <= 2) FbColors.Green else Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (user) AppColors.Primary.copy(alpha = 0.16f) else Color.Transparent).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.width(26.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(Modifier.width(3.dp).height(14.dp).background(zone))
            Text("$pos", color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
        Text(teamName(world, comp.national, r.team), Modifier.weight(1f), color = AppColors.TextPrimary, fontSize = 13.sp, maxLines = 1,
            fontWeight = if (user) FontWeight.Bold else FontWeight.Normal)
        for (v in listOf(r.p, r.w, r.d, r.l, r.gd)) FbCell("$v", 32.dp, AppColors.TextMuted, align = TextAlign.Center)
        FbCell("${r.pts}", 32.dp, AppColors.TextPrimary, bold = true, align = TextAlign.Center)
        Row(Modifier.width(60.dp).padding(start = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (ch in r.form) androidx.compose.foundation.layout.Box(Modifier.width(9.dp).height(9.dp).clip(RoundedCornerShape(2.dp))
                .background(when (ch) { 'G' -> FbColors.Green; 'B' -> FbColors.Yellow; else -> FbColors.Red }))
        }
    }
}
