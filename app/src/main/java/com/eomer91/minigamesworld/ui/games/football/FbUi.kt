package com.eomer91.minigamesworld.ui.games.football

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.Attr
import com.eomer91.minigamesworld.ui.games.football.engine.CompKind
import com.eomer91.minigamesworld.ui.games.football.engine.FbCalendar
import com.eomer91.minigamesworld.ui.games.football.engine.FbCountries
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.games.football.engine.Hidden
import com.eomer91.minigamesworld.ui.games.football.engine.Pos
import com.eomer91.minigamesworld.ui.games.football.engine.StageLabel
import com.eomer91.minigamesworld.ui.theme.AppColors
import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ====================================================================== renkler

object FbColors {
    val Pitch = Color(0xFF14532D)
    val Panel = Color(0xFF111827)
    val PanelHigh = Color(0xFF1F2937)
    val Line = Color(0xFF334155)
    val Green = Color(0xFF4ADE80)
    val Yellow = Color(0xFFFACC15)
    val Orange = Color(0xFFFB923C)
    val Red = Color(0xFFF87171)
    val Blue = Color(0xFF38BDF8)
    val Purple = Color(0xFFC084FC)

    /** 1..20 yetenek rengi */
    fun attr(v: Int): Color = when {
        v >= 17 -> Color(0xFF22C55E); v >= 14 -> Color(0xFF86EFAC); v >= 11 -> Color(0xFFFDE047)
        v >= 8 -> Color(0xFFFDBA74); else -> Color(0xFFF87171)
    }

    fun pct(v: Int): Color = when { v >= 85 -> Green; v >= 70 -> Yellow; v >= 55 -> Orange; else -> Red }
}

// ====================================================================== bicimler

/** Gosterim para birimleri (oyun ici her sey Euro; sabit kurla cevrilir). */
enum class FbCurrency(val code: String, val symbol: String, val rate: Double, val prefix: Boolean = true) {
    EUR("EUR", "€", 1.0), USD("USD", "$", 1.08), GBP("GBP", "£", 0.85), TRY("TRY", "₺", 45.0), JPY("JPY", "¥", 165.0),
    RUB("RUB", "₽", 100.0, false), CHF("CHF", "CHF ", 0.95), DKK("DKK", " kr", 7.46, false), HUF("HUF", " Ft", 400.0, false),
}

object FbFormat {
    /** Secili para birimi (ViewModel dunyadan ayarlar) */
    var currency: FbCurrency = FbCurrency.EUR

    fun money(v0: Long): String {
        val c = currency
        val v = (v0 * c.rate)
        val a = abs(v); val s = if (v < 0) "-" else ""
        val n = when {
            a >= 1e9 -> "%.2fB".format(a / 1e9)
            a >= 1e7 -> "%.0fM".format(a / 1e6)
            a >= 1e6 -> "%.1fM".format(a / 1e6)
            a >= 1e4 -> "%.0fK".format(a / 1e3)
            a >= 1e3 -> "%.1fK".format(a / 1e3)
            else -> "%.0f".format(a)
        }
        return s + if (c.prefix) c.symbol + n else n + c.symbol
    }

    fun wage(v: Int): String = money(v.toLong())

    fun date(world: FbWorld, day: Int = world.day): String {
        val cal = FbCalendar(world.season)
        val (y, m, d) = cal.date(day)
        val sym = DateFormatSymbols.getInstance(Locale.getDefault())
        val wd = sym.shortWeekdays[(cal.weekday(day) + 1) % 7 + 1] // Calendar.SUNDAY=1
        return "$wd $d ${sym.shortMonths[m - 1]} $y"
    }

    fun shortDate(world: FbWorld, day: Int): String {
        val (_, m, d) = FbCalendar(world.season).date(day)
        return "$d ${DateFormatSymbols.getInstance(Locale.getDefault()).shortMonths[m - 1]}"
    }

    fun season(s: Int) = "$s/${(s + 1) % 100}"

