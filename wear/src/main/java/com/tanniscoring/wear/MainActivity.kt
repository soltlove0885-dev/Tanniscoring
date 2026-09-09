package com.tanniscoring.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.material.MaterialTheme
import com.tanniscoring.wear.ui.WearScoreScreen

class MainActivity : ComponentActivity() {

    private val viewModel: WearMatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val ui by viewModel.uiState.collectAsState()
                WearScoreScreen(
                    state = ui,
                    onPointA = { viewModel.pointWonA() },
                    onPointB = { viewModel.pointWonB() },
                    onUndo = { viewModel.undo() },
                    onStart = { viewModel.startMatch() },
                    onToggleServer = { viewModel.toggleServer() },
                    onNewMatch = { viewModel.resetToStart() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.resyncToPhone()
    }
}

