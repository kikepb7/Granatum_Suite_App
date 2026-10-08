package com.granatum.feature.invoicing.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.documents.SharedFile
import com.granatum.core.presentation.documents.rememberFileOpener
import com.granatum.feature.invoicing.presentation.common.ErrorState
import com.granatum.feature.invoicing.presentation.common.LoadingState
import granatumsuite.feature.invoicing.presentation.generated.resources.*
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InvoiceOriginalRoot(
    invoiceId: String,
    onNavigateBack: () -> Unit,
    viewModel: InvoiceOriginalViewModel = koinViewModel { parametersOf(invoiceId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val opener = rememberFileOpener()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.original_title), onBackClick = onNavigateBack) },
    ) { padding ->
        val file = state.file
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            file == null -> ErrorState(state.error ?: return@Scaffold, onRetry = viewModel::retry, Modifier.padding(padding))
            else ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val shared = SharedFile(file.bytes, file.fileName, file.mimeType)
                    if (file.isImage) {
                        // Coil decodes the bytes directly; nothing is written to disk to look at it.
                        AsyncImage(
                            model = file.bytes,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    } else {
                        Text(
                            stringResource(Res.string.original_pdf),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppButton(stringResource(Res.string.open), onClick = { opener.open(shared) }, modifier = Modifier.weight(1f))
                        AppButton(stringResource(Res.string.share), onClick = {
                            opener.share(shared)
                        }, style = AppButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
                    }
                }
        }
    }
}
