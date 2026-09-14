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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onEndMatch: () -> Unit = {},
    onToggleNoAd: () -> Unit = {},
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
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!state.matchStarted || state.matchState == null) {
            StartMatchWear(
                noAd = state.draftNoAd,
                onToggleNoAd = onToggleNoAd,
                onStart = {
                    hapticPoint()
                    onStart()
                },
            )
            return
        }

        val match = state.matchState
        var confirmEnd by remember { mutableStateOf(false) }

        if (confirmEnd) {
            EndMatchConfirm(
                onConfirm = {
                    hapticUndo()
                    confirmEnd = false
                    onEndMatch()
                },
                onCancel = { confirmEnd = false },
            )
            return
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Title / meta — long-press to end match (not adjacent to score taps)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (!match.isMatchOver) {
                                hapticUndo()
                                confirmEnd = true
                            }
                        },
                    )
                    .padding(top = 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.caption2,
                    fontWeight = FontWeight.SemiBold,
                    color = WearCourtColors.TextMuted,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "S ${match.setsA}-${match.setsB}",
                        style = MaterialTheme.typography.caption1,
                        fontWeight = FontWeight.Bold,
                        color = WearCourtColors.TextPrimary,
                    )
                    Text(
                        text = "G ${match.gamesA}-${match.gamesB}",
                        style = MaterialTheme.typography.caption1,
                        fontWeight = FontWeight.Bold,
                        color = WearCourtColors.TextSecondary,
                    )
                    if (match.noAd) {
                        Text(
                            text = stringResource(R.string.no_ad_short),
                            style = MaterialTheme.typography.caption3,
                            fontWeight = FontWeight.Bold,
                            color = WearCourtColors.Accent,
                        )
                    }
                }
                val status = when {
                    match.isMatchOver -> stringResource(R.string.match_over)
                    match.isTiebreak -> stringResource(R.string.tiebreak)
                    match.isDeuce -> stringResource(R.string.deuce)
                    match.advantageA || match.advantageB -> stringResource(R.string.advantage_short)
                    else -> stringResource(
                        R.string.server_short,
                        if (match.server == Side.A) "A" else "B",
                    )
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.caption2,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        match.isTiebreak -> WearCourtColors.Serve
                        match.isMatchOver -> WearCourtColors.TextSecondary
                        match.isDeuce -> WearCourtColors.Accent
                        else -> WearCourtColors.Serve
                    },
                )
            }

            if (match.isMatchOver) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.match_over),
                        fontWeight = FontWeight.Bold,
                        color = WearCourtColors.TextPrimary,
                    )
                    Button(
                        onClick = onEndMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.primaryButtonColors(
                            backgroundColor = WearCourtColors.AccentDim,
                            contentColor = WearCourtColors.TextPrimary,
                        ),
                    ) {
                        Text(stringResource(R.string.end_match), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Two large square score boxes — tap = point, long-press = undo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScoreSquare(
                        label = match.playerA,
                        points = match.pointDisplayA,
                        isServing = match.server == Side.A,
                        accent = WearCourtColors.Accent,
                        onPoint = {
                            hapticPoint()
                            onPointA()
                        },
                        onUndo = {
                            hapticUndo()
                            onUndo()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    ScoreSquare(
                        label = match.playerB,
                        points = match.pointDisplayB,
                        isServing = match.server == Side.B,
                        accent = WearCourtColors.SideB,
                        onPoint = {
                            hapticPoint()
                            onPointB()
                        },
                        onUndo = {
                            hapticUndo()
                            onUndo()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.long_press_hint),
                    style = MaterialTheme.typography.caption3,
                    color = WearCourtColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScoreSquare(
    label: String,
    points: String,
    isServing: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onPoint: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                if (isServing) WearCourtColors.ServeContainer else WearCourtColors.SurfaceElevated,
            )
            .border(
                width = if (isServing) 2.5.dp else 1.dp,
                color = if (isServing) WearCourtColors.Serve else WearCourtColors.Border,
                shape = shape,
            )
            .combinedClickable(
                onClick = onPoint,
                onLongClick = onUndo,
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isServing) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(WearCourtColors.Serve),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = label.take(6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.caption2,
                    fontWeight = FontWeight.Bold,
                    color = if (isServing) WearCourtColors.Serve else WearCourtColors.TextSecondary,
                )
            }
            Text(
                text = points,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.TextPrimary,
                maxLines = 1,
            )
            // Subtle side accent bar cue
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(20.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = if (isServing) 1f else 0.45f)),
            )
        }
    }
}

@Composable
private fun EndMatchConfirm(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.end_match_confirm),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
            color = WearCourtColors.TextPrimary,
        )
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = WearCourtColors.Danger,
                contentColor = WearCourtColors.TextPrimary,
            ),
        ) {
            Text(stringResource(R.string.end_match), fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = ButtonDefaults.secondaryButtonColors(
                backgroundColor = WearCourtColors.Surface,
                contentColor = WearCourtColors.TextSecondary,
            ),
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun StartMatchWear(
    noAd: Boolean,
    onToggleNoAd: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            onClick = onToggleNoAd,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = ButtonDefaults.secondaryButtonColors(
                backgroundColor = if (noAd) WearCourtColors.AccentDim else WearCourtColors.Surface,
                contentColor = if (noAd) WearCourtColors.TextPrimary else WearCourtColors.TextSecondary,
            ),
        ) {
            Text(
                text = if (noAd) {
                    stringResource(R.string.no_ad_on)
                } else {
                    stringResource(R.string.no_ad_off)
                },
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
            )
        }
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = WearCourtColors.Accent,
                contentColor = WearCourtColors.Black,
            ),
        ) {
            Text(
                text = stringResource(R.string.start_match),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
