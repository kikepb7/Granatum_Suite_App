package com.granatum.feature.clockin.presentation.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.cards.AppAccentCard
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.CorrectionState
import com.granatum.feature.clockin.presentation.common.TimelineRow
import com.granatum.feature.clockin.presentation.common.timeLabel
import com.granatum.feature.clockin.presentation.common.workedLabel
import com.granatum.feature.clockin.presentation.history.Flags
import com.granatum.feature.clockin.presentation.mapper.label
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ShiftDetailRoot(
    shiftKey: String,
    onBack: () -> Unit,
    onRequestCorrection: (String) -> Unit,
    refreshToken: Int = 0,
    viewModel: ShiftDetailViewModel = koinViewModel(key = shiftKey) { parametersOf(shiftKey) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Coming back from a correction that was just sent: show it.
    LaunchedEffect(refreshToken) { state.shift?.serverId?.let { if (refreshToken > 0) viewModel.reload(it) } }
    ShiftDetailScreen(state, onBack, onRequestCorrection)
}

@Composable
fun ShiftDetailScreen(state: ShiftDetailUiState, onBack: () -> Unit, onRequestCorrection: (String) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.shift_detail_title), onBackClick = onBack) }
    ) { padding ->
        val shift = state.shift
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (shift == null) {
                if (state.loaded) Text(stringResource(Res.string.shift_not_found), color = MaterialTheme.colorScheme.extended.textPlaceholder)
                return@Column
            }
            Text(
                text = "${shift.start.timeLabel()} – ${shift.end?.timeLabel() ?: stringResource(Res.string.shift_in_progress)}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
            workedLabel(shift.workedMinutes, shift.isWorkedMinutesProvisional)?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.extended.textSecondary)
            }
            Flags(shift)
            shift.items.forEach { TimelineRow(it) }

            Text(
                text = stringResource(Res.string.corrections_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.extended.textPrimary,
                modifier = Modifier.padding(top = 16.dp).semantics { heading() }
            )
            when {
                state.correctionsUnavailable -> Text(stringResource(Res.string.corrections_offline), color = MaterialTheme.colorScheme.extended.textPlaceholder)
                state.corrections.isEmpty() -> Text(stringResource(Res.string.corrections_empty), color = MaterialTheme.colorScheme.extended.textPlaceholder)
                else -> state.corrections.forEach { CorrectionCard(it) }
            }
            if (shift.canRequestCorrection) {
                AppButton(
                    text = stringResource(Res.string.request_correction),
                    onClick = { onRequestCorrection(shift.serverId!!) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            } else {
                Text(stringResource(Res.string.correction_only_closed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.extended.textSecondary)
            }
        }
    }
}

@Composable
private fun CorrectionCard(correction: CorrectionModel) {
    val tone = when (correction.state) {
        CorrectionState.PENDIENTE -> AppTone.WARNING
        CorrectionState.APROBADA -> AppTone.SUCCESS
        CorrectionState.RECHAZADA -> AppTone.DANGER
    }
    AppAccentCard(tone = tone, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val color = when (correction.state) {
                CorrectionState.PENDIENTE -> MaterialTheme.colorScheme.extended.yellowCardText
                CorrectionState.APROBADA -> MaterialTheme.colorScheme.extended.success
                CorrectionState.RECHAZADA -> MaterialTheme.colorScheme.extended.redCardText
            }
            Text(stringResource(correction.state.label()), style = MaterialTheme.typography.labelLarge, color = color)
            Text(
                text = "${correction.proposed.entry.timeLabel()} – ${correction.proposed.exit.timeLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
            Text(correction.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.extended.textSecondary)
            correction.resolutionReason?.let {
                Text(stringResource(Res.string.correction_resolution, it), style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}
