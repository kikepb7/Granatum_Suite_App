package com.granatum.core.designsystem.components.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.chipLabel
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A single choice among a few options, as one pill track: the chosen segment fills with the brand
 * colour. For filters and settings with two to five values (a warehouse, a stock state, an order).
 */
@Composable
fun <T> AppSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.surfaceSunken, AppTheme.shapes.pill)
            .padding(4.dp)
            .selectableGroup()
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val container = animateColorAsState(if (isSelected) colors.brand else colors.surfaceSunken, label = "segmentFill").value
            val content = animateColorAsState(if (isSelected) colors.onBrand else colors.textSecondary, label = "segmentText").value
            Surface(
                shape = AppTheme.shapes.pill,
                color = container,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .selectable(selected = isSelected, enabled = enabled, role = Role.Tab, onClick = { onSelect(option) })
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.chipLabel,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (enabled) content else colors.textDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppSegmentedControlPreview() {
    AppTheme {
        AppSegmentedControl(options = listOf("Todos", "A", "B", "C"), selected = "B", onSelect = {}, label = { it })
    }
}
