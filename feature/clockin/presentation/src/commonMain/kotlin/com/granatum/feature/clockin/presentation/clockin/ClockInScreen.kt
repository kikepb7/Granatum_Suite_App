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
import com.granatum.feature.clockin.domain.model.ClockRejection
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.TimelineItem
import com.granatum.feature.clockin.presentation.common.TimelineRow
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClockInRoot(
    onOpenShift: (String) -> Unit,
    viewModel: ClockInViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is ClockInEvent.Error -> scope.launch { snackbarHostState.showSnackbar(event.message.asStringAsync()) }
            is ClockInEvent.OpenShift -> onOpenShift(event.shiftKey)
        }
    }

    ClockInScreen(state = state, onAction = viewModel::onAction, snackbarHostState = snackbarHostState)
}

/**
 * Glance at the state, see today's punches with whether each one is registered, then act. The
 * action sits at the bottom, within thumb reach.
 */
@Composable
fun ClockInScreen(
    state: ClockInUiState,
    onAction: (ClockInAction) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val today = state.today
    val items = today.shifts.flatMap { it.items }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.clockin_title)) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StatusHeader(status = state.status)

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (today.hasClockSkew) {
                    Notice(
                        title = stringResource(Res.string.clock_skew_title),
                        caption = stringResource(Res.string.clock_skew_caption),
                        background = MaterialTheme.colorScheme.extended.redCardBackground,
                        content = MaterialTheme.colorScheme.extended.redCardText
                    )
                }
                if (today.pendingCount > 0) {
                    Notice(
                        title = pluralStringResource(Res.plurals.pending_count, today.pendingCount, today.pendingCount),
                        caption = stringResource(Res.string.pending_caption),
                        background = MaterialTheme.colorScheme.extended.yellowCardBackground,
                        content = MaterialTheme.colorScheme.extended.yellowCardText
                    )
                }
                val tooOld = today.rejected.firstOrNull { it.rejection == ClockRejection.TooOld }
                if (today.rejected.isNotEmpty()) {
                    Notice(
                        title = pluralStringResource(Res.plurals.rejected_count, today.rejected.size, today.rejected.size),
                        caption = stringResource(Res.string.rejected_caption),
                        background = MaterialTheme.colorScheme.extended.redCardBackground,
                        content = MaterialTheme.colorScheme.extended.redCardText
                    )
                }
                if (tooOld != null) {
                    val shiftKey = today.shifts.firstOrNull { tooOld in it.items }?.takeIf { it.canRequestCorrection }?.key
                    if (shiftKey != null) {
                        AppButton(
                            text = stringResource(Res.string.request_correction),
                            onClick = { onAction(ClockInAction.OnRequestCorrection(shiftKey)) },
                            style = AppButtonStyle.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            TodayTimeline(items = items, modifier = Modifier.weight(1f).fillMaxWidth())

            ActionBar(
                state = state,
                onPrimaryClick = { onAction(ClockInAction.OnPrimaryButtonClick) },
                onBreakClick = { onAction(ClockInAction.OnBreakToggleClick) }
            )
        }
    }

    if (state.isChoosingBreak) {
        BreakTypeSheet(
            onChoose = { onAction(ClockInAction.OnBreakTypeChosen(it)) },
            onDismiss = { onAction(ClockInAction.OnBreakSheetDismissed) }
        )
    }
}

@Composable
private fun StatusHeader(status: ShiftStatus) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(10.dp).background(status.accentColor(), CircleShape))
            Text(
                text = stringResource(status.headline()),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
        }
        Text(
            text = stringResource(status.caption()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.extended.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Unsent and refused punches are the things this screen must never bury. */
@Composable
private fun Notice(title: String, caption: String, background: Color, content: Color) {
    Surface(shape = RoundedCornerShape(14.dp), color = background, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(8.dp).background(content, CircleShape))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = content)
                Text(text = caption, style = MaterialTheme.typography.bodySmall, color = content)
            }
        }
    }
}

@Composable
private fun TodayTimeline(items: List<TimelineItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.today_empty),
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
        items(items = items, key = { it.eventId ?: "${it.type}-${it.at}" }) { TimelineRow(it) }
    }
}

@Composable
private fun ActionBar(state: ClockInUiState, onPrimaryClick: () -> Unit, onBreakClick: () -> Unit) {
    val status = state.status
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (status != ShiftStatus.CLOCKED_OUT) {
            AppButton(
                text = stringResource(if (status == ShiftStatus.ON_BREAK) Res.string.action_end_break else Res.string.action_start_break),
                style = AppButtonStyle.SECONDARY,
                onClick = onBreakClick,
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (status == ShiftStatus.ON_BREAK) {
            Text(
                text = stringResource(Res.string.end_break_first),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            onClick = onPrimaryClick,
            enabled = !state.isProcessing && (status == ShiftStatus.CLOCKED_OUT || state.canClockOut),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = status.accentColor()),
            modifier = Modifier.fillMaxWidth().height(68.dp)
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = stringResource(if (status == ShiftStatus.CLOCKED_OUT) Res.string.action_clock_in else Res.string.action_clock_out),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/** Not being clocked in is a resting state, not a failure: it does not get the error colour. */
@Composable
private fun ShiftStatus.accentColor(): Color = when (this) {
    ShiftStatus.CLOCKED_OUT -> MaterialTheme.colorScheme.primary
    ShiftStatus.CLOCKED_IN -> MaterialTheme.colorScheme.extended.success
    ShiftStatus.ON_BREAK -> MaterialTheme.colorScheme.extended.yellowCardText
}

private fun ShiftStatus.headline() = when (this) {
    ShiftStatus.CLOCKED_OUT -> Res.string.status_clocked_out
    ShiftStatus.CLOCKED_IN -> Res.string.status_clocked_in
    ShiftStatus.ON_BREAK -> Res.string.status_on_break
}

private fun ShiftStatus.caption() = when (this) {
    ShiftStatus.CLOCKED_OUT -> Res.string.caption_clocked_out
    ShiftStatus.CLOCKED_IN -> Res.string.caption_clocked_in
    ShiftStatus.ON_BREAK -> Res.string.caption_on_break
}
