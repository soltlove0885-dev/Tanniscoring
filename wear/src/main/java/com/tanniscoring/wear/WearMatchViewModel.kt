package com.tanniscoring.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanniscoring.shared.MatchFormat
import com.tanniscoring.shared.MatchMode
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.PlayerNames
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.Side
import com.tanniscoring.shared.SyncTypes
import com.tanniscoring.shared.TennisScoringEngine
import com.tanniscoring.shared.toDto
import com.tanniscoring.wear.data.WearMatchRepository
import com.tanniscoring.wear.sync.PhoneSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Wear is the **scoring authority**.
 * Local taps and phone mirror events both call [engine];
 * resulting [MatchState] is pushed to the phone via MessageClient.
 */
class WearMatchViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TennisScoringEngine()
    private val sync = PhoneSyncManager.get(application.applicationContext)
    private val repo = WearMatchRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(WearUiState())
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
        // On open: resend current state so phone scoreboard catches up.
        pushCurrentStateToPhone()
    }

    fun setDraftPlayerA(name: String) = _uiState.update { it.copy(draftPlayerA = name) }
    fun setDraftPlayerB(name: String) = _uiState.update { it.copy(draftPlayerB = name) }
    fun setDraftBestOf(bestOf: Int) = _uiState.update { it.copy(draftBestOf = bestOf) }

    fun startMatch() {
        val s = _uiState.value
        val state = engine.startMatch(
            PlayerNames(s.draftPlayerA, s.draftPlayerB),
            MatchFormat.fromBestOf(s.draftBestOf),
            MatchMode.SINGLES,
        )
        publish(state)
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
        engine.clearMatch()
        repo.saveCurrentMatch(null)
        _uiState.update {
            it.copy(
                matchStarted = false,
                matchState = null,
                canUndo = false,
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
            )
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
                _uiState.update {
                    it.copy(
                        draftPlayerA = event.playerA ?: it.draftPlayerA,
                        draftPlayerB = event.playerB ?: it.draftPlayerB,
                        draftBestOf = event.bestOf ?: it.draftBestOf,
                    )
                }
                publish(state)
            }
            SyncTypes.REQUEST_STATE -> pushCurrentStateToPhone()
        }
    }

    private fun publish(state: MatchState) {
        _uiState.update {
            it.copy(
                matchStarted = true,
                matchState = state,
                canUndo = engine.canUndo(),
            )
        }
        repo.saveCurrentMatch(state)
        viewModelScope.launch { sync.sendState(state.toDto()) }
    }

    private fun pushCurrentStateToPhone() {
        viewModelScope.launch {
            val state = _uiState.value.matchState
            if (state != null) {
                sync.sendState(state.toDto())
            } else {
                sync.sendState(
                    MatchStateDto(matchActive = false, pointDisplayA = "-", pointDisplayB = "-"),
                )
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

data class WearUiState(
    val matchStarted: Boolean = false,
    val draftPlayerA: String = "선수 A",
    val draftPlayerB: String = "선수 B",
    val draftBestOf: Int = 3,
    val matchState: MatchState? = null,
    val canUndo: Boolean = false,
)
