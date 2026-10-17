package com.granatum.core.designsystem.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.granatum.core.designsystem.theme.AppTheme

/**
 * A panel that rises from the bottom over a dimmed screen, for content taller than
 * [AppBottomSheet] handles well: Material's sheet on iOS opens tall content half way, with its
 * bottom off screen. Here the panel is never taller than [maxHeightFraction] of the screen, so
 * content laid out as "scrolling body + pinned footer" always shows its footer. Tapping outside or
 * going back dismisses it.
 */
@Composable
fun AppSheetPanel(
    onDismiss: () -> Unit,
    maxHeightFraction: Float = 0.88f,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        BoxWithConstraints(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        ) {
            val maxPanelHeight = maxHeight * maxHeightFraction
            val panelColor = AppTheme.colors.surface
            AnimatedVisibility(visibleState = shown, enter = slideInVertically { it } + fadeIn()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = AppTheme.colors.surface,
                    modifier = Modifier
                        // The dialog stops above the bottom safe area on iOS: the panel's colour
                        // continues below its edge so it reaches the bottom of the screen.
                        .drawBehind { drawRect(panelColor, topLeft = Offset(0f, size.height / 2), size = Size(size.width, size.height / 2 + BLEED.toPx())) }
                        .fillMaxWidth()
                        .heightIn(max = maxPanelHeight)
                        // Taps inside the panel must not reach the dismissing background.
                        .pointerInput(Unit) { detectTapGestures { } }
                ) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 10.dp)
                                .size(width = 36.dp, height = 4.dp)
                                .background(AppTheme.colors.borderStrong, RoundedCornerShape(2.dp))
                        )
                        content()
                    }
                }
            }
        }
    }
}

private val BLEED = 120.dp
