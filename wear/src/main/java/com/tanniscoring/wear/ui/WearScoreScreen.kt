package com.tanniscoring.wear.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.platform.LocalView
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
import com.tanniscoring.shared.Side
import com.tanniscoring.wear.R
import com.tanniscoring.wear.WearUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearScoreScreen(
    state: WearUiState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    onToggleServer: () -> Unit = {},
) {
    val view = LocalView.current
    fun hapticPoint() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val match = state.match
        val active = state.hasState && match != null && match.matchActive

        if (!active) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.waiting),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp),
                )
                Text(
                    text = stringResource(R.string.waiting_hint),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
            return
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (!match!!.isMatchOver) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    onToggleServer()
                                }
                            },
                        ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PlayerMini(
                        name = match!!.playerA,
                        sets = match.setsA,
                        games = match.gamesA,
                        points = match.pointDisplayA,
                        isServing = match.server == Side.A.name && !match.isMatchOver,
                    )
                    PlayerMini(
                        name = match.playerB,
                        sets = match.setsB,
                        games = match.gamesB,
                        points = match.pointDisplayB,
                        isServing = match.server == Side.B.name && !match.isMatchOver,
                    )
                }
                val status = when {
                    match.isMatchOver -> stringResource(R.string.match_over)
                    match.isTiebreak -> stringResource(R.string.tiebreak)
                    match.isDeuce -> stringResource(R.string.deuce)
                    else -> stringResource(
                        R.string.server_short,
                        if (match.server == Side.A.name) "A" else "B",
                    )
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = if (match.isTiebreak) FontWeight.Bold else FontWeight.Normal,
                    color = if (match.isTiebreak) {
                        MaterialTheme.colors.secondary
                    } else {
                        MaterialTheme.colors.onSurface
                    },
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (match.isTiebreak && !match.isMatchOver) {
                    Text(
                        text = stringResource(R.string.tiebreak_full),
                        style = MaterialTheme.typography.caption3,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                    )
                }
            }

            PointButtons(
                onPointA = {
                    hapticPoint()
                    onPointA()
                },
                onPointB = {
                    hapticPoint()
                    onPointB()
                },
                onUndo = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onUndo()
                },
                onToggleServer = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onToggleServer()
                },
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
    isServing: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isServing) {
                Text(
                    text = "●",
                    color = MaterialTheme.colors.secondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = points,
            fontSize = 26.sp,
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
    onToggleServer: () -> Unit,
    enabled: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onPointA,
                enabled = enabled,
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.point_a), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onPointB,
                enabled = enabled,
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                colors = ButtonDefaults.primaryButtonColors(
                    backgroundColor = MaterialTheme.colors.secondary,
                ),
            ) {
                Text(stringResource(R.string.point_b), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactButton(
                onClick = onUndo,
                enabled = enabled,
                modifier = Modifier.size(width = 72.dp, height = 36.dp),
            ) {
                Text(stringResource(R.string.undo), style = MaterialTheme.typography.caption2)
            }
            CompactButton(
                onClick = onToggleServer,
                enabled = enabled,
                modifier = Modifier.size(width = 56.dp, height = 36.dp),
            ) {
                Text(stringResource(R.string.toggle_server), style = MaterialTheme.typography.caption2)
            }
        }
    }
}
