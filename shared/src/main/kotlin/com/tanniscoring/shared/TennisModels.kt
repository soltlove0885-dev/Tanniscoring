package com.tanniscoring.shared

/**
 * Point display values in standard tennis scoring.
 */
enum class PointValue(val display: String) {
    LOVE("0"),
    FIFTEEN("15"),
    THIRTY("30"),
    FORTY("40"),
    ADVANTAGE("AD");

    companion object {
        fun fromIndex(index: Int): PointValue = when (index) {
            0 -> LOVE
            1 -> FIFTEEN
            2 -> THIRTY
            else -> FORTY
        }
    }
}

/**
 * Match format: best-of-3 (default) or best-of-5.
 * setsToWin = (bestOf + 1) / 2
 */
enum class MatchFormat(val bestOf: Int) {
    BEST_OF_3(3),
    BEST_OF_5(5);

    val setsToWin: Int get() = (bestOf + 1) / 2

    companion object {
        fun fromBestOf(bestOf: Int): MatchFormat =
            if (bestOf >= 5) BEST_OF_5 else BEST_OF_3
    }
}

enum class MatchMode {
    SINGLES,
    DOUBLES;

    companion object {
        fun fromName(name: String?): MatchMode =
            if (name.equals("DOUBLES", ignoreCase = true)) DOUBLES else SINGLES
    }
}

data class PlayerNames(
    val playerA: String = "선수 A",
    val playerB: String = "선수 B",
)

data class SetScore(
    val gamesA: Int,
    val gamesB: Int,
)

enum class Side { A, B }

/**
 * Immutable snapshot of a match for UI / sync.
 */
data class MatchState(
    val playerA: String,
    val playerB: String,
    val format: MatchFormat,
    val mode: MatchMode = MatchMode.SINGLES,
    val setsA: Int,
    val setsB: Int,
    val gamesA: Int,
    val gamesB: Int,
    /** Raw point counts in the current game (or tiebreak). */
    val pointsA: Int,
    val pointsB: Int,
    val isDeuce: Boolean,
    val advantageA: Boolean,
    val advantageB: Boolean,
    val isTiebreak: Boolean,
    val isMatchOver: Boolean,
    val winner: Side?,
    val setHistory: List<SetScore>,
    /** Side currently serving. Rotates after each game; during tiebreak after odd points. */
    val server: Side = Side.A,
    /** True while a match is loaded on the phone (in progress or finished view). */
    val matchActive: Boolean = true,
) {
    val pointDisplayA: String get() = displayPoint(Side.A)
    val pointDisplayB: String get() = displayPoint(Side.B)
    val isDoubles: Boolean get() = mode == MatchMode.DOUBLES

    private fun displayPoint(side: Side): String {
        if (isMatchOver) return "-"
        if (isTiebreak) {
            return (if (side == Side.A) pointsA else pointsB).toString()
        }
        if (isDeuce) return "40"
        if (advantageA) return if (side == Side.A) "AD" else "40"
        if (advantageB) return if (side == Side.B) "AD" else "40"
        val pts = if (side == Side.A) pointsA else pointsB
        return PointValue.fromIndex(pts.coerceAtMost(3)).display
    }
}

/**
 * Wire messages between phone and Wear.
 *
 * Architecture choice: **Phone is the scoring authority.**
 * Wear sends PointWon / Undo / StartMatch events; phone applies them
 * and broadcasts FullState back via MessageClient / DataClient.
 */
object SyncPaths {
    const val PATH_EVENT = "/tanniscoring/event"
    const val PATH_STATE = "/tanniscoring/state"
}

object SyncTypes {
    const val POINT = "POINT"
    const val UNDO = "UNDO"
    const val START = "START"
    const val STATE = "STATE"
    const val TOGGLE_SERVER = "TOGGLE_SERVER"
    const val END = "END"
}

