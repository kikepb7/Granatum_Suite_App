package com.granatum.core.designsystem.components.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.granatum.core.designsystem.theme.AppTheme

/** Text painted with the coral gradient: for the one big figure of a block. */
@Composable
fun AppGradientText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = style.copy(brush = AppTheme.colors.brandGradient),
        modifier = modifier
    )
}

/** Counts from 0 up to [target], so a summary figure arrives instead of just appearing. */
@Composable
fun rememberCountUp(target: Int, durationMillis: Int = 800): Int {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) { progress.animateTo(target.toFloat(), tween(durationMillis = durationMillis)) }
    return progress.value.toInt()
}
