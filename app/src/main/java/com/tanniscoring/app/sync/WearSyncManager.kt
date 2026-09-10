package com.tanniscoring.app.sync

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.tanniscoring.app.R
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



    /** Push phone-side serve speed / label flash to Wear (optional small text). */
    suspend fun sendServe(dto: ServeInfoDto, retries: Int = 2) {
        val payload = SyncJson.encodeServe(dto).toByteArray(Charsets.UTF_8)
        var attempt = 0
        while (attempt < retries) {
            attempt++
            try {
                val nodes = nodeClient.connectedNodes.await()
                updateNodeState(nodes.size)
                if (nodes.isEmpty()) {
                    if (attempt < retries) delay(300L * attempt)
                    continue
                }
                for (node in nodes) {
                    messageClient.sendMessage(node.id, SyncPaths.PATH_SERVE, payload).await()
                }
                Log.d(TAG, "sendServe label=${dto.label} speed=${dto.speedKmH} nodes=${nodes.size}")
                return
            } catch (e: Exception) {
                Log.w(TAG, "sendServe attempt $attempt failed", e)
                if (attempt < retries) delay(300L * attempt)
            }
        }
    }

    /**
     * Launch Wear [MainActivity] on connected nodes (same applicationId).
     * Uses RemoteActivityHelper; no-ops gracefully if no nodes / app missing.
     */
    suspend fun openWearApp(activityContext: Context) {
        val launchIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(APP_PACKAGE, WEAR_MAIN_ACTIVITY))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val nodes = connectedNodeIds()
        if (nodes.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    activityContext,
                    activityContext.getString(
                        R.string.wear_open_need_connection,
                    ),
                    Toast.LENGTH_LONG,
                ).show()
            }
            return
        }
        val opened = startRemoteOnNodes(launchIntent, nodes)
        if (!opened) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    activityContext,
                    activityContext.getString(
                        R.string.wear_open_failed_hint,
                    ),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    /**
     * Open Play Store details for [APP_PACKAGE] on the watch via RemoteActivityHelper.
     * Falls back to phone Play + toast hint to use 「웨어러블에 설치」 / watch Play.
     */
    suspend fun openWearCompanionStore(activityContext: Context) {
        val marketIntent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(MARKET_URI))

        val nodes = connectedNodeIds()
        if (nodes.isNotEmpty()) {
            val openedRemote = startRemoteOnNodes(marketIntent, nodes)
            if (openedRemote) return
        }
        openPhonePlayStoreFallback(activityContext)
    }

    private suspend fun connectedNodeIds(): List<String> {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            updateNodeState(nodes.size)
            nodes.map { it.id }
        } catch (e: Exception) {
            Log.w(TAG, "connectedNodes failed", e)
            updateNodeState(0)
            emptyList()
        }
    }

    private suspend fun startRemoteOnNodes(
        intent: Intent,
        nodeIds: List<String>,
    ): Boolean = withContext(Dispatchers.IO) {
        val helper = RemoteActivityHelper(appContext)
        // null targetNodeId → all connected watches; also try each node explicitly.
        val attempts = listOf<String?>(null) + nodeIds
        for (nodeId in attempts) {
            try {
                val future = helper.startRemoteActivity(intent, nodeId)
                future.get(15, TimeUnit.SECONDS)
                Log.d(TAG, "startRemoteActivity ok (nodeId=$nodeId action=${intent.action})")
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "startRemoteActivity failed (nodeId=$nodeId)", e)
            }
        }
        false
    }

    private fun openPhonePlayStoreFallback(activityContext: Context) {
        val ctx = activityContext
        withContextToastHint(ctx)
        val market = Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val https = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$APP_PACKAGE"),
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

    private fun withContextToastHint(ctx: Context) {
        // Prefer main-thread Toast; callers may be on IO after remote fail.
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.wear_install_phone_fallback_hint),
                    Toast.LENGTH_LONG,
                ).show()
            }
        } catch (e: Exception) {
            Log.w(TAG, "toast hint failed", e)
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
        /** Same applicationId on phone + wear. */
        const val APP_PACKAGE = "com.tanniscoring.app"
        private const val WEAR_MAIN_ACTIVITY = "com.tanniscoring.wear.MainActivity"
        private const val MARKET_URI = "market://details?id=$APP_PACKAGE"

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
