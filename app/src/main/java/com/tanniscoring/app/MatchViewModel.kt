package com.tanniscoring.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanniscoring.app.data.MatchRepository
import com.tanniscoring.app.sync.WearSyncManager
import com.tanniscoring.shared.MatchHistoryEntry
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.SyncTypes
import com.tanniscoring.shared.toMatchState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phone is the **live scoreboard**.
 * Wear owns scoring; this ViewModel displays [MatchState] from Wear via MessageClient
 * and optionally forwards POINT/UNDO mirror controls to Wear.
 */
class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val sync = WearSyncManager.get(application.applicationContext)
    private val repo = MatchRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MatchUiState(history = repo.loadHistory()))
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var sequence = 0L
    private var lastPersistedFinishedFingerprint: String? = null
    private var wasWearConnected = false

    init {
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

    /** Opens Wear companion Play Store on the watch (preferred) or phone (fallback). */
    fun openWearCompanionApp(context: Context) {
        viewModelScope.launch {
            sync.openWearCompanionStore(context)
        }
    }

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
        _uiState.update {
            it.copy(
                matchStarted = false,
                matchState = null,
                canUndo = false,
                scoringFromWear = false,
                history = repo.loadHistory(),
            )
        }
    }

    fun requestWearState() {
        viewModelScope.launch { sync.requestState() }
    }

    private fun applyRemoteState(dto: MatchStateDto) {
        if (!dto.matchActive) {
            _uiState.update {
                it.copy(
                    matchStarted = false,
                    matchState = null,
                    canUndo = false,
                    scoringFromWear = false,
                )
            }
            repo.saveCurrentMatch(null)
            return
        }
        val state = dto.toMatchState()
        _uiState.update {
            it.copy(
                matchStarted = true,
                matchState = state,
                canUndo = !state.isMatchOver,
                scoringFromWear = true,
            )
        }
        repo.saveCurrentMatch(state)
        if (state.isMatchOver) {
            persistFinished(state)
            _uiState.update { it.copy(history = repo.loadHistory()) }
        }
    }

    private fun sendEvent(event: ScoringEventDto) {
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

data class MatchUiState(
    val matchStarted: Boolean = false,
    val matchState: MatchState? = null,
    val canUndo: Boolean = false,
    /** True when ≥1 Wear OS Data Layer node is connected — NOT that wear app is installed. */
    val wearConnected: Boolean = false,
    val wearNodeCount: Int = 0,
    /** True after receiving live state from Wear (scoreboard mode). */
    val scoringFromWear: Boolean = false,
    val history: List<MatchHistoryEntry> = emptyList(),
)
