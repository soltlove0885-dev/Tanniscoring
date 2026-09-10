package com.tanniscoring.shared

/**
 * Pure Kotlin tennis scoring engine (no Android deps).
 *
 * Rules (MVP):
 * - Points: 0 → 15 → 30 → 40
 * - Deuce when both at 40; then Advantage / back to Deuce / game
 * - Game: win by 2 from deuce, or from 40 when opponent below 40
 * - Set: first to 6 games with 2-game lead; at 6-6 → tiebreak to 7 (win by 2)
 * - Match: best-of-1, best-of-3, or best-of-5 via [MatchFormat]
 * - Server: changes after each completed game
 * - Tiebreak server: first point by the player due to serve; then switch after
 *   the 1st point and every 2 points thereafter (after odd totals: 1, 3, 5, …).
 *   After the tiebreak, the player who received the first TB point serves next.
 *
 * Undo restores the previous snapshot (stack-based).
 */
class TennisScoringEngine {

    private var playerA: String = "선수 A"
    private var playerB: String = "선수 B"
    private var format: MatchFormat = MatchFormat.BEST_OF_3
    private var mode: MatchMode = MatchMode.SINGLES

    private var setsA: Int = 0
    private var setsB: Int = 0
    private var gamesA: Int = 0
    private var gamesB: Int = 0
    private var pointsA: Int = 0
    private var pointsB: Int = 0
    private var setHistory: MutableList<SetScore> = mutableListOf()
    private var inTiebreak: Boolean = false
    private var matchOver: Boolean = false
    private var winner: Side? = null
    private var server: Side = Side.A
    /** Side that served (or will serve) the first point of the current tiebreak. */
    private var tiebreakInitialServer: Side = Side.A
    private var matchActive: Boolean = false

    private val history: ArrayDeque<Snapshot> = ArrayDeque()

    fun startMatch(
        names: PlayerNames = PlayerNames(),
        format: MatchFormat = MatchFormat.BEST_OF_3,
        mode: MatchMode = MatchMode.SINGLES,
        initialServer: Side = Side.A,
    ): MatchState {
        this.playerA = names.playerA.ifBlank { if (mode == MatchMode.DOUBLES) "팀 A" else "선수 A" }
        this.playerB = names.playerB.ifBlank { if (mode == MatchMode.DOUBLES) "팀 B" else "선수 B" }
        this.format = format
        this.mode = mode
        setsA = 0
        setsB = 0
        gamesA = 0
        gamesB = 0
        pointsA = 0
        pointsB = 0
        setHistory = mutableListOf()
        inTiebreak = false
        matchOver = false
        winner = null
        server = initialServer
        tiebreakInitialServer = initialServer
        matchActive = true
        history.clear()
        return snapshot()
    }

    fun pointWon(side: Side): MatchState {
        if (!matchActive || matchOver) return snapshot()
        pushHistory()
        if (inTiebreak) {
            applyTiebreakPoint(side)
        } else {
            applyGamePoint(side)
        }
        return snapshot()
    }

    fun undo(): MatchState {
        if (history.isEmpty()) return snapshot()
        val prev = history.removeLast()
        restore(prev)
        return snapshot()
    }

    fun toggleServer(): MatchState {
        if (!matchActive || matchOver) return snapshot()
        pushHistory()
        server = if (server == Side.A) Side.B else Side.A
        // If toggling before any TB points, treat as changing who opens the TB.
        if (inTiebreak && pointsA == 0 && pointsB == 0) {
            tiebreakInitialServer = server
        }
        return snapshot()
    }

    /** Manually end the match without declaring a winner (unless already decided). */
    fun endMatch(): MatchState {
        if (!matchActive) return snapshot()
        if (!matchOver) {
            pushHistory()
            matchOver = true
        }
        return snapshot()
    }

    fun clearMatch(): MatchState {
        matchActive = false
        matchOver = false
        winner = null
        history.clear()
        return snapshot().copy(matchActive = false)
    }

    /**
     * Restore from a persisted [MatchState]. Undo stack is cleared.
     */
    fun restoreFrom(state: MatchState): MatchState {
        playerA = state.playerA
        playerB = state.playerB
        format = state.format
        mode = state.mode
        setsA = state.setsA
        setsB = state.setsB
        gamesA = state.gamesA
        gamesB = state.gamesB
        pointsA = state.pointsA
        pointsB = state.pointsB
        setHistory = state.setHistory.toMutableList()
        inTiebreak = state.isTiebreak
        matchOver = state.isMatchOver
        winner = state.winner
        server = state.server
        matchActive = state.matchActive
        tiebreakInitialServer = if (state.isTiebreak) {
            inferTiebreakInitialServer(state.pointsA + state.pointsB, state.server)
        } else {
            state.server
        }
        history.clear()
        return snapshot()
    }

    fun currentState(): MatchState = snapshot()

    fun canUndo(): Boolean = history.isNotEmpty()

    private fun applyGamePoint(side: Side) {
        if (side == Side.A) pointsA++ else pointsB++

        val a = pointsA
        val b = pointsB

        if (a >= 3 && b >= 3) {
            when {
                a >= b + 2 -> winGame(Side.A)
                b >= a + 2 -> winGame(Side.B)
            }
            return
        }

        if (a >= 4 && a > b) {
            winGame(Side.A)
            return
        }
        if (b >= 4 && b > a) {
            winGame(Side.B)
        }
    }

