package com.granatum.feature.events.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppFloatingActionButton
import com.granatum.core.designsystem.components.cards.AppHeroCard
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.controls.AppSegmentedControl
import com.granatum.core.designsystem.components.feedback.AppBanner
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.feedback.AppLoadingState
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.components.icons.AppTabIcons
import com.granatum.core.designsystem.components.motion.appEntrance
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.components.topbar.TopBarIconButton
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.presentation.common.EventCardUiState
import com.granatum.feature.events.presentation.components.CalendarBadge
import com.granatum.feature.events.presentation.components.EventCard
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.add_event
import granatumsuite.feature.events.presentation.generated.resources.events_title
import granatumsuite.feature.events.presentation.generated.resources.list_empty_finished
import granatumsuite.feature.events.presentation.generated.resources.list_empty_upcoming
import granatumsuite.feature.events.presentation.generated.resources.list_empty_upcoming_hint
import granatumsuite.feature.events.presentation.generated.resources.list_stale
import granatumsuite.feature.events.presentation.generated.resources.materials_summary
import granatumsuite.feature.events.presentation.generated.resources.next_event
import granatumsuite.feature.events.presentation.generated.resources.refresh
import granatumsuite.feature.events.presentation.generated.resources.stat_units
import granatumsuite.feature.events.presentation.generated.resources.tab_finished
import granatumsuite.feature.events.presentation.generated.resources.tab_upcoming
import granatumsuite.feature.events.presentation.generated.resources.when_in_days
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/** The events tab: collects the ViewModel and its one-shot events, renders [EventListContent]. */
@Composable
internal fun EventListScreen(
    canManage: Boolean,
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    viewModel: EventListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is EventListUiEvent.ShowError -> snackbar.showSnackbar(event.message.asStringAsync())
            }
        }
    }
    EventListContent(
        uiState = uiState,
        canManage = canManage,
        snackbar = snackbar,
        onAction = viewModel::onAction,
        onOpenEvent = onOpenEvent,
        onCreateEvent = onCreateEvent
    )
}

@Composable
internal fun EventListContent(
    uiState: EventListScreenUiState,
    canManage: Boolean,
    onAction: (EventListUiAction) -> Unit,
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.events_title),
                actions = {
                    TopBarIconButton(onClick = { onAction(EventListUiAction.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
                    }
                }
            )
        },
        floatingActionButton = {
            if (canManage) {
                AppFloatingActionButton(onClick = onCreateEvent) {
                    Icon(AppIcons.Plus, contentDescription = stringResource(Res.string.add_event))
                }
            }
        }
    ) { padding ->
        when (uiState) {
            EventListScreenUiState.Loading -> AppLoadingState(Modifier.padding(padding))
            is EventListScreenUiState.Loaded -> LoadedContent(uiState.data, onAction, onOpenEvent, Modifier.padding(padding))
        }
    }
}

@Composable
private fun LoadedContent(
    data: EventListUiState,
    onAction: (EventListUiAction) -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontal = AppTheme.spacing.screenHorizontal
    Column(modifier.fillMaxSize()) {
        if (data.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AppTheme.colors.brand, trackColor = AppTheme.colors.brandSoft)
        }
        if (data.isStale) {
            AppBanner(title = stringResource(Res.string.list_stale), tone = AppTone.WARNING, modifier = Modifier.padding(horizontal = horizontal, vertical = 4.dp))
        }
        AppSegmentedControl(
            options = EventTab.entries,
            selected = data.selectedTab,
            onSelect = { onAction(EventListUiAction.SelectTab(it)) },
            label = { stringResource(if (it == EventTab.UPCOMING) Res.string.tab_upcoming else Res.string.tab_finished) },
            modifier = Modifier.padding(horizontal = horizontal, vertical = 8.dp)
        )
        if (data.isEmpty) {
            AppEmptyState(
                title = stringResource(if (data.selectedTab == EventTab.UPCOMING) Res.string.list_empty_upcoming else Res.string.list_empty_finished),
                description = if (data.selectedTab == EventTab.UPCOMING) stringResource(Res.string.list_empty_upcoming_hint) else null,
                icon = { Icon(AppTabIcons.Events, contentDescription = null, tint = AppTheme.colors.brand) }
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val next = data.nextEvent
            if (data.selectedTab == EventTab.UPCOMING && next != null) {
                item(key = "next") { NextEventHero(next, data.reservedUnits, onClick = { onOpenEvent(next.id) }, modifier = Modifier.appEntrance(0)) }
            }
            itemsIndexed(data.shown, key = { _, event -> event.id }) { index, event ->
                EventCard(event, onClick = { onOpenEvent(event.id) }, modifier = Modifier.animateItem().appEntrance(index + 1))
            }
        }
    }
}

/** The soonest event, large: what is coming next and how much stock the scheduled events hold. */
@Composable
private fun NextEventHero(event: EventCardUiState, reservedUnits: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppHeroCard(modifier = modifier.fillMaxWidth().clip(AppTheme.shapes.card).clickable(onClick = onClick), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalendarBadge(day = event.dayLabel, month = event.monthLabel, onHero = true, size = 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.next_event), style = MaterialTheme.typography.caption, color = AppTheme.colors.onHeroMuted)
                Text(
                    event.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.onHero,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${event.relativeLabel.asString()} · ${event.dateLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.onHeroMuted
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 14.dp)) {
            Icon(AppIcons.Box, contentDescription = null, tint = AppTheme.colors.onHeroMuted, modifier = Modifier.size(16.dp))
            Text(
                "${stringResource(Res.string.stat_units)}: $reservedUnits",
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.onHeroMuted
            )
        }
    }
}

@Preview
@Composable
private fun EventListContentPreview() {
    val card = EventCardUiState(
        id = "1",
        name = "Evento 1",
        description = "Boda de Juan y María",
        dayLabel = "13",
        monthLabel = "JUN",
        dateLabel = "sáb, 13 jun 2027",
        relativeLabel = UiText.Resource(Res.string.when_in_days, arrayOf(5)),
        materialsLabel = UiText.Resource(Res.string.materials_summary, arrayOf(3, 12)),
        isFinished = false,
        isToday = false
    )
    AppTheme {
        EventListContent(
            uiState = EventListScreenUiState.Loaded(
                EventListUiState(EventTab.UPCOMING, listOf(card, card.copy(id = "2", name = "Evento 2")), emptyList(), 12, isRefreshing = false, isStale = false)
            ),
            canManage = true,
            onAction = {},
            onOpenEvent = {},
            onCreateEvent = {}
        )
    }
}
