package com.tanniscoring.wear

import android.app.Application
import android.content.res.Configuration
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanniscoring.shared.BadmintonMatchState
import com.tanniscoring.shared.BadmintonScoringEngine
import com.tanniscoring.shared.MatchFormat
import com.tanniscoring.shared.MatchMode
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.PlayerNames
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.Side
import com.tanniscoring.shared.SportType
import com.tanniscoring.shared.SyncTypes
import com.tanniscoring.shared.TennisScoringEngine
import com.tanniscoring.shared.toBadmintonMatchState
import com.tanniscoring.shared.toDto
import com.tanniscoring.wear.data.LocalePreferences
import com.tanniscoring.wear.data.WearMatchRepository
import com.tanniscoring.wear.sync.PhoneSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Wear is the **scoring authority**.
 * Local taps and phone mirror events both call the active sport engine;
 * resulting state is pushed to the phone via MessageClient.
 */
class WearMatchViewModel(application: Application) : AndroidViewModel(application) {

    private val tennisEngine = TennisScoringEngine()
    private val badmintonEngine = BadmintonScoringEngine()
    private val sync = PhoneSyncManager.get(application.applicationContext)
    private val repo = WearMatchRepository(application.applicationContext)

    /** Last applied phone event sequence (ignore duplicates / redeliveries). */
    private var lastAppliedRemoteSequence: Long = -1L
    /** Debounce rapid identical POINT taps (local or remote). */
    private var lastPointAtMs: Long = 0L
    private var lastPointSide: Side? = null

    private val _uiState = MutableStateFlow(
        WearUiState(
            screen = if (LocalePreferences.hasChosenLanguage(application)) {
                WearScreen.SPORT_PICKER
            } else {
                WearScreen.LANGUAGE
            },
            draftPlayerA = application.getString(R.string.player_a),
            draftPlayerB = application.getString(R.string.player_b),
        ),
    )
    val uiState: StateFlow<WearUiState> = _uiState.asStateFlow()

    init {
        restoreIfNeeded()
        viewModelScope.launch {
            sync.incomingEvents.collect { handleRemoteEvent(it) }
        }
        viewModelScope.launch {
            sync.stateRequests.collect { pushCurrentStateToPhone() }
        }
        sync.startListening()
        pushCurrentStateToPhone()
    }

    fun chooseLanguage(tag: String) {
        LocalePreferences.setLanguage(getApplication(), tag)
        _uiState.update {
            it.copy(
                screen = WearScreen.SPORT_PICKER,
                draftPlayerA = localizedString(tag, R.string.player_a),
                draftPlayerB = localizedString(tag, R.string.player_b),
            )
        }
    }

