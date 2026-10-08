package com.granatum.feature.invoicing.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.dialogs.AppBottomSheet
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppAccountButton
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.documents.DocumentSource
import com.granatum.core.presentation.documents.rememberDocumentPicker
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceSummary
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.UploadDocument
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import com.granatum.feature.invoicing.presentation.common.ErrorState
import com.granatum.feature.invoicing.presentation.common.LoadingState
import com.granatum.feature.invoicing.presentation.common.Pill
import com.granatum.feature.invoicing.presentation.common.StateBadge
import com.granatum.feature.invoicing.presentation.common.label
import granatumsuite.feature.invoicing.presentation.generated.resources.*
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@Composable
fun InvoiceListRoot(
    onOpenInvoice: (String) -> Unit,
    onOpenQuarters: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenCompany: () -> Unit,
    viewModel: InvoiceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.onAction(InvoiceListAction.OnMessageShown)
        }
    }
    var choosingSource by remember { mutableStateOf(false) }
    val picker =
        rememberDocumentPicker { documents ->
            viewModel.onAction(InvoiceListAction.OnUpload(documents.map { UploadDocument(it.bytes, it.fileName, it.mimeType) }))
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { Header(onRefresh = { viewModel.onAction(InvoiceListAction.OnRefresh) }, onOpenQuarters, onOpenReports, onOpenCompany) },
        floatingActionButton = {
            if (state.uploadingCount == 0) {
                FloatingActionButton(onClick = { choosingSource = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.upload))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.companyMissing) CompanyBanner(onOpenCompany)
            if (state.uploadingCount > 0) UploadingBanner(state.uploadingCount)
            Filters(state, viewModel)
            when {
                state.isLoading && state.items.isEmpty() -> LoadingState()
                state.error != null && state.items.isEmpty() ->
                    ErrorState(
                        state.error!!,
                        onRetry = { viewModel.onAction(InvoiceListAction.OnRefresh) },
                    )
                state.items.isEmpty() ->
                    Text(
                        text = stringResource(if (state.isFiltered) Res.string.list_empty_filtered else Res.string.list_empty),
                        color = MaterialTheme.colorScheme.extended.textPlaceholder,
                        modifier = Modifier.padding(24.dp),
                    )
                else -> InvoiceList(state, onOpenInvoice, onLoadMore = { viewModel.onAction(InvoiceListAction.OnLoadMore) })
            }
        }
    }

    if (choosingSource) {
        AppBottomSheet(onDismiss = { choosingSource = false }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.upload), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                listOf(
                    DocumentSource.CAMERA to Res.string.upload_camera,
                    DocumentSource.GALLERY to Res.string.upload_gallery,
                    DocumentSource.FILES to Res.string.upload_files,
                ).forEach { (source, label) ->
                    AppButton(
                        text = stringResource(label),
                        onClick = {
                            choosingSource = false
                            picker.launch(source)
                        },
                        style = AppButtonStyle.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    state.uploadResults?.let { results ->
        AppBottomSheet(onDismiss = { viewModel.onAction(InvoiceListAction.OnDismissUploadResults) }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.upload_results_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                results.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(item.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(item.outcome.label()),
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    if (item.outcome == UploadOutcome.ACCEPTED || item.outcome == UploadOutcome.DUPLICATE) {
                                        MaterialTheme.colorScheme.extended.textSecondary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                            )
                        }
                        item.invoiceId?.let { id ->
                            TextButton(onClick = {
                                viewModel.onAction(InvoiceListAction.OnDismissUploadResults)
                                onOpenInvoice(id)
                            }) { Text(stringResource(Res.string.upload_open)) }
                        }
                    }
                }
                AppButton(
                    text = stringResource(Res.string.upload_done),
                    onClick = { viewModel.onAction(InvoiceListAction.OnDismissUploadResults) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Header(
    onRefresh: () -> Unit,
    onOpenQuarters: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenCompany: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Text(
                text = stringResource(Res.string.invoicing_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.extended.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf(
                        Res.string.quarters to onOpenQuarters,
                        Res.string.reports to onOpenReports,
                        Res.string.company to onOpenCompany,
                    ).forEach { (label, action) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                            menu = false
                            action()
                        })
                    }
                }
            }
            AppAccountButton()
        }
    }
}

@Composable
private fun CompanyBanner(onOpenCompany: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.extended.accentYellow, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                stringResource(Res.string.company_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.yellowCardText,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenCompany) { Text(stringResource(Res.string.company_missing_action)) }
        }
    }
}