    /** 0..200 CA -> 0..5 yildiz (yarim adim) */
    fun stars(ca: Int): Float = ((ca - 20) / 32f).coerceIn(0.5f, 5f).let { (it * 2).roundToInt() / 2f }

    fun attr(v: Float): Int = v.roundToInt().coerceIn(1, 20)
}

// ====================================================================== metin eslemeleri

@StringRes
fun posShort(p: Pos): Int = when (p) {
    Pos.GK -> R.string.fb_pos_gk; Pos.DR -> R.string.fb_pos_dr; Pos.DC -> R.string.fb_pos_dc; Pos.DL -> R.string.fb_pos_dl
    Pos.WBR -> R.string.fb_pos_wbr; Pos.WBL -> R.string.fb_pos_wbl; Pos.DM -> R.string.fb_pos_dm; Pos.MR -> R.string.fb_pos_mr
    Pos.MC -> R.string.fb_pos_mc; Pos.ML -> R.string.fb_pos_ml; Pos.AMR -> R.string.fb_pos_amr; Pos.AMC -> R.string.fb_pos_amc
    Pos.AML -> R.string.fb_pos_aml; Pos.ST -> R.string.fb_pos_st
}

@StringRes
fun attrName(a: Attr): Int = when (a) {
    Attr.CORNERS -> R.string.fb_a_corners; Attr.CROSSING -> R.string.fb_a_crossing; Attr.DRIBBLING -> R.string.fb_a_dribbling
    Attr.FINISHING -> R.string.fb_a_finishing; Attr.FIRST_TOUCH -> R.string.fb_a_first_touch; Attr.FREE_KICK -> R.string.fb_a_free_kick
    Attr.HEADING -> R.string.fb_a_heading; Attr.LONG_SHOTS -> R.string.fb_a_long_shots; Attr.MARKING -> R.string.fb_a_marking
    Attr.PASSING -> R.string.fb_a_passing; Attr.PENALTY -> R.string.fb_a_penalty; Attr.TACKLING -> R.string.fb_a_tackling
    Attr.TECHNIQUE -> R.string.fb_a_technique; Attr.AGGRESSION -> R.string.fb_a_aggression; Attr.ANTICIPATION -> R.string.fb_a_anticipation
    Attr.BRAVERY -> R.string.fb_a_bravery; Attr.COMPOSURE -> R.string.fb_a_composure; Attr.CONCENTRATION -> R.string.fb_a_concentration
    Attr.DECISIONS -> R.string.fb_a_decisions; Attr.DETERMINATION -> R.string.fb_a_determination; Attr.FLAIR -> R.string.fb_a_flair
    Attr.LEADERSHIP -> R.string.fb_a_leadership; Attr.OFF_THE_BALL -> R.string.fb_a_off_the_ball; Attr.POSITIONING -> R.string.fb_a_positioning
    Attr.TEAMWORK -> R.string.fb_a_teamwork; Attr.VISION -> R.string.fb_a_vision; Attr.WORK_RATE -> R.string.fb_a_work_rate
    Attr.ACCELERATION -> R.string.fb_a_acceleration; Attr.AGILITY -> R.string.fb_a_agility; Attr.BALANCE -> R.string.fb_a_balance
    Attr.JUMPING -> R.string.fb_a_jumping; Attr.NATURAL_FITNESS -> R.string.fb_a_natural_fitness; Attr.PACE -> R.string.fb_a_pace
    Attr.STAMINA -> R.string.fb_a_stamina; Attr.STRENGTH -> R.string.fb_a_strength; Attr.AERIAL_REACH -> R.string.fb_a_aerial_reach
    Attr.COMMAND_OF_AREA -> R.string.fb_a_command_of_area; Attr.HANDLING -> R.string.fb_a_handling; Attr.KICKING -> R.string.fb_a_kicking
    Attr.ONE_ON_ONES -> R.string.fb_a_one_on_ones; Attr.REFLEXES -> R.string.fb_a_reflexes
}

