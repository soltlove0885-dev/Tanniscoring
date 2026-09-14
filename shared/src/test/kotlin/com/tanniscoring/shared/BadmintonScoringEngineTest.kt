package com.tanniscoring.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BadmintonScoringEngineTest {

    private lateinit var engine: BadmintonScoringEngine

    @BeforeEach
    fun setUp() {
        engine = BadmintonScoringEngine()
        engine.startMatch(PlayerNames("A", "B"))
    }

    @Test
    fun `rally points increment`() {
        engine.pointWon(Side.A)
        engine.pointWon(Side.A)
        engine.pointWon(Side.B)
        val s = engine.currentState()
        assertEquals(2, s.pointsA)
        assertEquals(1, s.pointsB)
        assertFalse(s.isMatchOver)
    }

    @Test
    fun `win at 21 clear`() {
        repeat(21) { engine.pointWon(Side.A) }
        val s = engine.currentState()
        assertTrue(s.isMatchOver)
        assertEquals(Side.A, s.winner)
        assertEquals(21, s.pointsA)
        assertEquals(0, s.pointsB)
    }

    @Test
    fun `must win by 2 at 20-20`() {
        repeat(20) { engine.pointWon(Side.A) }
        repeat(20) { engine.pointWon(Side.B) }
        assertFalse(engine.currentState().isMatchOver)

        engine.pointWon(Side.A) // 21-20
        assertFalse(engine.currentState().isMatchOver)

        engine.pointWon(Side.A) // 22-20
        val s = engine.currentState()
        assertTrue(s.isMatchOver)
        assertEquals(Side.A, s.winner)
        assertEquals(22, s.pointsA)
    }

    @Test
    fun `cap at 30 wins by 1`() {
        // Build to 29-29 without either side winning early
        repeat(29) {
            engine.pointWon(Side.A)
            engine.pointWon(Side.B)
        }
        assertFalse(engine.currentState().isMatchOver)
        assertEquals(29, engine.currentState().pointsA)
        assertEquals(29, engine.currentState().pointsB)

        engine.pointWon(Side.B) // 29-30
        val s = engine.currentState()
        assertTrue(s.isMatchOver)
        assertEquals(Side.B, s.winner)
        assertEquals(30, s.pointsB)
    }

    @Test
    fun `undo restores previous`() {
        engine.pointWon(Side.A)
        engine.pointWon(Side.B)
        engine.undo()
        assertEquals(1, engine.currentState().pointsA)
        assertEquals(0, engine.currentState().pointsB)
        assertTrue(engine.canUndo())
        engine.undo()
        assertEquals(0, engine.currentState().pointsA)
        assertFalse(engine.canUndo())
    }

    @Test
    fun `no win at 21-20`() {
        repeat(20) { engine.pointWon(Side.A) }
        repeat(20) { engine.pointWon(Side.B) }
        engine.pointWon(Side.A)
        assertEquals(21, engine.currentState().pointsA)
        assertEquals(20, engine.currentState().pointsB)
        assertFalse(engine.currentState().isMatchOver)
        assertNull(engine.currentState().winner)
    }

    @Test
    fun `clearMatch deactivates`() {
        engine.pointWon(Side.A)
        val s = engine.clearMatch()
        assertFalse(s.matchActive)
    }

    @Test
    fun `rally winner becomes server`() {
        assertEquals(Side.A, engine.currentState().server)
        engine.pointWon(Side.B)
        assertEquals(Side.B, engine.currentState().server)
        engine.pointWon(Side.A)
        assertEquals(Side.A, engine.currentState().server)
        engine.undo()
        assertEquals(Side.B, engine.currentState().server)
    }
}
