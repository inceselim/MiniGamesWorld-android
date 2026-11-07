package com.eomer91.minigamesworld.ui.games.football.engine

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Dunyanin sikistirilmis ikili kaydi. Bicim surumlu: yeni alanlar eklenince VERSION artar ve
 * eski kayitlar varsayilanlarla okunur.
 */
object FbSave {
    private const val MAGIC = 0x46424D47 // "FBMG"
    const val VERSION = 1

    fun write(w: FbWorld, out: OutputStream) {
        DataOutputStream(BufferedOutputStream(GZIPOutputStream(out), 1 shl 16)).use { o ->
            o.writeInt(MAGIC); o.writeInt(VERSION)
            o.writeLong(w.seed); o.writeInt(w.season); o.writeInt(w.day); o.writeInt(w.userClub); o.writeUTF(w.managerName)

            o.writeInt(w.leagues.size)
            for (l in w.leagues) {
                o.writeShort(l.country); o.writeByte(l.tier); o.writeUTF(l.customName)
                o.writeByte(l.promoted); o.writeByte(l.relegated); o.writeByte(l.playoffSpots)
                writeInts(o, l.clubs)
            }

            o.writeInt(w.clubs.size)
            for (c in w.clubs) {
                o.writeUTF(c.name); o.writeUTF(c.shortName); o.writeShort(c.country); o.writeUTF(c.city)
                o.writeInt(c.league); o.writeShort(c.reputation); o.writeInt(c.color1); o.writeInt(c.color2)
                o.writeUTF(c.stadiumName); o.writeInt(c.capacity)
                o.writeByte(c.trainingFacilities); o.writeByte(c.youthFacilities); o.writeByte(c.youthRecruitment)
                o.writeByte(c.stadiumLevel); o.writeByte(c.socialFacilities)
                o.writeLong(c.balance); o.writeLong(c.transferBudget); o.writeLong(c.wageBudget); o.writeLong(c.sponsorIncome)
                o.writeInt(c.fanBase); o.writeUTF(c.managerName)
                writeInts(o, c.players); writeInts(o, c.staff)
            }

            o.writeInt(w.players.size)
            for (p in w.players) {
                o.writeUTF(p.firstName); o.writeUTF(p.lastName); o.writeShort(p.nation)
                o.writeShort(p.birthYear); o.writeShort(p.birthDay)
                o.write(p.attrs); o.write(p.hidden); o.write(p.posFam)
                o.writeByte(p.potential); o.writeByte(p.foot.ordinal); o.writeByte(p.heightCm - 100)
                o.writeInt(p.club); o.writeInt(p.wage); o.writeShort(p.contractEnd); o.writeInt(p.releaseClause)
                o.writeByte(p.squadNumber); o.writeByte(p.morale); o.writeByte(p.condition); o.writeByte(p.form)
                o.writeShort(p.injuryDays); o.writeByte(p.suspendedMatches); o.writeShort(p.reputation)
                o.writeLong(p.askingPrice); o.writeInt(p.loanFrom); o.writeShort(p.caps); o.writeShort(p.internationalGoals)
            }

            o.writeInt(w.staff.size)
            for (s in w.staff) {
                o.writeUTF(s.firstName); o.writeUTF(s.lastName); o.writeShort(s.nation); o.writeShort(s.birthYear)
                o.writeByte(s.role.ordinal); o.write(s.attrs)
                o.writeInt(s.club); o.writeInt(s.wage); o.writeShort(s.contractEnd); o.writeShort(s.reputation)
            }
        }
    }

