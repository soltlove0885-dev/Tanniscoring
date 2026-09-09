package com.tanniscoring.shared

/**
 * Pure Kotlin tennis scoring engine (no Android deps).
 *
 * Rules (MVP):
 * - Points: 0 → 15 → 30 → 40
 * - Deuce when both at 40; then Advantage / back to Deuce / game
 * - Game: win by 2 from deuce, or from 40 when opponent below 40
 * - Set: first to 6 games with 2-game lead; at 6-6 → tiebreak to 7 (win by 2)
 * - Match: best-of-3 or best-of-5 via [MatchFormat]
 *
 * Undo restores the previous snapshot (stack-based).
 */
class TennisScoringEngine {

    private var playerA: String = "선수 A"
    private var playerB: String = "선수 B"
    private var format: MatchFormat = MatchFormat.BEST_OF_3

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

    private val history: ArrayDeque<Snapshot> = ArrayDeque()

    fun startMatch(
        names: PlayerNames = PlayerNames(),
        format: MatchFormat = MatchFormat.BEST_OF_3,
    ): MatchState {
        this.playerA = names.playerA.ifBlank { "선수 A" }
        this.playerB = names.playerB.ifBlank { "선수 B" }
        this.format = format
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
        history.clear()
        return snapshot()
    }

    fun pointWon(side: Side): MatchState {
        if (matchOver) return snapshot()
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
        if ((a >= 7 || b >= 7) && kotlin.math.abs(a - b) >= 2) {
            if (a > b) winGame(Side.A) else winGame(Side.B)
        }
    }

    private fun winGame(side: Side) {
        if (side == Side.A) gamesA++ else gamesB++
        pointsA = 0
        pointsB = 0

        val wasTiebreak = inTiebreak
        inTiebreak = false

        val ga = gamesA
        val gb = gamesB

        when {
            // Just finished tiebreak (7-6)
            wasTiebreak && ga == 7 && gb == 6 -> winSet(Side.A)
            wasTiebreak && gb == 7 && ga == 6 -> winSet(Side.B)
            // Reach 6-6 → next points are tiebreak
            ga == 6 && gb == 6 -> inTiebreak = true
            // Normal set win
            ga >= 6 && ga - gb >= 2 -> winSet(Side.A)
            gb >= 6 && gb - ga >= 2 -> winSet(Side.B)
        }
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
    )

    private fun pushHistory() {
        history.addLast(
            Snapshot(
                playerA, playerB, format,
                setsA, setsB, gamesA, gamesB, pointsA, pointsB,
                setHistory.toList(), inTiebreak, matchOver, winner,
            )
        )
    }

    private fun restore(s: Snapshot) {
        playerA = s.playerA
        playerB = s.playerB
        format = s.format
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
    }

    private fun snapshot(): MatchState {
        val deuce = !inTiebreak && pointsA >= 3 && pointsB >= 3 && pointsA == pointsB
        val advA = !inTiebreak && pointsA >= 3 && pointsB >= 3 && pointsA == pointsB + 1
        val advB = !inTiebreak && pointsA >= 3 && pointsB >= 3 && pointsB == pointsA + 1
        return MatchState(
            playerA = playerA,
            playerB = playerB,
            format = format,
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
        )
    }
}
