package com.granatum.feature.events.presentation.detail

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.cards.AppHeroCard
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.dialogs.AppDestructiveConfirmationDialog
import com.granatum.core.designsystem.components.feedback.AppBanner
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.feedback.AppLoadingState
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.components.lists.AppSectionHeader
import com.granatum.core.designsystem.components.motion.appEntrance
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.components.topbar.TopBarIconButton
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.presentation.common.EventMaterialUiState
import com.granatum.feature.events.presentation.components.CalendarBadge
import com.granatum.feature.events.presentation.components.EventMaterialRow
import com.granatum.feature.events.presentation.components.MaterialPickerSheet
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.add_material
import granatumsuite.feature.events.presentation.generated.resources.cancel
import granatumsuite.feature.events.presentation.generated.resources.delete
import granatumsuite.feature.events.presentation.generated.resources.delete_confirm
import granatumsuite.feature.events.presentation.generated.resources.delete_description
import granatumsuite.feature.events.presentation.generated.resources.delete_title
import granatumsuite.feature.events.presentation.generated.resources.edit
import granatumsuite.feature.events.presentation.generated.resources.event_not_found
import granatumsuite.feature.events.presentation.generated.resources.event_title
import granatumsuite.feature.events.presentation.generated.resources.finish_confirm
import granatumsuite.feature.events.presentation.generated.resources.finish_description
import granatumsuite.feature.events.presentation.generated.resources.finish_now
import granatumsuite.feature.events.presentation.generated.resources.finish_title
import granatumsuite.feature.events.presentation.generated.resources.finished_on
import granatumsuite.feature.events.presentation.generated.resources.materials_empty
import granatumsuite.feature.events.presentation.generated.resources.materials_empty_hint
import granatumsuite.feature.events.presentation.generated.resources.materials_summary
import granatumsuite.feature.events.presentation.generated.resources.materials_title
import granatumsuite.feature.events.presentation.generated.resources.read_only_finished
import granatumsuite.feature.events.presentation.generated.resources.status_finished
import granatumsuite.feature.events.presentation.generated.resources.status_scheduled
import granatumsuite.feature.events.presentation.generated.resources.when_in_days
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun EventDetailScreen(
    eventId: String,
    canManage: Boolean,
    onNavigateBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: EventDetailViewModel = koinViewModel(key = "event-$eventId") { parametersOf(eventId, canManage) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                EventDetailUiEvent.Deleted -> onNavigateBack()
                is EventDetailUiEvent.ShowMessage -> snackbar.showSnackbar(event.message.asStringAsync())
            }
        }
    }
    EventDetailContent(
        uiState = uiState,
        canManage = canManage,
        pickerQuery = viewModel.pickerQuery,
        snackbar = snackbar,
        onAction = viewModel::onAction,
        onBack = onNavigateBack,
        onEdit = onEdit
    )
}

