package com.tanniscoring.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.SyncTypes
import com.tanniscoring.wear.sync.PhoneSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Wear does **not** own scoring. It sends events to the phone and
 * displays the FullState broadcast returned by the phone.
 */
class WearMatchViewModel(application: Application) : AndroidViewModel(application) {

    private val sync = PhoneSyncManager(application.applicationContext)
    private var sequence = 0L

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sync.incomingState.collect { dto ->
                _uiState.update { it.copy(match = dto, hasState = true) }
            }
        }
        sync.startListening()
    }

    fun sendPointA() = sendEvent(ScoringEventDto(type = SyncTypes.POINT, side = "A", sequence = nextSeq()))
    fun sendPointB() = sendEvent(ScoringEventDto(type = SyncTypes.POINT, side = "B", sequence = nextSeq()))
    fun sendUndo() = sendEvent(ScoringEventDto(type = SyncTypes.UNDO, sequence = nextSeq()))

    private fun sendEvent(event: ScoringEventDto) {
        viewModelScope.launch { sync.sendEvent(event) }
    }

    private fun nextSeq(): Long {
        sequence++
        return sequence
    }

    override fun onCleared() {
        sync.stopListening()
        super.onCleared()
    }
}

data class WearUiState(
    val match: MatchStateDto? = null,
    val hasState: Boolean = false,
)
