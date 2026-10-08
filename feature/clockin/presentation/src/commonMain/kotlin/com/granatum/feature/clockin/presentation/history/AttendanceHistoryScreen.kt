package com.granatum.feature.clockin.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.ShiftState
import com.granatum.feature.clockin.presentation.common.timeLabel
import com.granatum.feature.clockin.presentation.common.workedLabel
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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

@Composable
fun AttendanceHistoryScreen(state: HistoryUiState, onAction: (HistoryAction) -> Unit, onOpenShift: (String) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.history_title)) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MonthSelector(state, onAction)
            if (state.shifts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(stringResource(Res.string.history_empty), color = MaterialTheme.colorScheme.extended.textPlaceholder)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.shifts, key = { it.key }) { shift -> ShiftCard(shift, onClick = { onOpenShift(shift.key) }) }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(state: HistoryUiState, onAction: (HistoryAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onAction(HistoryAction.OnPreviousMonth) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.history_previous_month))
        }
        Text(
            text = "${state.month.month.toString().padStart(2, '0')}/${state.month.year}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.extended.textPrimary
        )
        IconButton(onClick = { onAction(HistoryAction.OnNextMonth) }, enabled = state.canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(Res.string.history_next_month))
        }
    }
}

@Composable
private fun ShiftCard(shift: ShiftModel, onClick: () -> Unit) {
    val date = shift.start.toLocalDateTime(TimeZone.currentSystemDefault()).date
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.extended.surfaceHigher,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = stringResource(Res.string.open_shift), onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${date.day.toString().padStart(2, '0')}/${date.month.ordinal.plus(1).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.extended.textPrimary
                )
                Text(
                    text = workedLabel(shift.workedMinutes, shift.isWorkedMinutesProvisional).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.extended.textSecondary
                )
            }
            Text(
                text = "${shift.start.timeLabel()} – ${shift.end?.timeLabel() ?: stringResource(Res.string.shift_in_progress)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.extended.textSecondary
            )
            Flags(shift)
        }
    }
}

@Composable
fun Flags(shift: ShiftModel) {
    val flags = buildList {
        if (shift.incomplete || shift.state == ShiftState.INCOMPLETE) add(Res.string.flag_incomplete to MaterialTheme.colorScheme.extended.redCardText)
        if (shift.corrected) add(Res.string.flag_corrected to MaterialTheme.colorScheme.extended.textSecondary)
        if (shift.reconstructed) add(Res.string.flag_reconstructed to MaterialTheme.colorScheme.extended.textSecondary)
        if (shift.hasPending) add(Res.string.flag_pending to MaterialTheme.colorScheme.extended.yellowCardText)
        if (shift.rejections.isNotEmpty()) add(Res.string.flag_rejected to MaterialTheme.colorScheme.extended.redCardText)
    }
    if (flags.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        flags.forEach { (text, color: Color) ->
            Text(text = stringResource(text), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
