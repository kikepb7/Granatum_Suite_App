package com.granatum.feature.inventory.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.dialogs.AppBottomSheet
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.validation.AdjustmentIssue
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Quantity and reason together: the server keeps the reason in the material's history (FR-005). */
@Composable
fun AdjustQuantitySheet(material: MaterialModel, state: MaterialDetailState, viewModel: MaterialDetailViewModel) {
    AppBottomSheet(onDismiss = { viewModel.onAction(MaterialDetailAction.OnAdjustDismiss) }) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.adjust_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.extended.textPrimary)
            Text(stringResource(Res.string.adjust_current, material.available, material.total), color = MaterialTheme.colorScheme.extended.textSecondary)
            val quantityError = when {
                state.adjustNotANumber -> stringResource(Res.string.adjust_not_a_number)
                AdjustmentIssue.OUT_OF_RANGE in state.adjustIssues -> stringResource(Res.string.adjust_out_of_range, material.total)
                AdjustmentIssue.UNCHANGED in state.adjustIssues -> stringResource(Res.string.adjust_unchanged)
                else -> null
            }
            AppTextField(
                state = viewModel.newQuantity,
                title = stringResource(Res.string.adjust_new),
                keyboardType = KeyboardType.Number,
                singleLine = true,
                isError = quantityError != null,
                supportingText = quantityError,
                modifier = Modifier.fillMaxWidth()
            )
            AppTextField(
                state = viewModel.reason,
                title = stringResource(Res.string.adjust_reason),
                placeholder = stringResource(Res.string.adjust_reason_placeholder),
                isError = AdjustmentIssue.REASON_REQUIRED in state.adjustIssues,
                supportingText = if (AdjustmentIssue.REASON_REQUIRED in state.adjustIssues) stringResource(Res.string.adjust_reason_required) else null,
                modifier = Modifier.fillMaxWidth()
            )
            AppButton(stringResource(Res.string.adjust_save), onClick = { viewModel.onAction(MaterialDetailAction.OnAdjustSave) }, isLoading = state.isBusy, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth())
        }
    }
}
