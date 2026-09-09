package com.tanniscoring.wear.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Wear → Phone: scoring events on [SyncPaths.PATH_EVENT]
 * Wear → Phone: request current state on [SyncPaths.PATH_REQUEST_STATE]
 * Phone → Wear: full state via DataClient + MessageClient on [SyncPaths.PATH_STATE]
 *
 * Uses a process-wide [incomingState] SharedFlow so [TanniscoringPhoneListenerService]
 * and the foreground [PhoneSyncManager] instance both update the waiting-screen UI.
 */
class PhoneSyncManager private constructor(context: Context) :
    MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {

    private val appContext = context.applicationContext
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val dataClient: DataClient = Wearable.getDataClient(appContext)
    private val nodeClient: NodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val incomingState: SharedFlow<MatchStateDto> = Companion.incomingState

    fun startListening() {
        messageClient.addListener(this)
        dataClient.addListener(this)
        // Pull any already-synced Data Layer item (survives missed MessageClient sends).
        scope.launch { readExistingStateDataItem() }
    }

    fun stopListening() {
        messageClient.removeListener(this)
        dataClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        handleMessage(messageEvent.path, messageEvent.data)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            handleDataChanged(dataEvents)
        } finally {
            dataEvents.release()
        }
    }

    /** Ask the phone to push the current match state (Data + Message). */
    suspend fun requestState() {
        val payload = ByteArray(0)
        try {
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                messageClient.sendMessage(node.id, SyncPaths.PATH_REQUEST_STATE, payload).await()
            }
            if (nodes.isEmpty()) {
                Log.w(TAG, "requestState: no connected phone node")
            } else {
                Log.d(TAG, "REQUEST_STATE sent to ${nodes.size} node(s)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "requestState failed", e)
        }
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

    private suspend fun readExistingStateDataItem() {
        try {
            val uri = Uri.parse("wear://*" + SyncPaths.PATH_STATE)
            val buffer = dataClient.getDataItems(uri).await()
            try {
                for (item in buffer) {
                    if (item.uri.path == SyncPaths.PATH_STATE) {
                        val json = DataMapItem.fromDataItem(item).dataMap.getString(KEY_JSON)
                        if (!json.isNullOrBlank()) {
                            emitStateJson(json, "existing-dataitem")
                        }
                    }
                }
            } finally {
                buffer.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "readExistingStateDataItem failed", e)
        }
    }

    companion object {
        private const val TAG = "PhoneSyncManager"
        const val KEY_JSON = "json"

        private val _incomingState = MutableSharedFlow<MatchStateDto>(
            replay = 1,
            extraBufferCapacity = 8,
        )
        val incomingState: SharedFlow<MatchStateDto> = _incomingState.asSharedFlow()

        @Volatile
        private var instance: PhoneSyncManager? = null

        fun get(context: Context): PhoneSyncManager {
            return instance ?: synchronized(this) {
                instance ?: PhoneSyncManager(context.applicationContext).also { instance = it }
            }
        }

        fun handleServiceMessage(messageEvent: MessageEvent) {
            handleMessage(messageEvent.path, messageEvent.data)
        }

        fun handleServiceDataChanged(dataEvents: DataEventBuffer) {
            // WearableListenerService releases the buffer after onDataChanged returns.
            handleDataChanged(dataEvents)
        }

        fun handleMessage(path: String, data: ByteArray) {
            if (path != SyncPaths.PATH_STATE) return
            val json = data.toString(Charsets.UTF_8)
            emitStateJson(json, "message")
        }

        fun handleDataChanged(dataEvents: DataEventBuffer) {
            try {
                for (event in dataEvents) {
                    if (event.type != DataEvent.TYPE_CHANGED) continue
                    val item = event.dataItem
                    if (item.uri.path != SyncPaths.PATH_STATE) continue
                    val json = DataMapItem.fromDataItem(item).dataMap.getString(KEY_JSON)
                    if (!json.isNullOrBlank()) {
                        emitStateJson(json, "data")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleDataChanged failed", e)
            }
        }

        private fun emitStateJson(json: String, source: String) {
            Log.d(TAG, "State from phone ($source): $json")
            runCatching { SyncJson.decodeState(json) }
                .onSuccess { _incomingState.tryEmit(it) }
                .onFailure { Log.e(TAG, "decode state failed", it) }
        }
    }
}
