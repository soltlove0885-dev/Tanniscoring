package com.tanniscoring.app.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tanniscoring.shared.SyncPaths

/**
 * Receives Wear messages even when the phone UI process has no active MessageClient listener.
 * Forwards [SyncPaths.PATH_EVENT] / [SyncPaths.PATH_REQUEST_STATE] into [WearSyncManager].
 */
class TanniscoringWearListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived path=${messageEvent.path}")
        when (messageEvent.path) {
            SyncPaths.PATH_EVENT,
            SyncPaths.PATH_REQUEST_STATE,
            -> WearSyncManager.get(applicationContext)
                .handleMessage(messageEvent.path, messageEvent.data)
            else -> Unit
        }
    }

    companion object {
        private const val TAG = "TanniscoringWearLS"
    }
}
