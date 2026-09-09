package com.tanniscoring.app.sync

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
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
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Phone-side Wearable Data Layer bridge.
 * - Listens for scoring events from Wear on [SyncPaths.PATH_EVENT]
 * - Broadcasts full match state to Wear on [SyncPaths.PATH_STATE]
 *
 * Note: [wearConnected] / [connectedNodeCount] reflect Wear OS **nodes** reachable
 * via the Data Layer — they do **not** mean `com.tanniscoring.wear` is installed.
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
            updateNodeState(nodes.size)
            for (node in nodes) {
                messageClient.sendMessage(node.id, SyncPaths.PATH_STATE, payload).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "sendState failed (emulator without Play Services is OK)", e)
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
        // From phone with null targetNodeId → all connected watches.
        // Also try each node explicitly if the broadcast-style call fails.
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
    }
}
