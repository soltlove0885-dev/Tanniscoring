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
import androidx.compose.ui.unit.sp
import com.tanniscoring.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportPickerScreen(
    onTennis: () -> Unit,
    onBadminton: () -> Unit,
    onChangeLanguage: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AutoSizeText(
                text = stringResource(R.string.choose_sport),
                fontSize = 22.sp,
                minFontSize = 16.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                color = CourtColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onTennis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                AutoSizeText(text = stringResource(R.string.sport_tennis), fontSize = 16.sp, minFontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onBadminton,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                AutoSizeText(text = stringResource(R.string.sport_badminton), fontSize = 16.sp, minFontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onChangeLanguage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AutoSizeText(text = stringResource(R.string.change_language), fontSize = 14.sp, minFontSize = 11.sp)
            }
        }
    }
}
