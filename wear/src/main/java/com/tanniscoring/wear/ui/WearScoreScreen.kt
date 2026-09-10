package com.tanniscoring.wear.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
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
    onStart: () -> Unit,
    onToggleServer: () -> Unit = {},
    onNewMatch: () -> Unit = {},
) {
    val view = LocalView.current
    fun hapticPoint() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
    fun hapticUndo() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WearCourtColors.Black)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!state.matchStarted || state.matchState == null) {
            StartMatchWear(
                onStart = {
                    hapticPoint()
                    onStart()
                },
            )
            return
        }

        val match = state.matchState

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (!match.isMatchOver) {
                                hapticUndo()
                                onUndo()
                            }
                        },
                    ),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PlayerMini(
                        name = match.playerA,
                        sets = match.setsA,
                        games = match.gamesA,
                        points = match.pointDisplayA,
                        isServing = match.server == Side.A && !match.isMatchOver,
                    )
                    PlayerMini(
                        name = match.playerB,
                        sets = match.setsB,
                        games = match.gamesB,
                        points = match.pointDisplayB,
                        isServing = match.server == Side.B && !match.isMatchOver,
                    )
                }
                val status = when {
                    match.isMatchOver -> stringResource(R.string.match_over)
                    match.isTiebreak -> stringResource(R.string.tiebreak)
                    match.isDeuce -> stringResource(R.string.deuce)
                    else -> stringResource(
                        R.string.server_short,
                        if (match.server == Side.A) "A" else "B",
                    )
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = if (match.isTiebreak || !match.isMatchOver) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = when {
                        match.isTiebreak -> WearCourtColors.Serve
                        match.isMatchOver -> WearCourtColors.TextSecondary
                        else -> WearCourtColors.Serve
                    },
                    modifier = Modifier.padding(top = 2.dp),
                )
                val serveText = state.serveWearText
                if (!match.isMatchOver && serveText != null) {
                    Text(
                        text = serveText,
                        style = MaterialTheme.typography.caption2,
                        fontWeight = FontWeight.Bold,
                        color = WearCourtColors.Serve,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!match.isMatchOver) {
                    Text(
                        text = stringResource(R.string.long_press_undo),
                        style = MaterialTheme.typography.caption3,
                        color = WearCourtColors.TextMuted,
                    )
                }
            }

            if (match.isMatchOver) {
                Button(
                    onClick = onNewMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = ButtonDefaults.primaryButtonColors(
                        backgroundColor = WearCourtColors.AccentDim,
                        contentColor = WearCourtColors.TextPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.new_match), fontWeight = FontWeight.Bold)
                }
            } else {
                PointButtons(
                    onPointA = {
                        hapticPoint()
                        onPointA()
                    },
                    onPointB = {
                        hapticPoint()
                        onPointB()
                    },
                    onLongPressUndo = {
                        hapticUndo()
                        onUndo()
                    },
                )
            }
        }
    }
}

@Composable
private fun StartMatchWear(onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.title3,
            fontWeight = FontWeight.Bold,
            color = WearCourtColors.TextPrimary,
        )
        Text(
            text = stringResource(R.string.start_defaults),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption2,
            color = WearCourtColors.TextSecondary,
        )
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = WearCourtColors.Accent,
                contentColor = WearCourtColors.Black,
            ),
        ) {
            Text(
                text = stringResource(R.string.start_match),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
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
    val shape = RoundedCornerShape(10.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(shape)
            .background(
                if (isServing) WearCourtColors.ServeContainer else WearCourtColors.Surface,
            )
            .then(
                if (isServing) {
                    Modifier.border(1.5.dp, WearCourtColors.Serve, shape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isServing) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(WearCourtColors.Serve),
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                color = if (isServing) WearCourtColors.Serve else WearCourtColors.TextPrimary,
            )
        }
        Text(
            text = points,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = WearCourtColors.TextPrimary,
        )
        Text(
            text = "S$sets G$games",
            style = MaterialTheme.typography.caption3,
            color = WearCourtColors.TextSecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PointButtons(
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onLongPressUndo: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(WearCourtColors.Accent)
                .combinedClickable(
                    onClick = onPointA,
                    onLongClick = onLongPressUndo,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.point_a),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.Black,
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(WearCourtColors.SideB)
                .combinedClickable(
                    onClick = onPointB,
                    onLongClick = onLongPressUndo,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.point_b),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.TextPrimary,
            )
        }
    }
}
