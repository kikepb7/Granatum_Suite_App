package com.granatum.feature.clockin.presentation.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.clockin.domain.model.CorrectionRequestModel
import com.granatum.feature.clockin.domain.model.EmployeeAttendanceSummaryModel
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.presentation.team.TeamAttendanceAction.OnApproveCorrection
import com.granatum.feature.clockin.presentation.team.TeamAttendanceAction.OnRefresh
import com.granatum.feature.clockin.presentation.team.TeamAttendanceAction.OnRejectCorrection
import com.granatum.feature.clockin.presentation.team.TeamAttendanceEvent.Error
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeamAttendanceRoot(
    viewModel: TeamAttendanceViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is Error -> scope.launch { snackbarHostState.showSnackbar(event.message.asStringAsync()) }
        }
    }

    TeamAttendanceScreen(state = state, onAction = viewModel::onAction, snackbarHostState = snackbarHostState)
}

@Composable
fun TeamAttendanceScreen(
    state: TeamAttendanceUiState,
    onAction: (TeamAttendanceAction) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = "Equipo") },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    AppButton(
                        text = if (state.isRefreshing) "Actualizando…" else "Actualizar",
                        onClick = { onAction(OnRefresh) },
                        enabled = !state.isRefreshing,
                        isLoading = state.isRefreshing,
                        style = AppButtonStyle.SECONDARY
                    )
                }
            }

            item {
                Text(
                    text = "Horas de los últimos 7 días",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.extended.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            items(items = state.summaries, key = { it.employeeId }) { summary ->
                EmployeeSummaryRow(summary = summary)
                HorizontalDivider(color = MaterialTheme.colorScheme.extended.surfaceOutline)
            }

            if (state.corrections.isNotEmpty()) {
                item {
                    Text(
                        text = "Solicitudes de corrección",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.extended.textPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
                items(items = state.corrections, key = { it.id }) { correction ->
                    CorrectionRow(
                        correction = correction,
                        onApprove = { onAction(OnApproveCorrection(correction.id)) },
                        onReject = { onAction(OnRejectCorrection(correction.id)) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.extended.surfaceOutline)
                }
            }
        }
    }
}

@Composable
private fun EmployeeSummaryRow(summary: EmployeeAttendanceSummaryModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = summary.employeeName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                text = summary.currentStatus.toLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textPlaceholder
            )
        }
        Text(
            text = "${summary.workedMinutesInPeriod / 60}h ${summary.workedMinutesInPeriod % 60}m",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CorrectionRow(
    correction: CorrectionRequestModel,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(text = correction.employeeName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(
            text = correction.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textPlaceholder
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            AppButton(text = "Aprobar", onClick = onApprove)
            AppButton(text = "Rechazar", style = AppButtonStyle.DESTRUCTIVE_SECONDARY, onClick = onReject)
        }
    }
}

private fun ShiftStatus.toLabel(): String = when (this) {
    ShiftStatus.CLOCKED_OUT -> "No fichado"
    ShiftStatus.CLOCKED_IN -> "Fichado"
    ShiftStatus.ON_BREAK -> "En pausa"
}
