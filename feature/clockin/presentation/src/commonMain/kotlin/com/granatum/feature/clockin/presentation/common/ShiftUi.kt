package com.granatum.feature.clockin.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.SyncState
import com.granatum.feature.clockin.domain.model.TimelineItem
import com.granatum.feature.clockin.presentation.mapper.label
import com.granatum.feature.clockin.presentation.mapper.toUiText
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/** One line of a shift: time, what it was, whether it is registered, and why not if it was refused. */
@Composable
fun TimelineRow(item: TimelineItem, modifier: Modifier = Modifier, withRail: Boolean = true) {
    val label = itemLabel(item)
    val badge = syncLabel(item.syncState)
    val tone = when (item.syncState) {
        SyncState.SYNCED -> AppTone.SUCCESS
        SyncState.SYNCING -> AppTone.NEUTRAL
        SyncState.PENDING -> AppTone.WARNING
        SyncState.REJECTED -> AppTone.DANGER
    }
    AppCard(tone = if (withRail) tone else null, modifier = modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "${item.at.timeLabel()} $label, $badge"
            }
        ) {
            Text(
                text = item.at.timeLabel(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.textPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            SyncBadge(item.syncState)
        }
        item.rejection?.let {
            Text(
                text = it.toUiText().asString(),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.danger,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun SyncBadge(syncState: SyncState) {
    val tone = when (syncState) {
        SyncState.SYNCED -> AppTone.SUCCESS
        SyncState.SYNCING -> AppTone.NEUTRAL
        SyncState.PENDING -> AppTone.WARNING
        SyncState.REJECTED -> AppTone.DANGER
    }
    AppStatusChip(text = syncLabel(syncState), tone = tone, showDot = true)
}

@Composable
fun syncLabel(syncState: SyncState): String = stringResource(
    when (syncState) {
        SyncState.SYNCED -> Res.string.sync_synced
        SyncState.SYNCING -> Res.string.sync_syncing
        SyncState.PENDING -> Res.string.sync_pending
        SyncState.REJECTED -> Res.string.sync_rejected
    }
)

@Composable
fun itemLabel(item: TimelineItem): String {
    val type = item.breakType?.let { stringResource(it.label()) }
    return when (item.type) {
        ClockEventType.CLOCK_IN -> stringResource(Res.string.event_clock_in)
        ClockEventType.CLOCK_OUT -> stringResource(Res.string.event_clock_out)
        ClockEventType.BREAK_START -> type?.let { stringResource(Res.string.event_break_start_typed, it) } ?: stringResource(Res.string.event_break_start)
        ClockEventType.BREAK_END -> type?.let { stringResource(Res.string.event_break_end_typed, it) } ?: stringResource(Res.string.event_break_end)
    }
}

@Composable
fun workedLabel(minutes: Int?, provisional: Boolean): String? = minutes?.let {
    stringResource(if (provisional) Res.string.worked_time_provisional else Res.string.worked_time, it / 60, it % 60)
}

fun Instant.timeLabel(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val time = toLocalDateTime(timeZone).time
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}
