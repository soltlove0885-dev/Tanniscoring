package com.tanniscoring.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanniscoring.app.MatchUiState
import com.tanniscoring.app.R
import com.tanniscoring.shared.BadmintonMatchState
import com.tanniscoring.shared.Side

@Composable
fun BadmintonMatchScreen(
    state: MatchUiState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    onEndMatch: () -> Unit,
    onNewMatch: () -> Unit,
    onRequestState: () -> Unit,
    onBackToSports: () -> Unit,
) {
    val match = state.badmintonState ?: return
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        BadmintonLandscape(state, match)
    } else {
        BadmintonPortrait(
            state = state,
            match = match,
            onPointA = onPointA,
            onPointB = onPointB,
            onUndo = onUndo,
            onEndMatch = onEndMatch,
            onNewMatch = onNewMatch,
            onRequestState = onRequestState,
            onBackToSports = onBackToSports,
        )
    }
}

@Composable
private fun BadmintonLandscape(state: MatchUiState, match: BadmintonMatchState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtColors.Black)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadmintonSide(
                name = match.playerA,
                points = match.pointsA.toString(),
                accent = CourtColors.Accent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Text(
                text = stringResource(R.string.points),
                color = CourtColors.TextMuted,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            BadmintonSide(
                name = match.playerB,
                points = match.pointsB.toString(),
                accent = CourtColors.Serve,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (match.isMatchOver) {
                Text(
                    stringResource(R.string.badminton_game_over),
                    color = CourtColors.Danger,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = if (state.wearConnected) {
                    stringResource(R.string.wear_connected)
                } else {
                    stringResource(R.string.wear_disconnected)
                },
                color = CourtColors.TextMuted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun BadmintonSide(
    name: String,
    points: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .padding(8.dp)
            .clip(shape)
            .background(CourtColors.Surface)
            .border(2.dp, accent, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = name,
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = points,
            color = CourtColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 96.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadmintonPortrait(
    state: MatchUiState,
    match: BadmintonMatchState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    onEndMatch: () -> Unit,
    onNewMatch: () -> Unit,
    onRequestState: () -> Unit,
    onBackToSports: () -> Unit,
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
                actions = {
                    Text(
                        text = if (state.wearConnected) {
                            stringResource(R.string.wear_connected)
                        } else {
                            stringResource(R.string.wear_disconnected)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = CourtColors.TextSecondary,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.scoringFromWear) {
                Text(
                    text = stringResource(R.string.scoring_from_wear),
                    style = MaterialTheme.typography.labelLarge,
                    color = CourtColors.Serve,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CourtColors.ServeContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = stringResource(R.string.badminton_rules_hint),
                style = MaterialTheme.typography.bodySmall,
                color = CourtColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BadmintonSide(
                    name = match.playerA,
                    points = match.pointsA.toString(),
                    accent = CourtColors.Accent,
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp),
                )
                BadmintonSide(
                    name = match.playerB,
                    points = match.pointsB.toString(),
                    accent = CourtColors.Serve,
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            if (match.isMatchOver) {
                val winnerName = when (match.winner) {
                    Side.A -> match.playerA
                    Side.B -> match.playerB
                    null -> "-"
                }
                Text(
                    stringResource(R.string.badminton_game_over),
                    style = MaterialTheme.typography.headlineSmall,
                    color = CourtColors.Danger,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.winner_fmt, winnerName), color = CourtColors.TextPrimary)
            } else {
                Text(
                    text = stringResource(R.string.phone_mirror_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = CourtColors.TextMuted,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onPointA,
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CourtColors.AccentDim,
                            contentColor = CourtColors.TextPrimary,
                        ),
                    ) {
                        Text(stringResource(R.string.point_a), fontSize = 18.sp)
                    }
                    Button(
                        onClick = onPointB,
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CourtColors.ServeDim,
                            contentColor = CourtColors.TextPrimary,
                        ),
                    ) {
                        Text(stringResource(R.string.point_b), fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onUndo,
                    enabled = state.canUndo,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CourtColors.Border),
                ) {
                    Text(stringResource(R.string.undo), color = CourtColors.TextSecondary)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onEndMatch,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CourtColors.Border),
                ) {
                    Text(stringResource(R.string.end_match), color = CourtColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNewMatch,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, CourtColors.Border),
            ) {
                Text(stringResource(R.string.new_match), color = CourtColors.TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRequestState,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, CourtColors.Border),
            ) {
                Text(stringResource(R.string.refresh_wear_state), color = CourtColors.TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBackToSports,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, CourtColors.Border),
            ) {
                Text(stringResource(R.string.back_to_sports), color = CourtColors.Serve)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
