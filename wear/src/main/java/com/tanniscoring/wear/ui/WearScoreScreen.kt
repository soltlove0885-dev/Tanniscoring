package com.tanniscoring.wear.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
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
import com.tanniscoring.shared.BadmintonMatchState
import com.tanniscoring.shared.Side
import com.tanniscoring.shared.SportType
import com.tanniscoring.wear.R
import com.tanniscoring.wear.WearScreen
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
    onChooseKorean: () -> Unit = {},
    onChooseEnglish: () -> Unit = {},
    onSelectTennis: () -> Unit = {},
    onSelectBadminton: () -> Unit = {},
    onShowSportPicker: () -> Unit = {},
    onShowLanguage: () -> Unit = {},
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
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state.screen) {
            WearScreen.LANGUAGE -> {
                LanguageWear(
                    onKorean = {
                        hapticPoint()
                        onChooseKorean()
                    },
                    onEnglish = {
                        hapticPoint()
                        onChooseEnglish()
                    },
                )
                return
            }
            WearScreen.SPORT_PICKER -> {
                SportPickerWear(
                    onTennis = {
                        hapticPoint()
                        onSelectTennis()
                    },
                    onBadminton = {
                        hapticPoint()
                        onSelectBadminton()
                    },
                    onLanguage = {
                        hapticUndo()
                        onShowLanguage()
                    },
                )
                return
            }
            WearScreen.IDLE -> Unit
        }

        if (!state.matchStarted) {
            StartMatchWear(
                sport = state.selectedSport ?: SportType.TENNIS,
                noAd = state.draftNoAd,
                onToggleNoAd = onToggleNoAd,
                onStart = {
                    hapticPoint()
                    onStart()
                },
                onBackSports = {
                    hapticUndo()
                    onShowSportPicker()
                },
            )
            return
        }

        if (state.selectedSport == SportType.BADMINTON && state.badmintonState != null) {
            BadmintonScoreWear(
                match = state.badmintonState,
                onPointA = {
                    hapticPoint()
                    onPointA()
                },
                onPointB = {
                    hapticPoint()
                    onPointB()
                },
                onUndo = {
                    hapticUndo()
                    onUndo()
                },
                onEndMatch = onEndMatch,
            )
            return
        }

        val match = state.matchState ?: return
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
                    text = stringResource(R.string.sport_tennis),
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
                AutoSizeText(
                    text = stringResource(R.string.long_press_hint),
                    fontSize = 9.sp,
                    minFontSize = 7.sp,
                    maxLines = 1,
                    color = WearCourtColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BadmintonScoreWear(
    match: BadmintonMatchState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    onEndMatch: () -> Unit,
) {
    var confirmEnd by remember { mutableStateOf(false) }
    if (confirmEnd) {
        EndMatchConfirm(
            onConfirm = {
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (!match.isMatchOver) confirmEnd = true
                    },
                )
                .padding(top = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.sport_badminton),
                style = MaterialTheme.typography.caption2,
                fontWeight = FontWeight.SemiBold,
                color = WearCourtColors.TextMuted,
            )
            Text(
                text = stringResource(R.string.badminton_rules),
                style = MaterialTheme.typography.caption3,
                color = WearCourtColors.Accent,
            )
            if (match.isMatchOver) {
                Text(
                    text = stringResource(R.string.badminton_game_over),
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    color = WearCourtColors.TextPrimary,
                )
            }
        }

        if (match.isMatchOver) {
            Button(
                onClick = onEndMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 8.dp),
                colors = ButtonDefaults.primaryButtonColors(
                    backgroundColor = WearCourtColors.AccentDim,
                    contentColor = WearCourtColors.TextPrimary,
                ),
            ) {
                Text(stringResource(R.string.end_match), fontWeight = FontWeight.Bold)
            }
        } else {
            // Top / bottom large halves for badminton
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ScoreSquare(
                    label = match.playerA,
                    points = match.pointsA.toString(),
                    isServing = match.server == Side.A && !match.isMatchOver,
                    accent = WearCourtColors.Accent,
                    onPoint = onPointA,
                    onUndo = onUndo,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    square = false,
                    serveIconRes = R.drawable.ic_serve_shuttlecock,
                )
                ScoreSquare(
                    label = match.playerB,
                    points = match.pointsB.toString(),
                    isServing = match.server == Side.B && !match.isMatchOver,
                    accent = WearCourtColors.SideB,
                    onPoint = onPointB,
                    onUndo = onUndo,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    square = false,
                    serveIconRes = R.drawable.ic_serve_shuttlecock,
                )
            }
            AutoSizeText(
                text = stringResource(R.string.long_press_hint),
                fontSize = 9.sp,
                minFontSize = 7.sp,
                maxLines = 1,
                color = WearCourtColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
            )
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
    square: Boolean = true,
    serveIconRes: Int = R.drawable.ic_serve_tennis,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .then(if (square) Modifier.aspectRatio(1f) else Modifier)
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
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isServing) {
                Image(
                    painter = painterResource(serveIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.height(1.dp))
            }
            AutoSizeText(
                text = label,
                maxLines = 1,
                fontSize = 11.sp,
                minFontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isServing) WearCourtColors.Serve else WearCourtColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            AutoSizeText(
                text = points,
                fontSize = if (square) 34.sp else 38.sp,
                minFontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(18.dp)
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
        AutoSizeText(
            text = stringResource(R.string.end_match_confirm),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            minFontSize = 10.sp,
            maxLines = 2,
            fontWeight = FontWeight.Bold,
            color = WearCourtColors.TextPrimary,
            modifier = Modifier.fillMaxWidth(),
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
private fun LanguageWear(
    onKorean: () -> Unit,
    onEnglish: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AutoSizeText(
            text = stringResource(R.string.choose_language),
            fontSize = 16.sp,
            minFontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = WearCourtColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onKorean,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = WearCourtColors.Accent,
                contentColor = WearCourtColors.Black,
            ),
        ) {
            AutoSizeText(
                text = stringResource(R.string.language_korean),
                fontSize = 15.sp,
                minFontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.Black,
            )
        }
        Button(
            onClick = onEnglish,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            colors = ButtonDefaults.secondaryButtonColors(
                backgroundColor = WearCourtColors.Surface,
                contentColor = WearCourtColors.TextPrimary,
            ),
        ) {
            AutoSizeText(
                text = stringResource(R.string.language_english),
                fontSize = 15.sp,
                minFontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun SportPickerWear(
    onTennis: () -> Unit,
    onBadminton: () -> Unit,
    onLanguage: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AutoSizeText(
            text = stringResource(R.string.choose_sport),
            fontSize = 16.sp,
            minFontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = WearCourtColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onTennis,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = WearCourtColors.Accent,
                contentColor = WearCourtColors.Black,
            ),
        ) {
            AutoSizeText(
                text = stringResource(R.string.sport_tennis),
                fontSize = 15.sp,
                minFontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.Black,
            )
        }
        Button(
            onClick = onBadminton,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = WearCourtColors.Serve,
                contentColor = WearCourtColors.Black,
            ),
        ) {
            AutoSizeText(
                text = stringResource(R.string.sport_badminton),
                fontSize = 15.sp,
                minFontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.Black,
            )
        }
        Button(
            onClick = onLanguage,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            colors = ButtonDefaults.secondaryButtonColors(
                backgroundColor = WearCourtColors.Surface,
                contentColor = WearCourtColors.TextSecondary,
            ),
        ) {
            AutoSizeText(
                text = stringResource(R.string.change_language),
                fontSize = 12.sp,
                minFontSize = 9.sp,
                color = WearCourtColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun StartMatchWear(
    sport: SportType,
    noAd: Boolean,
    onToggleNoAd: () -> Unit,
    onStart: () -> Unit,
    onBackSports: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AutoSizeText(
            text = stringResource(
                if (sport == SportType.BADMINTON) R.string.sport_badminton else R.string.sport_tennis,
            ),
            fontSize = 16.sp,
            minFontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = WearCourtColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        AutoSizeText(
            text = if (sport == SportType.BADMINTON) {
                stringResource(R.string.badminton_rules)
            } else {
                stringResource(R.string.start_defaults)
            },
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            minFontSize = 9.sp,
            maxLines = 2,
            color = WearCourtColors.TextSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
        if (sport == SportType.TENNIS) {
            Button(
                onClick = onToggleNoAd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                colors = ButtonDefaults.secondaryButtonColors(
                    backgroundColor = if (noAd) WearCourtColors.AccentDim else WearCourtColors.Surface,
                    contentColor = if (noAd) WearCourtColors.TextPrimary else WearCourtColors.TextSecondary,
                ),
            ) {
                AutoSizeText(
                    text = if (noAd) {
                        stringResource(R.string.no_ad_on)
                    } else {
                        stringResource(R.string.no_ad_off)
                    },
                    fontSize = 12.sp,
                    minFontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (noAd) WearCourtColors.TextPrimary else WearCourtColors.TextSecondary,
                )
            }
        }
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = WearCourtColors.Accent,
                contentColor = WearCourtColors.Black,
            ),
        ) {
            AutoSizeText(
                text = stringResource(R.string.start_match),
                fontSize = 15.sp,
                minFontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WearCourtColors.Black,
            )
        }
        Button(
            onClick = onBackSports,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            colors = ButtonDefaults.secondaryButtonColors(
                backgroundColor = WearCourtColors.Surface,
                contentColor = WearCourtColors.TextSecondary,
            ),
        ) {
            AutoSizeText(
                text = stringResource(R.string.back_to_sports),
                fontSize = 12.sp,
                minFontSize = 9.sp,
                color = WearCourtColors.TextSecondary,
            )
        }
    }
}
