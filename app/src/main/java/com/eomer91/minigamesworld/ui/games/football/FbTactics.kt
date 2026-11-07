package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.AttackFocus
import com.eomer91.minigamesworld.ui.games.football.engine.DefLine
import com.eomer91.minigamesworld.ui.games.football.engine.FbClubLife
import com.eomer91.minigamesworld.ui.games.football.engine.FbLineup
import com.eomer91.minigamesworld.ui.games.football.engine.FbSeason
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.Formation
import com.eomer91.minigamesworld.ui.games.football.engine.Marking
import com.eomer91.minigamesworld.ui.games.football.engine.Mentality
import com.eomer91.minigamesworld.ui.games.football.engine.PassingStyle
import com.eomer91.minigamesworld.ui.games.football.engine.Pressing
import com.eomer91.minigamesworld.ui.games.football.engine.Tactics
import com.eomer91.minigamesworld.ui.games.football.engine.Tempo
import com.eomer91.minigamesworld.ui.games.football.engine.TrainArea
import com.eomer91.minigamesworld.ui.games.football.engine.Width
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.roundToInt

private enum class TacticsPage { LINEUP, INSTRUCTIONS, TRAINING }

@Composable
fun FbTactics(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    var page by remember { mutableStateOf(TacticsPage.LINEUP) }
    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
            FbChip(stringResource(R.string.fb_lineup), page == TacticsPage.LINEUP, { page = TacticsPage.LINEUP })
            FbChip(stringResource(R.string.fb_instructions), page == TacticsPage.INSTRUCTIONS, { page = TacticsPage.INSTRUCTIONS })
            FbChip(stringResource(R.string.fb_team_training), page == TacticsPage.TRAINING, { page = TacticsPage.TRAINING })
        }
        when (page) {
            TacticsPage.LINEUP -> Lineup(world, ui, vm)
            TacticsPage.INSTRUCTIONS -> Instructions(world, ui, vm)
            TacticsPage.TRAINING -> TeamTraining(world, ui, vm)
        }
    }
}

/** Duzenlemeye baslamadan once kayitli taktigi olustur. */
private fun ensureTactics(world: FbWorld, vm: FootballViewModel): Tactics {
    val t = world.userTactics ?: vm.userTactics(world)
    world.userTactics = t
    return t
}

// ====================================================================== dizilis

