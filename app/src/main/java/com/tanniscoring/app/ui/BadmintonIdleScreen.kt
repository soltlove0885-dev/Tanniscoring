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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadmintonIdleScreen(
    wearConnected: Boolean,
    wearNodeCount: Int,
    onRequestState: () -> Unit,
    onOpenWearApp: () -> Unit,
    onInstallWearApp: () -> Unit,
    onBackToSports: () -> Unit,
    onChangeLanguage: () -> Unit,
    onSettings: () -> Unit = {},
) {
    Scaffold(
        containerColor = CourtColors.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sport_badminton)) },
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
                text = stringResource(R.string.badminton_waiting),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.badminton_waiting_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.badminton_rules_hint),
                style = MaterialTheme.typography.bodySmall,
                color = CourtColors.Accent,
                textAlign = TextAlign.Center,
            )
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
            OutlinedButton(onClick = onRequestState, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.refresh_wear_state))
            }
            OutlinedButton(onClick = onOpenWearApp, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wear_open_app))
            }
            Button(onClick = onInstallWearApp, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wear_install_app))
            }
            OutlinedButton(onClick = onBackToSports, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.back_to_sports))
            }
            OutlinedButton(onClick = onChangeLanguage, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.change_language))
            }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
