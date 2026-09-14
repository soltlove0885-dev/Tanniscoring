package com.tanniscoring.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanniscoring.app.data.MatchRepository
import com.tanniscoring.app.data.TournamentRepository
import com.tanniscoring.app.sync.WearSyncManager
import com.tanniscoring.shared.BadmintonMatchState
import com.tanniscoring.shared.BracketMatch
import com.tanniscoring.shared.SportType
import com.tanniscoring.shared.toBadmintonMatchState
import com.tanniscoring.app.data.LocalePreferences
import com.tanniscoring.shared.BracketMatchStatus
import com.tanniscoring.shared.MatchHistoryEntry
import com.tanniscoring.shared.MatchMode
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.Side
import com.tanniscoring.shared.SyncTypes
import com.tanniscoring.shared.Tournament
import com.tanniscoring.shared.TournamentBracket
import com.tanniscoring.shared.toMatchState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phone is the **live scoreboard** (+ tournament bracket host).
 * Wear owns scoring; this ViewModel displays [MatchState] from Wear via MessageClient
 * and optionally forwards POINT/UNDO mirror controls to Wear.
 * Tournament state lives on the phone; selecting a bracket match sends START to Wear.
 */
class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val sync = WearSyncManager.get(application.applicationContext)
    private val repo = MatchRepository(application.applicationContext)
    private val tournamentRepo = TournamentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        MatchUiState(
            history = repo.loadHistory(),
            tournament = tournamentRepo.load(),
            screen = if (LocalePreferences.hasChosenLanguage(application)) {
                PhoneScreen.SPORT_PICKER
            } else {
                PhoneScreen.LANGUAGE
            },
        ),
    )
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var sequence = 0L
    private var lastPersistedFinishedFingerprint: String? = null
    private var lastAdvancedTournamentMatchId: String? = null
    private var wasWearConnected = false
    private var lastScoreFingerprint: String? = null
    /** Debounce phone mirror taps so one press ≠ two MessageClient events. */
    private var lastMirrorEventAtMs: Long = 0L
    private var lastMirrorEventKey: String? = null

    init {
        val existingTournament = tournamentRepo.load()
        if (LocalePreferences.hasChosenLanguage(application) &&
            existingTournament != null &&
            !existingTournament.isComplete
        ) {
            _uiState.update {
                it.copy(
                    tournament = existingTournament,
                    selectedSport = SportType.TENNIS,
                    screen = PhoneScreen.TOURNAMENT_BRACKET,
                )
            }
        }
        viewModelScope.launch {
            sync.incomingState.collect { dto ->
                applyRemoteState(dto)
            }
        }
        viewModelScope.launch {
            sync.wearConnected.collect { connected ->
                _uiState.update { it.copy(wearConnected = connected) }
                if (connected && !wasWearConnected) {
                    sync.requestState()
                }
                wasWearConnected = connected
            }
        }
        viewModelScope.launch {
            sync.connectedNodeCount.collect { count ->
                _uiState.update { it.copy(wearNodeCount = count) }
            }
        }
        sync.startListening()
        viewModelScope.launch {
            sync.requestState()
            delay(1_500)
            sync.requestState()
        }
    }


    private fun scoreFingerprint(state: MatchState): String =
        "${state.setsA}-${state.setsB}|${state.gamesA}-${state.gamesB}|${state.pointsA}-${state.pointsB}|${state.isTiebreak}|${state.advantageA}|${state.advantageB}|${state.server}"

    fun pointWonA() = sendEvent(ScoringEventDto(type = SyncTypes.POINT, side = "A", sequence = nextSeq()))
    fun pointWonB() = sendEvent(ScoringEventDto(type = SyncTypes.POINT, side = "B", sequence = nextSeq()))
    fun undo() = sendEvent(ScoringEventDto(type = SyncTypes.UNDO, sequence = nextSeq()))
    fun toggleServer() = sendEvent(ScoringEventDto(type = SyncTypes.TOGGLE_SERVER, sequence = nextSeq()))
    fun endMatch() = sendEvent(ScoringEventDto(type = SyncTypes.END, sequence = nextSeq()))

    fun resetToStart() {
        val current = _uiState.value.matchState
        if (current != null && current.isMatchOver) {
            persistFinished(current)
        }
        repo.saveCurrentMatch(null)
        val tournament = _uiState.value.tournament
        val sport = _uiState.value.selectedSport
        lastScoreFingerprint = null
        _uiState.update {
            it.copy(
                matchStarted = false,
                matchState = null,
                badmintonState = null,
                canUndo = false,
                scoringFromWear = false,
                history = repo.loadHistory(),
                screen = when {
                    tournament != null -> PhoneScreen.TOURNAMENT_BRACKET
                    sport == SportType.BADMINTON -> PhoneScreen.BADMINTON_IDLE
                    else -> PhoneScreen.IDLE
                },
            )
        }
    }

    fun requestWearState() {
        viewModelScope.launch { sync.requestState() }
    }

    fun chooseLanguage(tag: String) {
        LocalePreferences.setLanguage(getApplication(), tag)
        _uiState.update { it.copy(screen = PhoneScreen.SPORT_PICKER) }
    }

    fun showLanguagePicker() {
        _uiState.update { it.copy(screen = PhoneScreen.LANGUAGE) }
    }

    fun selectSport(sport: SportType) {
        _uiState.update {
            it.copy(
                selectedSport = sport,
                screen = when (sport) {
                    SportType.TENNIS -> PhoneScreen.IDLE
                    SportType.BADMINTON -> PhoneScreen.BADMINTON_IDLE
                },
                // Clear opposite sport live state when switching
                matchState = if (sport == SportType.TENNIS) it.matchState else null,
                badmintonState = if (sport == SportType.BADMINTON) it.badmintonState else null,
                matchStarted = false,
            )
        }
    }

    fun showSportPicker() {
        _uiState.update {
            it.copy(
                screen = PhoneScreen.SPORT_PICKER,
                selectedSport = null,
                matchStarted = false,
                matchState = null,
                badmintonState = null,
                scoringFromWear = false,
            )
        }
        repo.saveCurrentMatch(null)
    }

    fun openWearApp(context: Context) {
        viewModelScope.launch { sync.openWearApp(context) }
    }

    fun openWearCompanionStore(context: Context) {
        viewModelScope.launch { sync.openWearCompanionStore(context) }
    }

    // --- Tournament ---

    fun openTournamentSetup() {
        // New tournament flow: wipe prior bracket + drafts so names never stick.
        tournamentRepo.save(null)
        lastAdvancedTournamentMatchId = null
        _uiState.update {
            it.copy(
                tournament = null,
                selectedSport = SportType.TENNIS,
                screen = PhoneScreen.TOURNAMENT_SETUP,
                draftPlayerCount = 4,
                draftBestOf = 3,
                draftNoAd = false,
                draftPlayerNames = List(8) { "" },
            )
        }
    }

    fun setDraftPlayerCount(count: Int) {
        _uiState.update { it.copy(draftPlayerCount = if (count >= 8) 8 else 4) }
    }

    fun setDraftBestOf(bestOf: Int) {
        _uiState.update { it.copy(draftBestOf = TournamentBracket.normalizeBestOf(bestOf)) }
    }

    fun setDraftNoAd(noAd: Boolean) {
        _uiState.update { it.copy(draftNoAd = noAd) }
    }

    fun setDraftPlayerName(index: Int, name: String) {
        _uiState.update { state ->
            val names = state.draftPlayerNames.toMutableList()
            if (index in names.indices) names[index] = name
            state.copy(draftPlayerNames = names)
        }
    }

    fun cancelTournamentSetup() {
        _uiState.update {
            it.copy(
                screen = PhoneScreen.IDLE,
                draftPlayerCount = 4,
                draftBestOf = 3,
                draftNoAd = false,
                draftPlayerNames = List(8) { "" },
            )
        }
    }

    fun createTournament() {
        val s = _uiState.value
        val names = s.draftPlayerNames.take(s.draftPlayerCount)
        if (names.any { it.isBlank() }) return
        val tournament = TournamentBracket.create(names, s.draftBestOf, defaultNoAd = s.draftNoAd)
        tournamentRepo.save(tournament)
        lastAdvancedTournamentMatchId = null
        _uiState.update {
            it.copy(
                tournament = tournament,
                screen = PhoneScreen.TOURNAMENT_BRACKET,
                // Clear drafts immediately so a later setup never shows old names.
                draftPlayerCount = 4,
                draftBestOf = 3,
                draftNoAd = false,
                draftPlayerNames = List(8) { "" },
            )
        }
    }

    fun showBracket() {
        _uiState.update { it.copy(screen = PhoneScreen.TOURNAMENT_BRACKET) }
    }

    fun showIdle() {
        // Leaving tournament/idle: wipe bracket + name drafts completely.
        tournamentRepo.save(null)
        lastAdvancedTournamentMatchId = null
        _uiState.update {
            it.copy(
                screen = PhoneScreen.IDLE,
                tournament = null,
                draftPlayerCount = 4,
                draftBestOf = 3,
                draftNoAd = false,
                draftPlayerNames = List(8) { "" },
            )
        }
    }

    fun openActiveScoreboard() {
        if (_uiState.value.matchState != null) {
            _uiState.update { it.copy(screen = PhoneScreen.MATCH_SCOREBOARD, matchStarted = true) }
        }
    }

    fun cycleMatchBestOf(match: BracketMatch) {
        val tournament = _uiState.value.tournament ?: return
        if (match.status == BracketMatchStatus.COMPLETED) return
        val next = when (match.bestOf) {
            1 -> 3
            3 -> 5
            else -> 1
        }
        val updated = TournamentBracket.setMatchBestOf(tournament, match.id, next)
        persistTournament(updated)
    }

    fun selectBracketMatch(match: BracketMatch) {
        val tournament = _uiState.value.tournament ?: return
        when (match.status) {
            BracketMatchStatus.READY -> startTournamentMatch(match)
            BracketMatchStatus.IN_PROGRESS -> {
                persistTournament(tournament.copy(activeMatchId = match.id))
                openActiveScoreboard()
            }
            else -> Unit
        }
    }

    fun endTournament() {
        tournamentRepo.save(null)
        lastAdvancedTournamentMatchId = null
        _uiState.update {
            it.copy(
                tournament = null,
                draftPlayerCount = 4,
                draftBestOf = 3,
                draftNoAd = false,
                draftPlayerNames = List(8) { "" },
                screen = if (it.matchStarted && it.matchState != null) {
                    PhoneScreen.MATCH_SCOREBOARD
                } else {
                    PhoneScreen.IDLE
                },
            )
        }
    }

    private fun startTournamentMatch(match: BracketMatch) {
        if (!match.canStart) return
        val tournament = _uiState.value.tournament ?: return
        val updated = TournamentBracket.markInProgress(tournament, match.id)
        persistTournament(updated)
        lastAdvancedTournamentMatchId = null
        sendEvent(
            ScoringEventDto(
                type = SyncTypes.START,
                playerA = match.playerA,
                playerB = match.playerB,
                bestOf = match.bestOf,
                mode = MatchMode.SINGLES.name,
                noAd = tournament.defaultNoAd,
                sport = SportType.TENNIS.name,
                sequence = nextSeq(),
            ),
        )
        _uiState.update { it.copy(screen = PhoneScreen.MATCH_SCOREBOARD) }
        viewModelScope.launch {
            delay(400)
            sync.openWearApp(getApplication())
        }
    }

    private fun applyRemoteState(dto: MatchStateDto) {
        val sport = SportType.fromName(dto.sport)
        if (!dto.matchActive) {
            _uiState.update {
                val screen = when {
                    it.screen == PhoneScreen.LANGUAGE -> PhoneScreen.LANGUAGE
                    it.screen == PhoneScreen.SPORT_PICKER -> PhoneScreen.SPORT_PICKER
                    it.screen == PhoneScreen.TOURNAMENT_SETUP -> PhoneScreen.TOURNAMENT_SETUP
                    it.tournament != null -> PhoneScreen.TOURNAMENT_BRACKET
                    it.selectedSport == SportType.BADMINTON ||
                        it.screen == PhoneScreen.BADMINTON_SCOREBOARD ||
                        it.screen == PhoneScreen.BADMINTON_IDLE -> PhoneScreen.BADMINTON_IDLE
                    else -> PhoneScreen.IDLE
                }
                val onScoreboard = it.screen == PhoneScreen.MATCH_SCOREBOARD ||
                    it.screen == PhoneScreen.BADMINTON_SCOREBOARD
                it.copy(
                    matchStarted = false,
                    matchState = null,
                    badmintonState = null,
                    canUndo = false,
                    scoringFromWear = false,
                    screen = if (onScoreboard) screen else it.screen,
                )
            }
            repo.saveCurrentMatch(null)
            lastScoreFingerprint = null
            return
        }
        if (sport == SportType.BADMINTON) {
            val state = dto.toBadmintonMatchState()
            lastScoreFingerprint = "BM|${state.pointsA}-${state.pointsB}|${state.isMatchOver}|${state.server}"
            _uiState.update {
                val stayPicker = it.screen == PhoneScreen.LANGUAGE ||
                    it.screen == PhoneScreen.SPORT_PICKER
                it.copy(
                    matchStarted = true,
                    selectedSport = SportType.BADMINTON,
                    badmintonState = state,
                    matchState = null,
                    canUndo = !state.isMatchOver,
                    scoringFromWear = true,
                    screen = if (stayPicker) it.screen else PhoneScreen.BADMINTON_SCOREBOARD,
                )
            }
            return
        }
        val state = dto.toMatchState()
        lastScoreFingerprint = scoreFingerprint(state)
        _uiState.update {
            val stayOnBracket = it.screen == PhoneScreen.TOURNAMENT_BRACKET ||
                it.screen == PhoneScreen.TOURNAMENT_SETUP
            val stayPicker = it.screen == PhoneScreen.LANGUAGE ||
                it.screen == PhoneScreen.SPORT_PICKER
            it.copy(
                matchStarted = true,
                selectedSport = SportType.TENNIS,
                matchState = state,
                badmintonState = null,
                canUndo = !state.isMatchOver,
                scoringFromWear = true,
                screen = when {
                    stayPicker -> it.screen
                    stayOnBracket -> it.screen
                    else -> PhoneScreen.MATCH_SCOREBOARD
                },
            )
        }
        repo.saveCurrentMatch(state)
        if (state.isMatchOver) {
            persistFinished(state)
            maybeAdvanceTournament(state)
            _uiState.update { it.copy(history = repo.loadHistory()) }
        } else {
            maybeMarkTournamentInProgress(state)
        }
    }

    private fun maybeMarkTournamentInProgress(state: MatchState) {
        val tournament = _uiState.value.tournament ?: return
        val activeId = tournament.activeMatchId ?: return
        val match = tournament.matchById(activeId) ?: return
        if (match.status != BracketMatchStatus.IN_PROGRESS) return
        if (match.playerA != state.playerA || match.playerB != state.playerB) return
        // already marked
    }

    private fun maybeAdvanceTournament(state: MatchState) {
        val tournament = _uiState.value.tournament ?: return
        val activeId = tournament.activeMatchId ?: return
        if (activeId == lastAdvancedTournamentMatchId) return
        val match = tournament.matchById(activeId) ?: return
        if (match.playerA != state.playerA || match.playerB != state.playerB) return
        val winner = state.winner ?: return
        val updated = TournamentBracket.advanceWinner(
            tournament,
            activeId,
            winner,
            setsA = state.setsA,
            setsB = state.setsB,
        )
        lastAdvancedTournamentMatchId = activeId
        persistTournament(updated)
    }

    private fun persistTournament(tournament: Tournament) {
        tournamentRepo.save(tournament)
        _uiState.update { it.copy(tournament = tournament) }
    }

    private fun sendEvent(event: ScoringEventDto) {
        // Debounce POINT/UNDO/TOGGLE rapid repeats (double-tap / multi-fire).
        if (event.type == SyncTypes.POINT ||
            event.type == SyncTypes.UNDO ||
            event.type == SyncTypes.TOGGLE_SERVER
        ) {
            val key = "${event.type}:${event.side.orEmpty()}"
            val now = System.currentTimeMillis()
            if (key == lastMirrorEventKey && now - lastMirrorEventAtMs < 400L) {
                return
            }
            lastMirrorEventKey = key
            lastMirrorEventAtMs = now
        }
        viewModelScope.launch { sync.sendEvent(event) }
    }

    private fun nextSeq(): Long {
        sequence++
        return sequence
    }

    private fun persistFinished(state: MatchState) {
        val fp = fingerprint(state)
        if (fp == lastPersistedFinishedFingerprint) return
        repo.appendFinishedMatch(state)
        lastPersistedFinishedFingerprint = fp
    }

    private fun fingerprint(state: MatchState): String =
        "${state.playerA}|${state.playerB}|${state.setsA}-${state.setsB}|${state.setHistory}|${state.winner}"

    override fun onCleared() {
        sync.stopListening()
        super.onCleared()
    }
}

enum class PhoneScreen {
    LANGUAGE,
    SPORT_PICKER,
    IDLE,
    BADMINTON_IDLE,
    BADMINTON_SCOREBOARD,
    TOURNAMENT_SETUP,
    TOURNAMENT_BRACKET,
    MATCH_SCOREBOARD,
}

data class MatchUiState(
    val matchStarted: Boolean = false,
    val matchState: MatchState? = null,
    val badmintonState: BadmintonMatchState? = null,
    val selectedSport: SportType? = null,
    val canUndo: Boolean = false,
    /** True when ≥1 Wear OS Data Layer node is connected — NOT that wear app is installed. */
    val wearConnected: Boolean = false,
    val wearNodeCount: Int = 0,
    /** True after receiving live state from Wear (scoreboard mode). */
    val scoringFromWear: Boolean = false,
    val history: List<MatchHistoryEntry> = emptyList(),
    val screen: PhoneScreen = PhoneScreen.SPORT_PICKER,
    val tournament: Tournament? = null,
    val draftPlayerCount: Int = 4,
    val draftBestOf: Int = 3,
    val draftNoAd: Boolean = false,
    val draftPlayerNames: List<String> = List(8) { "" },
)
