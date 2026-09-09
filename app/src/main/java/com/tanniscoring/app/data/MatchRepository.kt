package com.tanniscoring.app.data

import android.content.Context
import com.tanniscoring.shared.MatchHistoryEntry
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.SyncJson
import com.tanniscoring.shared.toDto
import com.tanniscoring.shared.toHistoryEntry
import com.tanniscoring.shared.toMatchState
import java.util.UUID

/**
 * Simple SharedPreferences JSON persistence for the current match and recent history.
 * No Room — intentionally lightweight for MVP.
 */
class MatchRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadCurrentMatch(): MatchState? {
        val json = prefs.getString(KEY_CURRENT, null) ?: return null
        return runCatching { SyncJson.decodeState(json).toMatchState() }.getOrNull()
            ?.takeIf { it.matchActive }
    }

    fun saveCurrentMatch(state: MatchState?) {
        if (state == null || !state.matchActive) {
            prefs.edit().remove(KEY_CURRENT).apply()
            return
        }
        prefs.edit().putString(KEY_CURRENT, SyncJson.encodeState(state.toDto())).apply()
    }

    fun loadHistory(): List<MatchHistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching { SyncJson.decodeHistoryList(json) }.getOrDefault(emptyList())
    }

    fun appendFinishedMatch(state: MatchState) {
        if (!state.isMatchOver) return
        val existing = loadHistory().toMutableList()
        // Avoid duplicate appends if process death reloads a finished match.
        val fingerprint = "${state.playerA}|${state.playerB}|${state.setsA}-${state.setsB}|${state.setHistory}"
        if (existing.any {
                "${it.playerA}|${it.playerB}|${it.setsA}-${it.setsB}|${it.setHistory}" == fingerprint
            }
        ) {
            return
        }
        val entry = state.toHistoryEntry(
            id = UUID.randomUUID().toString(),
            finishedAtEpochMs = System.currentTimeMillis(),
        )
        existing.add(0, entry)
        val trimmed = existing.take(MAX_HISTORY)
        prefs.edit().putString(KEY_HISTORY, SyncJson.encodeHistoryList(trimmed)).apply()
    }

    companion object {
        private const val PREFS = "tanniscoring_prefs"
        private const val KEY_CURRENT = "current_match_json"
        private const val KEY_HISTORY = "match_history_json"
        private const val MAX_HISTORY = 20
    }
}
