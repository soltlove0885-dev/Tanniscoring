package com.tanniscoring.app.data

import android.content.Context
import com.tanniscoring.shared.SyncJson
import com.tanniscoring.shared.Tournament

class TournamentRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Tournament? {
        val json = prefs.getString(KEY_TOURNAMENT, null) ?: return null
        return runCatching { SyncJson.decodeTournament(json) }.getOrNull()
    }

    fun save(tournament: Tournament?) {
        if (tournament == null) {
            prefs.edit().remove(KEY_TOURNAMENT).apply()
            return
        }
        prefs.edit().putString(KEY_TOURNAMENT, SyncJson.encodeTournament(tournament)).apply()
    }

    companion object {
        private const val PREFS = "tanniscoring_prefs"
        private const val KEY_TOURNAMENT = "tournament_json"
    }
}
