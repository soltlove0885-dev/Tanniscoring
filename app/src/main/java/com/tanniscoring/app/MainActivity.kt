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
import com.tanniscoring.app.ui.TournamentBracketScreen
import com.tanniscoring.app.ui.TournamentSetupScreen

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
                        ui.matchState?.isMatchOver != true &&
                        (ui.screen == PhoneScreen.MATCH_SCOREBOARD ||
                            (ui.screen == PhoneScreen.IDLE && ui.matchStarted))

                    LaunchedEffect(matchActive) {
                        if (matchActive) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    when {
                        ui.screen == PhoneScreen.TOURNAMENT_SETUP -> {
                            TournamentSetupScreen(
                                playerCount = ui.draftPlayerCount,
                                playerNames = ui.draftPlayerNames,
                                bestOf = ui.draftBestOf,
                                noAd = ui.draftNoAd,
                                onPlayerCountChange = viewModel::setDraftPlayerCount,
                                onPlayerNameChange = viewModel::setDraftPlayerName,
                                onBestOfChange = viewModel::setDraftBestOf,
                                onNoAdChange = viewModel::setDraftNoAd,
                                onCreate = { viewModel.createTournament() },
                                onCancel = { viewModel.cancelTournamentSetup() },
                            )
                        }
                        ui.screen == PhoneScreen.TOURNAMENT_BRACKET && ui.tournament != null -> {
                            TournamentBracketScreen(
                                tournament = ui.tournament!!,
                                wearConnected = ui.wearConnected,
                                onSelectMatch = viewModel::selectBracketMatch,
                                onOpenScoreboard = { viewModel.openActiveScoreboard() },
                                onCycleMatchBestOf = viewModel::cycleMatchBestOf,
                                onEndTournament = { viewModel.endTournament() },
                                onBack = { viewModel.showIdle() },
                            )
                        }
                        (ui.screen == PhoneScreen.MATCH_SCOREBOARD || ui.screen == PhoneScreen.IDLE) &&
                            ui.matchStarted && ui.matchState != null -> {
                            MatchScreen(
                                state = ui,
                                onPointA = { viewModel.pointWonA() },
                                onPointB = { viewModel.pointWonB() },
                                onUndo = { viewModel.undo() },
                                onToggleServer = { viewModel.toggleServer() },
                                onEndMatch = { viewModel.endMatch() },
                                onNewMatch = { viewModel.resetToStart() },
                                onRequestState = { viewModel.requestWearState() },
                                onBackToBracket = if (ui.tournament != null) {
                                    { viewModel.showBracket() }
                                } else {
                                    null
                                },
                            )
                        }
                        else -> {
                            ScoreboardIdleScreen(
                                history = ui.history,
                                wearConnected = ui.wearConnected,
                                wearNodeCount = ui.wearNodeCount,
                                hasTournament = ui.tournament != null,
                                onRequestState = { viewModel.requestWearState() },
                                onOpenWearApp = { viewModel.openWearApp(this@MainActivity) },
                                onInstallWearApp = { viewModel.openWearCompanionStore(this@MainActivity) },
                                onTournament = { viewModel.openTournamentSetup() },
                                onResumeTournament = { viewModel.showBracket() },
                            )
                        }
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
