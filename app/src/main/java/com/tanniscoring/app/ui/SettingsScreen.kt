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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tanniscoring.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    versionName: String,
    onTermsOfUse: () -> Unit,
    onChangeLanguage: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = CourtColors.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onTermsOfUse,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.terms_of_use))
            }
            OutlinedButton(
                onClick = onChangeLanguage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.change_language))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_about),
                color = CourtColors.TextSecondary,
            )
            Text(
                text = stringResource(R.string.app_version_fmt, versionName),
                color = CourtColors.TextMuted,
            )
            Text(
                text = stringResource(R.string.terms_last_updated),
                color = CourtColors.TextMuted,
            )
        }
    }
}
