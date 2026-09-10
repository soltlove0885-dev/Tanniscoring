package com.tanniscoring.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanniscoring.app.MatchUiState
import com.tanniscoring.app.R
import com.tanniscoring.shared.MatchMode
import com.tanniscoring.shared.MatchState
import com.tanniscoring.shared.Side

@Composable
fun MatchScreen(
    state: MatchUiState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    onToggleServer: () -> Unit,
    onEndMatch: () -> Unit,
    onNewMatch: () -> Unit,
    onRequestState: () -> Unit = {},
    onBackToBracket: (() -> Unit)? = null,
) {
    val match = state.matchState ?: return
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeScoreboard(
            state = state,
            match = match,
            onToggleServer = onToggleServer,
        )
    } else {
        PortraitMatchScreen(
            state = state,
            match = match,
            onPointA = onPointA,
            onPointB = onPointB,
            onUndo = onUndo,
            onToggleServer = onToggleServer,
            onEndMatch = onEndMatch,
            onNewMatch = onNewMatch,
            onRequestState = onRequestState,
            onBackToBracket = onBackToBracket,
        )
    }
}

/** Court scoreboard: huge scores, almost no chrome. */
@Composable
private fun LandscapeScoreboard(
    state: MatchUiState,
    match: MatchState,
    onToggleServer: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtColors.Black)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LandscapeSideColumn(
                name = match.playerA,
                sets = match.setsA,
                games = match.gamesA,
                points = match.pointDisplayA,
                isServing = match.server == Side.A && !match.isMatchOver,
                onServerTap = onToggleServer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )

            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LandscapeAxisLabel(stringResource(R.string.sets))
                Spacer(Modifier.height(28.dp))
                LandscapeAxisLabel(stringResource(R.string.games))
                Spacer(Modifier.height(36.dp))
                LandscapeAxisLabel(stringResource(R.string.points))
            }

            LandscapeSideColumn(
                name = match.playerB,
                sets = match.setsB,
                games = match.gamesB,
                points = match.pointDisplayB,
                isServing = match.server == Side.B && !match.isMatchOver,
                onServerTap = onToggleServer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        // Minimal status strip at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MatchStatusLine(match)
            Text(
                text = if (state.wearConnected) {
                    stringResource(R.string.wear_connected)
                } else {
                    stringResource(R.string.wear_disconnected)
                },
                style = MaterialTheme.typography.labelSmall,
                color = CourtColors.TextMuted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun LandscapeSideColumn(
    name: String,
    sets: Int,
    games: Int,
    points: String,
    isServing: Boolean,
    onServerTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clip(shape)
            .background(
                if (isServing) CourtColors.ServeContainer else CourtColors.Surface,
            )
            .then(
                if (isServing) {
                    Modifier.border(2.dp, CourtColors.Serve, shape)
                } else {
                    Modifier.border(1.dp, CourtColors.Border, shape)
                },
            )
            .clickable(onClick = onServerTap)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isServing) {
                ServeDot(size = 10.dp)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = name,
                color = if (isServing) CourtColors.Serve else CourtColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        LandscapeScoreValue(sets.toString(), 36.sp)
        Spacer(Modifier.height(10.dp))
        LandscapeScoreValue(games.toString(), 44.sp)
        Spacer(Modifier.height(10.dp))
        LandscapeScoreValue(points, 72.sp, emphasize = true)
    }
}

@Composable
private fun LandscapeScoreValue(
    text: String,
    size: TextUnit,
    emphasize: Boolean = false,
) {
    Text(
        text = text,
        color = if (emphasize) CourtColors.TextPrimary else CourtColors.TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = size,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LandscapeAxisLabel(text: String) {
    Text(
        text = text,
        color = CourtColors.TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitMatchScreen(
    state: MatchUiState,
    match: MatchState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    onToggleServer: () -> Unit,
    onEndMatch: () -> Unit,
    onNewMatch: () -> Unit,
    onRequestState: () -> Unit,
    onBackToBracket: (() -> Unit)? = null,
) {
    Scaffold(
        containerColor = CourtColors.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CourtColors.NearBlack,
                    titleContentColor = CourtColors.TextPrimary,
                    actionIconContentColor = CourtColors.TextSecondary,
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
                text = if (match.mode == MatchMode.DOUBLES) {
                    stringResource(R.string.doubles)
                } else {
                    stringResource(R.string.singles)
                },
                style = MaterialTheme.typography.labelLarge,
                color = CourtColors.Accent,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PortraitScoreColumn(
                    name = match.playerA,
                    sets = match.setsA,
                    games = match.gamesA,
                    points = match.pointDisplayA,
                    isServing = match.server == Side.A && !match.isMatchOver,
                    onServerTap = onToggleServer,
                    modifier = Modifier.weight(1f),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 36.dp),
                ) {
                    AxisChip(stringResource(R.string.sets))
                    AxisChip(stringResource(R.string.games))
                    AxisChip(stringResource(R.string.points))
                }
                PortraitScoreColumn(
                    name = match.playerB,
                    sets = match.setsB,
                    games = match.gamesB,
                    points = match.pointDisplayB,
                    isServing = match.server == Side.B && !match.isMatchOver,
                    onServerTap = onToggleServer,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CourtColors.ServeContainer)
                    .clickable(enabled = !match.isMatchOver, onClick = onToggleServer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                ServeDot(size = 8.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.server_fmt,
                        if (match.server == Side.A) match.playerA else match.playerB,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CourtColors.Serve,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))
            MatchStatusLine(match)

            if (match.setHistory.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.set_history),
                    style = MaterialTheme.typography.titleSmall,
                    color = CourtColors.TextSecondary,
                )
                Text(
                    text = match.setHistory.mapIndexed { i, s ->
                        "${i + 1}세트 ${s.gamesA}-${s.gamesB}"
                    }.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CourtColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            if (!match.isMatchOver) {
                Text(
                    text = stringResource(R.string.phone_mirror_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = CourtColors.TextMuted,
                    textAlign = TextAlign.Center,
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

            if (onBackToBracket != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBackToBracket,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CourtColors.Border),
                ) {
                    Text(stringResource(R.string.tournament_back_to_bracket), color = CourtColors.Serve)
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

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PortraitScoreColumn(
    name: String,
    sets: Int,
    games: Int,
    points: String,
    isServing: Boolean,
    onServerTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (isServing) CourtColors.ServeContainer else CourtColors.SurfaceElevated)
            .then(
                if (isServing) {
                    Modifier.border(2.dp, CourtColors.Serve, shape)
                } else {
                    Modifier.border(1.dp, CourtColors.Border, shape)
                },
            )
            .clickable(onClick = onServerTap)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isServing) {
                ServeDot(size = 8.dp)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isServing) CourtColors.Serve else CourtColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        ScoreCell(sets.toString(), 26.sp)
        Spacer(Modifier.height(6.dp))
        ScoreCell(games.toString(), 28.sp)
        Spacer(Modifier.height(6.dp))
        ScoreCell(points, 34.sp, emphasize = true)
    }
}

@Composable
private fun ScoreCell(text: String, size: TextUnit, emphasize: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (emphasize) CourtColors.SurfaceCard else CourtColors.Surface)
            .padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        color = CourtColors.TextPrimary,
    )
}

@Composable
private fun AxisChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(vertical = 14.dp)
            .width(40.dp),
        style = MaterialTheme.typography.labelMedium,
        color = CourtColors.TextMuted,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ServeDot(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(CourtColors.Serve),
    )
}

@Composable
private fun MatchStatusLine(match: MatchState) {
    when {
        match.isMatchOver -> {
            Text(
                text = stringResource(R.string.match_over),
                style = MaterialTheme.typography.headlineSmall,
                color = CourtColors.Danger,
                fontWeight = FontWeight.Bold,
            )
            val winnerName = when (match.winner) {
                Side.A -> match.playerA
                Side.B -> match.playerB
                null -> "-"
            }
            Text(
                stringResource(R.string.winner_fmt, winnerName),
                color = CourtColors.TextPrimary,
            )
        }
        match.isTiebreak -> Text(
            stringResource(R.string.tiebreak),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CourtColors.Serve,
        )
        match.isDeuce -> Text(
            stringResource(R.string.deuce),
            style = MaterialTheme.typography.titleMedium,
            color = CourtColors.TextSecondary,
        )
        match.advantageA || match.advantageB -> Text(
            stringResource(R.string.advantage),
            style = MaterialTheme.typography.titleMedium,
            color = CourtColors.TextSecondary,
        )
    }
}
