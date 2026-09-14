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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tanniscoring.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentSetupScreen(
    playerCount: Int,
    playerNames: List<String>,
    bestOf: Int,
    noAd: Boolean = false,
    onPlayerCountChange: (Int) -> Unit,
    onPlayerNameChange: (Int, String) -> Unit,
    onBestOfChange: (Int) -> Unit,
    onNoAdChange: (Boolean) -> Unit = {},
    onCreate: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        containerColor = CourtColors.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tournament_setup)) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tournament_player_count),
                style = MaterialTheme.typography.titleMedium,
                color = CourtColors.TextPrimary,
            )
            RadioRow(
                label = stringResource(R.string.tournament_4_players),
                selected = playerCount == 4,
                onClick = { onPlayerCountChange(4) },
            )
            RadioRow(
                label = stringResource(R.string.tournament_8_players),
                selected = playerCount == 8,
                onClick = { onPlayerCountChange(8) },
            )

            Text(
                text = stringResource(R.string.tournament_best_of),
                style = MaterialTheme.typography.titleMedium,
                color = CourtColors.TextPrimary,
            )
            RadioRow(
                label = stringResource(R.string.best_of_1),
                selected = bestOf == 1,
                onClick = { onBestOfChange(1) },
            )
            RadioRow(
                label = stringResource(R.string.best_of_3),
                selected = bestOf == 3,
                onClick = { onBestOfChange(3) },
            )
            RadioRow(
                label = stringResource(R.string.best_of_5),
                selected = bestOf == 5,
                onClick = { onBestOfChange(5) },
            )

            Text(
                text = stringResource(R.string.no_ad_title),
                style = MaterialTheme.typography.titleMedium,
                color = CourtColors.TextPrimary,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.no_ad_label),
                        color = CourtColors.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.no_ad_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = CourtColors.TextSecondary,
                    )
                }
                Switch(checked = noAd, onCheckedChange = onNoAdChange)
            }

            Text(
                text = stringResource(R.string.tournament_enter_names),
                style = MaterialTheme.typography.titleMedium,
                color = CourtColors.TextPrimary,
            )
            playerNames.take(playerCount).forEachIndexed { index, name ->
                OutlinedTextField(
                    value = name,
                    onValueChange = { onPlayerNameChange(index, it) },
                    label = { Text(stringResource(R.string.tournament_player_n, index + 1)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                enabled = playerNames.take(playerCount).all { it.isNotBlank() },
            ) {
                Text(stringResource(R.string.tournament_create))
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            color = CourtColors.TextPrimary,
        )
    }
}
