package com.tanniscoring.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanniscoring.app.data.LocalePreferences
import com.tanniscoring.app.data.MatchRepository
import com.tanniscoring.app.data.TournamentRepository
import com.tanniscoring.app.sync.WearSyncManager
import com.tanniscoring.shared.BadmintonMatchState
import com.tanniscoring.shared.BadmintonScoringEngine
import com.tanniscoring.shared.BracketMatch
import com.tanniscoring.shared.BracketMatchStatus
import com.tanniscoring.shared.MatchFormat
import com.tanniscoring.shared.MatchHistoryEntry
import com.tanniscoring.shared.MatchMode
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.PlayerNames
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.Side
import com.tanniscoring.shared.SportType
import com.tanniscoring.shared.SyncTypes
import com.tanniscoring.shared.TennisScoringEngine
import com.tanniscoring.shared.Tournament
import com.tanniscoring.shared.TournamentBracket
import com.tanniscoring.shared.toBadmintonMatchState
import com.tanniscoring.shared.toMatchState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phone can **start and score** tennis/badminton alone (local engines).
 * Wear remains an optional companion: when connected, START/POINT/UNDO sync both ways.
 * Matches started on Wear still drive the phone as a live scoreboard (+ mirror controls).
 */
class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val sync = WearSyncManager.get(application.applicationContext)
    private val repo = MatchRepository(application.applicationContext)
    private val tournamentRepo = TournamentRepository(application.applicationContext)
    private val tennisEngine = TennisScoringEngine()
    private val badmintonEngine = BadmintonScoringEngine()

    /** True when this phone started the active match (local engine is authority). */
    private var phoneAuthority = false

    private val _uiState = MutableStateFlow(
        MatchUiState(
            history = repo.loadHistory(),
            tournament = tournamentRepo.load(),
            draftPlayerA = application.getString(R.string.player_a),
            draftPlayerB = application.getString(R.string.player_b),
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
    private var lastLocalPointAtMs: Long = 0L
    private var lastLocalPointSide: Side? = null

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
                    if (phoneAuthority) {
                        // Only re-START companion when the phone match is still at 0–0
                        // (mid-match START would reset Wear and could echo-wipe phone).
                        resyncPhoneMatchToWearIfPristine()
                    } else {
                        sync.requestState()
                    }
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

    fun pointWonA() = onPoint(Side.A)
    fun pointWonB() = onPoint(Side.B)

    fun undo() {
        if (phoneAuthority) {
            applyLocalUndo()
            sendEvent(ScoringEventDto(type = SyncTypes.UNDO, sequence = nextSeq()))
        } else {
            sendEvent(ScoringEventDto(type = SyncTypes.UNDO, sequence = nextSeq()))
        }
    }

    fun toggleServer() {
        if (phoneAuthority) {
            if (_uiState.value.selectedSport == SportType.BADMINTON) return
            publishTennis(tennisEngine.toggleServer())
            sendEvent(ScoringEventDto(type = SyncTypes.TOGGLE_SERVER, sequence = nextSeq()))
        } else {
            sendEvent(ScoringEventDto(type = SyncTypes.TOGGLE_SERVER, sequence = nextSeq()))
        }
    }

    fun endMatch() {
        if (phoneAuthority) {
            when (_uiState.value.selectedSport) {
                SportType.BADMINTON -> publishBadminton(badmintonEngine.endMatch())
                else -> {
                    val state = tennisEngine.endMatch()
                    publishTennis(state)
                    if (state.isMatchOver) {
                        persistFinished(state)
                        maybeAdvanceTournament(state)
                        _uiState.update { it.copy(history = repo.loadHistory()) }
                    }
                }
            }
            sendEvent(ScoringEventDto(type = SyncTypes.END, sequence = nextSeq()))
            // Stay on scoreboard until New match; clear authority after reset.
        } else {
            sendEvent(ScoringEventDto(type = SyncTypes.END, sequence = nextSeq()))
        }
    }

    fun resetToStart() {
        val current = _uiState.value.matchState
        if (current != null && current.isMatchOver) {
            persistFinished(current)
        }
        if (phoneAuthority) {
            sendEvent(ScoringEventDto(type = SyncTypes.END, sequence = nextSeq()))
        }
        clearLocalEngines()
        phoneAuthority = false
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
        _uiState.update {
            it.copy(
                screen = PhoneScreen.SPORT_PICKER,
                draftPlayerA = getApplication<Application>().getString(R.string.player_a),
                draftPlayerB = getApplication<Application>().getString(R.string.player_b),
            )
        }
    }

    fun showLanguagePicker() {
        _uiState.update { it.copy(screen = PhoneScreen.LANGUAGE) }
    }

    fun showSettings() {
        _uiState.update {
            it.copy(
                settingsReturnScreen = when (it.screen) {
                    PhoneScreen.SETTINGS, PhoneScreen.TERMS -> it.settingsReturnScreen
                    else -> it.screen
                },
                screen = PhoneScreen.SETTINGS,
            )
        }
    }

    fun showTermsOfUse() {
        _uiState.update { it.copy(screen = PhoneScreen.TERMS) }
    }

    fun closeTermsOfUse() {
        _uiState.update { it.copy(screen = PhoneScreen.SETTINGS) }
    }

    fun closeSettings() {
        _uiState.update {
            it.copy(
                screen = it.settingsReturnScreen ?: PhoneScreen.SPORT_PICKER,
                settingsReturnScreen = null,
            )
        }
    }

    fun selectSport(sport: SportType) {
        _uiState.update {
            it.copy(
                selectedSport = sport,
                screen = when (sport) {
                    SportType.TENNIS -> PhoneScreen.IDLE
                    SportType.BADMINTON -> PhoneScreen.BADMINTON_IDLE
                },
                matchState = if (sport == SportType.TENNIS) it.matchState else null,
                badmintonState = if (sport == SportType.BADMINTON) it.badmintonState else null,
                matchStarted = false,
            )
        }
    }

    fun showSportPicker() {
        clearLocalEngines()
        phoneAuthority = false
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

    // --- Phone match setup / start ---

    fun openStartMatchSetup() {
        val app = getApplication<Application>()
        _uiState.update {
            val bestOf = if (it.draftBestOf == 3 || it.draftBestOf == 5) it.draftBestOf else 3
            it.copy(
                selectedSport = SportType.TENNIS,
                screen = PhoneScreen.START_MATCH,
                draftBestOf = bestOf,
                draftPlayerA = it.draftPlayerA.ifBlank { app.getString(R.string.player_a) },
                draftPlayerB = it.draftPlayerB.ifBlank { app.getString(R.string.player_b) },
            )
        }
    }

    fun cancelStartMatchSetup() {
        _uiState.update { it.copy(screen = PhoneScreen.IDLE) }
    }

    fun setDraftPlayerA(name: String) = _uiState.update { it.copy(draftPlayerA = name) }
    fun setDraftPlayerB(name: String) = _uiState.update { it.copy(draftPlayerB = name) }
    fun setDraftDoubles(doubles: Boolean) = _uiState.update { it.copy(draftDoubles = doubles) }
    fun setMatchDraftBestOf(bestOf: Int) {
        _uiState.update { it.copy(draftBestOf = TournamentBracket.normalizeBestOf(bestOf).coerceAtLeast(3)) }
    }

    /** Start tennis on the phone (Wear optional companion). */
    fun startPhoneTennisMatch() {
        val s = _uiState.value
        val mode = if (s.draftDoubles) MatchMode.DOUBLES else MatchMode.SINGLES
        val names = PlayerNames(
            s.draftPlayerA.ifBlank { getApplication<Application>().getString(R.string.player_a) },
            s.draftPlayerB.ifBlank { getApplication<Application>().getString(R.string.player_b) },
        )
        phoneAuthority = true
        badmintonEngine.clearMatch()
        val state = tennisEngine.startMatch(
            names,
            MatchFormat.fromBestOf(s.draftBestOf),
            mode,
            noAd = s.draftNoAd,
        )
        lastAdvancedTournamentMatchId = null
        publishTennis(state, fromWear = false)
        sendEvent(
            ScoringEventDto(
                type = SyncTypes.START,
                playerA = state.playerA,
                playerB = state.playerB,
                bestOf = state.format.bestOf,
                mode = state.mode.name,
                noAd = state.noAd,
                sport = SportType.TENNIS.name,
                sequence = nextSeq(),
            ),
        )
    }

    /** Start badminton on the phone with current/default names. */
    fun startPhoneBadmintonMatch() {
        val s = _uiState.value
        val names = PlayerNames(
            s.draftPlayerA.ifBlank { getApplication<Application>().getString(R.string.player_a) },
            s.draftPlayerB.ifBlank { getApplication<Application>().getString(R.string.player_b) },
        )
        phoneAuthority = true
        tennisEngine.clearMatch()
        val state = badmintonEngine.startMatch(names)
        publishBadminton(state, fromWear = false)
        sendEvent(
            ScoringEventDto(
                type = SyncTypes.START,
                playerA = state.playerA,
                playerB = state.playerB,
                sport = SportType.BADMINTON.name,
                sequence = nextSeq(),
            ),
        )
    }

    // --- Tournament ---

    fun openTournamentSetup() {
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
        phoneAuthority = true
        badmintonEngine.clearMatch()
        val state = tennisEngine.startMatch(
            PlayerNames(match.playerA ?: "A", match.playerB ?: "B"),
            MatchFormat.fromBestOf(match.bestOf),
            MatchMode.SINGLES,
            noAd = tournament.defaultNoAd,
        )
        publishTennis(state, fromWear = false)
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
        viewModelScope.launch {
            delay(400)
            if (_uiState.value.wearConnected) {
                sync.openWearApp(getApplication())
            }
        }
    }

    private fun onPoint(side: Side) {
        if (phoneAuthority) {
            val now = System.currentTimeMillis()
            if (side == lastLocalPointSide && now - lastLocalPointAtMs < 350L) return
            lastLocalPointSide = side
            lastLocalPointAtMs = now
            applyLocalPoint(side)
            sendEvent(
                ScoringEventDto(type = SyncTypes.POINT, side = side.name, sequence = nextSeq()),
            )
        } else {
            sendEvent(
                ScoringEventDto(type = SyncTypes.POINT, side = side.name, sequence = nextSeq()),
            )
        }
    }

    private fun applyLocalPoint(side: Side) {
        when (_uiState.value.selectedSport) {
            SportType.BADMINTON -> {
                val state = badmintonEngine.pointWon(side)
                publishBadminton(state, fromWear = false)
            }
            else -> {
                val state = tennisEngine.pointWon(side)
                publishTennis(state, fromWear = false)
                if (state.isMatchOver) {
                    persistFinished(state)
                    maybeAdvanceTournament(state)
                    _uiState.update { it.copy(history = repo.loadHistory()) }
                } else {
                    maybeMarkTournamentInProgress(state)
                }
            }
        }
    }

    private fun applyLocalUndo() {
        when (_uiState.value.selectedSport) {
            SportType.BADMINTON -> publishBadminton(badmintonEngine.undo(), fromWear = false)
            else -> publishTennis(tennisEngine.undo(), fromWear = false)
        }
    }

    private fun publishTennis(state: MatchState, fromWear: Boolean = false) {
        lastScoreFingerprint = scoreFingerprint(state)
        _uiState.update {
            it.copy(
                matchStarted = true,
                selectedSport = SportType.TENNIS,
                matchState = state,
                badmintonState = null,
                canUndo = if (fromWear) !state.isMatchOver else tennisEngine.canUndo(),
                scoringFromWear = fromWear && !phoneAuthority,
                screen = PhoneScreen.MATCH_SCOREBOARD,
            )
        }
        repo.saveCurrentMatch(state)
    }

    private fun publishBadminton(state: BadmintonMatchState, fromWear: Boolean = false) {
        lastScoreFingerprint = "BM|${state.pointsA}-${state.pointsB}|${state.isMatchOver}|${state.server}"
        _uiState.update {
            it.copy(
                matchStarted = true,
                selectedSport = SportType.BADMINTON,
                badmintonState = state,
                matchState = null,
                canUndo = if (fromWear) !state.isMatchOver else badmintonEngine.canUndo(),
                scoringFromWear = fromWear && !phoneAuthority,
                screen = PhoneScreen.BADMINTON_SCOREBOARD,
            )
        }
    }

    private fun clearLocalEngines() {
        tennisEngine.clearMatch()
        badmintonEngine.clearMatch()
    }

    private fun resyncPhoneMatchToWearIfPristine() {
        val ui = _uiState.value
        when {
            ui.selectedSport == SportType.BADMINTON && ui.badmintonState != null -> {
                val st = ui.badmintonState
                if (st.pointsA != 0 || st.pointsB != 0 || st.isMatchOver) return
                sendEvent(
                    ScoringEventDto(
                        type = SyncTypes.START,
                        playerA = st.playerA,
                        playerB = st.playerB,
                        sport = SportType.BADMINTON.name,
                        sequence = nextSeq(),
                    ),
                )
            }
            ui.matchState != null -> {
                val st = ui.matchState
                val pristine = st.pointsA == 0 && st.pointsB == 0 &&
                    st.gamesA == 0 && st.gamesB == 0 &&
                    st.setsA == 0 && st.setsB == 0 && !st.isMatchOver
                if (!pristine) return
                sendEvent(
                    ScoringEventDto(
                        type = SyncTypes.START,
                        playerA = st.playerA,
                        playerB = st.playerB,
                        bestOf = st.format.bestOf,
                        mode = st.mode.name,
                        noAd = st.noAd,
                        server = st.server.name,
                        sport = SportType.TENNIS.name,
                        sequence = nextSeq(),
                    ),
                )
            }
        }
    }

    private fun applyRemoteState(dto: MatchStateDto) {
        // Phone-owned match: ignore wear idle broadcasts that would wipe the phone scoreboard
        // (including finished matches still shown until New match).
        if (phoneAuthority && !dto.matchActive && _uiState.value.matchStarted) {
            return
        }

        val sport = SportType.fromName(dto.sport)
        if (!dto.matchActive) {
            if (phoneAuthority) {
                // Wear ended while phone already finished / idle — clear authority flag.
                phoneAuthority = false
            }
            _uiState.update {
                val screen = when {
                    it.screen == PhoneScreen.LANGUAGE -> PhoneScreen.LANGUAGE
                    it.screen == PhoneScreen.SPORT_PICKER -> PhoneScreen.SPORT_PICKER
                    it.screen == PhoneScreen.SETTINGS -> PhoneScreen.SETTINGS
                    it.screen == PhoneScreen.TERMS -> PhoneScreen.TERMS
                    it.screen == PhoneScreen.TOURNAMENT_SETUP -> PhoneScreen.TOURNAMENT_SETUP
                    it.screen == PhoneScreen.START_MATCH -> PhoneScreen.START_MATCH
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
            clearLocalEngines()
            repo.saveCurrentMatch(null)
            lastScoreFingerprint = null
            return
        }

        if (phoneAuthority) {
            // Companion watch scored — adopt only when state differs (skip echo of our own taps
            // so local undo history is preserved).
            if (sport == SportType.BADMINTON) {
                val incoming = dto.toBadmintonMatchState()
                val fp = "BM|${incoming.pointsA}-${incoming.pointsB}|${incoming.isMatchOver}|${incoming.server}"
                if (fp == lastScoreFingerprint) return
                val state = badmintonEngine.restoreFrom(incoming)
                publishBadminton(state, fromWear = false)
            } else {
                val incoming = dto.toMatchState()
                val fp = scoreFingerprint(incoming)
                if (fp == lastScoreFingerprint) return
                val state = tennisEngine.restoreFrom(incoming)
                publishTennis(state, fromWear = false)
                if (state.isMatchOver) {
                    persistFinished(state)
                    maybeAdvanceTournament(state)
                    _uiState.update { it.copy(history = repo.loadHistory()) }
                } else {
                    maybeMarkTournamentInProgress(state)
                }
            }
            return
        }

        // Wear-started match (phone is scoreboard + mirror).
        phoneAuthority = false
        if (sport == SportType.BADMINTON) {
            val state = dto.toBadmintonMatchState()
            lastScoreFingerprint = "BM|${state.pointsA}-${state.pointsB}|${state.isMatchOver}|${state.server}"
            _uiState.update {
                val stayPicker = it.screen == PhoneScreen.LANGUAGE ||
                    it.screen == PhoneScreen.SPORT_PICKER ||
                    it.screen == PhoneScreen.SETTINGS ||
                    it.screen == PhoneScreen.TERMS ||
                    it.screen == PhoneScreen.START_MATCH
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
                it.screen == PhoneScreen.SPORT_PICKER ||
                it.screen == PhoneScreen.SETTINGS ||
                it.screen == PhoneScreen.TERMS ||
                it.screen == PhoneScreen.START_MATCH
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
    START_MATCH,
    BADMINTON_IDLE,
    BADMINTON_SCOREBOARD,
    TOURNAMENT_SETUP,
    TOURNAMENT_BRACKET,
    MATCH_SCOREBOARD,
    SETTINGS,
    TERMS,
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
    /** True after receiving live state from a Wear-started match. */
    val scoringFromWear: Boolean = false,
    val history: List<MatchHistoryEntry> = emptyList(),
    val screen: PhoneScreen = PhoneScreen.SPORT_PICKER,
    val tournament: Tournament? = null,
    val draftPlayerCount: Int = 4,
    val draftBestOf: Int = 3,
    val draftNoAd: Boolean = false,
    val draftDoubles: Boolean = false,
    val draftPlayerA: String = "Player A",
    val draftPlayerB: String = "Player B",
    val draftPlayerNames: List<String> = List(8) { "" },
    /** Screen to restore when leaving Settings (not used for Terms → Settings). */
    val settingsReturnScreen: PhoneScreen? = null,
)
