package com.granatum.core.designsystem.components.lists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.chips.toneContent
import com.granatum.core.designsystem.theme.AppTheme

private val GutterWidth = 30.dp
private val NodeCenterY = 28.dp

/**
 * One stop of a vertical timeline: a gutter with a coral node and the line to its neighbours,
 * and the [content] (usually a card) beside it. A [AppTone.BRAND] node is the pure brand coral.
 */
@Composable
fun AppTimelineItem(
    modifier: Modifier = Modifier,
    tone: AppTone = AppTone.BRAND,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val node: Color = if (tone == AppTone.BRAND) colors.coral else colors.toneContent(tone)
    val line = colors.borderStrong
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(GutterWidth)
                .fillMaxHeight()
                .drawBehind {
                    val x = 10.dp.toPx()
                    val y = NodeCenterY.toPx()
                    val stroke = 2.dp.toPx()
                    if (!isFirst) drawLine(line, Offset(x, 0f), Offset(x, y), strokeWidth = stroke)
                    if (!isLast) drawLine(line, Offset(x, y), Offset(x, size.height), strokeWidth = stroke)
                    drawCircle(node.copy(alpha = 0.22f), radius = 10.dp.toPx(), center = Offset(x, y))
                    drawCircle(node, radius = 5.dp.toPx(), center = Offset(x, y))
                }
        )
        Box(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 10.dp)) { content() }
    }
}
