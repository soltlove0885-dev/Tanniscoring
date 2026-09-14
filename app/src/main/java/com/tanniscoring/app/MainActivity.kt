package com.tanniscoring.app

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tanniscoring.app.ui.BadmintonIdleScreen
import com.tanniscoring.app.ui.BadmintonMatchScreen
import com.tanniscoring.app.ui.CourtColors
import com.tanniscoring.app.ui.LanguagePickerScreen
import com.tanniscoring.app.ui.MatchScreen
import com.tanniscoring.app.ui.ScoreboardIdleScreen
import com.tanniscoring.app.ui.SportPickerScreen
import com.tanniscoring.app.ui.TanniscoringTheme
import com.tanniscoring.app.ui.TournamentBracketScreen
import com.tanniscoring.app.ui.TournamentSetupScreen
import com.tanniscoring.shared.SportType

class MainActivity : AppCompatActivity() {

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
                        (
                            (ui.matchState != null && ui.matchState?.isMatchOver != true) ||
                                (ui.badmintonState != null && ui.badmintonState?.isMatchOver != true)
                            ) &&
                        (
                            ui.screen == PhoneScreen.MATCH_SCOREBOARD ||
                                ui.screen == PhoneScreen.BADMINTON_SCOREBOARD ||
                                (ui.screen == PhoneScreen.IDLE && ui.matchStarted)
                            )

                    LaunchedEffect(matchActive) {
                        if (matchActive) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    when {
                        ui.screen == PhoneScreen.LANGUAGE -> {
                            LanguagePickerScreen(
                                onChooseKorean = { viewModel.chooseLanguage("ko") },
                                onChooseEnglish = { viewModel.chooseLanguage("en") },
                            )
                        }
                        ui.screen == PhoneScreen.SPORT_PICKER -> {
                            SportPickerScreen(
                                onTennis = { viewModel.selectSport(SportType.TENNIS) },
                                onBadminton = { viewModel.selectSport(SportType.BADMINTON) },
                                onChangeLanguage = { viewModel.showLanguagePicker() },
                            )
                        }
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
                        ui.screen == PhoneScreen.BADMINTON_SCOREBOARD &&
                            ui.matchStarted && ui.badmintonState != null -> {
                            BadmintonMatchScreen(
                                state = ui,
                                onPointA = { viewModel.pointWonA() },
                                onPointB = { viewModel.pointWonB() },
                                onUndo = { viewModel.undo() },
                                onEndMatch = { viewModel.endMatch() },
                                onNewMatch = { viewModel.resetToStart() },
                                onRequestState = { viewModel.requestWearState() },
                                onBackToSports = { viewModel.showSportPicker() },
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
                        ui.screen == PhoneScreen.BADMINTON_IDLE -> {
                            BadmintonIdleScreen(
                                wearConnected = ui.wearConnected,
                                wearNodeCount = ui.wearNodeCount,
                                onRequestState = { viewModel.requestWearState() },
                                onOpenWearApp = { viewModel.openWearApp(this@MainActivity) },
                                onInstallWearApp = { viewModel.openWearCompanionStore(this@MainActivity) },
                                onBackToSports = { viewModel.showSportPicker() },
                                onChangeLanguage = { viewModel.showLanguagePicker() },
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
                                onBackToSports = { viewModel.showSportPicker() },
                                onChangeLanguage = { viewModel.showLanguagePicker() },
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
