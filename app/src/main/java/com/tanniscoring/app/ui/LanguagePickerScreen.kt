package com.tanniscoring.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tanniscoring.app.R

@Composable
fun LanguagePickerScreen(
    onChooseKorean: () -> Unit,
    onChooseEnglish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.choose_language),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = CourtColors.TextPrimary,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onChooseKorean,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.language_korean))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onChooseEnglish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.language_english))
        }
    }
}
