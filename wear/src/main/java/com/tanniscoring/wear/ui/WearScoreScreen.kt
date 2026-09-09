package com.tanniscoring.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.tanniscoring.wear.R
import com.tanniscoring.wear.WearUiState

@Composable
fun WearScoreScreen(
    state: WearUiState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val match = state.match
        if (match == null || !state.hasState) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.waiting),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(8.dp),
                )
                Spacer(Modifier.height(8.dp))
                // Still allow tapping to send points once phone starts a match —
                // orphan events are ignored until phone is listening.
                PointButtons(onPointA, onPointB, onUndo, enabled = true)
            }
            return
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Compact scoreboard
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PlayerMini(
                        name = match.playerA,
                        sets = match.setsA,
                        games = match.gamesA,
                        points = match.pointDisplayA,
                    )
                    PlayerMini(
                        name = match.playerB,
                        sets = match.setsB,
                        games = match.gamesB,
                        points = match.pointDisplayB,
                    )
                }
                val status = when {
                    match.isMatchOver -> stringResource(R.string.match_over)
                    match.isTiebreak -> stringResource(R.string.tiebreak)
                    match.isDeuce -> stringResource(R.string.deuce)
                    else -> "${match.setsA}-${match.setsB}  ${match.gamesA}-${match.gamesB}"
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            PointButtons(
                onPointA = onPointA,
                onPointB = onPointB,
                onUndo = onUndo,
                enabled = !match.isMatchOver,
            )
        }
    }
}

@Composable
private fun PlayerMini(
    name: String,
    sets: Int,
    games: Int,
    points: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.caption1,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = points,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "S$sets G$games",
            style = MaterialTheme.typography.caption3,
        )
    }
}

@Composable
private fun PointButtons(
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    enabled: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onPointA,
                enabled = enabled,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.point_a), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onPointB,
                enabled = enabled,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                colors = ButtonDefaults.primaryButtonColors(
                    backgroundColor = MaterialTheme.colors.secondary,
                ),
            ) {
                Text(stringResource(R.string.point_b), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        CompactButton(onClick = onUndo, enabled = enabled) {
            Text(stringResource(R.string.undo), style = MaterialTheme.typography.caption2)
        }
    }
}
