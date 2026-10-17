package com.granatum.feature.events.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.theme.AppTheme

/** − value + small enough for a list row; the large one is the design system's AppQuantityStepper. */
@Composable
internal fun CompactQuantityStepper(
    value: Int,
    onChange: (Int) -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    min: Int = 1
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        StepButton(AppIcons.Minus, decreaseDescription, enabled = value > min) { onChange(value - 1) }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp)
        )
        StepButton(AppIcons.Plus, increaseDescription, enabled = true, filled = true) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppTheme.shapes.small,
        color = if (filled) colors.brandSoft else colors.surface,
        contentColor = if (filled) colors.brand else colors.textPrimary,
        border = if (filled) null else BorderStroke(1.dp, colors.border),
        modifier = Modifier.size(34.dp).semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    }
}
