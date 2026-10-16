package com.granatum.feature.inventory.presentation.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppPrimaryButton
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.chips.AppFilterChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.feedback.AppBanner
import com.granatum.core.designsystem.components.feedback.AppLoadingState
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.components.lists.AppIconBadge
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.presentation.documents.DocumentPicker
import com.granatum.core.presentation.documents.DocumentSource
import com.granatum.core.presentation.documents.rememberDocumentPicker
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.inventory.domain.validation.DraftIssue
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import com.granatum.feature.inventory.presentation.common.label
import com.granatum.feature.inventory.presentation.components.LocationPicker
import com.granatum.feature.inventory.presentation.components.ProductImage
import granatumsuite.feature.inventory.presentation.generated.resources.*
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProductFormRoot(
    productId: String?,
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProductFormViewModel = koinViewModel(key = "form-${productId ?: "new"}") { parametersOf(productId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.eventFlow) { if (it is ProductFormEvent.Saved) onSaved() }
    val picker = rememberDocumentPicker { documents ->
        documents.firstOrNull { it.mimeType.startsWith("image/") }?.let { viewModel.onAction(ProductFormAction.OnImagePicked(it.bytes, it.fileName, it.mimeType)) }
    }
    ProductFormScreen(
        state = state,
        fields = FormFields(viewModel.description, viewModel.height, viewModel.width, viewModel.quantity),
        picker = picker,
        onAction = viewModel::onAction,
        onBack = onNavigateBack
    )
}

private class FormFields(val description: TextFieldState, val height: TextFieldState, val width: TextFieldState, val quantity: TextFieldState)

@Composable
private fun ProductFormScreen(
    state: ProductFormState,
    fields: FormFields,
    picker: DocumentPicker,
    onAction: (ProductFormAction) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(if (state.isEdit) Res.string.form_edit else Res.string.form_new), onBackClick = onBack) }
    ) { padding ->
        if (!state.ready) {
            AppLoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = 12.dp)
        ) {
            ImageSection(state, picker, onAction)
            CategorySection(state, onAction)
            Section(AppIcons.Box, Res.string.field_description) {
                val error = when {
                    DraftIssue.DESCRIPTION_REQUIRED in state.issues -> stringResource(Res.string.required)
                    DraftIssue.DESCRIPTION_TOO_LONG in state.issues -> stringResource(Res.string.too_long, InventoryValidation.DESCRIPTION_MAX)
                    else -> null
                }
                AppTextField(
                    state = fields.description,
                    placeholder = stringResource(Res.string.form_description_hint),
                    isError = error != null,
                    supportingText = error ?: "${fields.description.text.length}/${InventoryValidation.DESCRIPTION_MAX}",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Section(AppIcons.Ruler, Res.string.field_dimensions) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberInput(fields.height, Res.string.field_height, NumberField.HEIGHT in state.badNumbers, KeyboardType.Decimal, Modifier.weight(1f))
                    NumberInput(fields.width, Res.string.field_width, NumberField.WIDTH in state.badNumbers, KeyboardType.Decimal, Modifier.weight(1f))
                }
                if (DraftIssue.INVALID_SIZE in state.issues) ErrorText(stringResource(Res.string.invalid_size))
            }
            Section(AppIcons.Plus, Res.string.field_quantity) {
                if (state.isEdit) {
                    Text(stringResource(Res.string.form_quantity_edit_hint), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
                } else {
                    NumberInput(
                        fields.quantity,
                        Res.string.units_short,
                        NumberField.QUANTITY in state.badNumbers || DraftIssue.NEGATIVE_QUANTITY in state.issues,
                        KeyboardType.Number,
                        Modifier.fillMaxWidth()
                    )
                }
            }
            Section(AppIcons.Location, Res.string.field_location, hint = Res.string.form_location_hint) {
                LocationPicker(selected = state.location, onSelect = { onAction(ProductFormAction.OnLocation(it)) })
                state.location?.let { location ->
                    Text(location.label(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.brand)
                }
                if (DraftIssue.LOCATION_REQUIRED in state.issues) ErrorText(stringResource(Res.string.location_required))
            }
            state.error?.let { AppBanner(title = it.asString(), tone = AppTone.DANGER) }
            AppPrimaryButton(
                text = stringResource(Res.string.form_save),
                onClick = { onAction(ProductFormAction.OnSave) },
                isLoading = state.isSaving,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun ImageSection(state: ProductFormState, picker: DocumentPicker, onAction: (ProductFormAction) -> Unit) {
    Section(AppIcons.Image, Res.string.field_image, hint = Res.string.form_image_hint) {
        if (state.hasImage) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
                ProductImage(image = state.shownImage, categoryName = "", corner = 18.dp, modifier = Modifier.matchParentSize())
                if (state.isUploadingImage) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.matchParentSize().background(AppTheme.colors.scrim, AppTheme.shapes.card)
                    ) {
                        CircularProgressIndicator(color = AppTheme.colors.onBrand, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                        Text(stringResource(Res.string.form_image_uploading), style = MaterialTheme.typography.caption, color = AppTheme.colors.onBrand, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PickButton(AppIcons.Camera, Res.string.form_image_camera_short, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { picker.launch(DocumentSource.CAMERA) }
                PickButton(AppIcons.Image, Res.string.form_image_gallery_short, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { picker.launch(DocumentSource.GALLERY) }
                TextButton(onClick = { onAction(ProductFormAction.OnRemoveImage) }, enabled = !state.isSaving) {
                    Text(stringResource(Res.string.form_image_remove), color = AppTheme.colors.danger)
                }
            }
        } else {
            Text(stringResource(Res.string.form_image_add), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickTile(AppIcons.Camera, Res.string.form_image_camera, Modifier.weight(1f)) { picker.launch(DocumentSource.CAMERA) }
                PickTile(AppIcons.Image, Res.string.form_image_gallery, Modifier.weight(1f)) { picker.launch(DocumentSource.GALLERY) }
            }
        }
        state.imageError?.let { ErrorText(it.asString()) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(state: ProductFormState, onAction: (ProductFormAction) -> Unit) {
    Section(AppIcons.Box, Res.string.field_category, hint = Res.string.form_category_hint) {
        if (state.categories.isEmpty()) {
            ErrorText(stringResource(Res.string.form_no_categories))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.forEach { category ->
                AppFilterChip(
                    text = category.name,
                    selected = state.categoryId == category.id,
                    onClick = { onAction(ProductFormAction.OnCategory(category.id)) },
                    leadingIcon = if (state.categoryId == category.id) {
                        { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    }
                )
            }
        }
        if (DraftIssue.CATEGORY_REQUIRED in state.issues) ErrorText(stringResource(Res.string.required))
    }
}

/** A titled card: every group of the form looks and reads the same. */
@Composable
private fun Section(
    icon: ImageVector,
    title: org.jetbrains.compose.resources.StringResource,
    hint: org.jetbrains.compose.resources.StringResource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIconBadge(icon = icon)
            Column {
                Text(
                    stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                hint?.let { Text(stringResource(it), style = MaterialTheme.typography.caption, color = AppTheme.colors.textTertiary) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) { content() }
    }
}

@Composable
private fun NumberInput(
    state: TextFieldState,
    title: org.jetbrains.compose.resources.StringResource,
    isError: Boolean,
    keyboard: KeyboardType,
    modifier: Modifier
) {
    AppTextField(
        state = state,
        title = stringResource(title),
        singleLine = true,
        keyboardType = keyboard,
        isError = isError,
        supportingText = if (isError) stringResource(Res.string.invalid_number) else null,
        modifier = modifier
    )
}

/** The large camera / gallery tiles shown while there is no image yet. */
@Composable
private fun PickTile(icon: ImageVector, label: org.jetbrains.compose.resources.StringResource, modifier: Modifier, onClick: () -> Unit) {
    AppCard(onClick = onClick, containerColor = AppTheme.colors.surfaceSunken, border = null, contentPadding = 16.dp, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = AppTheme.colors.brand, modifier = Modifier.size(28.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.textPrimary, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun PickButton(icon: ImageVector, label: org.jetbrains.compose.resources.StringResource, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = AppTheme.shapes.pill,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null, tint = AppTheme.colors.brand, modifier = Modifier.size(18.dp))
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.danger)
}