@StringRes
fun hiddenName(h: Hidden): Int = when (h) {
    Hidden.CONSISTENCY -> R.string.fb_h_consistency; Hidden.IMPORTANT_MATCHES -> R.string.fb_h_important_matches
    Hidden.INJURY_PRONENESS -> R.string.fb_h_injury_proneness; Hidden.PROFESSIONALISM -> R.string.fb_h_professionalism
    Hidden.AMBITION -> R.string.fb_h_ambition; Hidden.LOYALTY -> R.string.fb_h_loyalty; Hidden.ADAPTABILITY -> R.string.fb_h_adaptability
    Hidden.PRESSURE -> R.string.fb_h_pressure; Hidden.VERSATILITY -> R.string.fb_h_versatility
}

@StringRes
fun compName(k: CompKind): Int = when (k) {
    CompKind.LEAGUE -> R.string.fb_c_league; CompKind.PLAYOFF -> R.string.fb_c_playoff; CompKind.CUP -> R.string.fb_c_cup
    CompKind.SUPER_CUP -> R.string.fb_c_super_cup; CompKind.CHAMPIONS -> R.string.fb_c_champions; CompKind.EUROPA -> R.string.fb_c_europa
    CompKind.CONFERENCE -> R.string.fb_c_conference; CompKind.UEFA_SUPER -> R.string.fb_c_uefa_super; CompKind.FRIENDLY -> R.string.fb_c_friendly
    CompKind.NATIONS_LEAGUE -> R.string.fb_c_nations; CompKind.QUALIFIERS -> R.string.fb_c_qualifiers; CompKind.WORLD_CUP -> R.string.fb_c_world_cup
    CompKind.EURO -> R.string.fb_c_euro; CompKind.NAT_FRIENDLY -> R.string.fb_c_nat_friendly
}

@StringRes
fun stageName(s: StageLabel): Int = when (s) {
    StageLabel.LEAGUE -> R.string.fb_s_league; StageLabel.GROUPS -> R.string.fb_s_groups; StageLabel.LEAGUE_PHASE -> R.string.fb_s_league_phase
    StageLabel.Q1 -> R.string.fb_s_q1; StageLabel.Q2 -> R.string.fb_s_q2; StageLabel.Q3 -> R.string.fb_s_q3; StageLabel.QPO -> R.string.fb_s_qpo
    StageLabel.KPO -> R.string.fb_s_kpo; StageLabel.R128 -> R.string.fb_s_r128; StageLabel.R64 -> R.string.fb_s_r64; StageLabel.R32 -> R.string.fb_s_r32
    StageLabel.R16 -> R.string.fb_s_r16; StageLabel.QF -> R.string.fb_s_qf; StageLabel.SF -> R.string.fb_s_sf; StageLabel.FINAL -> R.string.fb_s_final
    StageLabel.PRELIM -> R.string.fb_s_prelim; StageLabel.MATCH -> R.string.fb_s_match
}

/** Lig adi: ozel ad yoksa "Ulke N. Lig". */
@Composable
fun leagueName(world: FbWorld, leagueId: Int): String {
    val lg = world.leagues.getOrNull(leagueId) ?: return ""
    if (lg.customName.isNotEmpty()) return lg.customName
    return stringResource(R.string.fb_league_name, FbCountries.all[lg.country].englishName, lg.tier)
}

/** Organizasyon gorunen adi (ulke/lig ekli). */
@Composable
fun compTitle(world: FbWorld, compId: Int): String {
    val c = world.state?.comps?.getOrNull(compId) ?: return ""
    return when (c.kind) {
        CompKind.LEAGUE -> leagueName(world, c.league)
        CompKind.PLAYOFF -> stringResource(R.string.fb_c_playoff_of, leagueName(world, c.league))
        CompKind.CUP, CompKind.SUPER_CUP -> FbCountries.all[c.country].englishName + " " + stringResource(compName(c.kind))
        else -> stringResource(compName(c.kind))
    }
}

