package com.tanniscoring.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanniscoring.app.MatchUiState
import com.tanniscoring.app.R
import com.tanniscoring.shared.MatchMode
import com.tanniscoring.shared.Side

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    state: MatchUiState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    onToggleServer: () -> Unit,
    onEndMatch: () -> Unit,
    onNewMatch: () -> Unit,
) {
    val match = state.matchState ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Text(
                        text = if (state.wearConnected) {
                            stringResource(R.string.wear_connected)
                        } else {
                            stringResource(R.string.wear_disconnected)
                        },
                        style = MaterialTheme.typography.labelSmall,
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
            Text(
                text = if (match.mode == MatchMode.DOUBLES) {
                    stringResource(R.string.doubles)
                } else {
                    stringResource(R.string.singles)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(4.dp))

            // Scoreboard header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScoreColumn(
                    name = match.playerA,
                    sets = match.setsA,
                    games = match.gamesA,
                    points = match.pointDisplayA,
                    isServing = match.server == Side.A && !match.isMatchOver,
                    onServerTap = onToggleServer,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Label(stringResource(R.string.sets))
                    Label(stringResource(R.string.games))
                    Label(stringResource(R.string.points))
                }
                ScoreColumn(
                    name = match.playerB,
                    sets = match.setsB,
                    games = match.gamesB,
                    points = match.pointDisplayB,
                    isServing = match.server == Side.B && !match.isMatchOver,
                    onServerTap = onToggleServer,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.server_fmt,
                    if (match.server == Side.A) match.playerA else match.playerB,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !match.isMatchOver, onClick = onToggleServer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text(
                text = stringResource(R.string.tap_to_toggle_server),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            when {
                match.isMatchOver -> {
                    Text(
                        text = stringResource(R.string.match_over),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    val winnerName = when (match.winner) {
                        Side.A -> match.playerA
                        Side.B -> match.playerB
                        null -> "-"
                    }
                    Text(stringResource(R.string.winner_fmt, winnerName))
                }
                match.isTiebreak -> Text(
                    stringResource(R.string.tiebreak),
                    style = MaterialTheme.typography.titleMedium,
                )
                match.isDeuce -> Text(
                    stringResource(R.string.deuce),
                    style = MaterialTheme.typography.titleMedium,
                )
                match.advantageA || match.advantageB -> Text(
                    stringResource(R.string.advantage),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (match.setHistory.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.set_history),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = match.setHistory.mapIndexed { i, s ->
                        "${i + 1}세트 ${s.gamesA}-${s.gamesB}"
                    }.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            if (!match.isMatchOver) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onPointA,
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                    ) {
                        Text(stringResource(R.string.point_a), fontSize = 18.sp)
                    }
                    Button(
                        onClick = onPointB,
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
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
                ) {
                    Text(stringResource(R.string.undo))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onEndMatch,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.end_match))
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNewMatch,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.new_match))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScoreColumn(
    name: String,
    sets: Int,
    games: Int,
    points: String,
    isServing: Boolean,
    onServerTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.clickable(onClick = onServerTap),
        ) {
            if (isServing) {
                Text(
                    text = "● ",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        BigNumber(sets.toString())
        BigNumber(games.toString())
        BigNumber(points)
    }
}

@Composable
private fun BigNumber(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 14.dp),
        style = MaterialTheme.typography.labelMedium,
    )
}
