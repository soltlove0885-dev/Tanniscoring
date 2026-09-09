package com.tanniscoring.app.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tanniscoring.shared.SyncPaths

/**
 * Receives Wear MessageClient payloads even when the phone UI has no active listener.
 * Forwards [SyncPaths.PATH_STATE] into [WearSyncManager] shared flow for immediate UI update.
 */
class TanniscoringWearListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived path=${messageEvent.path}")
        when (messageEvent.path) {
            SyncPaths.PATH_STATE -> WearSyncManager.handleServiceMessage(messageEvent)
            else -> Unit
        }
    }

    companion object {
        private const val TAG = "TanniscoringWearLS"
    }
}
