package com.tanniscoring.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanniscoring.app.data.MatchRepository
import com.tanniscoring.app.sync.WearSyncManager
import com.tanniscoring.shared.MatchFormat
import com.tanniscoring.shared.MatchHistoryEntry
import com.tanniscoring.shared.MatchMode
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.PlayerNames
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.Side
import com.tanniscoring.shared.SyncTypes
import com.tanniscoring.shared.TennisScoringEngine
import com.tanniscoring.shared.toDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phone is the **scoring authority**.
 * Local buttons and Wear events both call into [engine];
 * resulting [MatchState] is pushed to Wear via [WearSyncManager].
 */
class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TennisScoringEngine()
    private val sync = WearSyncManager(application.applicationContext)
    private val repo = MatchRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MatchUiState(history = repo.loadHistory()))
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var sequence = 0L
    private var lastPersistedFinishedFingerprint: String? = null

    init {
        restoreIfNeeded()
        viewModelScope.launch {
            sync.incomingEvents.collect { event ->
                handleRemoteEvent(event)
            }
        }
        viewModelScope.launch {
            sync.wearConnected.collect { connected ->
                _uiState.update { it.copy(wearConnected = connected) }
            }
        }
        viewModelScope.launch {
            sync.connectedNodeCount.collect { count ->
                _uiState.update { it.copy(wearNodeCount = count) }
            }
        }
        sync.startListening()
        // Push current state so Wear can catch up after process death / reconnect.
        _uiState.value.matchState?.let { state ->
            viewModelScope.launch { sync.sendState(state.toDto()) }
        }
    }

    fun setDraftPlayerA(name: String) = _uiState.update { it.copy(draftPlayerA = name) }
    fun setDraftPlayerB(name: String) = _uiState.update { it.copy(draftPlayerB = name) }
    fun setDraftBestOf(bestOf: Int) = _uiState.update { it.copy(draftBestOf = bestOf) }
    fun setDraftDoubles(doubles: Boolean) = _uiState.update {
        it.copy(
            draftDoubles = doubles,
            draftPlayerA = if (doubles && it.draftPlayerA == "선수 A") "팀 A" else
                if (!doubles && it.draftPlayerA == "팀 A") "선수 A" else it.draftPlayerA,
            draftPlayerB = if (doubles && it.draftPlayerB == "선수 B") "팀 B" else
                if (!doubles && it.draftPlayerB == "팀 B") "선수 B" else it.draftPlayerB,
        )
    }

    /** Opens Wear companion Play Store on the watch (preferred) or phone (fallback). */
    fun openWearCompanionApp(context: Context) {
        viewModelScope.launch {
            sync.openWearCompanionStore(context)
        }
    }

    fun startMatch() {
        val s = _uiState.value
        val format = MatchFormat.fromBestOf(s.draftBestOf)
        val mode = if (s.draftDoubles) MatchMode.DOUBLES else MatchMode.SINGLES
        val state = engine.startMatch(
            PlayerNames(s.draftPlayerA, s.draftPlayerB),
            format,
            mode,
        )
        lastPersistedFinishedFingerprint = null
        publish(state, matchStarted = true)
    }

    fun pointWonA() = applyPoint(Side.A)
    fun pointWonB() = applyPoint(Side.B)

    fun undo() {
        val state = engine.undo()
        publish(state)
    }

    fun toggleServer() {
        val state = engine.toggleServer()
        publish(state)
    }

    fun endMatch() {
        val state = engine.endMatch()
        publish(state)
    }

    fun resetToStart() {
        val current = engine.currentState()
        if (current.matchActive && current.isMatchOver) {
            persistFinished(current)
        }
        engine.clearMatch()
        repo.saveCurrentMatch(null)
        _uiState.update {
            it.copy(
                matchStarted = false,
                matchState = null,
                canUndo = false,
                history = repo.loadHistory(),
            )
        }
        viewModelScope.launch {
            sync.sendState(
                MatchStateDto(matchActive = false, pointDisplayA = "-", pointDisplayB = "-"),
            )
        }
    }

    private fun restoreIfNeeded() {
        val saved = repo.loadCurrentMatch() ?: return
        val state = engine.restoreFrom(saved)
        _uiState.update {
            it.copy(
                matchStarted = true,
                matchState = state,
                canUndo = false,
                draftPlayerA = state.playerA,
                draftPlayerB = state.playerB,
                draftBestOf = state.format.bestOf,
                draftDoubles = state.isDoubles,
                history = repo.loadHistory(),
            )
        }
        if (state.isMatchOver) {
            lastPersistedFinishedFingerprint = fingerprint(state)
        }
    }

    private fun applyPoint(side: Side) {
        val state = engine.pointWon(side)
        publish(state)
    }

    private fun handleRemoteEvent(event: ScoringEventDto) {
        when (event.type) {
            SyncTypes.POINT -> {
                val side = event.side?.let { runCatching { Side.valueOf(it) }.getOrNull() } ?: return
                applyPoint(side)
            }
            SyncTypes.UNDO -> undo()
            SyncTypes.TOGGLE_SERVER -> toggleServer()
            SyncTypes.END -> endMatch()
            SyncTypes.START -> {
                val format = MatchFormat.fromBestOf(event.bestOf ?: 3)
                val mode = MatchMode.fromName(event.mode)
                val state = engine.startMatch(
                    PlayerNames(event.playerA ?: "선수 A", event.playerB ?: "선수 B"),
                    format,
                    mode,
                    event.server?.let { runCatching { Side.valueOf(it) }.getOrNull() } ?: Side.A,
                )
                lastPersistedFinishedFingerprint = null
                _uiState.update {
                    it.copy(
                        draftPlayerA = event.playerA ?: it.draftPlayerA,
                        draftPlayerB = event.playerB ?: it.draftPlayerB,
                        draftBestOf = event.bestOf ?: it.draftBestOf,
                        draftDoubles = mode == MatchMode.DOUBLES,
                    )
                }
                publish(state, matchStarted = true)
            }
        }
    }

    private fun publish(state: MatchState, matchStarted: Boolean = true) {
        sequence++
        _uiState.update {
            it.copy(
                matchStarted = matchStarted,
                matchState = state,
                canUndo = engine.canUndo(),
            )
        }
        repo.saveCurrentMatch(state)
        if (state.isMatchOver) {
            persistFinished(state)
            _uiState.update { it.copy(history = repo.loadHistory()) }
        }
        viewModelScope.launch {
            sync.sendState(state.toDto())
        }
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

data class MatchUiState(
    val matchStarted: Boolean = false,
    val draftPlayerA: String = "선수 A",
    val draftPlayerB: String = "선수 B",
    val draftBestOf: Int = 3,
    val draftDoubles: Boolean = false,
    val matchState: MatchState? = null,
    val canUndo: Boolean = false,
    /** True when ≥1 Wear OS Data Layer node is connected — NOT that wear app is installed. */
    val wearConnected: Boolean = false,
    val wearNodeCount: Int = 0,
    val history: List<MatchHistoryEntry> = emptyList(),
)
