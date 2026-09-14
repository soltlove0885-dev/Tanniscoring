package com.tanniscoring.wear

import android.app.Application
import com.tanniscoring.wear.data.LocalePreferences

class TanniscoringWearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalePreferences.applySaved(this)
    }
}
