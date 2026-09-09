package com.tanniscoring.app.sync

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Phone-side Wearable Data Layer bridge.
 * - Listens for scoring events from Wear on [SyncPaths.PATH_EVENT]
 * - Broadcasts full match state to Wear on [SyncPaths.PATH_STATE]
 */
class WearSyncManager(context: Context) : MessageClient.OnMessageReceivedListener {

    private val appContext = context.applicationContext
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val nodeClient: NodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incomingEvents = MutableSharedFlow<ScoringEventDto>(extraBufferCapacity = 16)
    val incomingEvents: SharedFlow<ScoringEventDto> = _incomingEvents.asSharedFlow()

    private val _wearConnected = MutableStateFlow(false)
    val wearConnected: StateFlow<Boolean> = _wearConnected.asStateFlow()

    fun startListening() {
        messageClient.addListener(this)
        refreshNodes()
    }

    fun stopListening() {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != SyncPaths.PATH_EVENT) return
        val json = messageEvent.data.toString(Charsets.UTF_8)
        Log.d(TAG, "Event from wear: $json")
        runCatching {
            SyncJson.decodeEvent(json)
        }.onSuccess { event ->
            _incomingEvents.tryEmit(event)
        }.onFailure {
            Log.e(TAG, "Failed to decode event", it)
        }
    }

    suspend fun sendState(dto: MatchStateDto) {
        val payload = SyncJson.encodeState(dto).toByteArray(Charsets.UTF_8)
        try {
            val nodes = nodeClient.connectedNodes.await()
            _wearConnected.value = nodes.isNotEmpty()
            for (node in nodes) {
                messageClient.sendMessage(node.id, SyncPaths.PATH_STATE, payload).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "sendState failed (emulator without Play Services is OK)", e)
            _wearConnected.value = false
        }
    }

    private fun refreshNodes() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                _wearConnected.value = nodes.isNotEmpty()
            } catch (_: Exception) {
                _wearConnected.value = false
            }
        }
    }

    companion object {
        private const val TAG = "WearSyncManager"
    }
}
