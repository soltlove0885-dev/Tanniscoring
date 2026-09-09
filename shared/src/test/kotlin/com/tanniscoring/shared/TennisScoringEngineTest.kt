package com.tanniscoring.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TennisScoringEngineTest {

    private lateinit var engine: TennisScoringEngine

    @BeforeEach
    fun setUp() {
        engine = TennisScoringEngine()
        engine.startMatch(PlayerNames("김철수", "이영희"), MatchFormat.BEST_OF_3)
    }

    @Test
    fun `point progression 0-15-30-40`() {
        assertEquals("0", engine.currentState().pointDisplayA)
        engine.pointWon(Side.A)
        assertEquals("15", engine.currentState().pointDisplayA)
        engine.pointWon(Side.A)
        assertEquals("30", engine.currentState().pointDisplayA)
        engine.pointWon(Side.A)
        assertEquals("40", engine.currentState().pointDisplayA)
        assertEquals("0", engine.currentState().pointDisplayB)
    }

    @Test
    fun `game win from 40-0`() {
        repeat(4) { engine.pointWon(Side.A) }
        val s = engine.currentState()
        assertEquals(1, s.gamesA)
        assertEquals(0, s.gamesB)
        assertEquals("0", s.pointDisplayA)
        assertEquals("0", s.pointDisplayB)
    }

    @Test
    fun `deuce and advantage`() {
        // Both to 40
        repeat(3) { engine.pointWon(Side.A) }
        repeat(3) { engine.pointWon(Side.B) }
        var s = engine.currentState()
        assertTrue(s.isDeuce)
        assertEquals("40", s.pointDisplayA)
        assertEquals("40", s.pointDisplayB)

        // Advantage A
        engine.pointWon(Side.A)
        s = engine.currentState()
        assertTrue(s.advantageA)
        assertFalse(s.isDeuce)
        assertEquals("AD", s.pointDisplayA)
        assertEquals("40", s.pointDisplayB)

        // Back to deuce
        engine.pointWon(Side.B)
        s = engine.currentState()
        assertTrue(s.isDeuce)
        assertFalse(s.advantageA)

        // Advantage B then game B
        engine.pointWon(Side.B)
        assertTrue(engine.currentState().advantageB)
        engine.pointWon(Side.B)
        s = engine.currentState()
        assertEquals(0, s.gamesA)
        assertEquals(1, s.gamesB)
        assertEquals("0", s.pointDisplayA)
    }

    @Test
    fun `advantage then game win`() {
        repeat(3) { engine.pointWon(Side.A) }
        repeat(3) { engine.pointWon(Side.B) }
        engine.pointWon(Side.A) // AD A
        engine.pointWon(Side.A) // game A
        val s = engine.currentState()
        assertEquals(1, s.gamesA)
        assertEquals(0, s.gamesB)
    }

    @Test
    fun `set win at 6-4`() {
        engine.startMatch(PlayerNames("A", "B"), MatchFormat.BEST_OF_3)
        fun winGame(side: Side) = repeat(4) { engine.pointWon(side) }

        winGame(Side.A); winGame(Side.B)
        winGame(Side.A); winGame(Side.B)
        winGame(Side.A); winGame(Side.B)
        winGame(Side.A); winGame(Side.B)
        winGame(Side.A)
        winGame(Side.A) // 6-4

        val s = engine.currentState()
        assertEquals(1, s.setsA)
        assertEquals(0, s.setsB)
        assertEquals(0, s.gamesA)
        assertEquals(0, s.gamesB)
        assertEquals(1, s.setHistory.size)
        assertEquals(6, s.setHistory[0].gamesA)
        assertEquals(4, s.setHistory[0].gamesB)
    }

    @Test
    fun `match win best of 3`() {
        fun winGame(side: Side) = repeat(4) { engine.pointWon(side) }
        fun winSet(side: Side) = repeat(6) { winGame(side) }

        winSet(Side.A)
        assertEquals(1, engine.currentState().setsA)
        assertFalse(engine.currentState().isMatchOver)

        winSet(Side.A)
        val s = engine.currentState()
        assertEquals(2, s.setsA)
        assertTrue(s.isMatchOver)
        assertEquals(Side.A, s.winner)
    }

    @Test
    fun `best of 5 requires 3 sets`() {
        engine.startMatch(PlayerNames("A", "B"), MatchFormat.BEST_OF_5)
        fun winGame(side: Side) = repeat(4) { engine.pointWon(side) }
        fun winSet(side: Side) = repeat(6) { winGame(side) }

        winSet(Side.A)
        winSet(Side.A)
        assertFalse(engine.currentState().isMatchOver)
        winSet(Side.A)
        assertTrue(engine.currentState().isMatchOver)
        assertEquals(Side.A, engine.currentState().winner)
        assertEquals(3, engine.currentState().setsA)
    }

    @Test
    fun `undo restores previous point`() {
        engine.pointWon(Side.A)
        engine.pointWon(Side.A)
        assertEquals("30", engine.currentState().pointDisplayA)
        engine.undo()
        assertEquals("15", engine.currentState().pointDisplayA)
        engine.undo()
        assertEquals("0", engine.currentState().pointDisplayA)
        // Extra undo is no-op
        engine.undo()
        assertEquals("0", engine.currentState().pointDisplayA)
    }

    @Test
    fun `undo after game win`() {
        repeat(4) { engine.pointWon(Side.A) }
        assertEquals(1, engine.currentState().gamesA)
        engine.undo()
        assertEquals(0, engine.currentState().gamesA)
        assertEquals("40", engine.currentState().pointDisplayA)
    }

    @Test
    fun `server starts on A and rotates after each game`() {
        assertEquals(Side.A, engine.currentState().server)
        repeat(4) { engine.pointWon(Side.A) } // game A
        assertEquals(Side.B, engine.currentState().server)
        repeat(4) { engine.pointWon(Side.B) } // game B
        assertEquals(Side.A, engine.currentState().server)
    }

    @Test
    fun `toggle server and undo restores server`() {
        assertEquals(Side.A, engine.currentState().server)
        engine.toggleServer()
        assertEquals(Side.B, engine.currentState().server)
        engine.undo()
        assertEquals(Side.A, engine.currentState().server)
    }

    @Test
    fun `undo after game restores previous server`() {
        assertEquals(Side.A, engine.currentState().server)
        repeat(4) { engine.pointWon(Side.A) }
        assertEquals(Side.B, engine.currentState().server)
        engine.undo()
        assertEquals(Side.A, engine.currentState().server)
        assertEquals(0, engine.currentState().gamesA)
    }

    @Test
    fun `doubles mode keeps team names`() {
        engine.startMatch(
            PlayerNames("김/박", "이/최"),
            MatchFormat.BEST_OF_3,
            MatchMode.DOUBLES,
        )
        val s = engine.currentState()
        assertEquals(MatchMode.DOUBLES, s.mode)
        assertTrue(s.isDoubles)
        assertEquals("김/박", s.playerA)
        assertEquals("이/최", s.playerB)
    }

    @Test
    fun `restoreFrom survives snapshot roundtrip`() {
        engine.pointWon(Side.A)
        engine.pointWon(Side.A)
        engine.toggleServer()
        val saved = engine.currentState()
        val other = TennisScoringEngine()
        val restored = other.restoreFrom(saved)
        assertEquals(saved.pointsA, restored.pointsA)
        assertEquals(saved.server, restored.server)
        assertEquals(saved.playerA, restored.playerA)
        assertEquals(saved.matchActive, restored.matchActive)
    }

    @Test
    fun `endMatch marks over without winner if incomplete`() {
        engine.pointWon(Side.A)
        engine.endMatch()
        val s = engine.currentState()
        assertTrue(s.isMatchOver)
        assertEquals(null, s.winner)
        assertTrue(s.matchActive)
    }

    @Test
    fun `sync json roundtrip includes server and mode`() {
        engine.startMatch(
            PlayerNames("김철수", "이영희"),
            MatchFormat.BEST_OF_3,
            MatchMode.DOUBLES,
            Side.B,
        )
        engine.pointWon(Side.A)
        engine.pointWon(Side.B)
        val dto = engine.currentState().toDto()
        val json = SyncJson.encodeState(dto)
        val decoded = SyncJson.decodeState(json)
        assertEquals(dto.playerA, decoded.playerA)
        assertEquals(dto.pointsA, decoded.pointsA)
        assertEquals(dto.pointsB, decoded.pointsB)
        assertEquals(dto.pointDisplayA, decoded.pointDisplayA)
        assertEquals(Side.B.name, decoded.server)
        assertEquals(MatchMode.DOUBLES.name, decoded.mode)
        assertTrue(decoded.matchActive)

        val event = ScoringEventDto(
            type = SyncTypes.POINT,
            side = "A",
            mode = MatchMode.DOUBLES.name,
            sequence = 1,
        )
        val ej = SyncJson.encodeEvent(event)
        val ed = SyncJson.decodeEvent(ej)
        assertEquals(SyncTypes.POINT, ed.type)
        assertEquals("A", ed.side)
        assertEquals(MatchMode.DOUBLES.name, ed.mode)
    }

    @Test
    fun `history json roundtrip`() {
        val entry = MatchHistoryEntry(
            id = "abc",
            finishedAtEpochMs = 1_700_000_000_000L,
            playerA = "A",
            playerB = "B",
            bestOf = 3,
            mode = MatchMode.SINGLES.name,
            setsA = 2,
            setsB = 1,
            winner = "A",
            setHistory = listOf(SetScoreDto(6, 4), SetScoreDto(3, 6), SetScoreDto(6, 2)),
        )
        val json = SyncJson.encodeHistoryList(listOf(entry))
        val decoded = SyncJson.decodeHistoryList(json)
        assertEquals(1, decoded.size)
        assertEquals(entry.id, decoded[0].id)
        assertEquals(entry.setsA, decoded[0].setsA)
        assertEquals(3, decoded[0].setHistory.size)
        assertEquals(6, decoded[0].setHistory[0].gamesA)
    }
}