    private fun localizedString(tag: String, resId: Int): String {
        val app = getApplication<Application>()
        val config = Configuration(app.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return app.createConfigurationContext(config).getString(resId)
    }

    fun showLanguagePicker() {
        _uiState.update { it.copy(screen = WearScreen.LANGUAGE) }
    }

    fun selectSport(sport: SportType) {
        _uiState.update {
            it.copy(
                selectedSport = sport,
                screen = WearScreen.IDLE,
                matchStarted = false,
                matchState = null,
                badmintonState = null,
            )
        }
    }

    fun showSportPicker() {
        tennisEngine.clearMatch()
        badmintonEngine.clearMatch()
        repo.saveCurrentMatch(null)
        _uiState.update {
            it.copy(
                screen = WearScreen.SPORT_PICKER,
                selectedSport = null,
                matchStarted = false,
                matchState = null,
                badmintonState = null,
                canUndo = false,
            )
        }
        viewModelScope.launch {
            sync.sendState(
                MatchStateDto(matchActive = false, pointDisplayA = "-", pointDisplayB = "-"),
            )
        }
    }

    fun setDraftNoAd(noAd: Boolean) = _uiState.update { it.copy(draftNoAd = noAd) }
    fun toggleDraftNoAd() = _uiState.update { it.copy(draftNoAd = !it.draftNoAd) }

    fun startMatch() {
        val sport = _uiState.value.selectedSport ?: SportType.TENNIS
        if (sport == SportType.BADMINTON) {
            val state = badmintonEngine.startMatch(
                PlayerNames(
                    _uiState.value.draftPlayerA,
                    _uiState.value.draftPlayerB,
                ),
            )
            publishBadminton(state)
        } else {
            val s = _uiState.value
            val state = tennisEngine.startMatch(
                PlayerNames(s.draftPlayerA, s.draftPlayerB),
                MatchFormat.fromBestOf(s.draftBestOf),
                MatchMode.SINGLES,
                noAd = s.draftNoAd,
            )
            publishTennis(state)
        }
    }

    fun pointWonA() = applyPoint(Side.A)
    fun pointWonB() = applyPoint(Side.B)

    fun undo() {
        when (_uiState.value.selectedSport) {
            SportType.BADMINTON -> publishBadminton(badmintonEngine.undo())
            else -> publishTennis(tennisEngine.undo())
        }
    }

    fun toggleServer() {
        if (_uiState.value.selectedSport == SportType.BADMINTON) return
        publishTennis(tennisEngine.toggleServer())
    }

    fun endMatch() {
        when (_uiState.value.selectedSport) {
            SportType.BADMINTON -> publishBadminton(badmintonEngine.endMatch())
            else -> publishTennis(tennisEngine.endMatch())
        }
    }

    /**
     * Exit scoring UI to idle: clear engines + drafts, notify phone matchActive=false.
     */
    fun exitToIdle() {
        tennisEngine.clearMatch()
        badmintonEngine.clearMatch()
        repo.saveCurrentMatch(null)
        val sport = _uiState.value.selectedSport
        _uiState.update {
            it.copy(
                matchStarted = false,
                matchState = null,
                badmintonState = null,
                canUndo = false,
                draftPlayerA = defaultPlayerA(),
                draftPlayerB = defaultPlayerB(),
                draftBestOf = 3,
                draftNoAd = false,
                screen = WearScreen.IDLE,
                selectedSport = sport,
            )
        }
        viewModelScope.launch {
            sync.sendState(
                MatchStateDto(
                    sport = (sport ?: SportType.TENNIS).name,
                    matchActive = false,
                    pointDisplayA = "-",
                    pointDisplayB = "-",
                ),
            )
        }
    }

    fun resetToStart() = exitToIdle()

    private fun defaultPlayerA(): String =
        localizedString(LocalePreferences.currentTag(getApplication()), R.string.player_a)

    private fun defaultPlayerB(): String =
        localizedString(LocalePreferences.currentTag(getApplication()), R.string.player_b)

    private fun restoreIfNeeded() {
        if (!LocalePreferences.hasChosenLanguage(getApplication())) return
        val saved = repo.loadCurrentMatch() ?: return
        // Persisted matches are tennis (MatchState); badminton uses same DTO with sport field.
        val json = getApplication<Application>()
            .getSharedPreferences("tanniscoring_wear_prefs", android.content.Context.MODE_PRIVATE)
            .getString("current_match_json", null)
        val sport = if (json != null && json.contains("\"sport\":\"BADMINTON\"")) {
            SportType.BADMINTON
        } else {
            SportType.TENNIS
        }
        if (sport == SportType.BADMINTON) {
            val dto = runCatching {
                com.tanniscoring.shared.SyncJson.decodeState(json!!)
            }.getOrNull() ?: return
            val bm = dto.toBadmintonMatchState()
            val state = badmintonEngine.restoreFrom(bm)
            _uiState.update {
                it.copy(
                    selectedSport = SportType.BADMINTON,
                    screen = WearScreen.IDLE,
                    matchStarted = true,
                    badmintonState = state,
                    matchState = null,
                    canUndo = false,
                    draftPlayerA = state.playerA,
                    draftPlayerB = state.playerB,
                )
            }
        } else {
            val state = tennisEngine.restoreFrom(saved)
            _uiState.update {
                it.copy(
                    selectedSport = SportType.TENNIS,
                    screen = WearScreen.IDLE,
                    matchStarted = true,
                    matchState = state,
                    badmintonState = null,
                    canUndo = false,
                    draftPlayerA = state.playerA,
                    draftPlayerB = state.playerB,
                    draftBestOf = state.format.bestOf,
                    draftNoAd = state.noAd,
                )
            }
        }
    }

    private fun applyPoint(side: Side, fromRemote: Boolean = false) {
        val now = System.currentTimeMillis()
        val window = if (fromRemote) 250L else 350L
        if (side == lastPointSide && now - lastPointAtMs < window) {
            return
        }
        lastPointSide = side
        lastPointAtMs = now
        when (_uiState.value.selectedSport) {
            SportType.BADMINTON -> publishBadminton(badmintonEngine.pointWon(side))
            else -> publishTennis(tennisEngine.pointWon(side))
        }
    }

    private fun handleRemoteEvent(event: ScoringEventDto) {
        if (event.sequence > 0L) {
            if (event.sequence <= lastAppliedRemoteSequence) {
                return
            }
            lastAppliedRemoteSequence = event.sequence
        }
        when (event.type) {
            SyncTypes.POINT -> {
                val side = event.side?.let { runCatching { Side.valueOf(it) }.getOrNull() } ?: return
                applyPoint(side, fromRemote = true)
            }
            SyncTypes.UNDO -> undo()
            SyncTypes.TOGGLE_SERVER -> toggleServer()
            SyncTypes.END -> exitToIdle()
            SyncTypes.START -> {
                val sport = SportType.fromName(event.sport)
                if (sport == SportType.BADMINTON) {
                    val state = badmintonEngine.startMatch(
                        PlayerNames(event.playerA ?: defaultPlayerA(), event.playerB ?: defaultPlayerB()),
                    )
                    _uiState.update {
                        it.copy(
                            selectedSport = SportType.BADMINTON,
                            screen = WearScreen.IDLE,
                            draftPlayerA = event.playerA ?: it.draftPlayerA,
                            draftPlayerB = event.playerB ?: it.draftPlayerB,
                        )
                    }
                    publishBadminton(state)
                } else {
                    val format = MatchFormat.fromBestOf(event.bestOf ?: 3)
                    val mode = MatchMode.fromName(event.mode)
                    val noAd = event.noAd ?: false
                    val state = tennisEngine.startMatch(
                        PlayerNames(event.playerA ?: defaultPlayerA(), event.playerB ?: defaultPlayerB()),
                        format,
                        mode,
                        event.server?.let { runCatching { Side.valueOf(it) }.getOrNull() } ?: Side.A,
                        noAd = noAd,
                    )
                    _uiState.update {
                        it.copy(
                            selectedSport = SportType.TENNIS,
                            screen = WearScreen.IDLE,
                            draftPlayerA = event.playerA ?: it.draftPlayerA,
                            draftPlayerB = event.playerB ?: it.draftPlayerB,
                            draftBestOf = event.bestOf ?: it.draftBestOf,
                            draftNoAd = noAd,
                        )
                    }
                    publishTennis(state)
                }
            }
            SyncTypes.REQUEST_STATE -> pushCurrentStateToPhone()
        }
    }

    private fun publishTennis(state: MatchState) {
        _uiState.update {
            it.copy(
                selectedSport = SportType.TENNIS,
                matchStarted = true,
                matchState = state,
                badmintonState = null,
                canUndo = tennisEngine.canUndo(),
                screen = WearScreen.IDLE,
            )
        }
        repo.saveCurrentMatch(state)
        viewModelScope.launch { sync.sendState(state.toDto()) }
    }

    private fun publishBadminton(state: BadmintonMatchState) {
        _uiState.update {
            it.copy(
                selectedSport = SportType.BADMINTON,
                matchStarted = true,
                badmintonState = state,
                matchState = null,
                canUndo = badmintonEngine.canUndo(),
                screen = WearScreen.IDLE,
            )
        }
        // Persist via DTO JSON so restore can detect sport
        getApplication<Application>()
            .getSharedPreferences("tanniscoring_wear_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("current_match_json", com.tanniscoring.shared.SyncJson.encodeState(state.toDto()))
            .apply()
        viewModelScope.launch { sync.sendState(state.toDto()) }
    }

    private fun pushCurrentStateToPhone() {
        viewModelScope.launch {
            val ui = _uiState.value
            when {
                ui.selectedSport == SportType.BADMINTON && ui.badmintonState != null -> {
                    sync.sendState(ui.badmintonState.toDto())
                }
                ui.matchState != null -> {
                    sync.sendState(ui.matchState.toDto())
                }
                else -> {
                    sync.sendState(
                        MatchStateDto(
                            sport = (ui.selectedSport ?: SportType.TENNIS).name,
                            matchActive = false,
                            pointDisplayA = "-",
                            pointDisplayB = "-",
                        ),
                    )
                }
            }
        }
    }

    fun resyncToPhone() {
        pushCurrentStateToPhone()
    }

    override fun onCleared() {
        sync.stopListening()
        super.onCleared()
    }
}

enum class WearScreen {
    LANGUAGE,
    SPORT_PICKER,
    IDLE,
}

data class WearUiState(
    val screen: WearScreen = WearScreen.LANGUAGE,
    val selectedSport: SportType? = null,
    val matchStarted: Boolean = false,
    val draftPlayerA: String = "Player A",
    val draftPlayerB: String = "Player B",
    val draftBestOf: Int = 3,
    val draftNoAd: Boolean = false,
    val matchState: MatchState? = null,
    val badmintonState: BadmintonMatchState? = null,
    val canUndo: Boolean = false,
)