    fun read(input: InputStream): FbWorld {
        DataInputStream(BufferedInputStream(GZIPInputStream(input), 1 shl 16)).use { i ->
            require(i.readInt() == MAGIC) { "Gecersiz kayit" }
            val version = i.readInt()
            require(version in 1..VERSION) { "Bilinmeyen kayit surumu $version" }
            val w = FbWorld(i.readLong())
            w.season = i.readInt(); w.day = i.readInt(); w.userClub = i.readInt(); w.managerName = i.readUTF()

            repeat(i.readInt()) { id ->
                val l = FbLeague(id, i.readUnsignedShort(), i.readUnsignedByte())
                l.customName = i.readUTF()
                l.promoted = i.readUnsignedByte(); l.relegated = i.readUnsignedByte(); l.playoffSpots = i.readUnsignedByte()
                l.clubs.addAll(readInts(i))
                w.leagues.add(l)
            }

            repeat(i.readInt()) { id ->
                val name = i.readUTF(); val short = i.readUTF(); val country = i.readUnsignedShort(); val city = i.readUTF()
                val c = FbClub(id, name, short, country, city)
                c.league = i.readInt(); c.reputation = i.readUnsignedShort(); c.color1 = i.readInt(); c.color2 = i.readInt()
                c.stadiumName = i.readUTF(); c.capacity = i.readInt()
                c.trainingFacilities = i.readUnsignedByte(); c.youthFacilities = i.readUnsignedByte(); c.youthRecruitment = i.readUnsignedByte()
                c.stadiumLevel = i.readUnsignedByte(); c.socialFacilities = i.readUnsignedByte()
                c.balance = i.readLong(); c.transferBudget = i.readLong(); c.wageBudget = i.readLong(); c.sponsorIncome = i.readLong()
                c.fanBase = i.readInt(); c.managerName = i.readUTF()
                c.players.addAll(readInts(i)); c.staff.addAll(readInts(i))
                w.clubs.add(c)
            }

            repeat(i.readInt()) { id ->
                val first = i.readUTF(); val last = i.readUTF(); val nation = i.readUnsignedShort()
                val by = i.readShort().toInt(); val bd = i.readShort().toInt()
                val attrs = ByteArray(Attr.COUNT).also { i.readFully(it) }
                val hidden = ByteArray(Hidden.entries.size).also { i.readFully(it) }
                val fam = ByteArray(Pos.COUNT).also { i.readFully(it) }
                val pot = i.readUnsignedByte(); val foot = Foot.entries[i.readUnsignedByte()]; val h = i.readUnsignedByte() + 100
                val p = FbPlayer(id, first, last, nation, by, bd, attrs, hidden, fam, pot, foot, h)
                p.club = i.readInt(); p.wage = i.readInt(); p.contractEnd = i.readShort().toInt(); p.releaseClause = i.readInt()
                p.squadNumber = i.readUnsignedByte(); p.morale = i.readUnsignedByte(); p.condition = i.readUnsignedByte(); p.form = i.readUnsignedByte()
                p.injuryDays = i.readShort().toInt(); p.suspendedMatches = i.readUnsignedByte(); p.reputation = i.readUnsignedShort()
                p.askingPrice = i.readLong(); p.loanFrom = i.readInt(); p.caps = i.readUnsignedShort(); p.internationalGoals = i.readUnsignedShort()
                w.players.add(p)
            }

            repeat(i.readInt()) { id ->
                val first = i.readUTF(); val last = i.readUTF(); val nation = i.readUnsignedShort(); val by = i.readShort().toInt()
                val role = StaffRole.entries[i.readUnsignedByte()]
                val attrs = ByteArray(StaffAttr.entries.size).also { i.readFully(it) }
                val s = FbStaff(id, first, last, nation, by, role, attrs)
                s.club = i.readInt(); s.wage = i.readInt(); s.contractEnd = i.readShort().toInt(); s.reputation = i.readUnsignedShort()
                w.staff.add(s)
            }
            return w
        }
    }

    private fun writeInts(o: DataOutputStream, l: List<Int>) {
        o.writeInt(l.size); for (v in l) o.writeInt(v)
    }

    private fun readInts(i: DataInputStream): List<Int> = List(i.readInt()) { i.readInt() }
}
