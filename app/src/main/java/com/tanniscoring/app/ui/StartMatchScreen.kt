package com.tanniscoring.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartMatchScreen(
    playerA: String,
    playerB: String,
    bestOf: Int,
    onPlayerAChange: (String) -> Unit,
    onPlayerBChange: (String) -> Unit,
    onBestOfChange: (Int) -> Unit,
    onStart: () -> Unit,
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.start_match),
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = playerA,
                onValueChange = onPlayerAChange,
                label = { Text(stringResource(R.string.player_a)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = playerB,
                onValueChange = onPlayerBChange,
                label = { Text(stringResource(R.string.player_b)) },
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
            Spacer(Modifier.height(8.dp))
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
        }
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