/**
 * Flat DTO for MatchState over the wire (JSON-friendly).
 */
data class MatchStateDto(
    val playerA: String = "",
    val playerB: String = "",
    val bestOf: Int = 3,
    val mode: String = MatchMode.SINGLES.name,
    val setsA: Int = 0,
    val setsB: Int = 0,
    val gamesA: Int = 0,
    val gamesB: Int = 0,
    val pointsA: Int = 0,
    val pointsB: Int = 0,
    val isDeuce: Boolean = false,
    val advantageA: Boolean = false,
    val advantageB: Boolean = false,
    val isTiebreak: Boolean = false,
    val isMatchOver: Boolean = false,
    val winner: String? = null,
    val setHistory: List<SetScoreDto> = emptyList(),
    val pointDisplayA: String = "0",
    val pointDisplayB: String = "0",
    val server: String = Side.A.name,
    val matchActive: Boolean = true,
)

data class SetScoreDto(
    val gamesA: Int = 0,
    val gamesB: Int = 0,
)

/**
 * Event payload from Wear → Phone (or local phone buttons).
 */
data class ScoringEventDto(
    val type: String,
    val side: String? = null,
    val playerA: String? = null,
    val playerB: String? = null,
    val bestOf: Int? = null,
    val mode: String? = null,
    val server: String? = null,
    val sequence: Long = 0L,
)

/**
 * Compact finished-match record for local history.
 */
data class MatchHistoryEntry(
    val id: String,
    val finishedAtEpochMs: Long,
    val playerA: String,
    val playerB: String,
    val bestOf: Int,
    val mode: String,
    val setsA: Int,
    val setsB: Int,
    val winner: String?,
    val setHistory: List<SetScoreDto>,
)

fun MatchState.toDto(): MatchStateDto = MatchStateDto(
    playerA = playerA,
    playerB = playerB,
    bestOf = format.bestOf,
    mode = mode.name,
    setsA = setsA,
    setsB = setsB,
    gamesA = gamesA,
    gamesB = gamesB,
    pointsA = pointsA,
    pointsB = pointsB,
    isDeuce = isDeuce,
    advantageA = advantageA,
    advantageB = advantageB,
    isTiebreak = isTiebreak,
    isMatchOver = isMatchOver,
    winner = winner?.name,
    setHistory = setHistory.map { SetScoreDto(it.gamesA, it.gamesB) },
    pointDisplayA = pointDisplayA,
    pointDisplayB = pointDisplayB,
    server = server.name,
    matchActive = matchActive,
)

fun MatchStateDto.toMatchState(): MatchState = MatchState(
    playerA = playerA,
    playerB = playerB,
    format = MatchFormat.fromBestOf(bestOf),
    mode = MatchMode.fromName(mode),
    setsA = setsA,
    setsB = setsB,
    gamesA = gamesA,
    gamesB = gamesB,
    pointsA = pointsA,
    pointsB = pointsB,
    isDeuce = isDeuce,
    advantageA = advantageA,
    advantageB = advantageB,
    isTiebreak = isTiebreak,
    isMatchOver = isMatchOver,
    winner = winner?.let { runCatching { Side.valueOf(it) }.getOrNull() },
    setHistory = setHistory.map { SetScore(it.gamesA, it.gamesB) },
    server = server.let { runCatching { Side.valueOf(it) }.getOrDefault(Side.A) },
    matchActive = matchActive,
)

fun MatchState.toHistoryEntry(id: String, finishedAtEpochMs: Long): MatchHistoryEntry =
    MatchHistoryEntry(
        id = id,
        finishedAtEpochMs = finishedAtEpochMs,
        playerA = playerA,
        playerB = playerB,
        bestOf = format.bestOf,
        mode = mode.name,
        setsA = setsA,
        setsB = setsB,
        winner = winner?.name,
        setHistory = setHistory.map { SetScoreDto(it.gamesA, it.gamesB) },
    )
