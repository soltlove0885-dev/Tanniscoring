package com.tanniscoring.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tanniscoring.app.R
import com.tanniscoring.shared.BracketMatch
import com.tanniscoring.shared.BracketMatchStatus
import com.tanniscoring.shared.Tournament
import com.tanniscoring.shared.TournamentRound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentBracketScreen(
    tournament: Tournament,
    wearConnected: Boolean,
    onSelectMatch: (BracketMatch) -> Unit,
    onOpenScoreboard: () -> Unit,
    onCycleMatchBestOf: (BracketMatch) -> Unit,
    onEndTournament: () -> Unit,
    onBack: () -> Unit,
) {
    val rounds = buildList {
        if (tournament.playerCount == 8) add(TournamentRound.QUARTERFINAL)
        add(TournamentRound.SEMIFINAL)
        add(TournamentRound.FINAL)
    }

    Scaffold(
        containerColor = CourtColors.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.tournament_bracket_title,
                            tournament.playerCount,
                        ),
                    )
                },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.tournament_default_format,
                    bestOfLabel(tournament.defaultBestOf),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = CourtColors.TextSecondary,
            )
            if (tournament.champion != null) {
                Text(
                    text = stringResource(R.string.tournament_champion, tournament.champion!!),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CourtColors.Serve,
                )
            }
            Text(
                text = if (wearConnected) {
                    stringResource(R.string.wear_connected)
                } else {
                    stringResource(R.string.wear_disconnected)
                },
                style = MaterialTheme.typography.labelSmall,
                color = CourtColors.TextMuted,
            )
            Text(
                text = stringResource(R.string.tournament_bracket_hint),
                style = MaterialTheme.typography.bodySmall,
                color = CourtColors.TextMuted,
            )

            rounds.forEach { round ->
                Text(
                    text = round.displayKo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CourtColors.Accent,
                    modifier = Modifier.padding(top = 8.dp),
                )
                tournament.matchesInRound(round).forEach { match ->
                    BracketMatchCard(
                        match = match,
                        isActive = match.id == tournament.activeMatchId,
                        onClick = { onSelectMatch(match) },
                        onCycleBestOf = { onCycleMatchBestOf(match) },
                    )
                }
            }

            if (tournament.activeMatchId != null) {
                Button(
                    onClick = onOpenScoreboard,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CourtColors.AccentDim,
                        contentColor = CourtColors.TextPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.tournament_open_scoreboard))
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, CourtColors.Border),
            ) {
                Text(stringResource(R.string.back), color = CourtColors.TextSecondary)
            }
            OutlinedButton(
                onClick = onEndTournament,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, CourtColors.Danger),
            ) {
                Text(stringResource(R.string.tournament_end), color = CourtColors.Danger)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BracketMatchCard(
    match: BracketMatch,
    isActive: Boolean,
    onClick: () -> Unit,
    onCycleBestOf: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val canPlay = match.canStart || match.status == BracketMatchStatus.IN_PROGRESS
    val borderColor = when {
        isActive -> CourtColors.Serve
        match.status == BracketMatchStatus.COMPLETED -> CourtColors.Accent
        canPlay -> CourtColors.AccentDim
        else -> CourtColors.Border
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isActive) CourtColors.ServeContainer else CourtColors.SurfaceElevated)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = canPlay, onClick = onClick)
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = statusLabel(match),
                style = MaterialTheme.typography.labelMedium,
                color = CourtColors.TextSecondary,
            )
            Text(
                text = bestOfLabel(match.bestOf),
                style = MaterialTheme.typography.labelMedium,
                color = CourtColors.Serve,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        enabled = match.status != BracketMatchStatus.COMPLETED,
                        onClick = onCycleBestOf,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = match.playerA ?: "TBD",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (match.winnerSide?.name == "A") FontWeight.Bold else FontWeight.Normal,
            color = CourtColors.TextPrimary,
        )
        Text(
            text = "vs",
            style = MaterialTheme.typography.labelSmall,
            color = CourtColors.TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            text = match.playerB ?: "TBD",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (match.winnerSide?.name == "B") FontWeight.Bold else FontWeight.Normal,
            color = CourtColors.TextPrimary,
        )
        if (match.status == BracketMatchStatus.COMPLETED) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.tournament_match_result,
                    match.winnerName ?: "-",
                    match.setsA,
                    match.setsB,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = CourtColors.Accent,
            )
        } else if (canPlay) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (match.status == BracketMatchStatus.IN_PROGRESS) {
                    stringResource(R.string.tournament_tap_continue)
                } else {
                    stringResource(R.string.tournament_tap_play)
                },
                style = MaterialTheme.typography.bodySmall,
                color = CourtColors.Serve,
            )
        }
    }
}

@Composable
private fun statusLabel(match: BracketMatch): String = when (match.status) {
    BracketMatchStatus.PENDING -> stringResource(R.string.tournament_status_pending)
    BracketMatchStatus.READY -> stringResource(R.string.tournament_status_ready)
    BracketMatchStatus.IN_PROGRESS -> stringResource(R.string.tournament_status_live)
    BracketMatchStatus.COMPLETED -> stringResource(R.string.tournament_status_done)
}

@Composable
private fun bestOfLabel(bestOf: Int): String = when (bestOf) {
    1 -> stringResource(R.string.best_of_1)
    5 -> stringResource(R.string.best_of_5)
    else -> stringResource(R.string.best_of_3)
}
