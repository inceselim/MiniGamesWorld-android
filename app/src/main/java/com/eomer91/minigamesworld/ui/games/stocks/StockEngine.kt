package com.eomer91.minigamesworld.ui.games.stocks

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Hisseler (kurgusal). AA, CCA ve XAH eski "io stock trade" oyunundan; GLD yeni (piyasa dusunce yukselen guvenli liman).
 * [drift]: trend basina gunluk ortalama getiri, [vol]: gunluk oynaklik.
 */
enum class StockDef(val code: String, val company: String, val startPrice: Float, val vol: Float, val drift: Float, val color: Long) {
    AA("AA", "Aero Avia", 5f, 0.035f, 0.02f, 0xFF38BDF8),
    CCA("CCA", "Cosmo Cola", 9f, 0.02f, 0.012f, 0xFFF87171),
    XAH("XAH", "Xahor Tech", 1.2f, 0.06f, 0.035f, 0xFFA78BFA),
    GLD("GLD", "Golden Mine", 20f, 0.012f, 0.006f, 0xFFFACC15),
}

@Serializable
data class StockState(
    val def: String,
    val price: Float,
    val history: List<Float>,
    /** -1 dusus, 0 yatay, 1 yukselis */
    val trend: Int,
    val trendDaysLeft: Int,
    val nextTrend: Int,
    val owned: Int = 0,
    /** Ortalama alis maliyeti (komisyon dahil). */
    val avgCost: Float = 0f,
) {
    val stock: StockDef get() = StockDef.valueOf(def)
    val dayChange: Float get() = if (history.size < 2) 0f else (price / history[history.size - 2] - 1f)
}

enum class NewsType { TREND_UP, TREND_FLAT, TREND_DOWN, RUMOR_UP, RUMOR_DOWN, EARNINGS_GOOD, EARNINGS_BAD, MARKET_BULL, MARKET_BEAR }

@Serializable
data class NewsItem(val day: Int, val type: String, val stock: String? = null) {
    val newsType: NewsType get() = NewsType.valueOf(type)
}

@Serializable
data class MarketGame(
    val version: Int = 1,
    val day: Int = 1,
    val cash: Float = StockEngine.START_CASH,
    val stocks: List<StockState>,
    /** 1 boga, -1 ayi */
    val market: Int,
    val marketDaysLeft: Int,
    val news: List<NewsItem> = emptyList(),
    /** Ipucu (odullu reklam) alindigi gun; o gunden sonra [StockEngine.TIP_DAYS] gun trendler gorunur. */
    val tipDay: Int = -100,
    val over: Boolean = false,
) {
    val netWorth: Float get() = cash + stocks.sumOf { (it.owned * it.price).toDouble() }.toFloat()
    val score: Int get() = netWorth.toInt()
    val tipActive: Boolean get() = day - tipDay < StockEngine.TIP_DAYS
    fun stock(d: StockDef) = stocks.first { it.def == d.name }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String): MarketGame? = runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
    }
}

sealed interface TradeResult {
    data class Ok(val game: MarketGame, val shares: Int) : TradeResult
    data object NotEnoughCash : TradeResult
    data object NotEnoughShares : TradeResult
}

/** Borsa kurallari. Android'den bagimsiz, unit test ile test edilir. */
class StockEngine(private val random: Random = Random.Default) {

    fun newGame(): MarketGame {
        val market = if (random.nextBoolean()) 1 else -1
        val stocks = StockDef.entries.map { d ->
            val trend = nextTrend(null)
            StockState(d.name, d.startPrice, listOf(d.startPrice), trend, durationStock(), nextTrend(trend))
        }
        val news = listOf(NewsItem(1, (if (market > 0) NewsType.MARKET_BULL else NewsType.MARKET_BEAR).name))
        return MarketGame(stocks = stocks, market = market, marketDaysLeft = durationMarket(), news = news)
    }

    private fun nextTrend(current: Int?): Int {
        val opts = intArrayOf(-1, 0, 1)
        val w = floatArrayOf(1f, 0.6f, 1.3f)
        if (current != null) w[current + 1] *= 0.3f
        var roll = random.nextFloat() * w.sum()
        for (i in opts.indices) {
            roll -= w[i]
            if (roll <= 0f) return opts[i]
        }
        return 1
    }

    private fun durationStock() = 6 + random.nextInt(9)
    private fun durationMarket() = 10 + random.nextInt(11)

