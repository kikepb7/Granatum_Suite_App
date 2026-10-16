package com.granatum.feature.clockin.presentation.clockin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.brand.AppGradientText
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.buttons.AppPrimaryButton
import com.granatum.core.designsystem.components.cards.AppHeroCard
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.feedback.AppBanner
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.lists.AppSectionHeader
import com.granatum.core.designsystem.components.lists.AppTimelineItem
import com.granatum.core.designsystem.components.motion.appEntrance
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.largeTitle
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.clockin.domain.model.ClockRejection
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.SyncState
import com.granatum.feature.clockin.domain.model.TimelineItem
import com.granatum.feature.clockin.presentation.common.TimelineRow
import com.granatum.feature.clockin.presentation.common.timeLabel
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

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
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.clockin_title)) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StatusHeader(status = state.status)

            Column(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (today.hasClockSkew) {
                    AppBanner(
                        title = stringResource(Res.string.clock_skew_title),
                        caption = stringResource(Res.string.clock_skew_caption),
                        tone = AppTone.DANGER
                    )
                }
                if (today.pendingCount > 0) {
                    AppBanner(
                        title = pluralStringResource(Res.plurals.pending_count, today.pendingCount, today.pendingCount),
                        caption = stringResource(Res.string.pending_caption),
                        tone = AppTone.WARNING
                    )
                }
                val tooOld = today.rejected.firstOrNull { it.rejection == ClockRejection.TooOld }
                if (today.rejected.isNotEmpty()) {
                    AppBanner(
                        title = pluralStringResource(Res.plurals.rejected_count, today.rejected.size, today.rejected.size),
                        caption = stringResource(Res.string.rejected_caption),
                        tone = AppTone.DANGER
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
    val now = Clock.System.now().timeLabel()
    AppHeroCard(modifier = Modifier.padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AppStatusChip(text = stringResource(status.headline()), tone = status.tone(), showDot = true)
            AppGradientText(
                text = now,
                style = MaterialTheme.typography.largeTitle.copy(fontSize = 64.sp, lineHeight = 70.sp)
            )
            Text(
                text = stringResource(status.caption()),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.onHeroMuted
            )
        }
    }
}


@Composable
private fun TodayTimeline(items: List<TimelineItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) {
        AppEmptyState(title = stringResource(Res.string.today_empty), modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AppTheme.spacing.screenHorizontal)
    ) {
        item(key = "today-header") { AppSectionHeader(title = stringResource(Res.string.today_title)) }
        itemsIndexed(items = items, key = { _, item -> item.eventId ?: "${item.type}-${item.at}" }) { index, item ->
            AppTimelineItem(
                tone = item.syncState.tone(),
                isFirst = index == 0,
                isLast = index == items.lastIndex,
                modifier = Modifier.appEntrance(index)
            ) {
                TimelineRow(item, withRail = false)
            }
        }
    }
}

@Composable
private fun ActionBar(state: ClockInUiState, onPrimaryClick: () -> Unit, onBreakClick: () -> Unit) {
    val status = state.status
    Column(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.screenHorizontal),
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
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        AppPrimaryButton(
            text = stringResource(if (status == ShiftStatus.CLOCKED_OUT) Res.string.action_clock_in else Res.string.action_clock_out),
            onClick = onPrimaryClick,
            enabled = status == ShiftStatus.CLOCKED_OUT || state.canClockOut,
            isLoading = state.isProcessing,
            height = 60.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun SyncState.tone(): AppTone = when (this) {
    SyncState.SYNCED -> AppTone.BRAND
    SyncState.SYNCING -> AppTone.NEUTRAL
    SyncState.PENDING -> AppTone.WARNING
    SyncState.REJECTED -> AppTone.DANGER
}

/** Not being clocked in is a resting state, not a failure: it does not get the error colour. */
private fun ShiftStatus.tone(): AppTone = when (this) {
    ShiftStatus.CLOCKED_OUT -> AppTone.BRAND
    ShiftStatus.CLOCKED_IN -> AppTone.SUCCESS
    ShiftStatus.ON_BREAK -> AppTone.WARNING
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
