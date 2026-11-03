package com.eomer91.minigamesworld.ui.games.forest

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Orman Muhafizi piksel art cizimleri. Her karakter bir harf = bir piksel; '.' saydam.
 * Bir kez Bitmap'e cevrilir, cizimde FilterQuality.None ile keskin buyutulur.
 */
object ForestSprites {
    private val palette: Map<Char, Long> = mapOf(
        'k' to 0xFF1B1A2E,
        'g' to 0xFF3FA34D,
        'G' to 0xFF256B33,
        'l' to 0xFF8FE36B,
        's' to 0xFFF0C08E,
        'S' to 0xFFC98A5A,
        'b' to 0xFF8B5A2B,
        'B' to 0xFF5A3A1C,
        'w' to 0xFFFFFFFF,
        'y' to 0xFFF7D046,
        'Y' to 0xFFC9971C,
        'r' to 0xFFE04848,
        'R' to 0xFF8F2424,
        'p' to 0xFFA66EE0,
        'P' to 0xFF5E2F8A,
        'c' to 0xFF6FE0FF,
        'C' to 0xFF2E8FB8,
        'e' to 0xFFA3A9B3,
        'E' to 0xFF5D6470,
        'o' to 0xFFF08A24,
        't' to 0xFF5FAE6E,
        'T' to 0xFF2F6E45,
        'm' to 0xFFD9D2C3,
        'n' to 0xFF3D4A8A,
        'a' to 0xFF7A4FBF,
        'z' to 0xFFD8F5FF,
    )

