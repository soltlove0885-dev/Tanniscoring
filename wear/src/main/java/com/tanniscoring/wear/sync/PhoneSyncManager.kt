package com.tanniscoring.wear.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.tanniscoring.shared.MatchStateDto
import com.tanniscoring.shared.ScoringEventDto
import com.tanniscoring.shared.ServeInfoDto
import com.tanniscoring.shared.SyncJson
import com.tanniscoring.shared.SyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Wear-side MessageClient bridge.
 *
 * Phone and Wear share applicationId `com.tanniscoring.app` so Data Layer routes
 * reliably. Wear remains scoring authority.
 *
 * Wear → Phone: full [MatchStateDto] on [SyncPaths.PATH_STATE]
 * Phone → Wear: scoring events on [SyncPaths.PATH_EVENT]
 * Phone → Wear: REQUEST_STATE on [SyncPaths.PATH_REQUEST_STATE]
 */
class PhoneSyncManager private constructor(context: Context) :
    MessageClient.OnMessageReceivedListener {

    private val appContext = context.applicationContext
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val nodeClient: NodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val incomingEvents: SharedFlow<ScoringEventDto> = Companion.incomingEvents
    val stateRequests: SharedFlow<Unit> = Companion.stateRequests
    val incomingServe: SharedFlow<ServeInfoDto> = Companion.incomingServe

    fun startListening() {
        messageClient.addListener(this)
        Log.d(TAG, "startListening MessageClient")
    }

    fun stopListening() {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        handleMessage(messageEvent.path, messageEvent.data)
    }

    /**
     * Broadcast full match state to all connected phone nodes.
     * Retries a few times if no nodes are connected or send fails.
     */
    suspend fun sendState(dto: MatchStateDto, retries: Int = 3) {
        val json = SyncJson.encodeState(dto)
        val payload = json.toByteArray(Charsets.UTF_8)
        var attempt = 0
        while (attempt < retries) {
            attempt++
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w(TAG, "sendState attempt $attempt: no connected phone nodes")
                    if (attempt < retries) delay(400L * attempt)
                    continue
                }
                for (node in nodes) {
                    messageClient.sendMessage(node.id, SyncPaths.PATH_STATE, payload).await()
                }
                Log.d(
                    TAG,
                    "sendState ok nodes=${nodes.size} matchActive=${dto.matchActive} " +
                        "score=${dto.pointDisplayA}-${dto.pointDisplayB} attempt=$attempt",
                )
                return
            } catch (e: Exception) {
                Log.w(TAG, "sendState attempt $attempt failed", e)
                if (attempt < retries) delay(400L * attempt)
            }
        }
        Log.e(TAG, "sendState gave up after $retries attempts")
    }

    companion object {
        private const val TAG = "PhoneSyncManager"

        private val _incomingEvents = MutableSharedFlow<ScoringEventDto>(extraBufferCapacity = 16)
        val incomingEvents: SharedFlow<ScoringEventDto> = _incomingEvents.asSharedFlow()

        private val _stateRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
        val stateRequests: SharedFlow<Unit> = _stateRequests.asSharedFlow()

        private val _incomingServe = MutableSharedFlow<ServeInfoDto>(
            replay = 1,
            extraBufferCapacity = 8,
        )
        val incomingServe: SharedFlow<ServeInfoDto> = _incomingServe.asSharedFlow()

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

        fun handleMessage(path: String, data: ByteArray) {
            when (path) {
                SyncPaths.PATH_EVENT -> {
                    val json = data.toString(Charsets.UTF_8)
                    Log.d(TAG, "Event from phone: $json")
                    runCatching { SyncJson.decodeEvent(json) }
                        .onSuccess { _incomingEvents.tryEmit(it) }
                        .onFailure { Log.e(TAG, "decode event failed", it) }
                }
                SyncPaths.PATH_REQUEST_STATE -> {
                    Log.d(TAG, "REQUEST_STATE from phone")
                    _stateRequests.tryEmit(Unit)
                }
                SyncPaths.PATH_SERVE -> {
                    val json = data.toString(Charsets.UTF_8)
                    Log.d(TAG, "Serve from phone: $json")
                    runCatching { SyncJson.decodeServe(json) }
                        .onSuccess { _incomingServe.tryEmit(it) }
                        .onFailure { Log.e(TAG, "decode serve failed", it) }
                }
                else -> Log.d(TAG, "Ignoring path=$path")
            }
        }
    }
}
