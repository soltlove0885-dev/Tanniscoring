package com.tanniscoring.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanniscoring.app.sync.WearSyncManager
import com.tanniscoring.shared.MatchFormat
import com.tanniscoring.shared.MatchState
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

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var sequence = 0L

    init {
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
        sync.startListening()
    }

    fun setDraftPlayerA(name: String) = _uiState.update { it.copy(draftPlayerA = name) }
    fun setDraftPlayerB(name: String) = _uiState.update { it.copy(draftPlayerB = name) }
    fun setDraftBestOf(bestOf: Int) = _uiState.update { it.copy(draftBestOf = bestOf) }

    fun startMatch() {
        val s = _uiState.value
        val format = MatchFormat.fromBestOf(s.draftBestOf)
        val state = engine.startMatch(
            PlayerNames(s.draftPlayerA, s.draftPlayerB),
            format,
        )
        publish(state, matchStarted = true)
    }

    fun pointWonA() = applyPoint(Side.A)
    fun pointWonB() = applyPoint(Side.B)

    fun undo() {
        val state = engine.undo()
        publish(state)
    }

    fun resetToStart() {
        _uiState.update {
            it.copy(
                matchStarted = false,
                matchState = null,
                canUndo = false,
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
            SyncTypes.START -> {
                val format = MatchFormat.fromBestOf(event.bestOf ?: 3)
                val state = engine.startMatch(
                    PlayerNames(event.playerA ?: "선수 A", event.playerB ?: "선수 B"),
                    format,
                )
                _uiState.update {
                    it.copy(
                        draftPlayerA = event.playerA ?: it.draftPlayerA,
                        draftPlayerB = event.playerB ?: it.draftPlayerB,
                        draftBestOf = event.bestOf ?: it.draftBestOf,
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
        viewModelScope.launch {
            sync.sendState(state.toDto())
        }
    }

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
    val matchState: MatchState? = null,
    val canUndo: Boolean = false,
    val wearConnected: Boolean = false,
)
