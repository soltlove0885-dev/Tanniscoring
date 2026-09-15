package com.tanniscoring.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanniscoring.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfUseScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = CourtColors.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terms_of_use)) },
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
        Text(
            text = stringResource(R.string.terms_body),
            color = CourtColors.TextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        )
    }
}
