package com.granatum.feature.clockin.presentation.correction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.textfields.AppMultiLineTextField
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.presentation.mapper.label
import com.granatum.feature.clockin.presentation.mapper.toUiText
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CorrectionRoot(
    shiftServerId: String,
    onBack: () -> Unit,
    onSent: () -> Unit,
    viewModel: CorrectionViewModel = koinViewModel(key = "correction-$shiftServerId") { parametersOf(shiftServerId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { if (it == CorrectionEvent.Sent) onSent() }
    CorrectionScreen(state, viewModel, onBack)
}

@Composable
private fun CorrectionScreen(state: CorrectionUiState, viewModel: CorrectionViewModel, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.correction_title), onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.ready) return@Column
            AppTextField(state = viewModel.entry, title = stringResource(Res.string.correction_entry), singleLine = true, keyboardType = KeyboardType.Number, modifier = Modifier.fillMaxWidth())
            AppTextField(state = viewModel.exit, title = stringResource(Res.string.correction_exit), singleLine = true, keyboardType = KeyboardType.Number, modifier = Modifier.fillMaxWidth())

            state.breaks.forEachIndexed { index, b ->
                Text(
                    text = stringResource(b.type.label()),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.extended.textPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(state = b.start, title = stringResource(Res.string.correction_break_start), singleLine = true, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    AppTextField(state = b.end, title = stringResource(Res.string.correction_break_end), singleLine = true, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
                AppButton(
                    text = stringResource(Res.string.correction_remove_break),
                    onClick = { viewModel.onAction(CorrectionAction.OnRemoveBreak(index)) },
                    style = AppButtonStyle.TEXT
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BreakType.entries.forEach { type ->
                    AppButton(
                        text = "+ " + stringResource(type.label()),
                        onClick = { viewModel.onAction(CorrectionAction.OnAddBreak(type)) },
                        style = AppButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = stringResource(Res.string.correction_reason),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.extended.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
            AppMultiLineTextField(
                state = viewModel.reason,
                placeholder = stringResource(Res.string.correction_reason_placeholder),
                modifier = Modifier.fillMaxWidth()
            )

            val problems = buildList {
                if (state.badTime) add(stringResource(Res.string.correction_bad_time))
                state.issues.forEach { add(it.toUiText().asString()) }
                state.error?.let { add(it.asString()) }
            }
            problems.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.extended.redCardText) }

            AppButton(
                text = stringResource(Res.string.correction_submit),
                onClick = { viewModel.onAction(CorrectionAction.OnSubmit) },
                isLoading = state.isSending,
                enabled = !state.isSending,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
