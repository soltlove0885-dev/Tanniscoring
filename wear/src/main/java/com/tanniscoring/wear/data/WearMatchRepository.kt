package com.tanniscoring.wear.data

import android.content.Context
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.SyncJson
import com.tanniscoring.shared.toDto
import com.tanniscoring.shared.toMatchState

/**
 * Lightweight SharedPreferences persistence for the current Wear-owned match.
 */
class WearMatchRepository(context: Context) {

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

    companion object {
        private const val PREFS = "tanniscoring_wear_prefs"
        private const val KEY_CURRENT = "current_match_json"
    }
}
