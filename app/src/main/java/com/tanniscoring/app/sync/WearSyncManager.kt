package com.tanniscoring.app.sync

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
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
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Phone-side Wearable Data Layer bridge.
 * - Listens for scoring events from Wear on [SyncPaths.PATH_EVENT]
 * - Listens for state requests on [SyncPaths.PATH_REQUEST_STATE]
 * - Broadcasts full match state via DataClient ([SyncPaths.PATH_STATE]) + MessageClient
 *
 * Note: [wearConnected] / [connectedNodeCount] reflect Wear OS **nodes** reachable
 * via the Data Layer — they do **not** mean `com.tanniscoring.wear` is installed.
 *
 * MessageClient alone is unreliable on Samsung Wear (messages sent before a listener
 * is registered are lost). DataClient PutDataMapRequest + WearableListenerService
 * keep state durable and deliverable when Wear is on the waiting screen.
 */
class WearSyncManager private constructor(context: Context) : MessageClient.OnMessageReceivedListener {

    private val appContext = context.applicationContext
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val dataClient: DataClient = Wearable.getDataClient(appContext)
    private val nodeClient: NodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incomingEvents = MutableSharedFlow<ScoringEventDto>(extraBufferCapacity = 16)
    val incomingEvents: SharedFlow<ScoringEventDto> = _incomingEvents.asSharedFlow()

    private val _stateRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val stateRequests: SharedFlow<Unit> = _stateRequests.asSharedFlow()

    private val _wearConnected = MutableStateFlow(false)
    val wearConnected: StateFlow<Boolean> = _wearConnected.asStateFlow()

    private val _connectedNodeCount = MutableStateFlow(0)
    val connectedNodeCount: StateFlow<Int> = _connectedNodeCount.asStateFlow()

    fun startListening() {
        messageClient.addListener(this)
        refreshNodes()
    }

    fun stopListening() {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        handleMessage(messageEvent.path, messageEvent.data)
    }

    fun handleMessage(path: String, data: ByteArray) {
        when (path) {
            SyncPaths.PATH_EVENT -> {
                val json = data.toString(Charsets.UTF_8)
                Log.d(TAG, "Event from wear: $json")
                runCatching { SyncJson.decodeEvent(json) }
                    .onSuccess { _incomingEvents.tryEmit(it) }
                    .onFailure { Log.e(TAG, "Failed to decode event", it) }
            }
            SyncPaths.PATH_REQUEST_STATE -> {
                Log.d(TAG, "REQUEST_STATE from wear")
                _stateRequests.tryEmit(Unit)
            }
        }
    }

    /**
     * Primary: urgent Data Layer item at [SyncPaths.PATH_STATE] (key "json").
     * Secondary: MessageClient broadcast to connected nodes.
     */
    suspend fun sendState(dto: MatchStateDto) {
        val json = SyncJson.encodeState(dto)
        val payload = json.toByteArray(Charsets.UTF_8)
        try {
            val putRequest = PutDataMapRequest.create(SyncPaths.PATH_STATE).apply {
                dataMap.putString(KEY_JSON, json)
                // Force a change notification even if payload is identical (re-sync).
                dataMap.putLong(KEY_TS, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putRequest).await()
            Log.d(TAG, "putDataItem state ok (matchActive=${dto.matchActive})")
        } catch (e: Exception) {
            Log.w(TAG, "putDataItem failed (emulator without Play Services is OK)", e)
        }
        try {
            val nodes = nodeClient.connectedNodes.await()
            updateNodeState(nodes.size)
            for (node in nodes) {
                messageClient.sendMessage(node.id, SyncPaths.PATH_STATE, payload).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "sendState MessageClient failed", e)
            updateNodeState(0)
        }
    }

    /**
     * Prefer opening Play Store details for [WEAR_PACKAGE] on connected Wear nodes via
     * [RemoteActivityHelper]. Falls back to the phone Play Store so the user can pick a watch.
     */
    suspend fun openWearCompanionStore(activityContext: Context) {
        val marketIntent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(MARKET_URI))

        val nodes = try {
            nodeClient.connectedNodes.await()
        } catch (e: Exception) {
            Log.w(TAG, "connectedNodes failed", e)
            emptyList()
        }
        updateNodeState(nodes.size)

        if (nodes.isNotEmpty()) {
            val openedRemote = tryOpenRemotePlayStore(marketIntent, nodes.map { it.id })
            if (openedRemote) return
        }
        openPhonePlayStoreFallback(activityContext)
    }

    private suspend fun tryOpenRemotePlayStore(
        marketIntent: Intent,
        nodeIds: List<String>,
    ): Boolean = withContext(Dispatchers.IO) {
        val helper = RemoteActivityHelper(appContext)
        val attempts = listOf<String?>(null) + nodeIds
        for (nodeId in attempts) {
            try {
                val future = helper.startRemoteActivity(marketIntent, nodeId)
                future.get(15, TimeUnit.SECONDS)
                Log.d(TAG, "Remote Play Store opened (nodeId=$nodeId)")
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "startRemoteActivity failed (nodeId=$nodeId)", e)
            }
        }
        false
    }

    private fun openPhonePlayStoreFallback(activityContext: Context) {
        val ctx = activityContext
        val market = Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val https = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$WEAR_PACKAGE"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            try {
                ctx.startActivity(https)
            } catch (e: Exception) {
                Log.e(TAG, "Could not open Play Store for wear companion", e)
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
        const val WEAR_PACKAGE = "com.tanniscoring.wear"
        private const val MARKET_URI = "market://details?id=$WEAR_PACKAGE"
        const val KEY_JSON = "json"
        const val KEY_TS = "ts"

        @Volatile
        private var instance: WearSyncManager? = null

        fun get(context: Context): WearSyncManager {
            return instance ?: synchronized(this) {
                instance ?: WearSyncManager(context.applicationContext).also { instance = it }
            }
        }

        /** Called from [TanniscoringWearListenerService] when the Activity/ViewModel may be gone. */
        fun handleServiceMessage(messageEvent: MessageEvent) {
            val mgr = instance
            if (mgr != null) {
                mgr.handleMessage(messageEvent.path, messageEvent.data)
            } else {
                Log.d(TAG, "Service message ${messageEvent.path} with no WearSyncManager yet")
            }
        }
    }
}
