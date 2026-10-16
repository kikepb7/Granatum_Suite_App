package com.granatum.core.designsystem.components.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.chipLabel
import com.granatum.core.designsystem.theme.labelXSmall
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Opens a filters panel. Shows how many filters are on with a badge, and turns brand-coloured
 * while any is, so a narrowed list never looks like the whole one.
 */
@Composable
fun AppFilterButton(
    activeCount: Int,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.Filters
) {
    val colors = AppTheme.colors
    val active = activeCount > 0
    val container = animateColorAsState(if (active) colors.brandSoft else colors.surface, label = "filterFill").value
    val content = animateColorAsState(if (active) colors.brand else colors.textSecondary, label = "filterIcon").value
    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            shape = AppTheme.shapes.medium,
            color = container,
            border = BorderStroke(1.dp, if (active) colors.brand else colors.border),
            modifier = Modifier.size(52.dp).semantics { this.contentDescription = contentDescription }
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp)) }
        }
        AnimatedVisibility(
            visible = active,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp)
        ) {
            Surface(shape = CircleShape, color = colors.brand, modifier = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 5.dp)) {
                    Text(activeCount.toString(), style = MaterialTheme.typography.labelXSmall, fontWeight = FontWeight.Bold, color = colors.onBrand)
                }
            }
        }
    }
}

/** An active filter, removed with one tap on it. */
@Composable
fun AppRemovablePill(
    text: String,
    onRemove: () -> Unit,
    removeDescription: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val colors = AppTheme.colors
    Surface(
        onClick = onRemove,
        shape = AppTheme.shapes.pill,
        color = colors.brandSoft,
        border = BorderStroke(1.dp, colors.brand.copy(alpha = 0.35f)),
        modifier = modifier.semantics { contentDescription = "$removeDescription: $text" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = colors.brand, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.chipLabel, fontWeight = FontWeight.SemiBold, color = colors.brand)
            Spacer(Modifier.width(6.dp))
            Icon(AppIcons.Close, contentDescription = null, tint = colors.brand, modifier = Modifier.size(14.dp))
        }
    }
}

@Preview
@Composable
private fun AppFilterControlsPreview() {
    AppTheme {
        Row {
            AppFilterButton(activeCount = 2, onClick = {}, contentDescription = "Filtros")
            Spacer(Modifier.width(16.dp))
            AppRemovablePill(text = "Almacén B", onRemove = {}, removeDescription = "Quitar")
        }
    }
}