// ====================================================================== bilesenler

@Composable
fun FbPanel(modifier: Modifier = Modifier, title: String? = null, trailing: (@Composable RowScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(FbColors.Panel).padding(12.dp)) {
        if (title != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                trailing?.invoke(this)
            }
        }
        content()
    }
}

@Composable
fun FbPill(text: String, color: Color = FbColors.PanelHigh, textColor: Color = AppColors.TextPrimary, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(color).let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

/** Secilebilir hap (filtre/sekme) */
@Composable
fun FbChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Secili: dolu mavi + tik; secili degil: koyu zemin + ince cerceve (hangisinin aktif oldugu bir bakista belli olsun)
    Box(
        modifier.clip(RoundedCornerShape(50))
            .background(if (selected) AppColors.Primary else FbColors.PanelHigh)
            .border(if (selected) 2.dp else 1.dp, if (selected) Color.White.copy(alpha = 0.85f) else AppColors.TextSecondary.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (selected) "✓ $text" else text, color = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
            fontSize = 13.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, maxLines = 1)
    }
}

/** Kulup armasi: renkli daire + kisa ad */
@Composable
fun FbCrest(color1: Int, color2: Int, short: String, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Color(color1)).border(3.dp, Color(color2), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val c = Color(color1)
        val light = (c.red * 0.299f + c.green * 0.587f + c.blue * 0.114f) > 0.6f
        Text(short.take(3), color = if (light) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.3f).sp, maxLines = 1)
    }
}

@Composable
fun FbStars(value: Float, size: Dp = 14.dp, color: Color = FbColors.Yellow) {
    Row {
        for (i in 0 until 5) {
            val fill = (value - i).coerceIn(0f, 1f)
            Canvas(Modifier.size(size)) {
                val r = this.size.minDimension / 2f
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val path = Path()
                for (k in 0 until 10) {
                    val ang = Math.PI / 2 + k * Math.PI / 5
                    val rr = if (k % 2 == 0) r else r * 0.45f
                    val x = c.x + (rr * cos(ang)).toFloat(); val y = c.y - (rr * sin(ang)).toFloat()
                    if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color.copy(alpha = 0.25f))
                if (fill > 0f) {
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(0f, 0f, this.size.width * fill, this.size.height)
                    drawPath(path, color)
                    drawContext.canvas.restore()
                }
                drawPath(path, color, style = Stroke(width = 1f))
            }
        }
    }
}

/** 1..20 yetenek kutusu */
@Composable
fun FbAttrValue(v: Int, width: Dp = 30.dp) {
    Box(Modifier.width(width).clip(RoundedCornerShape(6.dp)).background(FbColors.attr(v).copy(alpha = 0.18f)).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
        Text("$v", color = FbColors.attr(v), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

/** Yuzde cubugu (kondisyon, moral) */
@Composable
fun FbBar(pct: Int, color: Color = FbColors.pct(pct), width: Dp = 44.dp, height: Dp = 6.dp) {
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(3.dp)).background(FbColors.Line)) {
        Box(Modifier.width(width * (pct.coerceIn(0, 100) / 100f)).height(height).background(color))
    }
}

@Composable
fun FbCell(text: String, width: Dp, color: Color = AppColors.TextPrimary, bold: Boolean = false, align: TextAlign = TextAlign.Start, size: TextUnit = 13.sp) {
    Text(text, Modifier.width(width), color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = size,
        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align)
}

@Composable
fun FbButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = AppColors.Primary, textColor: Color = AppColors.OnPrimary, enabled: Boolean = true) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (enabled) color else FbColors.Line)
            .let { if (enabled) it.clickable(onClick = onClick) else it }.padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) textColor else AppColors.TextSecondary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1) }
}

@Composable
fun FbKeyValue(key: String, value: String, valueColor: Color = AppColors.TextPrimary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = AppColors.TextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
