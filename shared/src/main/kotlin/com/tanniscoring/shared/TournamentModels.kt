package com.tanniscoring.shared

import java.util.UUID

enum class TournamentRound {
    QUARTERFINAL,
    SEMIFINAL,
    FINAL;

    val displayKo: String
        get() = when (this) {
            QUARTERFINAL -> "8강"
            SEMIFINAL -> "준결승"
            FINAL -> "결승"
        }
}

enum class BracketMatchStatus {
    PENDING,
    READY,
    IN_PROGRESS,
    COMPLETED,
}

/**
 * One slot in a single-elimination bracket.
 * [nextMatchId] / [nextSlotIsA] describe where the winner advances.
 */
data class BracketMatch(
    val id: String,
    val round: TournamentRound,
    val slotIndex: Int,
    val playerA: String?,
    val playerB: String?,
    val bestOf: Int,
    val status: BracketMatchStatus,
    val winnerSide: Side? = null,
    val setsA: Int = 0,
    val setsB: Int = 0,
    val nextMatchId: String? = null,
    val nextSlotIsA: Boolean = true,
) {
    val winnerName: String?
        get() = when (winnerSide) {
            Side.A -> playerA
            Side.B -> playerB
            null -> null
        }

    val canStart: Boolean
        get() = status == BracketMatchStatus.READY &&
            !playerA.isNullOrBlank() &&
            !playerB.isNullOrBlank()

    val label: String
        get() {
            val a = playerA?.ifBlank { null } ?: "TBD"
            val b = playerB?.ifBlank { null } ?: "TBD"
            return "$a vs $b"
        }
}

data class Tournament(
    val id: String,
    val playerCount: Int,
    val defaultBestOf: Int,
    val players: List<String>,
    val matches: List<BracketMatch>,
    val activeMatchId: String? = null,
    val champion: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
) {
    val isComplete: Boolean get() = champion != null

    fun matchById(id: String): BracketMatch? = matches.find { it.id == id }

    fun activeMatch(): BracketMatch? = activeMatchId?.let { matchById(it) }

    fun matchesInRound(round: TournamentRound): List<BracketMatch> =
        matches.filter { it.round == round }.sortedBy { it.slotIndex }
}

object TournamentBracket {

    fun create(
        playerNames: List<String>,
        defaultBestOf: Int = 3,
        id: String = UUID.randomUUID().toString(),
        createdAtEpochMs: Long = System.currentTimeMillis(),
    ): Tournament {
        val cleaned = playerNames.map { it.trim() }.filter { it.isNotEmpty() }
        require(cleaned.size == 4 || cleaned.size == 8) {
            "Tournament requires 4 or 8 players, got ${cleaned.size}"
        }
        val bestOf = normalizeBestOf(defaultBestOf)
        val matches = if (cleaned.size == 4) buildFour(cleaned, bestOf) else buildEight(cleaned, bestOf)
        return Tournament(
            id = id,
            playerCount = cleaned.size,
            defaultBestOf = bestOf,
            players = cleaned,
            matches = matches,
            createdAtEpochMs = createdAtEpochMs,
        )
    }

    fun setMatchBestOf(tournament: Tournament, matchId: String, bestOf: Int): Tournament {
        val normalized = normalizeBestOf(bestOf)
        val updated = tournament.matches.map { m ->
            if (m.id == matchId && m.status != BracketMatchStatus.COMPLETED) {
                m.copy(bestOf = normalized)
            } else {
                m
            }
        }
        return tournament.copy(matches = updated)
    }

    fun markInProgress(tournament: Tournament, matchId: String): Tournament {
        val match = tournament.matchById(matchId) ?: return tournament
        if (!match.canStart && match.status != BracketMatchStatus.IN_PROGRESS) return tournament
        val updated = tournament.matches.map { m ->
            when {
                m.id == matchId -> m.copy(status = BracketMatchStatus.IN_PROGRESS)
                m.status == BracketMatchStatus.IN_PROGRESS -> m.copy(status = BracketMatchStatus.READY)
                else -> m
            }
        }
        return tournament.copy(matches = updated, activeMatchId = matchId)
    }

    fun clearActiveMatch(tournament: Tournament): Tournament =
        tournament.copy(activeMatchId = null)

