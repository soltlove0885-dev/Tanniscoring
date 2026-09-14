package com.tanniscoring.wear.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocalePreferences {
    private const val PREFS = "tanniscoring_wear_prefs"
    private const val KEY_LANG = "app_language_tag"
    private const val KEY_CHOSEN = "app_language_chosen"

    fun hasChosenLanguage(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CHOSEN, false)

    fun currentTag(context: Context): String =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "ko") ?: "ko"

    fun applySaved(context: Context) {
        if (!hasChosenLanguage(context)) return
        val tag = currentTag(context)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun setLanguage(context: Context, tag: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, tag)
            .putBoolean(KEY_CHOSEN, true)
            .apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
