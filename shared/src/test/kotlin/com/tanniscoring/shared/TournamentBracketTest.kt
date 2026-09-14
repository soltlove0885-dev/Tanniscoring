package com.tanniscoring.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TournamentBracketTest {

    @Test
    fun `four player bracket has semis and final`() {
        val t = TournamentBracket.create(listOf("A", "B", "C", "D"), defaultBestOf = 3)
        assertEquals(4, t.playerCount)
        assertEquals(3, t.matches.size)
        assertEquals(2, t.matchesInRound(TournamentRound.SEMIFINAL).size)
        assertEquals(1, t.matchesInRound(TournamentRound.FINAL).size)
        val sf0 = t.matchById("sf-0")!!
        assertEquals("A", sf0.playerA)
        assertEquals("B", sf0.playerB)
        assertTrue(sf0.canStart)
        assertEquals(BracketMatchStatus.PENDING, t.matchById("final-0")!!.status)
    }

    @Test
    fun `eight player bracket has quarters semis final`() {
        val names = (1..8).map { "P$it" }
        val t = TournamentBracket.create(names, defaultBestOf = 1)
        assertEquals(8, t.playerCount)
        assertEquals(4, t.matchesInRound(TournamentRound.QUARTERFINAL).size)
        assertEquals(2, t.matchesInRound(TournamentRound.SEMIFINAL).size)
        assertEquals(1, t.matchesInRound(TournamentRound.FINAL).size)
        assertEquals(1, t.defaultBestOf)
        t.matchesInRound(TournamentRound.QUARTERFINAL).forEach {
            assertEquals(BracketMatchStatus.READY, it.status)
            assertEquals(1, it.bestOf)
        }
    }

    @Test
    fun `advance winner fills next slot and champion`() {
        val t0 = TournamentBracket.create(listOf("Kim", "Lee", "Park", "Choi"), 3)
        val t1 = TournamentBracket.advanceWinner(t0, "sf-0", Side.A, setsA = 2, setsB = 0)
        assertEquals(BracketMatchStatus.COMPLETED, t1.matchById("sf-0")!!.status)
        assertEquals("Kim", t1.matchById("final-0")!!.playerA)
        assertNull(t1.matchById("final-0")!!.playerB)
        assertEquals(BracketMatchStatus.PENDING, t1.matchById("final-0")!!.status)

        val t2 = TournamentBracket.advanceWinner(t1, "sf-1", Side.B, setsA = 1, setsB = 2)
        assertEquals("Choi", t2.matchById("final-0")!!.playerB)
        assertEquals(BracketMatchStatus.READY, t2.matchById("final-0")!!.status)
        assertTrue(t2.matchById("final-0")!!.canStart)

        val t3 = TournamentBracket.markInProgress(t2, "final-0")
        assertEquals("final-0", t3.activeMatchId)
        assertEquals(BracketMatchStatus.IN_PROGRESS, t3.matchById("final-0")!!.status)

        val t4 = TournamentBracket.advanceWinner(t3, "final-0", Side.A, 2, 1)
        assertEquals("Kim", t4.champion)
        assertTrue(t4.isComplete)
        assertNull(t4.activeMatchId)
    }

    @Test
    fun `eight player advances through quarters`() {
        val names = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        var t = TournamentBracket.create(names, 3)
        t = TournamentBracket.advanceWinner(t, "qf-0", Side.A)
        t = TournamentBracket.advanceWinner(t, "qf-1", Side.B)
        assertEquals("A", t.matchById("sf-0")!!.playerA)
        assertEquals("D", t.matchById("sf-0")!!.playerB)
        assertEquals(BracketMatchStatus.READY, t.matchById("sf-0")!!.status)
        assertEquals(BracketMatchStatus.PENDING, t.matchById("sf-1")!!.status)
    }

    @Test
    fun `per match bestOf override`() {
        val t0 = TournamentBracket.create(listOf("A", "B", "C", "D"), 3)
        val t1 = TournamentBracket.setMatchBestOf(t0, "sf-0", 5)
        assertEquals(5, t1.matchById("sf-0")!!.bestOf)
        assertEquals(3, t1.matchById("sf-1")!!.bestOf)
        assertEquals(3, t1.defaultBestOf)
    }

    @Test
    fun `best of 1 match format wins on first set`() {
        val engine = TennisScoringEngine()
        engine.startMatch(PlayerNames("A", "B"), MatchFormat.BEST_OF_1)
        fun winGame(side: Side) = repeat(4) { engine.pointWon(side) }
        repeat(6) { winGame(Side.A) }
        val s = engine.currentState()
        assertTrue(s.isMatchOver)
        assertEquals(Side.A, s.winner)
        assertEquals(1, s.setsA)
    }

    @Test
    fun `tournament json roundtrip`() {
        var t = TournamentBracket.create(listOf("김", "이", "박", "최"), 5)
        t = TournamentBracket.markInProgress(t, "sf-0")
        t = TournamentBracket.advanceWinner(t, "sf-0", Side.B, 0, 2)
        val json = SyncJson.encodeTournament(t)
        val decoded = SyncJson.decodeTournament(json)
        assertEquals(t.id, decoded.id)
        assertEquals(t.players, decoded.players)
        assertEquals(t.matches.size, decoded.matches.size)
        assertEquals("이", decoded.matchById("final-0")!!.playerA)
        assertEquals(5, decoded.defaultBestOf)
        assertNull(decoded.activeMatchId)
        assertEquals(BracketMatchStatus.COMPLETED, decoded.matchById("sf-0")!!.status)
    }

    @Test
    fun `fromBestOf maps 1 3 5`() {
        assertEquals(MatchFormat.BEST_OF_1, MatchFormat.fromBestOf(1))
        assertEquals(MatchFormat.BEST_OF_3, MatchFormat.fromBestOf(3))
        assertEquals(MatchFormat.BEST_OF_5, MatchFormat.fromBestOf(5))
    }

    @Test
    fun `tournament defaultNoAd is persisted`() {
        val t = TournamentBracket.create(listOf("A", "B", "C", "D"), defaultBestOf = 3, defaultNoAd = true)
        assertTrue(t.defaultNoAd)
        val json = SyncJson.encodeTournament(t)
        val decoded = SyncJson.decodeTournament(json)
        assertTrue(decoded.defaultNoAd)
        assertEquals(t.defaultBestOf, decoded.defaultBestOf)
    }

}