@Composable
private fun Lineup(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    var selected by remember { mutableIntStateOf(-1) }
    val tactics = world.userTactics ?: vm.userTactics(world)
    val sheet = FbSeason.clubSheet(world, world.userClub)
    val f = sheet.formation
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1.15f).fillMaxHeight()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (fm in Formation.entries) FbChip(fm.label, fm == f, { vm.edit { w -> w.userTactics = ensureTactics(w, vm).copy(formation = fm) } })
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FbColors.Pitch)) {
                val w = maxWidth; val h = maxHeight
                Canvas(Modifier.fillMaxSize()) {
                    val line = Color.White.copy(alpha = 0.35f)
                    val st = Stroke(width = 2f)
                    drawRect(line, Offset(8f, 8f), Size(size.width - 16f, size.height - 16f), style = st)
                    drawLine(line, Offset(size.width / 2, 8f), Offset(size.width / 2, size.height - 8f), 2f)
                    drawCircle(line, size.height * 0.15f, Offset(size.width / 2, size.height / 2), style = st)
                    val bw = size.width * 0.14f; val bh = size.height * 0.56f
                    drawRect(line, Offset(8f, (size.height - bh) / 2), Size(bw, bh), style = st)
                    drawRect(line, Offset(size.width - 8f - bw, (size.height - bh) / 2), Size(bw, bh), style = st)
                }
                for ((i, slot) in f.slots.withIndex()) {
                    val p = sheet.xi.getOrNull(i)
                    val x = w * (0.06f + slot.x * 0.86f) - 34.dp
                    val y = h * (0.06f + (1f - slot.y) * 0.84f) - 22.dp
                    Column(
                        Modifier.offset(x, y).width(68.dp).clickable { selected = if (selected == i) -1 else i },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val rating = if (p != null) FbFormat.attr(FbLineup.slotScore(p, slot.pos)) else 0
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(if (selected == i) FbColors.Yellow else Color(world.clubs[world.userClub].color1))
                                .border(2.dp, if (p != null && p.fam(slot.pos) < 14) FbColors.Red else Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("${p?.squadNumber ?: ""}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                        Text(p?.lastName ?: "-", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                        Text("${stringResource(posShort(slot.pos))} $rating", color = FbColors.attr(rating), fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FbButton(stringResource(R.string.fb_auto_pick), { vm.edit { it.userLineup = null; selected = -1 } }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                FbButton(stringResource(R.string.fb_best_formation), {
                    vm.edit { w -> w.userTactics = ensureTactics(w, vm).copy(formation = FbLineup.bestFormation(w.clubPlayers(w.userClub))); w.userLineup = null }
                }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                Text(stringResource(R.string.fb_team_strength), color = AppColors.TextSecondary, fontSize = 12.sp)
                FbStars(sheet.strength / 3.2f, 14.dp)
            }
        }
        // Oyuncu listesi: secili mevkiye uygunluk
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = if (selected >= 0) stringResource(R.string.fb_pick_for, stringResource(posShort(f.slots[selected].pos))) else stringResource(R.string.fb_tap_position)) {
            val players = world.clubPlayers(world.userClub).filter { !it.youth }
            val pos = if (selected >= 0) f.slots[selected].pos else null
            val list = if (pos != null) players.sortedByDescending { if (FbLineup.available(it)) FbLineup.slotScore(it, pos) else -1f }
                else players.sortedBy { it.bestPos().ordinal }
            val inXi = sheet.xi.map { it.id }.toSet()
            val onBench = sheet.bench.map { it.id }.toSet()
            LazyColumn {
                items(list, key = { it.id }) { p ->
                    val ok = FbLineup.available(p)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (p.id in inXi) AppColors.Primary.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable(enabled = ok && selected >= 0) {
                                val s = selected
                                vm.edit { w ->
                                    ensureTactics(w, vm)
                                    val cur = FbSeason.clubSheet(w, w.userClub).xi.map { it.id }.toMutableList()
                                    while (cur.size < 11) cur.add(-1)
                                    val j = cur.indexOf(p.id)
                                    if (j >= 0) cur[j] = cur[s]
                                    cur[s] = p.id
                                    w.userLineup = cur.toIntArray()
                                }
                                selected = -1
                            }.padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FbCell(stringResource(posShort(p.bestPos())), 40.dp, AppColors.TextSecondary, bold = true)
                        Text(p.fullName, color = if (ok) AppColors.TextPrimary else AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        val tag = when { !ok -> stringResource(if (p.injuryDays > 0) R.string.fb_injured else R.string.fb_suspended); p.id in inXi -> stringResource(R.string.fb_starting); p.id in onBench -> stringResource(R.string.fb_bench); else -> "" }
                        Text(tag, color = if (!ok) FbColors.Red else AppColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(54.dp))
                        if (pos != null) FbAttrValue(FbFormat.attr(FbLineup.slotScore(p, pos)))
                        Spacer(Modifier.width(6.dp))
                        FbBar(p.condition, width = 36.dp)
                    }
                }
            }
        }
    }
}

// ====================================================================== talimatlar

