package com.tanniscoring.wear

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tanniscoring.shared.SportType
import com.tanniscoring.wear.ui.TanniscoringWearTheme
import com.tanniscoring.wear.ui.WearScoreScreen

class MainActivity : AppCompatActivity() {

    private val viewModel: WearMatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TanniscoringWearTheme {
                val ui by viewModel.uiState.collectAsState()
                val matchActive = ui.matchStarted &&
                    (
                        (ui.matchState != null && ui.matchState?.isMatchOver != true) ||
                            (ui.badmintonState != null && ui.badmintonState?.isMatchOver != true)
                        )

                LaunchedEffect(matchActive) {
                    if (matchActive) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                WearScoreScreen(
                    state = ui,
                    onPointA = { viewModel.pointWonA() },
                    onPointB = { viewModel.pointWonB() },
                    onUndo = { viewModel.undo() },
                    onStart = { viewModel.startMatch() },
                    onToggleServer = { viewModel.toggleServer() },
                    onNewMatch = { viewModel.resetToStart() },
                    onEndMatch = { viewModel.exitToIdle() },
                    onToggleNoAd = { viewModel.toggleDraftNoAd() },
                    onChooseKorean = { viewModel.chooseLanguage("ko") },
                    onChooseEnglish = { viewModel.chooseLanguage("en") },
                    onSelectTennis = { viewModel.selectSport(SportType.TENNIS) },
                    onSelectBadminton = { viewModel.selectSport(SportType.BADMINTON) },
                    onShowSportPicker = { viewModel.showSportPicker() },
                    onShowLanguage = { viewModel.showLanguagePicker() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.resyncToPhone()
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}
