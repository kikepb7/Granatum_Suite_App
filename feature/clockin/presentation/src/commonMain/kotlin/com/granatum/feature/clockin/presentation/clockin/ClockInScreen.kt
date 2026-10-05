package com.granatum.feature.clockin.presentation.clockin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.clockin.domain.model.ClockEventModel
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.SyncState
import com.granatum.feature.clockin.presentation.clockin.ClockInAction.OnBreakToggleClick
import com.granatum.feature.clockin.presentation.clockin.ClockInAction.OnPrimaryButtonClick
import com.granatum.feature.clockin.presentation.clockin.ClockInEvent.Error
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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

/**
 * Layout follows how the screen is actually used: glance at the state, see today's punches,
 * then act. The action sits at the bottom, within thumb reach, rather than floating in the
 * middle of the screen.
 */
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            StatusHeader(status = state.status)

            if (state.pendingSyncCount > 0) {
                PendingSyncNotice(
                    count = state.pendingSyncCount,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            TodayTimeline(
                events = state.todayEvents,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            ActionBar(
                status = state.status,
                isProcessing = state.isProcessing,
                onPrimaryClick = { onAction(OnPrimaryButtonClick) },
                onBreakClick = { onAction(OnBreakToggleClick) }
            )
        }
    }
}

/** The state you came to check, stated plainly rather than squeezed into a chip. */
@Composable
private fun StatusHeader(status: ShiftStatus) {
    val accent = status.accentColor()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(10.dp).background(accent, CircleShape))
            Text(
                text = status.headline(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
        }
        Text(
            text = status.caption(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.extended.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Unsent punches are the one thing this screen must never bury: the whole offline-first
 * promise is that a punch is never lost, so the count gets a card, not grey micro-copy.
 */
@Composable
private fun PendingSyncNotice(count: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.extended.yellowCardBackground,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(8.dp).background(
                    MaterialTheme.colorScheme.extended.yellowCardText,
                    CircleShape
                )
            )
            Column {
                Text(
                    text = if (count == 1) "1 fichaje sin enviar" else "$count fichajes sin enviar",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.extended.yellowCardText
                )
                Text(
                    text = "Se enviarán solos al recuperar la conexión",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.extended.yellowCardText
                )
            }
        }
    }
}

/** Today's punches. The ViewModel already loaded these; the old screen simply discarded them. */
@Composable
private fun TodayTimeline(events: List<ClockEventModel>, modifier: Modifier = Modifier) {
    if (events.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Todavía no has fichado hoy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.extended.textPlaceholder
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = events, key = { it.id }) { event ->
            TimelineRow(event = event)
        }
    }
}

@Composable
private fun TimelineRow(event: ClockEventModel) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.extended.surfaceHigher,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = event.clientTimestamp.toLocalTimeLabel(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
            Text(
                text = event.type.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.extended.textSecondary,
                modifier = Modifier.weight(1f)
            )
            SyncBadge(syncState = event.syncState)
        }
    }
}

/** Per-punch sync state, so "did that one go through?" is answerable at a glance. */
@Composable
private fun SyncBadge(syncState: SyncState) {
    val (label, color) = when (syncState) {
        SyncState.SYNCED -> "Enviado" to MaterialTheme.colorScheme.extended.success
        SyncState.SYNCING -> "Enviando" to MaterialTheme.colorScheme.extended.textSecondary
        SyncState.PENDING -> "Pendiente" to MaterialTheme.colorScheme.extended.yellowCardText
        SyncState.FAILED -> "Fallido" to MaterialTheme.colorScheme.extended.redCardText
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Actions live at the bottom: that is where the thumb is, and where the eye ends up. */
@Composable
private fun ActionBar(
    status: ShiftStatus,
    isProcessing: Boolean,
    onPrimaryClick: () -> Unit,
    onBreakClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (status != ShiftStatus.CLOCKED_OUT) {
            AppButton(
                text = if (status == ShiftStatus.ON_BREAK) "Terminar pausa" else "Iniciar pausa",
                style = AppButtonStyle.SECONDARY,
                onClick = onBreakClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = onPrimaryClick,
            enabled = !isProcessing,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = status.accentColor()),
            modifier = Modifier.fillMaxWidth().height(68.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = if (status == ShiftStatus.CLOCKED_OUT) "Fichar entrada" else "Fichar salida",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Not being clocked in is a resting state, not a failure — the old screen painted it with the
 * error colour, which told the user something had gone wrong when nothing had.
 */
@Composable
private fun ShiftStatus.accentColor(): Color = when (this) {
    ShiftStatus.CLOCKED_OUT -> MaterialTheme.colorScheme.primary
    ShiftStatus.CLOCKED_IN -> MaterialTheme.colorScheme.extended.success
    ShiftStatus.ON_BREAK -> MaterialTheme.colorScheme.extended.yellowCardText
}

private fun ShiftStatus.headline(): String = when (this) {
    ShiftStatus.CLOCKED_OUT -> "Sin fichar"
    ShiftStatus.CLOCKED_IN -> "Trabajando"
    ShiftStatus.ON_BREAK -> "En pausa"
}

private fun ShiftStatus.caption(): String = when (this) {
    ShiftStatus.CLOCKED_OUT -> "Ficha tu entrada para empezar la jornada"
    ShiftStatus.CLOCKED_IN -> "Tu jornada está en curso"
    ShiftStatus.ON_BREAK -> "Tu jornada está pausada"
}

private fun ClockEventType.label(): String = when (this) {
    ClockEventType.CLOCK_IN -> "Entrada"
    ClockEventType.CLOCK_OUT -> "Salida"
    ClockEventType.BREAK_START -> "Inicio de pausa"
    ClockEventType.BREAK_END -> "Fin de pausa"
}

private fun kotlin.time.Instant.toLocalTimeLabel(): String {
    val time = toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}