    private fun applyTiebreakPoint(side: Side) {
        if (side == Side.A) pointsA++ else pointsB++
        val a = pointsA
        val b = pointsB
        val total = a + b
        if ((a >= 7 || b >= 7) && kotlin.math.abs(a - b) >= 2) {
            if (a > b) winGame(Side.A) else winGame(Side.B)
        } else if (total % 2 == 1) {
            // After 1st, 3rd, 5th… point: switch server for the next point(s).
            rotateServer()
        }
    }

    private fun winGame(side: Side) {
        if (side == Side.A) gamesA++ else gamesB++
        pointsA = 0
        pointsB = 0

        val wasTiebreak = inTiebreak
        inTiebreak = false

        if (wasTiebreak) {
            // Receiver of the first TB point serves the next game/set.
            server = opposite(tiebreakInitialServer)
        } else {
            rotateServer()
        }

        val ga = gamesA
        val gb = gamesB

        when {
            // Just finished tiebreak (7-6)
            wasTiebreak && ga == 7 && gb == 6 -> winSet(Side.A)
            wasTiebreak && gb == 7 && ga == 6 -> winSet(Side.B)
            // Reach 6-6 → next points are tiebreak; current server (post-game rotate) opens TB
            ga == 6 && gb == 6 -> {
                inTiebreak = true
                tiebreakInitialServer = server
            }
            // Normal set win
            ga >= 6 && ga - gb >= 2 -> winSet(Side.A)
            gb >= 6 && gb - ga >= 2 -> winSet(Side.B)
        }
    }

    private fun rotateServer() {
        server = opposite(server)
    }

    private fun opposite(side: Side): Side = if (side == Side.A) Side.B else Side.A

    /**
     * After [pointsPlayed] TB points with [currentServer] about to serve (or last set),
     * recover who served point 1. Switches occur after odd totals → switch count = (n+1)/2.
     */
    private fun inferTiebreakInitialServer(pointsPlayed: Int, currentServer: Side): Side {
        val switches = (pointsPlayed + 1) / 2
        return if (switches % 2 == 0) currentServer else opposite(currentServer)
    }

    private fun winSet(side: Side) {
        setHistory.add(SetScore(gamesA, gamesB))
        if (side == Side.A) setsA++ else setsB++
        gamesA = 0
        gamesB = 0
        pointsA = 0
        pointsB = 0
        inTiebreak = false

        if (setsA >= format.setsToWin) {
            matchOver = true
            winner = Side.A
        } else if (setsB >= format.setsToWin) {
            matchOver = true
            winner = Side.B
        }
    }

    private data class Snapshot(
        val playerA: String,
        val playerB: String,
        val format: MatchFormat,
        val mode: MatchMode,
        val setsA: Int,
        val setsB: Int,
        val gamesA: Int,
        val gamesB: Int,
        val pointsA: Int,
        val pointsB: Int,
        val setHistory: List<SetScore>,
        val inTiebreak: Boolean,
        val matchOver: Boolean,
        val winner: Side?,
        val server: Side,
        val tiebreakInitialServer: Side,
        val matchActive: Boolean,
    )

    private fun pushHistory() {
        history.addLast(
            Snapshot(
                playerA, playerB, format, mode,
                setsA, setsB, gamesA, gamesB, pointsA, pointsB,
                setHistory.toList(), inTiebreak, matchOver, winner, server,
                tiebreakInitialServer, matchActive,
            )
        )
    }

    private fun restore(s: Snapshot) {
        playerA = s.playerA
        playerB = s.playerB
        format = s.format
        mode = s.mode
        setsA = s.setsA
        setsB = s.setsB
        gamesA = s.gamesA
        gamesB = s.gamesB
        pointsA = s.pointsA
        pointsB = s.pointsB
        setHistory = s.setHistory.toMutableList()
        inTiebreak = s.inTiebreak
        matchOver = s.matchOver
        winner = s.winner
        server = s.server
        tiebreakInitialServer = s.tiebreakInitialServer
        matchActive = s.matchActive
    }

    private fun snapshot(): MatchState {
        val deuce = !inTiebreak && pointsA >= 3 && pointsB >= 3 && pointsA == pointsB
        val advA = !inTiebreak && pointsA >= 3 && pointsB >= 3 && pointsA == pointsB + 1
        val advB = !inTiebreak && pointsA >= 3 && pointsB >= 3 && pointsB == pointsA + 1
        return MatchState(
            playerA = playerA,
            playerB = playerB,
            format = format,
            mode = mode,
            setsA = setsA,
            setsB = setsB,
            gamesA = gamesA,
            gamesB = gamesB,
            pointsA = pointsA,
            pointsB = pointsB,
            isDeuce = deuce,
            advantageA = advA,
            advantageB = advB,
            isTiebreak = inTiebreak,
            isMatchOver = matchOver,
            winner = winner,
            setHistory = setHistory.toList(),
            server = server,
            matchActive = matchActive,
        )
    }
}
