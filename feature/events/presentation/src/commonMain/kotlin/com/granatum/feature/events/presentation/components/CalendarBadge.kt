package com.granatum.feature.events.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.labelXSmall

/** A tear-off calendar page: the month above, the day large. */
@Composable
internal fun CalendarBadge(
    day: String,
    month: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    size: Dp = 56.dp,
    onHero: Boolean = false
) {
    val colors = AppTheme.colors
    val container = when {
        onHero -> Color.White.copy(alpha = 0.12f)
        muted -> colors.surfaceSunken
        else -> colors.brandSoft
    }
    val accent = when {
        onHero -> colors.onHero
        muted -> colors.textTertiary
        else -> colors.brand
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.size(size).background(container, AppTheme.shapes.medium),
        verticalArrangement = Arrangement.Center
    ) {
        Text(month, style = MaterialTheme.typography.labelXSmall, fontWeight = FontWeight.Bold, color = accent)
        Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (onHero) colors.onHero else colors.textPrimary)
    }
}
