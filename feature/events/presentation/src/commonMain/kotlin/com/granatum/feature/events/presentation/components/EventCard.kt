package com.granatum.feature.events.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.feature.events.presentation.common.EventCardUiState
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.status_finished
import org.jetbrains.compose.resources.stringResource

/** An event at a glance: when (badge and «En 5 días»), what it is and how much material it takes. */
@Composable
internal fun EventCard(event: EventCardUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(
        onClick = onClick,
        onClickLabel = event.name,
        tone = if (event.isToday && !event.isFinished) AppTone.BRAND else null,
        contentPadding = 14.dp,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CalendarBadge(day = event.dayLabel, month = event.monthLabel, muted = event.isFinished)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                event.description?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(AppIcons.Box, contentDescription = null, tint = AppTheme.colors.textTertiary, modifier = Modifier.size(14.dp))
                    Text(event.materialsLabel.asString(), style = MaterialTheme.typography.caption, color = AppTheme.colors.textSecondary)
                }
            }
            if (event.isFinished) {
                AppStatusChip(text = stringResource(Res.string.status_finished), tone = AppTone.NEUTRAL)
            } else {
                AppStatusChip(text = event.relativeLabel.asString(), tone = if (event.isToday) AppTone.BRAND else AppTone.INFO, showDot = event.isToday)
            }
        }
    }
}
