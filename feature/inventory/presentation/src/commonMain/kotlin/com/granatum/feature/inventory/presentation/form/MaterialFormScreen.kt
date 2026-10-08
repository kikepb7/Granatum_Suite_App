package com.granatum.feature.inventory.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.model.SizeUnit
import com.granatum.feature.inventory.domain.validation.DraftIssue
import com.granatum.feature.inventory.presentation.common.label
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MaterialFormRoot(
    materialId: String?,
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MaterialFormViewModel = koinViewModel(key = "form-${materialId ?: "new"}") { parametersOf(materialId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.eventFlow) { if (it is MaterialFormEvent.Saved) onSaved() }
    MaterialFormScreen(state, viewModel, onNavigateBack)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialFormScreen(state: MaterialFormState, vm: MaterialFormViewModel, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(if (state.isEdit) Res.string.form_edit_title else Res.string.form_new_title), onBackClick = onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.ready) return@Column
            Field(vm.name, Res.string.form_name, issue(state, DraftIssue.NAME_REQUIRED to Res.string.issue_name_required, DraftIssue.NAME_TOO_LONG to Res.string.issue_name_too_long))

            Label(Res.string.form_category)
            if (state.categories.isEmpty()) Text(stringResource(Res.string.form_no_categories), color = MaterialTheme.colorScheme.extended.redCardText)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { c -> FilterChip(selected = state.categoryId == c.id, onClick = { vm.onAction(MaterialFormAction.OnCategory(c.id)) }, label = { Text(c.name) }) }
            }
            if (DraftIssue.CATEGORY_REQUIRED in state.issues) ErrorText(Res.string.issue_category_required)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!state.isEdit) {
                    Field(vm.available, Res.string.form_available, numberError(state, NumberField.AVAILABLE), KeyboardType.Number, Modifier.weight(1f))
                }
                Field(vm.total, Res.string.form_total, numberError(state, NumberField.TOTAL), KeyboardType.Number, Modifier.weight(1f))
            }
            if (DraftIssue.NEGATIVE_QUANTITY in state.issues) ErrorText(Res.string.issue_negative_quantity)
            if (DraftIssue.AVAILABLE_OVER_TOTAL in state.issues) ErrorText(Res.string.issue_available_over_total)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(vm.height, Res.string.form_height, numberError(state, NumberField.HEIGHT), KeyboardType.Decimal, Modifier.weight(1f))
                Field(vm.width, Res.string.form_width, numberError(state, NumberField.WIDTH), KeyboardType.Decimal, Modifier.weight(1f))
            }
            Field(vm.diameter, Res.string.form_diameter, numberError(state, NumberField.DIAMETER), KeyboardType.Decimal)
            Label(Res.string.form_unit)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SizeUnit.entries.forEach { u -> FilterChip(selected = state.unit == u, onClick = { vm.onAction(MaterialFormAction.OnUnit(u)) }, label = { Text(u.name.lowercase()) }) }
            }
            if (DraftIssue.NEGATIVE_SIZE in state.issues) ErrorText(Res.string.issue_negative_size)

            Field(vm.color, Res.string.form_color, issue(state, DraftIssue.COLOR_REQUIRED to Res.string.issue_required))
            Field(vm.physicalMaterial, Res.string.form_material, issue(state, DraftIssue.MATERIAL_REQUIRED to Res.string.issue_required))
            Label(Res.string.form_condition)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MaterialCondition.entries.forEach { c -> FilterChip(selected = state.condition == c, onClick = { vm.onAction(MaterialFormAction.OnCondition(c)) }, label = { Text(stringResource(c.label())) }) }
            }
            Field(vm.location, Res.string.form_location, issue(state, DraftIssue.LOCATION_REQUIRED to Res.string.issue_required))
            Field(vm.price, Res.string.form_price, numberError(state, NumberField.PRICE) ?: issue(state, DraftIssue.NEGATIVE_PRICE to Res.string.issue_negative_price), KeyboardType.Decimal)
            Field(vm.supplier, Res.string.form_supplier, issue(state, DraftIssue.SUPPLIER_REQUIRED to Res.string.issue_required))
            if (state.isEdit && state.photos.isNotEmpty()) {
                Text(stringResource(Res.string.form_photos_kept), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.extended.textSecondary)
            }
            state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.extended.redCardText, style = MaterialTheme.typography.bodySmall) }
            AppButton(stringResource(Res.string.form_save), onClick = { vm.onAction(MaterialFormAction.OnSave) }, isLoading = state.isSaving, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Field(state: TextFieldState, title: StringResource, error: String?, keyboard: KeyboardType = KeyboardType.Text, modifier: Modifier = Modifier.fillMaxWidth()) {
    AppTextField(state = state, title = stringResource(title), singleLine = true, keyboardType = keyboard, isError = error != null, supportingText = error, modifier = modifier)
}

@Composable
private fun Label(text: StringResource) {
    Text(stringResource(text), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.extended.textPrimary)
}

@Composable
private fun ErrorText(text: StringResource) {
    Text(stringResource(text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.extended.redCardText)
}

@Composable
private fun issue(state: MaterialFormState, vararg map: Pair<DraftIssue, StringResource>): String? =
    map.firstOrNull { it.first in state.issues }?.let { stringResource(it.second) }

@Composable
private fun numberError(state: MaterialFormState, field: NumberField): String? =
    if (field in state.badNumbers) stringResource(Res.string.form_bad_number) else null
