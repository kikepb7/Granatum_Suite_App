package com.granatum.feature.invoicing.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.invoicing.presentation.common.ErrorState
import com.granatum.feature.invoicing.presentation.common.LoadingState
import com.granatum.feature.invoicing.presentation.common.SectionTitle
import com.granatum.feature.invoicing.presentation.common.label
import granatumsuite.feature.invoicing.presentation.generated.resources.*
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InvoiceHistoryRoot(
    invoiceId: String,
    onNavigateBack: () -> Unit,
    viewModel: InvoiceHistoryViewModel = koinViewModel { parametersOf(invoiceId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.history_title), onBackClick = onNavigateBack) },
    ) { padding ->
        val history = state.history
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            history == null -> ErrorState(state.error ?: return@Scaffold, onRetry = viewModel::retry, Modifier.padding(padding))
            else ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(stringResource(Res.string.history_changes))
                    if (history.changes.isEmpty()) {
                        Text(
                            stringResource(Res.string.history_empty),
                            color = MaterialTheme.colorScheme.extended.textPlaceholder,
                        )
                    }
                    history.changes.forEach { change ->
                        Entry {
                            Text("${stringResource(change.action.label())} · ${change.at.label()}", fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(Res.string.history_author, change.authorId.take(8)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            change.previousValues.forEach { (field, value) ->
                                Text(
                                    stringResource(Res.string.history_previous, field, value),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.extended.textSecondary,
                                )
                            }
                        }
                    }
                    SectionTitle(stringResource(Res.string.history_recognitions))
                    if (history.recognitions.isEmpty()) {
                        Text(
                            stringResource(Res.string.history_empty),
                            color = MaterialTheme.colorScheme.extended.textPlaceholder,
                        )
                    }
                    history.recognitions.forEach { attempt ->
                        Entry {
                            Text(
                                stringResource(Res.string.history_recognition_item, attempt.result, attempt.createdAt.label()),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(attempt.model, style = MaterialTheme.typography.bodySmall)
                            attempt.error?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun Entry(content: @Composable () -> Unit) {
    AppCard(contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}
