package com.tanniscoring.wear.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.SyncJson
import com.tanniscoring.shared.SyncPaths
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await

/**
 * Wear → Phone: scoring events on [SyncPaths.PATH_EVENT]
 * Phone → Wear: full state on [SyncPaths.PATH_STATE]
 */
class PhoneSyncManager(context: Context) : MessageClient.OnMessageReceivedListener {

    private val appContext = context.applicationContext
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val nodeClient: NodeClient = Wearable.getNodeClient(appContext)

    private val _incomingState = MutableSharedFlow<MatchStateDto>(extraBufferCapacity = 8)
    val incomingState: SharedFlow<MatchStateDto> = _incomingState.asSharedFlow()

    fun startListening() {
        messageClient.addListener(this)
    }

    fun stopListening() {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != SyncPaths.PATH_STATE) return
        val json = messageEvent.data.toString(Charsets.UTF_8)
        Log.d(TAG, "State from phone: $json")
        runCatching { SyncJson.decodeState(json) }
            .onSuccess { _incomingState.tryEmit(it) }
            .onFailure { Log.e(TAG, "decode state failed", it) }
    }

    suspend fun sendEvent(event: ScoringEventDto) {
        val payload = SyncJson.encodeEvent(event).toByteArray(Charsets.UTF_8)
        try {
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                messageClient.sendMessage(node.id, SyncPaths.PATH_EVENT, payload).await()
            }
            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected phone node")
            }
        } catch (e: Exception) {
            Log.w(TAG, "sendEvent failed", e)
        }
    }

    companion object {
        private const val TAG = "PhoneSyncManager"
    }
}
