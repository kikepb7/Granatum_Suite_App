package com.granatum.core.designsystem.components.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.granatum_symbol_star
import org.jetbrains.compose.resources.vectorResource

/**
 * The block a screen opens with: deep petróleo gradient, a coral glow in one corner and the brand
 * star as a faint oversized watermark. Text inside reads from `AppTheme.colors.onHero`.
 */
@Composable
fun AppHeroCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.card)
            .background(colors.heroGradient)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.glow, Color.Transparent),
                        center = Offset(size.width * 0.95f, 0f),
                        radius = size.width * 0.75f
                    )
                )
            }
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            Image(
                imageVector = vectorResource(Res.drawable.granatum_symbol_star),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.onHero),
                alpha = 0.07f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 64.dp, y = (-56).dp)
                    .requiredSize(240.dp)
            )
        }
        CompositionLocalProvider(LocalContentColor provides colors.onHero) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
