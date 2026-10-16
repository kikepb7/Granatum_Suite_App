package com.granatum.feature.invoicing.presentation.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.documents.SharedFile
import com.granatum.core.presentation.documents.rememberFileOpener
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.invoicing.domain.model.ReportFormat
import com.granatum.feature.invoicing.domain.model.ReportGroup
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.presentation.common.ErrorState
import com.granatum.feature.invoicing.presentation.common.LoadingState
import com.granatum.feature.invoicing.presentation.common.SectionTitle
import com.granatum.feature.invoicing.presentation.common.label
import granatumsuite.feature.invoicing.presentation.generated.resources.*
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReportRoot(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val opener = rememberFileOpener()
    ObserveAsEvents(viewModel.fileEvents) { file -> opener.open(SharedFile(file.bytes, file.fileName, file.mimeType)) }
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.onAction(ReportAction.OnMessageShown)
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { AppTopBar(title = stringResource(Res.string.report_title), onBackClick = onNavigateBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PeriodType.MONTHLY to Res.string.period_monthly,
                    PeriodType.QUARTERLY to Res.string.period_quarterly,
                    PeriodType.YEARLY to Res.string.period_yearly,
                ).forEach { (type, label) ->
                    FilterChip(selected = state.type == type, onClick = {
                        viewModel.onAction(ReportAction.OnType(type))
                    }, label = { Text(stringResource(label)) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = {
                    viewModel.onAction(ReportAction.OnStep(-1))
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) }
                Text(
                    periodLabel(state.period),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    viewModel.onAction(ReportAction.OnStep(1))
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            }
            val report = state.report
            when {
                state.isLoading -> LoadingState(Modifier.padding(32.dp))
                report == null -> ErrorState(state.error ?: return@Column, onRetry = { viewModel.onAction(ReportAction.OnRetry) })
                else -> {
                    Text(
                        stringResource(Res.string.report_range, report.from.label(), report.to.label()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (report.pending > 0) {
                        Text(
                            stringResource(Res.string.report_pending, report.pending),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (report.closedQuarters.isNotEmpty()) {
                        Text(
                            stringResource(
                                Res.string.report_closed_quarters,
                                report.closedQuarters.joinToString { (year, quarter) ->
                                    "${quarter}T $year"
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Group(stringResource(Res.string.report_issued), report.issued)
                    Group(stringResource(Res.string.report_received), report.received)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppButton(
                            stringResource(Res.string.report_download_pdf),
                            onClick = { viewModel.onAction(ReportAction.OnDownload(ReportFormat.PDF)) },
                            isLoading = state.downloading == ReportFormat.PDF,
                            enabled = state.downloading == null,
                            modifier = Modifier.weight(1f),
                        )
                        AppButton(
                            stringResource(Res.string.report_download_csv),
                            onClick = { viewModel.onAction(ReportAction.OnDownload(ReportFormat.CSV)) },
                            isLoading = state.downloading == ReportFormat.CSV,
                            enabled = state.downloading == null,
                            style = AppButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun periodLabel(period: ReportPeriod): String =
    when (period) {
        is ReportPeriod.Monthly -> "${period.month.toString().padStart(2, '0')}/${period.year}"
        is ReportPeriod.Quarterly -> "${period.quarter}T ${period.year}"
        is ReportPeriod.Yearly -> period.year.toString()
    }

@Composable
private fun Group(
    title: String,
    group: ReportGroup,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(title)
            Line(stringResource(Res.string.report_count), group.count.toString())
            Line(stringResource(Res.string.report_base), stringResource(Res.string.amount_eur, group.base.format()))
            group.vatByRate.forEach { (rate, amount) ->
                Line(
                    stringResource(Res.string.report_vat_rate, rate.removeSuffix(".00").replace('.', ',')),
                    stringResource(Res.string.amount_eur, amount.format()),
                )
            }
            if (!group.surcharge.isZero) {
                Line(
                    stringResource(Res.string.report_surcharge),
                    stringResource(Res.string.amount_eur, group.surcharge.format()),
                )
            }
            if (!group.withholding.isZero) {
                Line(
                    stringResource(Res.string.report_withholding),
                    stringResource(Res.string.amount_eur, group.withholding.format()),
                )
            }
            group.noQuota.forEach { (cause, base) ->
                Line(
                    stringResource(Res.string.report_no_quota, stringResource(cause.label())),
                    stringResource(Res.string.amount_eur, base.format()),
                )
            }
            Spacer(Modifier.padding(2.dp))
            Line(stringResource(Res.string.report_total), stringResource(Res.string.amount_eur, group.total.format()), bold = true)
        }
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
    bold: Boolean = false,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            fontWeight = if (bold) FontWeight.Bold else null,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
