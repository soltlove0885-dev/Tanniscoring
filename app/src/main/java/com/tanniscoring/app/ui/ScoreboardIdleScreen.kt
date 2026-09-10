package com.tanniscoring.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tanniscoring.app.R
import com.tanniscoring.shared.MatchHistoryEntry
import com.tanniscoring.shared.MatchMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardIdleScreen(
    history: List<MatchHistoryEntry>,
    wearConnected: Boolean,
    wearNodeCount: Int,
    hasTournament: Boolean = false,
    onRequestState: () -> Unit,
    onOpenWearApp: () -> Unit,
    onInstallWearApp: () -> Unit,
    onTournament: () -> Unit = {},
    onResumeTournament: () -> Unit = {},
) {
    Scaffold(
        containerColor = CourtColors.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CourtColors.NearBlack,
                    titleContentColor = CourtColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.waiting_for_wear),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.waiting_for_wear_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onTournament,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tournament_mode))
            }
            if (hasTournament) {
                OutlinedButton(
                    onClick = onResumeTournament,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.tournament_resume))
                }
            }
            Text(
                text = if (wearConnected) {
                    stringResource(R.string.wear_status_paired_hint)
                } else {
                    stringResource(R.string.wear_disconnected)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (wearConnected && wearNodeCount > 0) {
                Text(
                    text = stringResource(R.string.wear_status_connected_fmt, wearNodeCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onRequestState,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.refresh_wear_state))
            }
            OutlinedButton(
                onClick = onOpenWearApp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.wear_open_app))
            }
            Button(
                onClick = onInstallWearApp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.wear_install_app))
            }
            Text(
                text = stringResource(R.string.wear_auto_install_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (history.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.recent_matches),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                history.take(10).forEach { entry ->
                    HistoryRow(entry)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryRow(entry: MatchHistoryEntry) {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val whenText = fmt.format(Date(entry.finishedAtEpochMs))
    val modeLabel = if (entry.mode == MatchMode.DOUBLES.name) {
        stringResource(R.string.doubles)
    } else {
        stringResource(R.string.singles)
    }
    val sets = entry.setHistory.joinToString(" ") { "${it.gamesA}-${it.gamesB}" }
        .ifBlank { "${entry.setsA}-${entry.setsB}" }
    val winner = when (entry.winner) {
        "A" -> entry.playerA
        "B" -> entry.playerB
        else -> "-"
    }
    Column(modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
        Text(
            text = "${entry.playerA} vs ${entry.playerB}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "$whenText · $modeLabel · $sets · ${stringResource(R.string.winner_fmt, winner)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
