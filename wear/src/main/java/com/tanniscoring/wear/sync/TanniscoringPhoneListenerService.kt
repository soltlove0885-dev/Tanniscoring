package com.tanniscoring.wear.sync

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tanniscoring.shared.SyncJson
import com.tanniscoring.shared.SyncPaths
import com.tanniscoring.wear.MainActivity

/**
 * Receives phone state while Wear is on the waiting screen (or backgrounded).
 * Updates the shared [PhoneSyncManager] flow so [WearMatchViewModel] refreshes UI.
 * When an active match arrives and the UI may be backgrounded, brings [MainActivity] forward.
 */
class TanniscoringPhoneListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived path=${messageEvent.path}")
        if (messageEvent.path != SyncPaths.PATH_STATE) return
        PhoneSyncManager.handleServiceMessage(messageEvent)
        val json = messageEvent.data.toString(Charsets.UTF_8)
        val active = runCatching { SyncJson.decodeState(json).matchActive }.getOrDefault(false)
        if (active) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged")
        PhoneSyncManager.handleServiceDataChanged(dataEvents)
    }

    companion object {
        private const val TAG = "TanniscoringPhoneLS"
    }
}
