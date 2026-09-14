package com.tanniscoring.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text

/** Shrink font on round/narrow Wear faces so KO/EN labels and scores aren't clipped. */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 9.sp,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
    softWrap: Boolean = maxLines > 1,
) {
    var size by remember(text, fontSize) { mutableStateOf(fontSize) }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = fontWeight,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = TextOverflow.Clip,
        textAlign = textAlign,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && size.value > minFontSize.value) {
                val next = (size.value - 1f).coerceAtLeast(minFontSize.value)
                if (next < size.value) size = next.sp
            }
        },
    )
}
