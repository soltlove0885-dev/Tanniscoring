package com.tanniscoring.shared

/**
 * Phone-side serve session labels shown on scoreboard / Wear.
 * Speeds are always estimates (camera motion MVP — not radar).
 */
enum class ServeLabel(val wire: String, val displayKo: String) {
    FIRST("1st", "1st"),
    SECOND("2nd", "2nd"),
    FAULT("fault", "폴트");

    companion object {
        fun fromWire(value: String?): ServeLabel = when (value?.lowercase()) {
            "2nd", "second" -> SECOND
            "fault" -> FAULT
            else -> FIRST
        }
    }
}

enum class ServePhase {
    /** Waiting for first ball/motion after point or match start. */
    WAITING_FIRST,
    /** 1st serve detected; watching for return / no-return / fault. */
    AFTER_FIRST,
    /** Need 2nd serve (fault or no-return after 1st). */
    WAITING_SECOND,
    /** 2nd serve detected; waiting for point or double-fault path. */
    AFTER_SECOND,
    /** Match over / inactive — ignore camera. */
    INACTIVE,
}

/**
 * Immutable snapshot of the phone serve session for UI + Wear sync.
 */
data class ServeSessionState(
    val phase: ServePhase = ServePhase.WAITING_FIRST,
    val label: ServeLabel = ServeLabel.FIRST,
    /** Estimated km/h; null until a serve motion is measured. */
    val speedKmH: Float? = null,
    /** True briefly after a new speed value so UI can pulse/gold-flash. */
    val flash: Boolean = false,
    val flashToken: Long = 0L,
    /** Calibration meters-per-pixel (or equivalent scale factor). */
    val metersPerPixel: Float = ServeCalibration.DEFAULT_METERS_PER_PIXEL,
    val calibrationPreset: String = ServeCalibration.PRESET_NET_STANDARD,
) {
    val speedDisplay: String?
        get() = speedKmH?.let { String.format("%.0f", it) }

    val wearText: String?
        get() {
            val sp = speedDisplay ?: return when (label) {
                ServeLabel.FAULT -> "폴트"
                ServeLabel.SECOND -> "2nd"
                ServeLabel.FIRST -> null
            }
            return when (label) {
                ServeLabel.FAULT -> "폴트 · 추정 ${sp}"
                ServeLabel.SECOND -> "2nd · 추정 ${sp}"
                ServeLabel.FIRST -> "1st · 추정 ${sp}"
            }
        }
}

object ServeCalibration {
    /** Rough default: phone near net / baseline looking across court. */
    const val PRESET_NET_STANDARD = "네트 근처 표준 거리"
    const val PRESET_BASELINE = "베이스라인 먼 거리"
    const val PRESET_CUSTOM = "사용자 보정"

    /** Tuned so ~typical recreational serve lands ~80–160 추정 km/h with MVP motion. */
    const val DEFAULT_METERS_PER_PIXEL = 0.045f
    const val BASELINE_METERS_PER_PIXEL = 0.075f

    fun metersPerPixelForPreset(preset: String): Float = when (preset) {
        PRESET_BASELINE -> BASELINE_METERS_PER_PIXEL
        else -> DEFAULT_METERS_PER_PIXEL
    }
}

/**
 * Wire DTO: Phone → Wear serve flash (optional small text on watch).
 */
data class ServeInfoDto(
    val speedKmH: Float? = null,
    val label: String = ServeLabel.FIRST.wire,
    val flash: Boolean = false,
    val flashToken: Long = 0L,
    val active: Boolean = true,
)

object ServeSync {
    fun toDto(state: ServeSessionState, active: Boolean = true): ServeInfoDto = ServeInfoDto(
        speedKmH = state.speedKmH,
        label = state.label.wire,
        flash = state.flash,
        flashToken = state.flashToken,
        active = active && state.phase != ServePhase.INACTIVE,
    )
}
