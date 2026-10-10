package com.granatum.core.designsystem.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.statValue
import com.granatum.core.designsystem.theme.caption

/** A figure with its caption, for the summaries at the top of a screen. */
@Composable
fun AppStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    valueColor: Color = AppTheme.colors.textPrimary
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.textTertiary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.statValue,
                color = valueColor
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}
