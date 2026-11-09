package com.eomer91.minigamesworld.ui.games.island.engine

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Ada Patronu kaydi (surumlu ikili bicim, gzip). */
object IsSave {
    private const val MAGIC = 0x49534C44 // "ISLD"
    const val VERSION = 1

    fun write(w: IsWorld, out: OutputStream) {
        DataOutputStream(BufferedOutputStream(GZIPOutputStream(out, 1 shl 15), 1 shl 15)).use { o ->
            o.writeInt(MAGIC); o.writeInt(VERSION)
            val s = w.settings
            o.writeUTF(s.companyName); o.writeInt(s.color); o.writeByte(s.bots)
            o.writeByte(s.difficulty.ordinal); o.writeByte(s.mapType.ordinal); o.writeByte(s.mapSize.ordinal)
            o.writeByte(s.goal.ordinal); o.writeLong(s.seed)
            o.writeInt(w.n); o.writeInt(w.day); o.writeInt(w.startYear); o.writeLong(w.rnd.state)
            o.writeBoolean(w.gameOver); o.writeInt(w.winner)
            o.write(w.terrain); o.write(w.waterDist)
            for (v in w.owner) o.writeByte(v)
            for (c in 0..2) { o.writeDouble(w.demand[c]); o.writeFloat(w.present[c]); o.writeFloat(w.lost[c]) }

            o.writeInt(w.companies.size)
            for (co in w.companies) {
                o.writeUTF(co.name); o.writeInt(co.color); o.writeBoolean(co.bot); o.writeByte(co.personality.ordinal)
                o.writeLong(co.balance); o.writeFloat(co.reputation)
                o.writeByte(Fin.entries.size)
                for (v in co.ledger) o.writeLong(v)
                for (v in co.lastLedger) o.writeLong(v)
                o.writeLong(co.monthIn); o.writeLong(co.monthOut); o.writeLong(co.lastMonthIn); o.writeLong(co.lastMonthOut)
                for (v in co.adUntil) o.writeInt(v)
                o.writeLong(co.loanLeft); o.writeFloat(co.loanRate); o.writeLong(co.loanPayment); o.writeShort(co.loanMonthsLeft)
                o.writeByte(co.redMonths); o.writeBoolean(co.bankrupt); o.writeInt(co.lastDirtyDay)
                o.writeDouble(co.satSum); o.writeDouble(co.satW)
            }

            o.writeInt(w.buildings.size)
            for (b in w.buildings) {
                if (b == null) { o.writeBoolean(false); continue }
                o.writeBoolean(true)
                o.writeByte(b.type.id); o.writeByte(b.owner); o.writeShort(b.x); o.writeShort(b.y)
                o.writeFloat(b.price); o.writeShort(b.buildLeft); o.writeInt(b.closedDays); o.writeByte(b.level)
                for (v in b.today) o.writeFloat(v)
                for (v in b.loc) o.writeFloat(v)
                o.writeLong(b.monthRevenue); o.writeLong(b.lastMonthRevenue); o.writeFloat(b.monthUse); o.writeFloat(b.lastMonthUse)
            }

            o.writeInt(w.auctions.size)
            for (a in w.auctions) {
                o.writeInt(a.building); o.writeLong(a.minBid); o.writeInt(a.endDay)
                o.writeLong(a.topBid); o.writeByte(a.topBidder); o.writeBoolean(a.done)
            }

            o.writeInt(w.news.size)
            for (nw in w.news) {
                o.writeInt(nw.day); o.writeByte(nw.kind.ordinal); o.writeByte(nw.company); o.writeByte(nw.other)
                o.writeLong(nw.value); o.writeShort(nw.type); o.writeBoolean(nw.read)
            }
        }
    }

    fun read(inp: InputStream): IsWorld {
        DataInputStream(BufferedInputStream(GZIPInputStream(inp, 1 shl 15), 1 shl 15)).use { i ->
            require(i.readInt() == MAGIC) { "not an island save" }
            val version = i.readInt()
            require(version in 1..VERSION) { "unsupported version $version" }
            val s = IsSettings()
            s.companyName = i.readUTF(); s.color = i.readInt(); s.bots = i.readByte().toInt()
            s.difficulty = Difficulty.entries[i.readByte().toInt()]; s.mapType = MapType.entries[i.readByte().toInt()]
            s.mapSize = MapSize.entries[i.readByte().toInt()]; s.goal = Goal.entries[i.readByte().toInt()]; s.seed = i.readLong()
            val n = i.readInt()
            val w = IsWorld(s, n)
            w.day = i.readInt(); w.startYear = i.readInt(); w.rnd.state = i.readLong()
            w.gameOver = i.readBoolean(); w.winner = i.readInt()
            i.readFully(w.terrain); i.readFully(w.waterDist)
            for (k in 0 until n * n) w.owner[k] = i.readByte().toInt()
            for (c in 0..2) { w.demand[c] = i.readDouble(); w.present[c] = i.readFloat(); w.lost[c] = i.readFloat() }

            repeat(i.readInt()) { id ->
                val co = Company(id, i.readUTF(), i.readInt(), i.readBoolean(), Personality.entries[i.readByte().toInt()])
                co.balance = i.readLong(); co.reputation = i.readFloat()
                val nf = i.readByte().toInt()
                for (k in 0 until nf) { val v = i.readLong(); if (k < co.ledger.size) co.ledger[k] = v }
                for (k in 0 until nf) { val v = i.readLong(); if (k < co.lastLedger.size) co.lastLedger[k] = v }
                co.monthIn = i.readLong(); co.monthOut = i.readLong(); co.lastMonthIn = i.readLong(); co.lastMonthOut = i.readLong()
                for (k in 0..2) co.adUntil[k] = i.readInt()
                co.loanLeft = i.readLong(); co.loanRate = i.readFloat(); co.loanPayment = i.readLong(); co.loanMonthsLeft = i.readShort().toInt()
                co.redMonths = i.readByte().toInt(); co.bankrupt = i.readBoolean(); co.lastDirtyDay = i.readInt()
                co.satSum = i.readDouble(); co.satW = i.readDouble()
                w.companies.add(co)
            }

            val nb = i.readInt()
            for (id in 0 until nb) {
                if (!i.readBoolean()) { w.buildings.add(null); continue }
                val type = IsCatalog[i.readByte().toInt()]
                val b = Building(id, type, i.readByte().toInt(), i.readShort().toInt(), i.readShort().toInt())
                b.price = i.readFloat(); b.buildLeft = i.readShort().toInt(); b.closedDays = i.readInt(); b.level = i.readByte().toInt()
                for (k in 0..2) b.today[k] = i.readFloat()
                for (k in 0..2) b.loc[k] = i.readFloat()
                b.monthRevenue = i.readLong(); b.lastMonthRevenue = i.readLong(); b.monthUse = i.readFloat(); b.lastMonthUse = i.readFloat()
                w.buildings.add(b)
                for (yy in b.y until b.y + type.h) for (xx in b.x until b.x + type.w) w.bAt[w.idx(xx, yy)] = id
            }

            repeat(i.readInt()) { id ->
                val a = IsAuction(id, i.readInt(), i.readLong(), i.readInt())
                a.topBid = i.readLong(); a.topBidder = i.readByte().toInt(); a.done = i.readBoolean()
                w.auctions.add(a)
            }

            repeat(i.readInt()) {
                val nw = IsNews(i.readInt(), NewsKind.entries[i.readByte().toInt()], i.readByte().toInt(), i.readByte().toInt(), i.readLong(), i.readShort().toInt())
                nw.read = i.readBoolean()
                w.news.add(nw)
            }
            return w
        }
    }
}