    private fun gaussian(): Float {
        val u1 = random.nextDouble().coerceAtLeast(1e-9)
        val u2 = random.nextDouble()
        return (sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)).toFloat()
    }

    /** Bir gun ilerlet. Son gun bitince [MarketGame.over] olur. */
    fun nextDay(g: MarketGame): MarketGame {
        if (g.over) return g
        val day = g.day + 1
        val news = ArrayList<NewsItem>()

        val stocks = g.stocks.map { s ->
            val d = s.stock
            val marketEffect = MARKET_EFFECT * (if (d == StockDef.GLD) -g.market else g.market)
            var r = d.drift * s.trend + marketEffect + gaussian() * d.vol
            if (random.nextFloat() < SHOCK_CHANCE) {
                val up = random.nextBoolean()
                r += (if (up) 1 else -1) * (0.10f + random.nextFloat() * 0.15f)
                news += NewsItem(day, (if (up) NewsType.EARNINGS_GOOD else NewsType.EARNINGS_BAD).name, d.name)
            }
            val price = (s.price * (1f + r)).coerceAtLeast(MIN_PRICE)
            val history = (s.history + price).takeLast(HISTORY)

            var trend = s.trend
            var left = s.trendDaysLeft - 1
            var next = s.nextTrend
            if (left == RUMOR_DAYS_BEFORE && random.nextFloat() < RUMOR_CHANCE) {
                // Soylenti cogu zaman dogru cikar (yatay trendde soylenti yok)
                val dir = if (random.nextFloat() < RUMOR_ACCURACY) next else if (random.nextBoolean()) 1 else -1
                if (dir != 0) news += NewsItem(day, (if (dir > 0) NewsType.RUMOR_UP else NewsType.RUMOR_DOWN).name, d.name)
            }
            if (left <= 0) {
                trend = next
                next = nextTrend(trend)
                left = durationStock()
                val type = when (trend) { 1 -> NewsType.TREND_UP; -1 -> NewsType.TREND_DOWN; else -> NewsType.TREND_FLAT }
                news += NewsItem(day, type.name, d.name)
            }
            s.copy(price = price, history = history, trend = trend, trendDaysLeft = left, nextTrend = next)
        }

        var market = g.market
        var mLeft = g.marketDaysLeft - 1
        if (mLeft <= 0) {
            market = -market
            mLeft = durationMarket()
            news += NewsItem(day, (if (market > 0) NewsType.MARKET_BULL else NewsType.MARKET_BEAR).name)
        }
        return g.copy(
            day = day,
            stocks = stocks,
            market = market,
            marketDaysLeft = mLeft,
            news = (news + g.news).take(MAX_NEWS),
            over = day >= DAYS,
        )
    }

    /** En fazla alinabilecek adet (komisyon dahil). */
    fun maxBuy(g: MarketGame, d: StockDef): Int = (g.cash / (g.stock(d).price * (1f + FEE))).toInt().coerceAtLeast(0)

    fun buy(g: MarketGame, d: StockDef, shares: Int): TradeResult {
        if (g.over || shares <= 0) return TradeResult.NotEnoughCash
        val s = g.stock(d)
        val cost = shares * s.price * (1f + FEE)
        if (cost > g.cash + 1e-3f) return TradeResult.NotEnoughCash
        val owned = s.owned + shares
        val avg = (s.avgCost * s.owned + cost) / owned
        val stocks = g.stocks.map { if (it.def == d.name) it.copy(owned = owned, avgCost = avg) else it }
        return TradeResult.Ok(g.copy(cash = (g.cash - cost).coerceAtLeast(0f), stocks = stocks), shares)
    }

    fun sell(g: MarketGame, d: StockDef, shares: Int): TradeResult {
        val s = g.stock(d)
        if (g.over || shares <= 0 || shares > s.owned) return TradeResult.NotEnoughShares
        val income = shares * s.price * (1f - FEE)
        val owned = s.owned - shares
        val stocks = g.stocks.map { if (it.def == d.name) it.copy(owned = owned, avgCost = if (owned == 0) 0f else it.avgCost) else it }
        return TradeResult.Ok(g.copy(cash = g.cash + income, stocks = stocks), shares)
    }

    fun useTip(g: MarketGame) = g.copy(tipDay = g.day)

    companion object {
        const val START_CASH = 1000f
        const val DAYS = 60
        const val FEE = 0.005f
        const val HISTORY = 40
        const val MAX_NEWS = 12
        const val MIN_PRICE = 0.1f
        const val MARKET_EFFECT = 0.006f
        const val SHOCK_CHANCE = 0.04f
        const val RUMOR_DAYS_BEFORE = 2
        const val RUMOR_CHANCE = 0.8f
        const val RUMOR_ACCURACY = 0.8f
        const val TIP_DAYS = 5
    }
}
