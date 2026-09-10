package com.tanniscoring.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanniscoring.app.R
import com.tanniscoring.shared.ServeLabel
import com.tanniscoring.shared.ServePhase
import com.tanniscoring.shared.ServeSessionState
import kotlinx.coroutines.delay

/**
 * Prominent gold-pulse serve speed flash for OLED landscape scoreboard.
 * Copy always says 추정 (estimate) — never radar accuracy.
 */
@Composable
fun ServeSpeedFlashBanner(
    serve: ServeSessionState,
    large: Boolean,
    modifier: Modifier = Modifier,
    onFlashConsumed: () -> Unit = {},
) {
    if (serve.phase == ServePhase.INACTIVE) return
    val speed = serve.speedDisplay
    val showSpeed = speed != null
    val labelText = when (serve.label) {
        ServeLabel.FIRST -> stringResource(R.string.serve_label_1st)
        ServeLabel.SECOND -> stringResource(R.string.serve_label_2nd)
        ServeLabel.FAULT -> stringResource(R.string.serve_label_fault)
    }
    val waitingText = when (serve.phase) {
        ServePhase.WAITING_FIRST -> stringResource(R.string.serve_waiting_1st)
        ServePhase.WAITING_SECOND -> stringResource(R.string.serve_waiting_2nd)
        else -> null
    }

    val pulse = rememberInfiniteTransition(label = "servePulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "servePulseAlpha",
    )
    val scale = remember { Animatable(1f) }
    LaunchedEffect(serve.flashToken, serve.flash) {
        if (serve.flash) {
            scale.snapTo(0.86f)
            scale.animateTo(1.08f, tween(220, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(180))
            delay(1_600)
            onFlashConsumed()
        }
    }

    val shape = RoundedCornerShape(if (large) 14.dp else 10.dp)
    val borderColor = when {
        serve.flash || showSpeed -> CourtColors.Serve
        serve.label == ServeLabel.FAULT -> CourtColors.Danger
        else -> CourtColors.Border
    }
    val bg = Brush.horizontalGradient(
        listOf(
            CourtColors.ServeContainer,
            if (serve.flash) Color(0xFF3A3010) else CourtColors.SurfaceElevated,
            CourtColors.ServeContainer,
        ),
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(
                width = if (serve.flash) 2.dp else 1.dp,
                color = borderColor.copy(alpha = if (serve.flash) pulseAlpha else 1f),
                shape = shape,
            )
            .scale(if (serve.flash) scale.value else 1f)
            .alpha(if (serve.flash) 0.75f + 0.25f * pulseAlpha else 1f)
            .padding(
                horizontal = if (large) 18.dp else 12.dp,
                vertical = if (large) 10.dp else 6.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = labelText,
                    color = if (serve.label == ServeLabel.FAULT) {
                        CourtColors.Danger
                    } else {
                        CourtColors.Serve
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = if (large) 14.sp else 11.sp,
                )
                if (showSpeed) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.serve_speed_estimate_fmt, speed!!),
                        color = CourtColors.Serve,
                        fontWeight = FontWeight.Black,
                        fontSize = if (large) 34.sp else 22.sp,
                        textAlign = TextAlign.Center,
                    )
                } else if (waitingText != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = waitingText,
                        color = CourtColors.TextMuted,
                        fontSize = if (large) 12.sp else 10.sp,
                    )
                }
            }
            if (showSpeed && large) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.serve_estimate_disclaimer),
                    color = CourtColors.TextMuted,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
fun ServeSpeedToggleRow(
    speedOn: Boolean,
    preset: String,
    onToggle: () -> Unit,
    onCyclePreset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onToggle,
            border = BorderStroke(1.dp, if (speedOn) CourtColors.Serve else CourtColors.Border),
        ) {
            Text(
                text = if (speedOn) {
                    stringResource(R.string.serve_speed_on)
                } else {
                    stringResource(R.string.serve_speed_off)
                },
                color = if (speedOn) CourtColors.Serve else CourtColors.TextSecondary,
            )
        }
        OutlinedButton(
            onClick = onCyclePreset,
            border = BorderStroke(1.dp, CourtColors.Border),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(
                text = stringResource(R.string.serve_cal_fmt, preset),
                color = CourtColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}