    private val heroRows = listOf(
        "................",
        ".....kkkkkk.....",
        "....kGGGGGGk....",
        "...kGgggggGGk...",
        "...kGgssssgGk...",
        "...kGsksksGk.y..",
        "....ksssssk..b..",
        "....kGggggGk.b..",
        "...kGgylgygGkb..",
        "..kssggggggkssb.",
        "..kSkgggggGk.kb.",
        "....kbBYYBbk.b..",
        "....kgggggGk.b..",
        "....kgGk.kGk....",
        "....kBBk.kBBk...",
        "...kkkk...kkkk..",
    )
    private val hero2Rows = listOf(
        "................",
        ".....kkkkkk.....",
        "....kGGGGGGk....",
        "...kGgggggGGk...",
        "...kGgssssgGk...",
        "...kGsksksGk.y..",
        "....ksssssk..b..",
        "....kGggggGk.b..",
        "...kGgylgygGkb..",
        "..kssggggggkssb.",
        "..kSkgggggGk.kb.",
        "....kbBYYBbk.b..",
        "....kgggggGk.b..",
        ".....kgGkGk.....",
        ".....kBBkBBk....",
        "....kkkk.kkkk...",
    )
    private val goblinRows = listOf(
        "................",
        ".kk..........kk.",
        ".ktk..kkkk..ktk.",
        "..ktkkttttkktk..",
        "...ktttttttTk...",
        "...ktrkttrkTk...",
        "...ktttttttTk...",
        "....ktwkkwtk....",
        "....kkttttkk..k.",
        "..ktkbbbbktk.kBk",
        "..ktkbBBbkTkkBk.",
        "...kkbbbbkk.kk..",
        "....ktkktk......",
        "....kTk.kTk.....",
        "...kkkk.kkkk....",
        "................",
    )
    private val wolfRows = listOf(
        "................",
        "................",
        "..k.k...........",
        ".kekek..........",
        ".keeeekkkkkk....",
        "kkeEkeeeeeeekk..",
        "kwekeeeeeeeeeEk.",
        ".kkeeeeeeeeeeEkk",
        "..kmeeeeeeeeEEk.",
        "...kmmeeeeeEEk..",
        "...keEkkkkeEk...",
        "...keEk..keEk...",
        "...kEk...kEk....",
        "...kk....kk.....",
        "................",
        "................",
    )
    private val orcRows = listOf(
        "....kkkkkkkk....",
        "...knnnnnnnnk...",
        "..kntttttttTnk..",
        "..kttrktktrkTk..",
        "..ktttttttttTk..",
        "..ktwkttttkwTk..",
        "...kttkkkkttk...",
        ".kkTttttttttTkk.",
        "kttkkbbbbbbkkttk",
        "kttkbBBBBBBbkttk",
        "kTTkbbbbbbbbkTTk",
        ".kkkbbBBBBbbkkk.",
        "...ktttkkktttk..",
        "...kTTk...kTTk..",
        "..kkkkk...kkkkk.",
        "................",
    )
    private val batRows = listOf(
        "................",
        "................",
        "................",
        "k.............k.",
        "kk....k..k...kk.",
        "kPk...kkkk..kPk.",
        "kPPk.kppppk.kPPk",
        "kPPPkpwppwpkPPPk",
        ".kPPPppppppPPPk.",
        "..kkPPpppppPPkk.",
        "....kkPkkPkk....",
        "......k..k......",
        "................",
        "................",
        "................",
        "................",
    )
    private val treantRows = listOf(
        ".....kk..kk.....",
        "....kGlk.klGk...",
        "..kkkGlkkGlGkk..",
        ".kGllGGlllGGllk.",
        ".kGGkkkkkkkkGGk.",
        "..kkbbbbbbbbkk..",
        "..kbbykbbkybbk..",
        "..kbbbbbbbbbbk..",
        "..kbBkkkkkkBbk..",
        ".kbbBkwwwwkBbbk.",
        "kbbkbBBBBBBbkbbk",
        "kBkkbbbbbbbbkkBk",
        ".k.kbBbbbbBbk.k.",
        "...kbbk..kbbk...",
        "..kBBBk..kBBBk..",
        "..kkkkk..kkkkk..",
    )
    private val gemRows = listOf(
        "...kk...",
        "..kzck..",
        ".kzccak.",
        "kzccaaPk",
        "kcccaaPk",
        ".kcaaPk.",
        "..kaPk..",
        "...kk...",
    )
    private val gem2Rows = listOf(
        "...kk...",
        "..kwck..",
        ".kwccCk.",
        "kwccCCnk",
        "kcccCCnk",
        ".kcCCnk.",
        "..kCnk..",
        "...kk...",
    )
    private val coinRows = listOf(
        "..kkkk..",
        ".kyyyYk.",
        "kyywyyYk",
        "kywyyyYk",
        "kyyyyyYk",
        "kyyyyYYk",
        ".kYYYYk.",
        "..kkkk..",
    )
    private val meatRows = listOf(
        "........",
        "..kkkk..",
        ".krrrrk.",
        "krrRrrrk",
        "krrrrRkk",
        ".kRrrkwk",
        "..kkkkwk",
        "......kk",
    )
    private val chestRows = listOf(
        "................",
        "................",
        "..kkkkkkkkkkkk..",
        ".kbbbbbbbbbbbbk.",
        ".kbBBBBBBBBBBbk.",
        ".kkkkkkyykkkkkk.",
        ".kbbbbbkYkbbbbk.",
        ".kbbbbbkykbbbbk.",
        ".kbbbbbbbbbbbbk.",
        ".kBBBBBBBBBBBBk.",
        "..kkkkkkkkkkkk..",
        "................",
    )
    private val bushRows = listOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....kkkk.......",
        "...kkgllgkkk....",
        "..kglgggglgGk...",
        ".kgglggGgggGGk..",
        ".kGggggggGgGGk..",
        "..kGGgGGGGGGk...",
        "...kkkkkkkkk....",
        "................",
        "................",
        "................",
    )
    private val flowerRows = listOf(
        "........",
        "...kk...",
        "..kyyk..",
        ".kyrryk.",
        "..kyyk..",
        "...gk...",
        "..kgGk..",
        "...gg...",
    )
    private val treeRows = listOf(
        ".....kkkkkk.....",
        "...kkgllllgkk...",
        "..kglllggllgGk..",
        ".kgllggggglgGGk.",
        ".kglgggggggGGGk.",
        "kgggggGggggGGGGk",
        "kGgggGGGgggGGGGk",
        "kGGggGGGGgGGGGGk",
        ".kGGGGGGGGGGGGk.",
        "..kkGGGkkGGGkk..",
        "....kkbBBbkk....",
        ".....kbBBbk.....",
        ".....kbBBbk.....",
        "....kbbBBbbk....",
        "...kkkkkkkkkk...",
        "................",
    )
    private val tuftRows = listOf(
        "........",
        "........",
        "........",
        "........",
        "..g..g..",
        ".g.gg.g.",
        "..gGGg..",
        "........",
    )
    private val rockRows = listOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....kkkkk......",
        "....keeeeek.....",
        "...keemeeeEk....",
        "..keeeeeeEEEk...",
        "..kEeeeEEEEEk...",
        "...kkkkkkkkk....",
        "................",
        "................",
        "................",
    )

    private fun build(rows: List<String>): ImageBitmap {
        val w = rows.first().length
        require(rows.all { it.length == w }) { "Sprite satirlari esit uzunlukta olmali" }
        val bmp = Bitmap.createBitmap(w, rows.size, Bitmap.Config.ARGB_8888)
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch -> palette[ch]?.let { bmp.setPixel(x, y, it.toInt()) } }
        }
        return bmp.asImageBitmap()
    }

    val hero: ImageBitmap by lazy { build(heroRows) }
    val hero2: ImageBitmap by lazy { build(hero2Rows) }
    val goblin: ImageBitmap by lazy { build(goblinRows) }
    val wolf: ImageBitmap by lazy { build(wolfRows) }
    val orc: ImageBitmap by lazy { build(orcRows) }
    val bat: ImageBitmap by lazy { build(batRows) }
    val treant: ImageBitmap by lazy { build(treantRows) }
    val gem: ImageBitmap by lazy { build(gemRows) }
    val gem2: ImageBitmap by lazy { build(gem2Rows) }
    val coin: ImageBitmap by lazy { build(coinRows) }
    val meat: ImageBitmap by lazy { build(meatRows) }
    val chest: ImageBitmap by lazy { build(chestRows) }
    val bush: ImageBitmap by lazy { build(bushRows) }
    val flower: ImageBitmap by lazy { build(flowerRows) }
    val tree: ImageBitmap by lazy { build(treeRows) }
    val tuft: ImageBitmap by lazy { build(tuftRows) }
    val rock: ImageBitmap by lazy { build(rockRows) }
}
