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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Phone-side MessageClient bridge.
 *
 * Phone and Wear share applicationId `com.tanniscoring.app` (Wear module keeps
 * a separate Kotlin namespace). Same package is required for reliable Data Layer
 * delivery on Galaxy Watch / Wear OS.
 *
 * Wear → Phone: full match state on [SyncPaths.PATH_STATE]
 * Phone → Wear: POINT/UNDO (etc.) on [SyncPaths.PATH_EVENT]
 * Phone → Wear: REQUEST_STATE on [SyncPaths.PATH_REQUEST_STATE]
 */
class WearSyncManager private constructor(context: Context) : MessageClient.OnMessageReceivedListener {

    private val appContext = context.applicationContext
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val nodeClient: NodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val incomingState: SharedFlow<MatchStateDto> = Companion.incomingState

    private val _wearConnected = MutableStateFlow(false)
    val wearConnected: StateFlow<Boolean> = _wearConnected.asStateFlow()

    private val _connectedNodeCount = MutableStateFlow(0)
    val connectedNodeCount: StateFlow<Int> = _connectedNodeCount.asStateFlow()

    fun startListening() {
        messageClient.addListener(this)
        refreshNodes()
        Log.d(TAG, "startListening MessageClient")
    }

    fun stopListening() {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        handleMessage(messageEvent.path, messageEvent.data)
    }

    fun handleMessage(path: String, data: ByteArray) {
        when (path) {
            SyncPaths.PATH_STATE -> {
                val json = data.toString(Charsets.UTF_8)
                Log.d(TAG, "State from wear: $json")
                runCatching { SyncJson.decodeState(json) }
                    .onSuccess { _incomingState.tryEmit(it) }
                    .onFailure { Log.e(TAG, "Failed to decode state", it) }
            }
            else -> Log.d(TAG, "Ignoring path=$path")
        }
    }

    /** Ask Wear to re-broadcast current match state. Retries if no nodes. */
    suspend fun requestState(retries: Int = 3) {
        val payload = ByteArray(0)
        var attempt = 0
        while (attempt < retries) {
            attempt++
            try {
                val nodes = nodeClient.connectedNodes.await()
                updateNodeState(nodes.size)
                if (nodes.isEmpty()) {
                    Log.w(TAG, "requestState attempt $attempt: no wear nodes")
                    if (attempt < retries) delay(400L * attempt)
                    continue
                }
                for (node in nodes) {
                    messageClient.sendMessage(node.id, SyncPaths.PATH_REQUEST_STATE, payload).await()
                }
                Log.d(TAG, "REQUEST_STATE sent to ${nodes.size} node(s)")
                return
            } catch (e: Exception) {
                Log.w(TAG, "requestState attempt $attempt failed", e)
                if (attempt < retries) delay(400L * attempt)
            }
        }
    }

    /** Send scoring event to Wear (Wear applies scoring). */
    suspend fun sendEvent(event: ScoringEventDto, retries: Int = 3) {
        val payload = SyncJson.encodeEvent(event).toByteArray(Charsets.UTF_8)
        var attempt = 0
        while (attempt < retries) {
            attempt++
            try {
                val nodes = nodeClient.connectedNodes.await()
                updateNodeState(nodes.size)
                if (nodes.isEmpty()) {
                    Log.w(TAG, "sendEvent attempt $attempt: no wear nodes")
                    if (attempt < retries) delay(400L * attempt)
                    continue
                }
                for (node in nodes) {
                    messageClient.sendMessage(node.id, SyncPaths.PATH_EVENT, payload).await()
                }
                Log.d(TAG, "sendEvent ${event.type} to ${nodes.size} node(s)")
                return
            } catch (e: Exception) {
                Log.w(TAG, "sendEvent attempt $attempt failed", e)
                if (attempt < retries) delay(400L * attempt)
            }
        }
    }

    private fun refreshNodes() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                updateNodeState(nodes.size)
            } catch (_: Exception) {
                updateNodeState(0)
            }
        }
    }

    private fun updateNodeState(count: Int) {
        _connectedNodeCount.value = count
        _wearConnected.value = count > 0
    }

    companion object {
        private const val TAG = "WearSyncManager"

        private val _incomingState = MutableSharedFlow<MatchStateDto>(
            replay = 1,
            extraBufferCapacity = 8,
        )
        val incomingState: SharedFlow<MatchStateDto> = _incomingState.asSharedFlow()

        @Volatile
        private var instance: WearSyncManager? = null

        fun get(context: Context): WearSyncManager {
            return instance ?: synchronized(this) {
                instance ?: WearSyncManager(context.applicationContext).also { instance = it }
            }
        }

        fun handleServiceMessage(messageEvent: MessageEvent) {
            if (messageEvent.path == SyncPaths.PATH_STATE) {
                val json = messageEvent.data.toString(Charsets.UTF_8)
                Log.d(TAG, "Service state from wear: $json")
                runCatching { SyncJson.decodeState(json) }
                    .onSuccess { _incomingState.tryEmit(it) }
                    .onFailure { Log.e(TAG, "Failed to decode state", it) }
            } else {
                instance?.handleMessage(messageEvent.path, messageEvent.data)
            }
        }
    }
}