@Composable
private fun Instructions(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val t = world.userTactics ?: vm.userTactics(world)
    fun set(f: (Tactics) -> Tactics) = vm.edit { w -> w.userTactics = f(ensureTactics(w, vm)) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Option(stringResource(R.string.fb_tac_mentality), stringResource(R.string.fb_tip_mentality), Mentality.entries, t.mentality,
                { stringResource(when (it) { Mentality.VERY_DEFENSIVE -> R.string.fb_o_very_defensive; Mentality.DEFENSIVE -> R.string.fb_o_defensive
                    Mentality.BALANCED -> R.string.fb_o_balanced; Mentality.ATTACKING -> R.string.fb_o_attacking; Mentality.VERY_ATTACKING -> R.string.fb_o_very_attacking }) }) { v -> set { it.copy(mentality = v) } }
            Option(stringResource(R.string.fb_tac_passing), stringResource(R.string.fb_tip_passing), PassingStyle.entries, t.passing,
                { stringResource(when (it) { PassingStyle.SHORT -> R.string.fb_o_short; PassingStyle.MIXED -> R.string.fb_o_mixed; PassingStyle.DIRECT -> R.string.fb_o_direct }) }) { v -> set { it.copy(passing = v) } }
            Option(stringResource(R.string.fb_tac_tempo), stringResource(R.string.fb_tip_tempo), Tempo.entries, t.tempo,
                { stringResource(when (it) { Tempo.SLOW -> R.string.fb_o_slow; Tempo.NORMAL -> R.string.fb_o_normal; Tempo.FAST -> R.string.fb_o_fast }) }) { v -> set { it.copy(tempo = v) } }
            Option(stringResource(R.string.fb_tac_width), stringResource(R.string.fb_tip_width), Width.entries, t.width,
                { stringResource(when (it) { Width.NARROW -> R.string.fb_o_narrow; Width.NORMAL -> R.string.fb_o_normal; Width.WIDE -> R.string.fb_o_wide }) }) { v -> set { it.copy(width = v) } }
            Option(stringResource(R.string.fb_tac_focus), stringResource(R.string.fb_tip_focus), AttackFocus.entries, t.focus,
                { stringResource(when (it) { AttackFocus.CENTER -> R.string.fb_o_center; AttackFocus.BALANCED -> R.string.fb_o_balanced; AttackFocus.FLANKS -> R.string.fb_o_flanks }) }) { v -> set { it.copy(focus = v) } }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Option(stringResource(R.string.fb_tac_pressing), stringResource(R.string.fb_tip_pressing), Pressing.entries, t.pressing,
                { stringResource(when (it) { Pressing.LOW -> R.string.fb_o_low; Pressing.MEDIUM -> R.string.fb_o_medium; Pressing.HIGH -> R.string.fb_o_high }) }) { v -> set { it.copy(pressing = v) } }
            Option(stringResource(R.string.fb_tac_defline), stringResource(R.string.fb_tip_defline), DefLine.entries, t.defLine,
                { stringResource(when (it) { DefLine.DEEP -> R.string.fb_o_deep; DefLine.NORMAL -> R.string.fb_o_normal; DefLine.HIGH -> R.string.fb_o_high }) }) { v -> set { it.copy(defLine = v) } }
            Option(stringResource(R.string.fb_tac_marking), stringResource(R.string.fb_tip_marking), Marking.entries, t.marking,
                { stringResource(when (it) { Marking.ZONAL -> R.string.fb_o_zonal; Marking.MAN -> R.string.fb_o_man }) }) { v -> set { it.copy(marking = v) } }
            Toggle(stringResource(R.string.fb_tac_counter), stringResource(R.string.fb_tip_counter), t.counter) { v -> set { it.copy(counter = v) } }
            Toggle(stringResource(R.string.fb_tac_offside), stringResource(R.string.fb_tip_offside), t.offsideTrap) { v -> set { it.copy(offsideTrap = v) } }
            Toggle(stringResource(R.string.fb_tac_time_wasting), stringResource(R.string.fb_tip_time_wasting), t.timeWasting) { v -> set { it.copy(timeWasting = v) } }
            Toggle(stringResource(R.string.fb_tac_hard_tackling), stringResource(R.string.fb_tip_hard_tackling), t.hardTackling) { v -> set { it.copy(hardTackling = v) } }
            FbButton(stringResource(R.string.fb_open_guide), { vm.setTab(FbTab.GUIDE) }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
        }
    }
}

@Composable
private fun <T> Option(title: String, tip: String, values: List<T>, current: T, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    FbPanel(Modifier.fillMaxWidth()) {
        Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (v in values) FbChip(label(v), v == current, { onPick(v) })
        }
        Text(tip, color = AppColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun Toggle(title: String, tip: String, on: Boolean, onChange: (Boolean) -> Unit) {
    FbPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            FbChip(stringResource(if (on) R.string.fb_on else R.string.fb_off), on, { onChange(!on) })
        }
        Text(tip, color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// ====================================================================== takim antrenmani

@Composable
private fun TeamTraining(world: FbWorld, ui: FbUiState, vm: FootballViewModel) {
    val c = world.clubs[world.userClub]
    val q = FbClubLife.trainQuality(world, c)
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_training_focus)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FbChip(stringResource(R.string.fb_o_balanced), c.trainArea < 0, { vm.edit { c.trainArea = -1 } })
                    for (a in TrainArea.entries) FbChip(stringResource(trainAreaName(a)), c.trainArea == a.ordinal, { vm.edit { c.trainArea = a.ordinal } })
                }
                Text(stringResource(R.string.fb_tip_training_focus), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_training_intensity)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FbChip(stringResource(R.string.fb_o_light), c.intensity == 0, { vm.edit { c.intensity = 0 } })
                    FbChip(stringResource(R.string.fb_o_normal), c.intensity == 1, { vm.edit { c.intensity = 1 } })
                    FbChip(stringResource(R.string.fb_o_intense), c.intensity == 2, { vm.edit { c.intensity = 2 } })
                }
                Text(stringResource(R.string.fb_tip_intensity), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_coaching_quality)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (a in TrainArea.entries) {
                    val v = q.coach[a.ordinal].roundToInt()
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(trainAreaName(a)), color = AppColors.TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        FbBar(v * 5, FbColors.attr(v), 90.dp, 8.dp)
                        Spacer(Modifier.width(8.dp)); FbAttrValue(v)
                    }
                }
                Spacer(Modifier.height(8.dp))
                FbKeyValue(stringResource(R.string.fb_f_training), "${c.trainingFacilities}/20")
                FbKeyValue(stringResource(R.string.fb_f_youth), "${c.youthFacilities}/20")
                FbKeyValue(stringResource(R.string.fb_youth_coaching), "${q.youthCoach.roundToInt()}/20")
                FbKeyValue(stringResource(R.string.fb_physio), "${q.physio.roundToInt()}/20")
                Text(stringResource(R.string.fb_tip_coaches), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