    /**
     * Record the winner of [matchId] and advance them into the next bracket slot.
     */
    fun advanceWinner(
        tournament: Tournament,
        matchId: String,
        winner: Side,
        setsA: Int = 0,
        setsB: Int = 0,
    ): Tournament {
        val match = tournament.matchById(matchId) ?: return tournament
        if (match.status == BracketMatchStatus.COMPLETED) return tournament
        val winnerName = when (winner) {
            Side.A -> match.playerA
            Side.B -> match.playerB
        } ?: return tournament

        var matches = tournament.matches.map { m ->
            if (m.id == matchId) {
                m.copy(
                    status = BracketMatchStatus.COMPLETED,
                    winnerSide = winner,
                    setsA = setsA,
                    setsB = setsB,
                )
            } else {
                m
            }
        }

        var champion: String? = tournament.champion
        val nextId = match.nextMatchId
        if (nextId == null) {
            champion = winnerName
        } else {
            matches = matches.map { m ->
                if (m.id != nextId) return@map m
                val withPlayer = if (match.nextSlotIsA) {
                    m.copy(playerA = winnerName)
                } else {
                    m.copy(playerB = winnerName)
                }
                val bothReady = !withPlayer.playerA.isNullOrBlank() && !withPlayer.playerB.isNullOrBlank()
                when {
                    withPlayer.status == BracketMatchStatus.COMPLETED -> withPlayer
                    bothReady && withPlayer.status == BracketMatchStatus.PENDING ->
                        withPlayer.copy(status = BracketMatchStatus.READY)
                    bothReady -> withPlayer.copy(status = BracketMatchStatus.READY)
                    else -> withPlayer
                }
            }
        }

        val clearActive = tournament.activeMatchId == matchId
        return tournament.copy(
            matches = matches,
            activeMatchId = if (clearActive) null else tournament.activeMatchId,
            champion = champion,
        )
    }

    fun normalizeBestOf(bestOf: Int): Int = when {
        bestOf <= 1 -> 1
        bestOf >= 5 -> 5
        else -> 3
    }

    private fun buildFour(players: List<String>, bestOf: Int): List<BracketMatch> {
        val finalId = "final-0"
        val sf0 = BracketMatch(
            id = "sf-0",
            round = TournamentRound.SEMIFINAL,
            slotIndex = 0,
            playerA = players[0],
            playerB = players[1],
            bestOf = bestOf,
            status = BracketMatchStatus.READY,
            nextMatchId = finalId,
            nextSlotIsA = true,
        )
        val sf1 = BracketMatch(
            id = "sf-1",
            round = TournamentRound.SEMIFINAL,
            slotIndex = 1,
            playerA = players[2],
            playerB = players[3],
            bestOf = bestOf,
            status = BracketMatchStatus.READY,
            nextMatchId = finalId,
            nextSlotIsA = false,
        )
        val final = BracketMatch(
            id = finalId,
            round = TournamentRound.FINAL,
            slotIndex = 0,
            playerA = null,
            playerB = null,
            bestOf = bestOf,
            status = BracketMatchStatus.PENDING,
            nextMatchId = null,
            nextSlotIsA = true,
        )
        return listOf(sf0, sf1, final)
    }

    private fun buildEight(players: List<String>, bestOf: Int): List<BracketMatch> {
        val sf0 = "sf-0"
        val sf1 = "sf-1"
        val finalId = "final-0"
        val quarters = (0 until 4).map { i ->
            val a = players[i * 2]
            val b = players[i * 2 + 1]
            val goesToSf0 = i < 2
            BracketMatch(
                id = "qf-$i",
                round = TournamentRound.QUARTERFINAL,
                slotIndex = i,
                playerA = a,
                playerB = b,
                bestOf = bestOf,
                status = BracketMatchStatus.READY,
                nextMatchId = if (goesToSf0) sf0 else sf1,
                nextSlotIsA = i % 2 == 0,
            )
        }
        val semis = listOf(
            BracketMatch(
                id = sf0,
                round = TournamentRound.SEMIFINAL,
                slotIndex = 0,
                playerA = null,
                playerB = null,
                bestOf = bestOf,
                status = BracketMatchStatus.PENDING,
                nextMatchId = finalId,
                nextSlotIsA = true,
            ),
            BracketMatch(
                id = sf1,
                round = TournamentRound.SEMIFINAL,
                slotIndex = 1,
                playerA = null,
                playerB = null,
                bestOf = bestOf,
                status = BracketMatchStatus.PENDING,
                nextMatchId = finalId,
                nextSlotIsA = false,
            ),
        )
        val final = BracketMatch(
            id = finalId,
            round = TournamentRound.FINAL,
            slotIndex = 0,
            playerA = null,
            playerB = null,
            bestOf = bestOf,
            status = BracketMatchStatus.PENDING,
            nextMatchId = null,
            nextSlotIsA = true,
        )
        return quarters + semis + final
    }
}
