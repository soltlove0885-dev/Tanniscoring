package com.tanniscoring.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tanniscoring.app.ui.CourtColors
import com.tanniscoring.app.ui.MatchScreen
import com.tanniscoring.app.ui.ScoreboardIdleScreen
import com.tanniscoring.app.ui.TanniscoringTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TanniscoringTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CourtColors.Black,
                ) {
                    val ui by viewModel.uiState.collectAsState()
                    val matchActive = ui.matchStarted &&
                        ui.matchState != null &&
                        ui.matchState?.isMatchOver != true

                    LaunchedEffect(matchActive) {
                        if (matchActive) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    if (ui.matchStarted && ui.matchState != null) {
                        MatchScreen(
                            state = ui,
                            onPointA = { viewModel.pointWonA() },
                            onPointB = { viewModel.pointWonB() },
                            onUndo = { viewModel.undo() },
                            onToggleServer = { viewModel.toggleServer() },
                            onEndMatch = { viewModel.endMatch() },
                            onNewMatch = { viewModel.resetToStart() },
                            onRequestState = { viewModel.requestWearState() },
                        )
                    } else {
                        ScoreboardIdleScreen(
                            history = ui.history,
                            wearConnected = ui.wearConnected,
                            wearNodeCount = ui.wearNodeCount,
                            onRequestState = { viewModel.requestWearState() },
                            onOpenWearApp = { viewModel.openWearApp(this@MainActivity) },
                            onInstallWearApp = { viewModel.openWearCompanionStore(this@MainActivity) },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}