@Composable
private fun UploadingBanner(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(stringResource(Res.string.uploading, count), style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Filters(
    state: InvoiceListState,
    viewModel: InvoiceListViewModel,
) {
    var pickingDates by remember { mutableStateOf(false) }
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp)) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.extended.textPlaceholder)
            Spacer(Modifier.width(8.dp))
            AppTextField(
                state = viewModel.query,
                placeholder = stringResource(Res.string.search_placeholder),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            FilterChip(selected = state.filter.state == null, onClick = {
                viewModel.onAction(InvoiceListAction.OnState(null))
            }, label = { Text(stringResource(Res.string.filter_all)) })
            InvoiceState.entries.forEach { s ->
                FilterChip(selected = state.filter.state == s, onClick = {
                    viewModel.onAction(InvoiceListAction.OnState(s))
                }, label = { Text(stringResource(s.label())) })
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            FilterChip(selected = state.filter.type == InvoiceType.ISSUED, onClick = {
                viewModel.onAction(InvoiceListAction.OnType(if (state.filter.type == InvoiceType.ISSUED) null else InvoiceType.ISSUED))
            }, label = { Text(stringResource(Res.string.filter_issued)) })
            FilterChip(selected = state.filter.type == InvoiceType.RECEIVED, onClick = {
                viewModel.onAction(InvoiceListAction.OnType(if (state.filter.type == InvoiceType.RECEIVED) null else InvoiceType.RECEIVED))
            }, label = { Text(stringResource(Res.string.filter_received)) })
            val from = state.filter.from
            val to = state.filter.to
            FilterChip(
                selected = from != null || to != null,
                onClick = {
                    if (from != null ||
                        to != null
                    ) {
                        viewModel.onAction(InvoiceListAction.OnDates(null, null))
                    } else {
                        pickingDates = true
                    }
                },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = {
                    Text(
                        if (from != null || to != null) {
                            stringResource(Res.string.filter_dates_range, from?.label() ?: "…", to?.label() ?: "…")
                        } else {
                            stringResource(Res.string.filter_dates)
                        },
                    )
                },
            )
        }
    }

    if (pickingDates) {
        val picker = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingDates = false },
            confirmButton = {
                TextButton(onClick = {
                    pickingDates = false
                    viewModel.onAction(
                        InvoiceListAction.OnDates(picker.selectedStartDateMillis?.toDate(), picker.selectedEndDateMillis?.toDate()),
                    )
                }) { Text(stringResource(Res.string.upload_done)) }
            },
            dismissButton = { TextButton(onClick = { pickingDates = false }) { Text(stringResource(Res.string.cancel)) } },
        ) {
            DateRangePicker(state = picker, modifier = Modifier.weight(1f))
        }
    }
}

/** The pickers speak UTC midnight milliseconds. */
internal fun Long.toDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

internal fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

@Composable
private fun InvoiceList(
    state: InvoiceListState,
    onOpenInvoice: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearEnd, state.items.size) { if (nearEnd && state.hasMore) onLoadMore() }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.items, key = { it.id }) { invoice -> InvoiceRow(invoice, onClick = { onOpenInvoice(invoice.id) }) }
        if (state.isLoadingMore) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(Modifier.size(24.dp)) }
            }
        }
        if (state.loadMoreFailed) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.list_load_more_error),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onLoadMore) { Text(stringResource(Res.string.retry)) }
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(
    invoice: InvoiceSummary,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.extended.surfaceHigher,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = invoice.counterparty?.name ?: invoice.counterparty?.taxId ?: stringResource(Res.string.no_counterparty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.extended.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                invoice.total?.let {
                    Text(
                        stringResource(Res.string.amount_eur, it.format()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text =
                    listOf(
                        stringResource(invoice.type.label()),
                        invoice.number ?: stringResource(Res.string.no_number),
                        invoice.issueDate?.label() ?: stringResource(Res.string.no_date),
                    ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StateBadge(invoice.state)
                if (invoice.warningCount > 0 && invoice.state != InvoiceState.CONFIRMED && invoice.state != InvoiceState.DISCARDED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Pill(
                            stringResource(Res.string.warnings_count, invoice.warningCount),
                            MaterialTheme.colorScheme.extended.redCardBackground,
                            MaterialTheme.colorScheme.extended.redCardText,
                        )
                    }
                }
            }
        }
    }
}
