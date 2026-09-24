package com.granatum.feature.clockin.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.DailyAttendanceSummary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AttendanceHistoryRoot(
    viewModel: AttendanceHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AttendanceHistoryScreen(state = state)
}

@Composable
fun AttendanceHistoryScreen(state: List<DailyAttendanceSummary>) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = "Mi historial") }
    ) { padding ->
        if (state.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(
                    text = "Todavía no tienes fichajes registrados.",
                    color = MaterialTheme.colorScheme.extended.textPlaceholder
                )
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(items = state, key = { it.date.toString() }) { day ->
                DaySummaryRow(day = day)
                HorizontalDivider(color = MaterialTheme.colorScheme.extended.surfaceOutline)
            }
        }
    }
}

@Composable
private fun DaySummaryRow(day: DailyAttendanceSummary) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = day.date.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.extended.textPrimary
        )
        Text(
            text = "${day.workedMinutes.toHoursLabel()} trabajadas · ${day.breakMinutes} min de pausa",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textPlaceholder
        )
        if (day.hasPendingSync) {
            Text(
                text = "Pendiente de sincronizar",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = day.events.joinToString(separator = "  ·  ") { it.type.toShortLabel() },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.extended.textPlaceholder
        )
    }
}

private fun Int.toHoursLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return "${hours}h ${minutes}m"
}

private fun ClockEventType.toShortLabel(): String = when (this) {
    ClockEventType.CLOCK_IN -> "Entrada"
    ClockEventType.CLOCK_OUT -> "Salida"
    ClockEventType.BREAK_START -> "Inicio pausa"
    ClockEventType.BREAK_END -> "Fin pausa"
}
