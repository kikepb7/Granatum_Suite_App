package com.granatum.feature.clockin.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.brand.AppGradientText
import com.granatum.core.designsystem.components.brand.rememberCountUp
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.cards.AppHeroCard
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.lists.AppTimelineItem
import com.granatum.core.designsystem.components.motion.appEntrance
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.components.topbar.TopBarIconButton
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.largeTitle
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.ShiftState
import com.granatum.feature.clockin.presentation.common.timeLabel
import com.granatum.feature.clockin.presentation.common.workedLabel
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AttendanceHistoryRoot(
    onOpenShift: (String) -> Unit,
    viewModel: AttendanceHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AttendanceHistoryScreen(state = state, onAction = viewModel::onAction, onOpenShift = onOpenShift)
}

/**
 * The month at a glance (hero with the hours worked), then the shifts as a vertical timeline
 * grouped by day: a coral node per shift, tinted by how it went.
 */
@Composable
fun AttendanceHistoryScreen(state: HistoryUiState, onAction: (HistoryAction) -> Unit, onOpenShift: (String) -> Unit) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.history_title)) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MonthHero(state, onAction)
            if (state.shifts.isEmpty()) {
                AppEmptyState(title = stringResource(Res.string.history_empty))
            } else {
                val days = state.shifts.map { it.start.toLocalDateTime(TimeZone.currentSystemDefault()).date }
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = AppTheme.spacing.screenHorizontal,
                        end = AppTheme.spacing.screenHorizontal,
                        top = 16.dp,
                        bottom = 24.dp
                    )
                ) {
                    itemsIndexed(state.shifts, key = { _, shift -> shift.key }) { index, shift ->
                        val day = days[index]
                        AppTimelineItem(
                            tone = shift.tone(),
                            isFirst = index == 0,
                            isLast = index == state.shifts.lastIndex,
                            modifier = Modifier.appEntrance(index)
                        ) {
                            ShiftCard(
                                shift = shift,
                                day = day,
                                showDay = index == 0 || days[index - 1] != day,
                                onClick = { onOpenShift(shift.key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHero(state: HistoryUiState, onAction: (HistoryAction) -> Unit) {
    val totalMinutes = state.shifts.sumOf { it.workedMinutes ?: 0 }
    val hours = rememberCountUp(totalMinutes / 60)
    AppHeroCard(
        modifier = Modifier.padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = 8.dp),
        contentPadding = 20.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TopBarIconButton(onClick = { onAction(HistoryAction.OnPreviousMonth) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.history_previous_month))
            }
            Text(
                text = "${state.month.month.toString().padStart(2, '0')}/${state.month.year}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.onHero,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            TopBarIconButton(onClick = { if (state.canGoNext) onAction(HistoryAction.OnNextMonth) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.history_next_month),
                    tint = if (state.canGoNext) LocalContentColor.current else AppTheme.colors.textDisabled
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            AppGradientText(
                text = hours.toString(),
                style = MaterialTheme.typography.largeTitle.copy(fontSize = 64.sp, lineHeight = 68.sp)
            )
            Text(
                text = "h ${(totalMinutes % 60).toString().padStart(2, '0')}min",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.onHeroMuted,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
        }
        Text(
            text = stringResource(Res.string.history_month_total) + " · " +
                pluralStringResource(Res.plurals.history_shift_count, state.shifts.size, state.shifts.size),
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.onHeroMuted
        )
    }
}

private fun ShiftModel.tone(): AppTone = when {
    incomplete || state == ShiftState.INCOMPLETE || rejections.isNotEmpty() -> AppTone.DANGER
    hasPending -> AppTone.WARNING
    else -> AppTone.BRAND
}

@Composable
private fun ShiftCard(shift: ShiftModel, day: LocalDate, showDay: Boolean, onClick: () -> Unit) {
    Column {
        if (showDay) {
            Text(
                text = "${day.day.toString().padStart(2, '0')}/${day.month.ordinal.plus(1).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.padding(bottom = 6.dp, top = 2.dp)
            )
        }
        AppCard(onClick = onClick, onClickLabel = stringResource(Res.string.open_shift), contentPadding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${shift.start.timeLabel()} – ${shift.end?.timeLabel() ?: stringResource(Res.string.shift_in_progress)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = workedLabel(shift.workedMinutes, shift.isWorkedMinutesProvisional).orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textSecondary
                    )
                }
                Flags(shift)
            }
        }
    }
}

@Composable
fun Flags(shift: ShiftModel) {
    val flags = buildList {
        if (shift.incomplete || shift.state == ShiftState.INCOMPLETE) add(Res.string.flag_incomplete to AppTone.DANGER)
        if (shift.corrected) add(Res.string.flag_corrected to AppTone.NEUTRAL)
        if (shift.reconstructed) add(Res.string.flag_reconstructed to AppTone.NEUTRAL)
        if (shift.hasPending) add(Res.string.flag_pending to AppTone.WARNING)
        if (shift.rejections.isNotEmpty()) add(Res.string.flag_rejected to AppTone.DANGER)
    }
    if (flags.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        flags.forEach { (text, tone) ->
            AppStatusChip(text = stringResource(text), tone = tone)
        }
    }
}
