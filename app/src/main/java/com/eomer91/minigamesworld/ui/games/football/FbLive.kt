package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.DefLine
import com.eomer91.minigamesworld.ui.games.football.engine.EvType
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.MatchEvent
import com.eomer91.minigamesworld.ui.games.football.engine.MatchPlayer
import com.eomer91.minigamesworld.ui.games.football.engine.MatchSim
import com.eomer91.minigamesworld.ui.games.football.engine.Mentality
import com.eomer91.minigamesworld.ui.games.football.engine.Pos
import com.eomer91.minigamesworld.ui.games.football.engine.Pressing
import com.eomer91.minigamesworld.ui.games.football.engine.Tempo
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

/** Canli macin oynatma durumu (olay olay ilerler, top ve oyuncular arada canlandirilir). */
private class Playback {
    var idx = 0
    var elapsed = 0f
    var hg = 0
    var ag = 0
    var shotsH = 0
    var shotsA = 0
    var passH = 0
    var passA = 0
    var ball = Offset(0.5f, 0.5f)
    val pos = HashMap<MatchPlayer, Offset>()
    var flash = 0f
    var flashGoalSide = -1
    var done = false
    var processed = 0
}

/** Maci izleme olcegi: 1x'te 1 gercek saniye = 30 mac saniyesi (90 dakika ~3 dakika). */
private const val BASE_RATE = 30f

private val MAJOR = setOf(
    EvType.GOAL, EvType.SAVE, EvType.MISS, EvType.BLOCK, EvType.YELLOW, EvType.RED, EvType.INJURY, EvType.SUB, EvType.CORNER,
    EvType.FREE_KICK, EvType.PENALTY, EvType.OFFSIDE, EvType.HALF_TIME, EvType.FULL_TIME, EvType.EXTRA_TIME, EvType.SHOOTOUT_GOAL,
    EvType.SHOOTOUT_MISS, EvType.FOUL, EvType.THROUGH_BALL,
)