@Composable
internal fun EventDetailContent(
    uiState: EventDetailScreenUiState,
    canManage: Boolean,
    pickerQuery: TextFieldState,
    onAction: (EventDetailUiAction) -> Unit,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() }
) {
    val data = (uiState as? EventDetailScreenUiState.Loaded)?.data
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.event_title),
                onBackClick = onBack,
                actions = {
                    if (data?.canEdit == true) {
                        TopBarIconButton(onClick = { onEdit(data.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                        }
                    }
                    if (data != null && canManage) {
                        TopBarIconButton(onClick = { onAction(EventDetailUiAction.RequestDelete) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            EventDetailScreenUiState.Loading -> AppLoadingState(Modifier.padding(padding))
            EventDetailScreenUiState.NotFound -> AppEmptyState(
                title = stringResource(Res.string.event_not_found),
                modifier = Modifier.padding(padding)
            )
            is EventDetailScreenUiState.Loaded -> LoadedContent(uiState.data, onAction, Modifier.padding(padding))
        }
    }
    if (data != null) {
        data.picker?.let { picker ->
            MaterialPickerSheet(
                state = picker,
                query = pickerQuery,
                onSelect = { onAction(EventDetailUiAction.SelectPickerMaterial(it)) },
                onQuantityChange = { onAction(EventDetailUiAction.ChangePickerQuantity(it)) },
                onConfirm = { onAction(EventDetailUiAction.ConfirmPicker) },
                onRetry = { onAction(EventDetailUiAction.RetryPicker) },
                onDismiss = { onAction(EventDetailUiAction.ClosePicker) }
            )
        }
        data.dialog?.let { dialog ->
            val finish = dialog == EventDetailDialog.FINISH
            AppDestructiveConfirmationDialog(
                title = stringResource(if (finish) Res.string.finish_title else Res.string.delete_title),
                description = stringResource(if (finish) Res.string.finish_description else Res.string.delete_description),
                confirmButtonText = stringResource(if (finish) Res.string.finish_confirm else Res.string.delete_confirm),
                cancelButtonText = stringResource(Res.string.cancel),
                onConfirmClick = { onAction(EventDetailUiAction.ConfirmDialog) },
                onCancelClick = { onAction(EventDetailUiAction.DismissDialog) },
                onDismiss = { onAction(EventDetailUiAction.DismissDialog) }
            )
        }
    }
}

@Composable
private fun LoadedContent(data: EventDetailUiState, onAction: (EventDetailUiAction) -> Unit, modifier: Modifier = Modifier) {
    val horizontal = AppTheme.spacing.screenHorizontal
    LazyColumn(
        contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "hero") { Hero(data, Modifier.appEntrance(0)) }
        if (data.isFinished) {
            item(key = "finished") { AppBanner(title = stringResource(Res.string.read_only_finished), tone = AppTone.INFO) }
        } else if (data.canEdit) {
            item(key = "finish") {
                AppButton(
                    text = stringResource(Res.string.finish_now),
                    onClick = { onAction(EventDetailUiAction.RequestFinish) },
                    style = AppButtonStyle.SECONDARY,
                    isLoading = data.isBusy,
                    enabled = !data.isBusy,
                    leadingIcon = { Icon(AppIcons.Check, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item(key = "materials-header") {
            AppSectionHeader(
                title = stringResource(Res.string.materials_title),
                modifier = Modifier.padding(top = 10.dp),
                action = if (data.canEdit) {
                    {
                        TextButton(onClick = { onAction(EventDetailUiAction.OpenPicker) }) {
                            Icon(AppIcons.Plus, contentDescription = null, tint = AppTheme.colors.brand, modifier = Modifier.size(18.dp))
                            Text(
                                stringResource(Res.string.add_material),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.brand,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                } else {
                    null
                }
            )
        }
        if (data.materials.isEmpty()) {
            item(key = "materials-empty") {
                AppEmptyState(
                    title = stringResource(Res.string.materials_empty),
                    description = if (data.canEdit) stringResource(Res.string.materials_empty_hint) else null,
                    icon = { Icon(AppIcons.Box, contentDescription = null, tint = AppTheme.colors.brand) }
                )
            }
        }
        items(data.materials, key = { it.materialId }) { material ->
            EventMaterialRow(
                material = material,
                editable = data.canEdit,
                onQuantityChange = { onAction(EventDetailUiAction.ChangeQuantity(material.materialId, it)) },
                onRemove = { onAction(EventDetailUiAction.RemoveMaterial(material.materialId)) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

/** When it is, what it is and its state, on the brand's hero surface. */
@Composable
private fun Hero(data: EventDetailUiState, modifier: Modifier = Modifier) {
    AppHeroCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalendarBadge(day = data.dayLabel, month = data.monthLabel, onHero = true, size = 68.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppStatusChip(
                    text = stringResource(if (data.isFinished) Res.string.status_finished else Res.string.status_scheduled),
                    // INFO and not NEUTRAL: the neutral chip disappears on the dark hero.
                    tone = if (data.isFinished) AppTone.INFO else AppTone.SUCCESS,
                    showDot = !data.isFinished
                )
                Text(data.name, style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, lineHeight = 30.sp), fontWeight = FontWeight.Bold, color = AppTheme.colors.onHero)
            }
        }
        data.description?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onHero, modifier = Modifier.padding(top = 14.dp))
        }
        Text(
            data.finishedOnLabel?.let { stringResource(Res.string.finished_on, it) } ?: "${data.relativeLabel.asString()} · ${data.dateLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.onHeroMuted,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(data.materialsLabel.asString(), style = MaterialTheme.typography.caption, color = AppTheme.colors.onHeroMuted, modifier = Modifier.padding(top = 2.dp))
    }
}

@Preview
@Composable
private fun EventDetailContentPreview() {
    AppTheme {
        EventDetailContent(
            uiState = EventDetailScreenUiState.Loaded(
                EventDetailUiState(
                    id = "1",
                    name = "Evento 1",
                    description = "Boda de Juan y María",
                    dayLabel = "13",
                    monthLabel = "JUN",
                    dateLabel = "sáb, 13 jun 2027",
                    relativeLabel = UiText.Resource(Res.string.when_in_days, arrayOf(5)),
                    isFinished = false,
                    finishedOnLabel = null,
                    materials = listOf(
                        EventMaterialUiState("m1", "Jarrones", "Jarrón cilíndrico alto", 5, null, null, isMissing = false, isSaving = false)
                    ),
                    materialsLabel = UiText.Resource(Res.string.materials_summary, arrayOf(1, 5)),
                    canEdit = true,
                    picker = null,
                    dialog = null,
                    isBusy = false
                )
            ),
            canManage = true,
            pickerQuery = TextFieldState(),
            onAction = {},
            onBack = {},
            onEdit = {}
        )
    }
}
