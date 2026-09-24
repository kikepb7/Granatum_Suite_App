package com.granatum.feature.clockin.presentation.clockin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.presentation.clockin.ClockInAction.OnBreakToggleClick
import com.granatum.feature.clockin.presentation.clockin.ClockInAction.OnPrimaryButtonClick
import com.granatum.feature.clockin.presentation.clockin.ClockInEvent.Error
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClockInRoot(
    viewModel: ClockInViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is Error -> scope.launch { snackbarHostState.showSnackbar(event.message.asStringAsync()) }
        }
    }

    ClockInScreen(state = state, onAction = viewModel::onAction, snackbarHostState = snackbarHostState)
}

@Composable
fun ClockInScreen(
    state: ClockInUiState,
    onAction: (ClockInAction) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = "Fichaje") },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StatusPill(status = state.status)

            if (state.pendingSyncCount > 0) {
                Text(
                    text = "${state.pendingSyncCount} fichaje(s) pendientes de sincronizar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.extended.textPlaceholder
                )
            }

            BigClockButton(
                status = state.status,
                isProcessing = state.isProcessing,
                onClick = { onAction(OnPrimaryButtonClick) }
            )

            if (state.status != ShiftStatus.CLOCKED_OUT) {
                AppButton(
                    text = if (state.status == ShiftStatus.ON_BREAK) "Terminar pausa" else "Iniciar pausa",
                    style = AppButtonStyle.SECONDARY,
                    onClick = { onAction(OnBreakToggleClick) },
                    enabled = !state.isProcessing,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: ShiftStatus) {
    Surface(shape = RoundedCornerShape(20.dp), color = status.toContainerColor()) {
        Text(
            text = status.toDisplayLabel(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BigClockButton(status: ShiftStatus, isProcessing: Boolean, onClick: () -> Unit) {
    val label = when (status) {
        ShiftStatus.CLOCKED_OUT -> "Fichar entrada"
        ShiftStatus.CLOCKED_IN -> "Fichar salida"
        ShiftStatus.ON_BREAK -> "Fichar salida"
    }
    val containerColor = if (status == ShiftStatus.CLOCKED_OUT) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Button(
        onClick = onClick,
        enabled = !isProcessing,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f)
    ) {
        if (isProcessing) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ShiftStatus.toContainerColor() = when (this) {
    ShiftStatus.CLOCKED_OUT -> MaterialTheme.colorScheme.errorContainer
    ShiftStatus.CLOCKED_IN -> MaterialTheme.colorScheme.primaryContainer
    ShiftStatus.ON_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
}

private fun ShiftStatus.toDisplayLabel(): String = when (this) {
    ShiftStatus.CLOCKED_OUT -> "No fichado"
    ShiftStatus.CLOCKED_IN -> "Fichado"
    ShiftStatus.ON_BREAK -> "En pausa"
}
