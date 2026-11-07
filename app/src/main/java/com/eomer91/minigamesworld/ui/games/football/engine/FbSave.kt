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
    const val VERSION = 7

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

            // v2: taktik + sezon durumu
            // v3: kulup yonetimi ve oyuncu antrenman alanlari
            for (c in w.clubs) {
                o.writeByte(c.ticketLevel + 2); o.writeUTF(c.sponsorName); o.writeShort(c.sponsorEnd)
                o.writeByte(c.trainArea + 1); o.writeByte(c.intensity)
                o.writeByte(c.upgradeType + 1); o.writeShort(c.upgradeSeason); o.writeShort(c.upgradeDay)
                for (v in c.ledger) o.writeLong(v)
                for (v in c.lastLedger) o.writeLong(v)
                writeInts(o, c.balanceHistory); o.writeShort(c.soldOut); o.writeByte(c.lastPosition)
            }
            for (p in w.players) { o.writeShort(p.playTime); o.writeByte(p.trainArea + 1); o.writeByte(p.trainPos + 1); o.writeFloat(p.devAcc) }
            // v4: transfer
            for (p in w.players) {
                o.writeShort(p.loanUntil); o.writeByte(p.loanWagePct); o.writeBoolean(p.loanListed)
                o.writeInt(p.preContract); o.writeInt(p.preContractWage); o.writeInt(p.sellOnClub); o.writeByte(p.sellOnPct); o.writeBoolean(p.wantsAway)
            }
            writeMarket(o, w.market)
            // v5: reklam ile acilanlar
            writeInts(o, w.revealed.toList()); o.writeInt(w.adBonusWeek); o.writeByte(w.adBonusCount)
            // v6: elle kurulan ilk 11
            writeInts(o, w.userLineup?.toList() ?: emptyList())
            // v7: altyapi, gelisim, haber metni
            for (p in w.players) { o.writeBoolean(p.youth); o.writeShort(p.caStart) }
            for (n in w.market.news) o.writeUTF(n.text)
            val ut = w.userTactics
            o.writeBoolean(ut != null)
            if (ut != null) writeTactics(o, ut)
            val st = w.state
            o.writeBoolean(st != null)
            if (st != null) writeState(o, st)
        }
    }

    private fun writeTactics(o: DataOutputStream, t: Tactics) {
        o.writeByte(t.formation.ordinal); o.writeByte(t.mentality.ordinal); o.writeByte(t.passing.ordinal); o.writeByte(t.tempo.ordinal)
        o.writeByte(t.width.ordinal); o.writeByte(t.pressing.ordinal); o.writeByte(t.defLine.ordinal); o.writeByte(t.marking.ordinal)
        o.writeByte(t.focus.ordinal)
        o.writeByte((if (t.counter) 1 else 0) or (if (t.offsideTrap) 2 else 0) or (if (t.timeWasting) 4 else 0) or (if (t.hardTackling) 8 else 0))
    }

    private fun readTactics(i: DataInputStream): Tactics {
        val f = Formation.entries[i.readUnsignedByte()]; val m = Mentality.entries[i.readUnsignedByte()]
        val p = PassingStyle.entries[i.readUnsignedByte()]; val te = Tempo.entries[i.readUnsignedByte()]
        val wi = Width.entries[i.readUnsignedByte()]; val pr = Pressing.entries[i.readUnsignedByte()]
        val dl = DefLine.entries[i.readUnsignedByte()]; val mk = Marking.entries[i.readUnsignedByte()]
        val fo = AttackFocus.entries[i.readUnsignedByte()]; val fl = i.readUnsignedByte()
        return Tactics(formation = f, mentality = m, passing = p, tempo = te, width = wi, pressing = pr, defLine = dl, marking = mk, focus = fo,
            counter = fl and 1 != 0, offsideTrap = fl and 2 != 0, timeWasting = fl and 4 != 0, hardTackling = fl and 8 != 0)
    }

    private fun writeState(o: DataOutputStream, st: FbSeasonState) {
        o.writeInt(st.coef.size)
        for (row in st.coef) for (v in row) o.writeFloat(v)
        for (v in st.elo) o.writeFloat(v)
        for (v in st.coefPts) o.writeFloat(v)
        for (v in st.coefClubs) o.writeShort(v)
        for (v in st.finMul) o.writeFloat(v)
        o.writeShort(st.leagueMul.size); for (v in st.leagueMul) o.writeFloat(v)
        o.writeInt(st.honours.size)
        for (h in st.honours) { o.writeShort(h.season); o.writeByte(h.kind.ordinal); o.writeShort(h.country); o.writeByte(h.tier); o.writeInt(h.winner); o.writeInt(h.runnerUp) }
        o.writeInt(st.comps.size)
        for (c in st.comps) {
            o.writeByte(c.kind.ordinal); o.writeShort(c.country); o.writeShort(c.league); o.writeByte(c.confed)
            o.writeInt(c.winner); o.writeInt(c.runnerUp); o.writeBoolean(c.done)
            o.writeByte(c.stages.size)
            for (s in c.stages) {
                o.writeByte(s.type.ordinal); o.writeByte(s.label.ordinal); o.writeByte(s.legs)
                writeArr(o, s.days)
                o.writeShort(s.groups.size); for (g in s.groups) writeArr(o, g)
                o.writeShort(s.ties.size); for (t in s.ties) { o.writeInt(t.a); o.writeInt(t.b); o.writeInt(t.winner) }
                writeArr(o, s.byes); writeInts(o, s.extra)
                o.writeBoolean(s.built); o.writeBoolean(s.complete); o.writeShort(s.target); o.writeShort(s.losersTo)
            }
        }
        o.writeInt(st.fixtures.size)
        for (f in st.fixtures) {
            o.writeShort(f.comp); o.writeByte(f.stage); o.writeShort(f.group); o.writeByte(f.round); o.writeShort(f.day)
            o.writeInt(f.home); o.writeInt(f.away); o.writeByte(f.leg)
            o.writeByte(f.hg); o.writeByte(f.ag); o.writeByte(f.ph); o.writeByte(f.pa); o.writeBoolean(f.extraTime)
        }
        o.writeInt(st.stats.size)
        for ((k, s) in st.stats) {
            o.writeLong(k); o.writeShort(s.apps); o.writeShort(s.subs); o.writeShort(s.goals); o.writeShort(s.assists)
            o.writeInt(s.ratingSum); o.writeShort(s.yellows); o.writeShort(s.reds); o.writeShort(s.motm)
        }
        o.writeInt(st.yellowAcc.size)
        for ((k, v) in st.yellowAcc) { o.writeInt(k); o.writeShort(v) }
    }

    private fun readState(i: DataInputStream, stateVersion: Int): FbSeasonState {
        val n = i.readInt()
        val st = FbSeasonState(n)
        for (row in st.coef) for (k in row.indices) row[k] = i.readFloat()
        for (k in 0 until n) st.elo[k] = i.readFloat()
        for (k in 0 until n) st.coefPts[k] = i.readFloat()
        for (k in 0 until n) st.coefClubs[k] = i.readShort().toInt()
        if (stateVersion >= 3) for (k in 0 until n) st.finMul[k] = i.readFloat()
        if (stateVersion >= 4) st.leagueMul = FloatArray(i.readShort().toInt()) { i.readFloat() }
        repeat(i.readInt()) {
            st.honours.add(FbHonour(i.readShort().toInt(), CompKind.entries[i.readUnsignedByte()], i.readShort().toInt(), i.readUnsignedByte(), i.readInt(), i.readInt()))
        }
        repeat(i.readInt()) { id ->
            val c = FbComp(id, CompKind.entries[i.readUnsignedByte()], i.readShort().toInt(), i.readShort().toInt(), i.readByte().toInt())
            c.winner = i.readInt(); c.runnerUp = i.readInt(); c.done = i.readBoolean()
            repeat(i.readUnsignedByte()) {
                val s = FbStage(StageType.entries[i.readUnsignedByte()], StageLabel.entries[i.readUnsignedByte()], i.readUnsignedByte(), readArr(i))
                repeat(i.readShort().toInt()) { s.groups.add(readArr(i)) }
                repeat(i.readShort().toInt()) { val t = FbTie(i.readInt(), i.readInt()); t.winner = i.readInt(); s.ties.add(t) }
                s.byes = readArr(i); s.extra.addAll(readInts(i))
                s.built = i.readBoolean(); s.complete = i.readBoolean(); s.target = i.readShort().toInt(); s.losersTo = i.readShort().toInt()
                c.stages.add(s)
            }
            st.comps.add(c)
        }
        repeat(i.readInt()) { id ->
            val f = FbFixture(id, i.readShort().toInt(), i.readByte().toInt(), i.readShort().toInt(), i.readUnsignedByte(), i.readShort().toInt(), i.readInt(), i.readInt())
            f.leg = i.readByte().toInt(); f.hg = i.readByte().toInt(); f.ag = i.readByte().toInt(); f.ph = i.readByte().toInt(); f.pa = i.readByte().toInt()
            f.extraTime = i.readBoolean()
            st.fixtures.add(f)
        }
        repeat(i.readInt()) {
            val k = i.readLong(); val s = PlayerCompStats()
            s.apps = i.readShort().toInt(); s.subs = i.readShort().toInt(); s.goals = i.readShort().toInt(); s.assists = i.readShort().toInt()
            s.ratingSum = i.readInt(); s.yellows = i.readShort().toInt(); s.reds = i.readShort().toInt(); s.motm = i.readShort().toInt()
            st.stats[k] = s
        }
        repeat(i.readInt()) { st.yellowAcc[i.readInt()] = i.readShort().toInt() }
        st.indexDays()
        return st
    }

    private fun writeMarket(o: DataOutputStream, m: FbMarket) {
        o.writeInt(m.nextId)
        o.writeInt(m.offers.size)
        for (x in m.offers) {
            o.writeInt(x.id); o.writeInt(x.player); o.writeInt(x.from); o.writeInt(x.to); o.writeLong(x.fee); o.writeBoolean(x.loan)
            o.writeByte(x.loanWagePct); o.writeByte(x.sellOnPct); o.writeByte(x.status.ordinal); o.writeLong(x.counterFee)
            o.writeShort(x.responseDay); o.writeShort(x.season); o.writeByte(x.rounds); o.writeByte(x.patience)
        }
        o.writeInt(m.auctions.size)
        for (a in m.auctions) {
            o.writeInt(a.id); o.writeInt(a.player); o.writeInt(a.seller); o.writeLong(a.reserve); o.writeShort(a.endSeason); o.writeShort(a.endDay)
            o.writeLong(a.topBid); o.writeInt(a.topBidder); o.writeShort(a.bids); o.writeBoolean(a.done)
        }
        writeInts(o, m.shortlist)
        o.writeInt(m.news.size)
        for (n in m.news) {
            o.writeByte(n.kind.ordinal); o.writeShort(n.season); o.writeShort(n.day); o.writeInt(n.player); o.writeInt(n.club); o.writeInt(n.club2)
            o.writeLong(n.value); o.writeBoolean(n.read); o.writeByte(n.credibility)
        }
        o.writeInt(m.history.size)
        for (h in m.history) {
            o.writeShort(h.season); o.writeShort(h.day); o.writeUTF(h.playerName); o.writeInt(h.player); o.writeInt(h.from); o.writeInt(h.to); o.writeLong(h.fee); o.writeBoolean(h.loan)
        }
    }

    private fun readMarket(i: DataInputStream, m: FbMarket) {
        m.nextId = i.readInt()
        repeat(i.readInt()) {
            val x = FbOffer(i.readInt(), i.readInt(), i.readInt(), i.readInt(), i.readLong(), i.readBoolean(), i.readUnsignedByte(), i.readUnsignedByte())
            x.status = OfferStatus.entries[i.readUnsignedByte()]; x.counterFee = i.readLong()
            x.responseDay = i.readShort().toInt(); x.season = i.readShort().toInt(); x.rounds = i.readUnsignedByte(); x.patience = i.readByte().toInt()
            m.offers.add(x)
        }
        repeat(i.readInt()) {
            val a = FbAuction(i.readInt(), i.readInt(), i.readInt(), i.readLong(), i.readShort().toInt(), i.readShort().toInt())
            a.topBid = i.readLong(); a.topBidder = i.readInt(); a.bids = i.readShort().toInt(); a.done = i.readBoolean()
            m.auctions.add(a)
        }
        m.shortlist.addAll(readInts(i))
        repeat(i.readInt()) {
            val n = FbNews(NewsKind.entries[i.readUnsignedByte()], i.readShort().toInt(), i.readShort().toInt(), i.readInt(), i.readInt(), i.readInt(), i.readLong())
            n.read = i.readBoolean(); n.credibility = i.readUnsignedByte()
            m.news.add(n)
        }
        repeat(i.readInt()) {
            m.history.add(FbTransferRecord(i.readShort().toInt(), i.readShort().toInt(), i.readUTF(), i.readInt(), i.readInt(), i.readInt(), i.readLong(), i.readBoolean()))
        }
    }

    private fun writeArr(o: DataOutputStream, a: IntArray) { o.writeShort(a.size); for (v in a) o.writeInt(v) }
    private fun readArr(i: DataInputStream): IntArray = IntArray(i.readShort().toInt()) { i.readInt() }

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
            if (version >= 3) {
                for (c in w.clubs) {
                    c.ticketLevel = i.readUnsignedByte() - 2; c.sponsorName = i.readUTF(); c.sponsorEnd = i.readShort().toInt()
                    c.trainArea = i.readUnsignedByte() - 1; c.intensity = i.readUnsignedByte()
                    c.upgradeType = i.readUnsignedByte() - 1; c.upgradeSeason = i.readShort().toInt(); c.upgradeDay = i.readShort().toInt()
                    for (k in c.ledger.indices) c.ledger[k] = i.readLong()
                    for (k in c.lastLedger.indices) c.lastLedger[k] = i.readLong()
                    c.balanceHistory.addAll(readInts(i)); c.soldOut = i.readShort().toInt(); c.lastPosition = i.readUnsignedByte()
                }
                for (p in w.players) { p.playTime = i.readShort().toInt(); p.trainArea = i.readUnsignedByte() - 1; p.trainPos = i.readUnsignedByte() - 1; p.devAcc = i.readFloat() }
            }
            if (version >= 4) {
                for (p in w.players) {
                    p.loanUntil = i.readShort().toInt(); p.loanWagePct = i.readUnsignedByte(); p.loanListed = i.readBoolean()
                    p.preContract = i.readInt(); p.preContractWage = i.readInt(); p.sellOnClub = i.readInt(); p.sellOnPct = i.readUnsignedByte(); p.wantsAway = i.readBoolean()
                }
                readMarket(i, w.market)
            }
            if (version >= 5) { w.revealed.addAll(readInts(i)); w.adBonusWeek = i.readInt(); w.adBonusCount = i.readUnsignedByte() }
            if (version >= 6) w.userLineup = readInts(i).takeIf { it.size == 11 }?.toIntArray()
            if (version >= 7) {
                for (p in w.players) { p.youth = i.readBoolean(); p.caStart = i.readShort().toInt() }
                for (n in w.market.news) n.text = i.readUTF()
            }
            if (version >= 2) {
                if (i.readBoolean()) w.userTactics = readTactics(i)
                if (i.readBoolean()) w.state = readState(i, version)
            }
            for (p in w.players) p.invalidate()
            return w
        }
    }

    private fun writeInts(o: DataOutputStream, l: List<Int>) {
        o.writeInt(l.size); for (v in l) o.writeInt(v)
    }

    private fun readInts(i: DataInputStream): List<Int> = List(i.readInt()) { i.readInt() }
}