@Composable
fun FbLiveMatch(world: FbWorld, sim: MatchSim, vm: FootballViewModel) {
    val pb = remember(sim) { Playback() }
    var tick by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    var speed by remember { mutableIntStateOf(2) }
    var showTactics by remember { mutableStateOf(false) }
    val feed = remember(sim) { mutableStateListOf<MatchEvent>() }
    val userSide = if (sim.home.club == world.userClub) 0 else 1
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(sim) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (isActive) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
                    last = now
                    if (!paused && !showTactics) advance(sim, pb, dt * speed * BASE_RATE, feed, userSide)
                    animate(sim, pb, dt)
                    tick = now
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        // Skor tabelasi
        // tick okunur: her karede yeniden cizilir
        val ev = sim.events.getOrNull(pb.idx + (tick and 0L).toInt())
        val minute = ev?.let { (it.sec / 60 + 1).coerceAtMost(120) } ?: 1
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FbColors.Panel).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            val hc = world.clubs[sim.home.club]; val ac = world.clubs[sim.away.club]
            FbCrest(hc.color1, hc.color2, hc.shortName, 32.dp)
            Text(hc.name, color = if (userSide == 0) AppColors.Primary else AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.End, maxLines = 1)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${pb.hg} - ${pb.ag}", color = FbColors.Yellow, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(if (pb.done) stringResource(R.string.fb_full_time) else "$minute'", color = AppColors.TextSecondary, fontSize = 12.sp)
            }
            Text(ac.name, color = if (userSide == 1) AppColors.Primary else AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1)
            FbCrest(ac.color1, ac.color2, ac.shortName, 32.dp)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Saha
            Box(Modifier.weight(1.6f).fillMaxHeight().clip(RoundedCornerShape(12.dp))) {
                PitchCanvas(world, sim, pb, tick, userSide)
                if (pb.flash > 0f) {
                    Box(Modifier.align(Alignment.Center).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 26.dp, vertical = 10.dp)) {
                        Text(stringResource(R.string.fb_goal_flash), color = FbColors.Yellow, fontSize = 40.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            // Anlatim ve istatistik
            Column(Modifier.weight(1f).fillMaxHeight()) {
                FbPanel(Modifier.fillMaxWidth()) {
                    val pt = (pb.passH + pb.passA).coerceAtLeast(1)
                    LiveStat(stringResource(R.string.fb_possession), "${pb.passH * 100 / pt}%", "${pb.passA * 100 / pt}%")
                    LiveStat(stringResource(R.string.fb_shots_short), "${pb.shotsH}", "${pb.shotsA}")
                }
                Spacer(Modifier.height(6.dp))
                FbPanel(Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn {
                        items(feed) { e ->
                            val col = when (e.type) { EvType.GOAL -> FbColors.Yellow; EvType.RED -> FbColors.Red; EvType.YELLOW -> FbColors.Orange
                                EvType.HALF_TIME, EvType.FULL_TIME, EvType.EXTRA_TIME -> AppColors.Primary; else -> if (e.side == userSide) AppColors.TextPrimary else AppColors.TextMuted }
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text(if (e.type == EvType.HALF_TIME || e.type == EvType.FULL_TIME) "" else "${(e.sec / 60 + 1).coerceAtMost(120)}'",
                                    color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(34.dp))
                                Text(commentary(world, sim, e), color = col, fontSize = 13.sp, fontWeight = if (e.type == EvType.GOAL) FontWeight.ExtraBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
        // Kontroller
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pb.done) {
                FbButton(stringResource(R.string.fb_finish_match), { vm.finishLive() }, color = FbColors.Green, textColor = Color.Black)
            } else {
                FbButton(stringResource(if (paused) R.string.fb_resume else R.string.fb_pause), { paused = !paused }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
                for (s in listOf(1, 2, 4, 8)) FbChip("${s}x", speed == s, { speed = s })
                FbButton(stringResource(R.string.fb_tactics_subs), { showTactics = true }, color = AppColors.Primary)
                Spacer(Modifier.weight(1f))
                FbButton(stringResource(R.string.fb_skip_to_end), {
                    while (!sim.finished) sim.step()
                    advance(sim, pb, Float.MAX_VALUE / 4, feed, userSide)
                }, color = FbColors.PanelHigh, textColor = AppColors.TextPrimary)
            }
        }
    }
    if (showTactics) LiveTacticsDialog(world, sim, userSide) { showTactics = false }
}

@Composable
private fun LiveStat(label: String, h: String, a: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(h, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
        Text(label, color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(a, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
    }
}

// ====================================================================== oynatma

/** Olay sureleri: sonraki olaya kadar gecen mac saniyesi (devre arasi uzun). */
private fun duration(sim: MatchSim, i: Int): Float {
    val e = sim.events[i]
    if (e.type == EvType.HALF_TIME || e.type == EvType.EXTRA_TIME) return 60f
    if (e.type == EvType.GOAL) return 45f
    val n = sim.events.getOrNull(i + 1) ?: return 3f
    return max(2f, (n.sec - e.sec).toFloat())
}

private fun advance(sim: MatchSim, pb: Playback, matchSec: Float, feed: MutableList<MatchEvent>, userSide: Int) {
    if (pb.done) return
    pb.elapsed += matchSec
    while (true) {
        while (!sim.finished && sim.events.size < pb.idx + 3) sim.step()
        if (pb.idx >= sim.events.size - 1) {
            if (sim.finished) { consume(sim, pb, feed); pb.done = true }
            break
        }
        val d = duration(sim, pb.idx)
        if (pb.elapsed < d) break
        pb.elapsed -= d
        pb.idx++
        consume(sim, pb, feed)
    }
}

/** Gosterilen olaya kadar skor, istatistik ve anlatimi guncelle. */
private fun consume(sim: MatchSim, pb: Playback, feed: MutableList<MatchEvent>) {
    while (pb.processed <= min(pb.idx, sim.events.size - 1)) {
        val e = sim.events[pb.processed]
        when (e.type) {
            EvType.GOAL -> { if (e.side == 0) pb.hg++ else pb.ag++; pb.flash = 2.2f; pb.flashGoalSide = e.side }
            EvType.PASS, EvType.LONG_BALL, EvType.THROUGH_BALL -> if (e.side == 0) pb.passH++ else pb.passA++
            else -> Unit
        }
        if (e.type == EvType.GOAL || e.type == EvType.MISS || e.type == EvType.BLOCK) { if (e.side == 0) pb.shotsH++ else pb.shotsA++ }
        if (e.type == EvType.SAVE) { if (e.side == 0) pb.shotsA++ else pb.shotsH++ }
        if (e.type in MAJOR && (e.type != EvType.FOUL || pb.processed % 2 == 0)) feed.add(0, e)
        pb.processed++
    }
}

/** Top ve oyuncu konumlarini yumusak gecisle guncelle. */
private fun animate(sim: MatchSim, pb: Playback, dt: Float) {
    if (pb.flash > 0f) pb.flash -= dt
    val e = sim.events.getOrNull(pb.idx) ?: return
    val n = sim.events.getOrNull(pb.idx + 1)
    val target = if (n != null && !pb.done) {
        val f = (pb.elapsed / duration(sim, pb.idx)).coerceIn(0f, 1f)
        Offset(e.x + (n.x - e.x) * f, e.y + (n.y - e.y) * f)
    } else Offset(e.x, e.y)
    pb.ball = Offset(pb.ball.x + (target.x - pb.ball.x) * min(1f, dt * 8f), pb.ball.y + (target.y - pb.ball.y) * min(1f, dt * 8f))
    val attacking = e.side
    for ((si, side) in listOf(sim.home, sim.away).withIndex()) {
        val f = side.tactics.formation
        val push = if (attacking == si) 0.1f else -0.04f
        val line = when (side.tactics.defLine) { DefLine.HIGH -> 0.05f; DefLine.DEEP -> -0.05f; else -> 0f }
        for ((i, mp) in side.onPitch.withIndex()) {
            if (mp == null) continue
            val slot = f.slots.getOrNull(i) ?: continue
            var x = slot.x * 0.92f + (if (mp.pos == Pos.GK) 0f else push + line)
            var y = slot.y
            // Topa dogru kayma
            val bx = if (si == 0) pb.ball.x else 1f - pb.ball.x
            val by = if (si == 0) pb.ball.y else 1f - pb.ball.y
            if (mp.pos != Pos.GK) { x += (bx - 0.5f) * 0.3f; y += (by - 0.5f) * 0.22f }
            if (mp.p.id == e.player && e.side == si) { x = bx; y = by }
            x = x.coerceIn(0.02f, 0.98f); y = y.coerceIn(0.04f, 0.96f)
            val t = if (si == 0) Offset(x, y) else Offset(1f - x, 1f - y)
            val cur = pb.pos[mp] ?: t
            pb.pos[mp] = Offset(cur.x + (t.x - cur.x) * min(1f, dt * 2.5f), cur.y + (t.y - cur.y) * min(1f, dt * 2.5f))
        }
    }
}

@Composable
private fun PitchCanvas(world: FbWorld, sim: MatchSim, pb: Playback, tick: Long, userSide: Int) {
    val hc = world.clubs[sim.home.club]; val ac = world.clubs[sim.away.club]
    val homeCol = Color(hc.color1)
    var awayCol = Color(ac.color1)
    if (colorClose(homeCol, awayCol)) awayCol = Color(ac.color2)
    if (colorClose(homeCol, awayCol)) awayCol = Color.White
    Canvas(Modifier.fillMaxSize()) {
        if (tick == -1L) return@Canvas
        val w = size.width; val h = size.height
        // Cimen seritleri
        for (k in 0 until 12) drawRect(if (k % 2 == 0) Color(0xFF2E7D32) else Color(0xFF2A7330), Offset(w * k / 12f, 0f), Size(w / 12f + 1f, h))
        val m = 10f
        val lc = Color.White.copy(alpha = 0.7f); val st = Stroke(2.5f)
        drawRect(lc, Offset(m, m), Size(w - 2 * m, h - 2 * m), style = st)
        drawLine(lc, Offset(w / 2, m), Offset(w / 2, h - m), 2.5f)
        drawCircle(lc, h * 0.13f, Offset(w / 2, h / 2), style = st)
        val bw = w * 0.13f; val bh = h * 0.55f; val sw = w * 0.05f; val sh = h * 0.28f
        drawRect(lc, Offset(m, (h - bh) / 2), Size(bw, bh), style = st)
        drawRect(lc, Offset(w - m - bw, (h - bh) / 2), Size(bw, bh), style = st)
        drawRect(lc, Offset(m, (h - sh) / 2), Size(sw, sh), style = st)
        drawRect(lc, Offset(w - m - sw, (h - sh) / 2), Size(sw, sh), style = st)
        drawRect(Color.White, Offset(2f, h * 0.44f), Size(m - 2f, h * 0.12f))
        drawRect(Color.White, Offset(w - m, h * 0.44f), Size(m - 2f, h * 0.12f))
        fun px(o: Offset) = Offset(m + o.x * (w - 2 * m), m + o.y * (h - 2 * m))
        val r = h * 0.032f
        for ((si, side) in listOf(sim.home, sim.away).withIndex()) {
            val col = if (si == 0) homeCol else awayCol
            for (mp in side.onPitch) {
                if (mp == null) continue
                val p = pb.pos[mp] ?: continue
                val c = px(p)
                val gk = mp.pos == Pos.GK
                drawCircle(Color.Black.copy(alpha = 0.3f), r, Offset(c.x + 2f, c.y + 3f))
                drawCircle(if (gk) Color(0xFF111111) else col, r, c)
                drawCircle(if (si == userSide) FbColors.Yellow else Color.White, r, c, style = Stroke(if (si == userSide) 3f else 2f))
            }
        }
        val b = px(pb.ball)
        drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.55f, Offset(b.x + 3f, b.y + 4f))
        drawCircle(Color.White, r * 0.55f, b)
        drawCircle(Color.Black, r * 0.55f, b, style = Stroke(1.5f))
    }
}

private fun colorClose(a: Color, b: Color): Boolean =
    kotlin.math.abs(a.red - b.red) + kotlin.math.abs(a.green - b.green) + kotlin.math.abs(a.blue - b.blue) < 0.45f

// ====================================================================== anlatim

@Composable
private fun commentary(world: FbWorld, sim: MatchSim, e: MatchEvent): String {
    val p = world.players.getOrNull(e.player)?.lastName ?: ""
    val o = world.players.getOrNull(e.other)?.lastName ?: ""
    val team = if (e.side == 0) sim.home.name else if (e.side == 1) sim.away.name else ""
    val v = (e.sec + e.player) % 3
    return when (e.type) {
        EvType.GOAL -> when (v) { 0 -> stringResource(R.string.fb_ev_goal_1, p, team); 1 -> stringResource(R.string.fb_ev_goal_2, p, team); else -> stringResource(R.string.fb_ev_goal_3, p, team) } +
            (if (o.isNotEmpty()) " " + stringResource(R.string.fb_ev_assist, o) else "")
        EvType.SAVE -> if (v == 0) stringResource(R.string.fb_ev_save_1, p, o) else stringResource(R.string.fb_ev_save_2, p, o)
        EvType.MISS -> if (v == 0) stringResource(R.string.fb_ev_miss_1, p) else stringResource(R.string.fb_ev_miss_2, p)
        EvType.BLOCK -> stringResource(R.string.fb_ev_block, p, o)
        EvType.YELLOW -> stringResource(R.string.fb_ev_yellow, p)
        EvType.RED -> stringResource(R.string.fb_ev_red, p)
        EvType.INJURY -> stringResource(R.string.fb_ev_injury, p)
        EvType.SUB -> stringResource(R.string.fb_ev_sub, team, p, o)
        EvType.CORNER -> stringResource(R.string.fb_ev_corner, team)
        EvType.FREE_KICK -> stringResource(R.string.fb_ev_free_kick, team, p)
        EvType.PENALTY -> stringResource(R.string.fb_ev_penalty, team, p)
        EvType.OFFSIDE -> stringResource(R.string.fb_ev_offside, p)
        EvType.FOUL -> stringResource(R.string.fb_ev_foul, p, o)
        EvType.THROUGH_BALL -> stringResource(R.string.fb_ev_through, p, o)
        EvType.HALF_TIME -> stringResource(R.string.fb_ev_half_time)
        EvType.FULL_TIME -> stringResource(R.string.fb_ev_full_time)
        EvType.EXTRA_TIME -> stringResource(R.string.fb_ev_extra_time)
        EvType.SHOOTOUT_GOAL -> stringResource(R.string.fb_ev_so_goal, p, team)
        EvType.SHOOTOUT_MISS -> stringResource(R.string.fb_ev_so_miss, p, team)
        else -> ""
    }
}

// ====================================================================== mac ici taktik ve degisiklik

@Composable
private fun LiveTacticsDialog(world: FbWorld, sim: MatchSim, userSide: Int, onClose: () -> Unit) {
    val side = if (userSide == 0) sim.home else sim.away
    var outSel by remember { mutableStateOf<MatchPlayer?>(null) }
    var version by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f).clip(RoundedCornerShape(18.dp)).background(AppColors.Background).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.fb_tactics_subs), color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.fb_subs_left, side.subsLeft + version * 0), color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(end = 10.dp))
                FbButton(stringResource(R.string.fb_back_to_match), onClose, color = FbColors.Green, textColor = Color.Black)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.fb_on_pitch)) {
                    LazyColumn {
                        items(side.active) { mp ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (outSel === mp) FbColors.Red.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable { outSel = if (outSel === mp) null else mp }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                FbCell(stringResource(posShort(mp.pos)), 40.dp, AppColors.TextSecondary, bold = true)
                                Text(mp.p.fullName, color = AppColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                if (mp.yellow > 0) Box(Modifier.width(8.dp).height(11.dp).background(FbColors.Yellow))
                                Spacer(Modifier.width(6.dp))
                                FbBar(mp.condition.toInt(), width = 36.dp)
                                Text(" %.1f".format(mp.rating), color = AppColors.TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
                FbPanel(Modifier.weight(1f).fillMaxHeight(), title = stringResource(if (outSel != null) R.string.fb_choose_sub else R.string.fb_bench)) {
                    LazyColumn {
                        items(side.bench.filter { !it.injured }) { mp ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = outSel != null && side.subsLeft > 0) {
                                    val out = outSel ?: return@clickable
                                    sim.substitute(userSide, out, mp); outSel = null; version++
                                }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                FbCell(stringResource(posShort(mp.p.bestPos())), 40.dp, AppColors.TextSecondary, bold = true)
                                Text(mp.p.fullName, color = AppColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                outSel?.let { o -> FbAttrValue(FbFormat.attr(mp.p.posRating(o.pos) * (0.6f + 0.4f * mp.p.fam(o.pos) / 20f))) }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val t = side.tactics
                    fun set(nt: com.eomer91.minigamesworld.ui.games.football.engine.Tactics) { sim.setTactics(userSide, nt); version++ }
                    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_tac_mentality)) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (mm in Mentality.entries) FbChip(stringResource(when (mm) { Mentality.VERY_DEFENSIVE -> R.string.fb_o_very_defensive; Mentality.DEFENSIVE -> R.string.fb_o_defensive
                                Mentality.BALANCED -> R.string.fb_o_balanced; Mentality.ATTACKING -> R.string.fb_o_attacking; Mentality.VERY_ATTACKING -> R.string.fb_o_very_attacking }), t.mentality == mm, { set(t.copy(mentality = mm)) })
                        }
                    }
                    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_tac_pressing)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (pp in Pressing.entries) FbChip(stringResource(when (pp) { Pressing.LOW -> R.string.fb_o_low; Pressing.MEDIUM -> R.string.fb_o_medium; Pressing.HIGH -> R.string.fb_o_high }), t.pressing == pp, { set(t.copy(pressing = pp)) })
                        }
                    }
                    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_tac_tempo)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (tt in Tempo.entries) FbChip(stringResource(when (tt) { Tempo.SLOW -> R.string.fb_o_slow; Tempo.NORMAL -> R.string.fb_o_normal; Tempo.FAST -> R.string.fb_o_fast }), t.tempo == tt, { set(t.copy(tempo = tt)) })
                        }
                    }
                    FbPanel(Modifier.fillMaxWidth(), title = stringResource(R.string.fb_tac_defline)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (dl in DefLine.entries) FbChip(stringResource(when (dl) { DefLine.DEEP -> R.string.fb_o_deep; DefLine.NORMAL -> R.string.fb_o_normal; DefLine.HIGH -> R.string.fb_o_high }), t.defLine == dl, { set(t.copy(defLine = dl)) })
                        }
                    }
                    FbPanel(Modifier.fillMaxWidth()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FbChip(stringResource(R.string.fb_tac_counter), t.counter, { set(t.copy(counter = !t.counter)) })
                            FbChip(stringResource(R.string.fb_tac_time_wasting), t.timeWasting, { set(t.copy(timeWasting = !t.timeWasting)) })
                            FbChip(stringResource(R.string.fb_tac_hard_tackling), t.hardTackling, { set(t.copy(hardTackling = !t.hardTackling)) })
                        }
                    }
                }
            }
        }
    }
}
