package com.tanniscoring.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServeSessionEngineTest {

    @Test
    fun firstServeMotionSetsSpeedAndFlash() {
        val eng = ServeSessionEngine()
        eng.onMatchActive()
        val s = eng.onServeMotion(2_000f)
        assertEquals(ServePhase.AFTER_FIRST, s.phase)
        assertEquals(ServeLabel.FIRST, s.label)
        assertNotNull(s.speedKmH)
        assertTrue(s.flash)
    }

    @Test
    fun noReturnAfterFirstGoesToSecond() {
        val eng = ServeSessionEngine()
        eng.onMatchActive()
        eng.onServeMotion(2_000f)
        val s = eng.onNoReturnTimeout()
        assertEquals(ServePhase.WAITING_SECOND, s.phase)
        assertEquals(ServeLabel.FAULT, s.label)
        assertFalse(s.flash)
    }

    @Test
    fun returnWithoutScoreAfterFirstIsFaultThenSecond() {
        val eng = ServeSessionEngine()
        eng.onMatchActive()
        eng.onServeMotion(1_800f)
        val s = eng.onReturnWithoutScoreChange()
        assertEquals(ServePhase.WAITING_SECOND, s.phase)
        assertEquals(ServeLabel.FAULT, s.label)
    }

    @Test
    fun secondServeMotionLabeledSecond() {
        val eng = ServeSessionEngine()
        eng.onMatchActive()
        eng.onServeMotion(2_000f)
        eng.onNoReturnTimeout()
        val s = eng.onServeMotion(1_500f)
        assertEquals(ServePhase.AFTER_SECOND, s.phase)
        assertEquals(ServeLabel.SECOND, s.label)
        assertTrue(s.flash)
    }

    @Test
    fun pointScoredClearsAndResetsToFirst() {
        val eng = ServeSessionEngine()
        eng.onMatchActive()
        eng.onServeMotion(2_000f)
        eng.onPointScored()
        val s = eng.snapshot()
        assertEquals(ServePhase.WAITING_FIRST, s.phase)
        assertEquals(ServeLabel.FIRST, s.label)
        assertNull(s.speedKmH)
        assertFalse(s.flash)
    }

    @Test
    fun estimateKmHIsClamped() {
        assertTrue(ServeSessionEngine.estimateKmH(10f, 0.045f) >= 20f)
        assertTrue(ServeSessionEngine.estimateKmH(1_000_000f, 0.045f) <= 250f)
    }

    @Test
    fun serveJsonRoundTrip() {
        val dto = ServeInfoDto(speedKmH = 142.7f, label = "2nd", flash = true, flashToken = 3, active = true)
        val json = SyncJson.encodeServe(dto)
        val back = SyncJson.decodeServe(json)
        assertEquals(142, back.speedKmH?.toInt())
        assertEquals("2nd", back.label)
        assertTrue(back.flash)
        assertEquals(3L, back.flashToken)
    }
}
