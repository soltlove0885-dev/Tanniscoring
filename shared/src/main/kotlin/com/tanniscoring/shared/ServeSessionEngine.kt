package com.tanniscoring.shared

/**
 * Pure serve-session state machine (phone-owned).
 *
 * User logic:
 * 1. First ball/motion → 1st serve + speed flash
 * 2. No return → need 2nd serve
 * 3. Return-like motion but score unchanged → fault → 2nd serve
 * 4. Point scored → clear flash, reset to 1st for next server
 */
class ServeSessionEngine(
    private var state: ServeSessionState = ServeSessionState(),
) {
    fun snapshot(): ServeSessionState = state

    fun setCalibration(preset: String, metersPerPixel: Float = ServeCalibration.metersPerPixelForPreset(preset)) {
        state = state.copy(
            calibrationPreset = preset,
            metersPerPixel = metersPerPixel,
        )
    }

    fun setMetersPerPixel(mpp: Float) {
        state = state.copy(
            metersPerPixel = mpp.coerceIn(0.01f, 0.25f),
            calibrationPreset = ServeCalibration.PRESET_CUSTOM,
        )
    }

    /** Match started or became active — ready for 1st serve. */
    fun onMatchActive() {
        state = ServeSessionState(
            phase = ServePhase.WAITING_FIRST,
            label = ServeLabel.FIRST,
            speedKmH = null,
            flash = false,
            flashToken = state.flashToken,
            metersPerPixel = state.metersPerPixel,
            calibrationPreset = state.calibrationPreset,
        )
    }

    fun onMatchInactive() {
        state = state.copy(
            phase = ServePhase.INACTIVE,
            speedKmH = null,
            flash = false,
            label = ServeLabel.FIRST,
        )
    }

    /**
     * Score changed (point/game/set from Wear or phone mirror).
     * Clears serve flash and resets to waiting for 1st serve.
     */
    fun onPointScored() {
        if (state.phase == ServePhase.INACTIVE) return
        state = state.copy(
            phase = ServePhase.WAITING_FIRST,
            label = ServeLabel.FIRST,
            speedKmH = null,
            flash = false,
        )
    }

    /**
     * Motion-energy peak treated as a serve candidate.
     * [pixelsPerSecond] is peak travel rate in analysis pixels/sec.
     */
    fun onServeMotion(pixelsPerSecond: Float): ServeSessionState {
        if (state.phase == ServePhase.INACTIVE) return state
        if (state.phase == ServePhase.AFTER_FIRST || state.phase == ServePhase.AFTER_SECOND) {
            // Already have a serve for this attempt — ignore extra peaks as serve.
            return state
        }
        val speed = estimateKmH(pixelsPerSecond, state.metersPerPixel)
        val token = state.flashToken + 1
        state = when (state.phase) {
            ServePhase.WAITING_FIRST -> state.copy(
                phase = ServePhase.AFTER_FIRST,
                label = ServeLabel.FIRST,
                speedKmH = speed,
                flash = true,
                flashToken = token,
            )
            ServePhase.WAITING_SECOND -> state.copy(
                phase = ServePhase.AFTER_SECOND,
                label = ServeLabel.SECOND,
                speedKmH = speed,
                flash = true,
                flashToken = token,
            )
            else -> state
        }
        return state
    }

    /**
     * Return-like motion in mid/lower frame after a serve, but score has not changed.
     * Marks fault and goes to 2nd serve (or stays fault after 2nd).
     */
    fun onReturnWithoutScoreChange(): ServeSessionState {
        return when (state.phase) {
            ServePhase.AFTER_FIRST -> {
                state = state.copy(
                    phase = ServePhase.WAITING_SECOND,
                    label = ServeLabel.FAULT,
                    flash = false,
                    // Keep last 1st-serve speed visible until 2nd is hit.
                )
                state
            }
            ServePhase.AFTER_SECOND -> {
                // Double fault path is normally a scored point; if score still
                // unchanged after return-like chaos, just clear flash and wait.
                state = state.copy(flash = false, label = ServeLabel.FAULT)
                state
            }
            else -> state
        }
    }

    /**
     * No return motion within timeout after 1st (or 2nd) serve → need next serve / fault.
     */
    fun onNoReturnTimeout(): ServeSessionState {
        return when (state.phase) {
            ServePhase.AFTER_FIRST -> {
                state = state.copy(
                    phase = ServePhase.WAITING_SECOND,
                    label = ServeLabel.FAULT,
                    flash = false,
                )
                state
            }
            ServePhase.AFTER_SECOND -> {
                state = state.copy(flash = false)
                state
            }
            else -> state
        }
    }

    /** Clear flash flag after UI animation completes (keep speed value). */
    fun clearFlash() {
        if (state.flash) {
            state = state.copy(flash = false)
        }
    }

    fun resetFlashOnly() {
        state = state.copy(flash = false, speedKmH = null)
    }

    companion object {
        /**
         * pixels/sec × m/px → m/s × 3.6 → km/h. Clamped to a sane recreational range.
         */
        fun estimateKmH(pixelsPerSecond: Float, metersPerPixel: Float): Float {
            val mps = pixelsPerSecond.coerceAtLeast(0f) * metersPerPixel
            val kmh = mps * 3.6f
            return kmh.coerceIn(20f, 250f)
        }
    }
}
