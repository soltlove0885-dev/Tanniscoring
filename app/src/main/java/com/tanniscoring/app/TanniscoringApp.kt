package com.tanniscoring.app

import android.app.Application
import com.tanniscoring.app.data.LocalePreferences

class TanniscoringApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalePreferences.applySaved(this)
    }
}
