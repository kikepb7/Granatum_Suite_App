package com.granatum.core.designsystem.components.chips

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.chipLabel

/** A selectable filter: soft brand fill and border when on, a quiet outline when off. */
@Composable
fun AppFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val container = animateColorAsState(if (selected) colors.brandSoft else colors.surface, label = "chipFill").value
    val outline = animateColorAsState(if (selected) colors.brand else colors.border, label = "chipBorder").value
    val content = animateColorAsState(if (selected) colors.brand else colors.textSecondary, label = "chipText").value
    Surface(
        onClick = onClick,
        shape = AppTheme.shapes.pill,
        color = container,
        border = BorderStroke(1.dp, outline),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.chipLabel.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = content
            )
        }
    }
}
