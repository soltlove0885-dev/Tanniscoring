package com.tanniscoring.shared

/**
 * Pure Kotlin badminton rally-point engine (no Android deps).
 *
 * Recreational / club rules:
 * - First to [POINTS_TO_WIN] (21), must win by 2
 * - Continues past 20-20 until one leads by 2 OR reaches [POINT_CAP] (30)
 * - At 29-all, next point wins
 * - Rally winner becomes the server
 */
class BadmintonScoringEngine {

    companion object {
        const val POINTS_TO_WIN = 21
        const val POINT_CAP = 30
        const val WIN_BY = 2
    }

    private var playerA: String = "Player A"
    private var playerB: String = "Player B"
    private var pointsA: Int = 0
    private var pointsB: Int = 0
    private var matchOver: Boolean = false
    private var winner: Side? = null
    private var matchActive: Boolean = false
    private var server: Side = Side.A

    private val history: ArrayDeque<Snapshot> = ArrayDeque()

    fun startMatch(
        names: PlayerNames = PlayerNames("Player A", "Player B"),
        initialServer: Side = Side.A,
    ): BadmintonMatchState {
        playerA = names.playerA.ifBlank { "Player A" }
        playerB = names.playerB.ifBlank { "Player B" }
        pointsA = 0
        pointsB = 0
        matchOver = false
        winner = null
        matchActive = true
        server = initialServer
        history.clear()
        return snapshot()
    }

    fun pointWon(side: Side): BadmintonMatchState {
        if (!matchActive || matchOver) return snapshot()
        pushHistory()
        if (side == Side.A) pointsA++ else pointsB++
        server = side // rally winner serves next
        checkGameOver()
        return snapshot()
    }

    fun undo(): BadmintonMatchState {
        if (history.isEmpty()) return snapshot()
        val prev = history.removeLast()
        restore(prev)
        return snapshot()
    }

    fun endMatch(): BadmintonMatchState {
        if (!matchActive) return snapshot()
        if (!matchOver) {
            pushHistory()
            matchOver = true
        }
        return snapshot()
    }

    fun clearMatch(): BadmintonMatchState {
        matchActive = false
        matchOver = false
        winner = null
        history.clear()
        return snapshot().copy(matchActive = false)
    }

    fun restoreFrom(state: BadmintonMatchState): BadmintonMatchState {
        playerA = state.playerA
        playerB = state.playerB
        pointsA = state.pointsA
        pointsB = state.pointsB
        matchOver = state.isMatchOver
        winner = state.winner
        matchActive = state.matchActive
        server = state.server
        history.clear()
        return snapshot()
    }

    fun currentState(): BadmintonMatchState = snapshot()

    fun canUndo(): Boolean = history.isNotEmpty()

    private fun checkGameOver() {
        val a = pointsA
        val b = pointsB
        when {
            a >= POINT_CAP -> {
                matchOver = true
                winner = Side.A
            }
            b >= POINT_CAP -> {
                matchOver = true
                winner = Side.B
            }
            a >= POINTS_TO_WIN && a - b >= WIN_BY -> {
                matchOver = true
                winner = Side.A
            }
            b >= POINTS_TO_WIN && b - a >= WIN_BY -> {
                matchOver = true
                winner = Side.B
            }
        }
    }

    private data class Snapshot(
        val playerA: String,
        val playerB: String,
        val pointsA: Int,
        val pointsB: Int,
        val matchOver: Boolean,
        val winner: Side?,
        val matchActive: Boolean,
        val server: Side,
    )

    private fun pushHistory() {
        history.addLast(
            Snapshot(playerA, playerB, pointsA, pointsB, matchOver, winner, matchActive, server),
        )
    }

    private fun restore(s: Snapshot) {
        playerA = s.playerA
        playerB = s.playerB
        pointsA = s.pointsA
        pointsB = s.pointsB
        matchOver = s.matchOver
        winner = s.winner
        matchActive = s.matchActive
        server = s.server
    }

    private fun snapshot(): BadmintonMatchState = BadmintonMatchState(
        playerA = playerA,
        playerB = playerB,
        pointsA = pointsA,
        pointsB = pointsB,
        isMatchOver = matchOver,
        winner = winner,
        matchActive = matchActive,
        server = server,
    )
}
