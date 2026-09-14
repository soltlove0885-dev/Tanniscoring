package com.tanniscoring.shared

/**
 * Sport selection for multi-sport scoring (1.5.0+).
 */
enum class SportType {
    TENNIS,
    BADMINTON;

    companion object {
        fun fromName(name: String?): SportType =
            if (name.equals("BADMINTON", ignoreCase = true)) BADMINTON else TENNIS
    }
}

/**
 * Immutable badminton game snapshot (single recreational game).
 *
 * Rules (standard recreational / club):
 * - Rally point scoring
 * - First to 21, must win by 2
 * - From 20-20 continue until +2 or a side reaches 30 (cap)
 * - At 29-29, next point wins (30)
 */
data class BadmintonMatchState(
    val playerA: String,
    val playerB: String,
    val pointsA: Int,
    val pointsB: Int,
    val isMatchOver: Boolean,
    val winner: Side?,
    val matchActive: Boolean = true,
) {
    val pointDisplayA: String get() = if (isMatchOver && winner != null) pointsA.toString() else pointsA.toString()
    val pointDisplayB: String get() = if (isMatchOver && winner != null) pointsB.toString() else pointsB.toString()
}

fun BadmintonMatchState.toDto(): MatchStateDto = MatchStateDto(
    sport = SportType.BADMINTON.name,
    playerA = playerA,
    playerB = playerB,
    pointsA = pointsA,
    pointsB = pointsB,
    pointDisplayA = pointDisplayA,
    pointDisplayB = pointDisplayB,
    isMatchOver = isMatchOver,
    winner = winner?.name,
    matchActive = matchActive,
    // Tennis fields unused for badminton
    bestOf = 1,
    mode = MatchMode.SINGLES.name,
    noAd = false,
    setsA = 0,
    setsB = 0,
    gamesA = 0,
    gamesB = 0,
    isDeuce = false,
    advantageA = false,
    advantageB = false,
    isTiebreak = false,
    setHistory = emptyList(),
    server = Side.A.name,
)

fun MatchStateDto.toBadmintonMatchState(): BadmintonMatchState = BadmintonMatchState(
    playerA = playerA.ifBlank { "Player A" },
    playerB = playerB.ifBlank { "Player B" },
    pointsA = pointsA,
    pointsB = pointsB,
    isMatchOver = isMatchOver,
    winner = winner?.let { runCatching { Side.valueOf(it) }.getOrNull() },
    matchActive = matchActive,
)
