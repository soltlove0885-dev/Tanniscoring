package com.tanniscoring.wear.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tanniscoring.shared.SyncPaths

/**
 * Receives phone MessageClient payloads while Wear UI may be backgrounded.
 * Forwards PATH_EVENT / PATH_REQUEST_STATE into [PhoneSyncManager].
 */
class TanniscoringPhoneListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived path=${messageEvent.path}")
        when (messageEvent.path) {
            SyncPaths.PATH_EVENT,
            SyncPaths.PATH_REQUEST_STATE,
            -> PhoneSyncManager.handleServiceMessage(messageEvent)
            else -> Unit
        }
    }

    companion object {
        private const val TAG = "TanniscoringPhoneLS"
    }
}
