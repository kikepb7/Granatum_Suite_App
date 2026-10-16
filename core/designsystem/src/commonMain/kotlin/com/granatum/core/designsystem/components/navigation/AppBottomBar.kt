package com.granatum.core.designsystem.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.labelXSmall

data class AppBottomBarItemModel(val label: String, val icon: @Composable () -> Unit)

private const val SelectedWeight = 2.6f
private val BarItemHeight = 52.dp

/**
 * Floating glass pill with the app's destinations. Only the selected one shows its label, inside
 * a brand-gradient capsule that grows with a spring while the others shrink to icons, so five
 * destinations never crowd. Icons take the content colour set here, so a plain `Icon` just works.
 * The bar sits below the content in the layout (it never covers it) and keeps clear of the
 * system navigation area.
 */
@Composable
fun AppBottomBar(
    items: List<AppBottomBarItemModel>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val colors = AppTheme.colors
    val shape = AppTheme.shapes.pill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
    ) {
        Surface(
            shape = shape,
            color = colors.barSurface,
            border = BorderStroke(1.dp, if (colors.isDark) colors.border else Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = AppTheme.elevation.floating,
                    shape = shape,
                    ambientColor = colors.glow,
                    spotColor = colors.glow
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(6.dp)
            ) {
                items.forEachIndexed { index, item ->
                    BarItem(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onItemClick(index) },
                        modifier = Modifier.weight(animatedWeight(index == selectedIndex))
                    )
                }
            }
        }
    }
}

@Composable
private fun animatedWeight(selected: Boolean): Float = animateFloatAsState(
    targetValue = if (selected) SelectedWeight else 1f,
    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
    label = "bottomBarWeight"
).value

@Composable
private fun BarItem(
    item: AppBottomBarItemModel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val tint = animateColorAsState(
        targetValue = if (selected) colors.onBrand else colors.onBarMuted,
        label = "bottomBarTint"
    ).value
    val capsule = animateFloatAsState(if (selected) 1f else 0f, label = "bottomBarCapsule").value
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(BarItemHeight)
            .clip(AppTheme.shapes.pill)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .alpha(capsule)
                .background(colors.brandGradient)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalContentColor provides tint) {
                Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) { item.icon() }
            }
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelXSmall.copy(fontWeight = FontWeight.Bold),
                    color = tint,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}
