package com.granatum.core.designsystem.components.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.chips.toneContent
import com.granatum.core.designsystem.theme.AppTheme

private val RailWidth = 4.dp
private val RailInset = 18.dp
private val RailGap = 14.dp

/**
 * The card of the app: a borderless tonal surface with a large radius. In light it floats on a
 * soft two-layer shadow; in dark it lifts by tone alone. With a [tone] it carries a slim accent
 * rail on its leading edge, so a status reads before any text. Clickable when [onClick] is set,
 * with a small press-in scale.
 *
 * [border] is optional and off by default; pass a brush only for an outlined variant.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    containerColor: Color = AppTheme.colors.surface,
    border: Brush? = null,
    shape: CornerBasedShape = AppTheme.shapes.card,
    contentPadding: Dp = 18.dp,
    tone: AppTone? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardPress",
    )
    val lift = if (colors.isDark) 0.dp else AppTheme.elevation.card
    Surface(
        shape = shape,
        color = containerColor,
        border = border?.let { BorderStroke(width = 1.dp, brush = it) },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.shadow(
                elevation = lift,
                shape = shape,
                ambientColor = colors.cardShadow.copy(alpha = 0.10f),
                spotColor = colors.cardShadow.copy(alpha = 0.18f),
            ),
    ) {
        val tap =
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClickLabel = onClickLabel,
                    role = Role.Button,
                    onClick = onClick,
                )
            } else {
                Modifier
            }
        val rail = if (tone != null) Modifier.drawRail(colors.toneContent(tone)) else Modifier
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
            Column(
                modifier =
                    rail
                        .then(tap)
                        .padding(start = if (tone != null) RailGap else 0.dp)
                        .padding(contentPadding),
                content = content,
            )
        }
    }
}

private fun Modifier.drawRail(color: Color): Modifier =
    drawBehind {
        val width = RailWidth.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(x = 10.dp.toPx(), y = RailInset.toPx()),
            size = Size(width, size.height - 2 * RailInset.toPx()),
            cornerRadius = CornerRadius(width / 2f),
        )
    }

/** A card whose leading rail carries a status: [AppCard] with a [tone]. */
@Composable
fun AppAccentCard(
    tone: AppTone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        onClickLabel = onClickLabel,
        contentPadding = contentPadding,
        tone = tone,
        content = content,
    )
}

/** A recessed block for grouped rows inside a card: sunken tone, no shadow, smaller radius. */
@Composable
fun AppInsetCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = AppTheme.shapes.medium,
        color = AppTheme.colors.surfaceSunken,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
