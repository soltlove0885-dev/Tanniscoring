package com.tanniscoring.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tanniscoring.app.R
import com.tanniscoring.shared.MatchHistoryEntry
import com.tanniscoring.shared.MatchMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartMatchScreen(
    playerA: String,
    playerB: String,
    bestOf: Int,
    doubles: Boolean,
    history: List<MatchHistoryEntry>,
    wearConnected: Boolean = false,
    wearNodeCount: Int = 0,
    onPlayerAChange: (String) -> Unit,
    onPlayerBChange: (String) -> Unit,
    onBestOfChange: (Int) -> Unit,
    onDoublesChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onOpenWearApp: () -> Unit = {},
    onInstallWearApp: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.start_match),
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = stringResource(R.string.match_type),
                style = MaterialTheme.typography.titleMedium,
            )
            BestOfOption(
                label = stringResource(R.string.singles),
                selected = !doubles,
                onClick = { onDoublesChange(false) },
            )
            BestOfOption(
                label = stringResource(R.string.doubles),
                selected = doubles,
                onClick = { onDoublesChange(true) },
            )

            OutlinedTextField(
                value = playerA,
                onValueChange = onPlayerAChange,
                label = {
                    Text(
                        if (doubles) stringResource(R.string.team_a)
                        else stringResource(R.string.player_a),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = playerB,
                onValueChange = onPlayerBChange,
                label = {
                    Text(
                        if (doubles) stringResource(R.string.team_b)
                        else stringResource(R.string.player_b),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                text = stringResource(R.string.best_of),
                style = MaterialTheme.typography.titleMedium,
            )
            BestOfOption(
                label = stringResource(R.string.best_of_3),
                selected = bestOf == 3,
                onClick = { onBestOfChange(3) },
            )
            BestOfOption(
                label = stringResource(R.string.best_of_5),
                selected = bestOf == 5,
                onClick = { onBestOfChange(5) },
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.start_match))
            }
            Text(
                text = stringResource(R.string.sync_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (wearConnected) {
                    stringResource(R.string.wear_status_paired_hint)
                } else {
                    stringResource(R.string.wear_disconnected)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (wearConnected && wearNodeCount > 0) {
                Text(
                    text = stringResource(R.string.wear_status_connected_fmt, wearNodeCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            )
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.recent_matches),
                    style = MaterialTheme.typography.titleMedium,
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
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
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

@Composable
private fun BestOfOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
