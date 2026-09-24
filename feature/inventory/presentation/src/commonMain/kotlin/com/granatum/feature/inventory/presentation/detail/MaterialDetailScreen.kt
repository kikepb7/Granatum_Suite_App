package com.granatum.feature.inventory.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.dialogs.AppBottomSheet
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.inventory.domain.model.StockMovementModel
import com.granatum.feature.inventory.presentation.detail.MaterialDetailAction.OnConfirmQuantityUpdate
import com.granatum.feature.inventory.presentation.detail.MaterialDetailAction.OnDismissQuantitySheet
import com.granatum.feature.inventory.presentation.detail.MaterialDetailAction.OnEditClick
import com.granatum.feature.inventory.presentation.detail.MaterialDetailAction.OnUpdateQuantityClick
import com.granatum.feature.inventory.presentation.detail.MaterialDetailEvent.Error
import com.granatum.feature.inventory.presentation.detail.MaterialDetailEvent.NavigateToEdit
import com.granatum.feature.inventory.presentation.list.toDisplayName
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MaterialDetailRoot(
    materialId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: MaterialDetailViewModel = koinViewModel(parameters = { parametersOf(materialId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is NavigateToEdit -> onNavigateToEdit(event.materialId)
            is Error -> scope.launch { snackbarHostState.showSnackbar(event.message.asStringAsync()) }
        }
    }

    MaterialDetailScreen(
        state = state,
        newQuantityState = viewModel.newQuantityState,
        reasonState = viewModel.reasonState,
        onAction = viewModel::onAction,
        onBackClick = onNavigateBack,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun MaterialDetailScreen(
    state: MaterialDetailUiState,
    newQuantityState: TextFieldState,
    reasonState: TextFieldState,
    onAction: (MaterialDetailAction) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val material = state.material

    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = material?.name ?: "Material", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (material == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(text = "Cargando…", color = MaterialTheme.colorScheme.extended.textPlaceholder)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoRow(label = "Categoría", value = material.category.toDisplayName())
            InfoRow(label = "Estado", value = material.status.toDisplayName())
            InfoRow(label = "Cantidad", value = "${material.quantity} unidades")
            material.size?.let { InfoRow(label = "Tamaño", value = it) }
            material.color?.let { InfoRow(label = "Color", value = it) }
            material.location?.let { InfoRow(label = "Ubicación", value = it) }
            material.notes?.let { InfoRow(label = "Notas", value = it) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    text = "Actualizar cantidad",
                    onClick = { onAction(OnUpdateQuantityClick) },
                    modifier = Modifier.fillMaxWidth()
                )
                AppButton(
                    text = "Editar material",
                    style = AppButtonStyle.SECONDARY,
                    onClick = { onAction(OnEditClick) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Historial de cambios",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.extended.textPrimary
            )

            if (state.history.isEmpty()) {
                Text(
                    text = "Sin movimientos registrados todavía.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.extended.textPlaceholder
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(items = state.history, key = { it.id }) { movement ->
                        StockMovementRow(movement = movement)
                        HorizontalDivider(color = MaterialTheme.colorScheme.extended.surfaceOutline)
                    }
                }
            }
        }

        if (state.isQuantitySheetVisible) {
            AppBottomSheet(onDismiss = { onAction(OnDismissQuantitySheet) }) {
                QuantityUpdateContent(
                    currentQuantity = material.quantity,
                    newQuantityState = newQuantityState,
                    reasonState = reasonState,
                    isSubmitting = state.isUpdatingQuantity,
                    onConfirm = { onAction(OnConfirmQuantityUpdate) }
                )
            }
        }
    }
}

@Composable
private fun QuantityUpdateContent(
    currentQuantity: Int,
    newQuantityState: TextFieldState,
    reasonState: TextFieldState,
    isSubmitting: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Actualizar cantidad",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = "Cantidad actual: $currentQuantity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textPlaceholder
        )
        AppTextField(
            state = newQuantityState,
            title = "Nueva cantidad",
            singleLine = true,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth()
        )
        AppTextField(
            state = reasonState,
            title = "Motivo (obligatorio)",
            placeholder = "Ej: entrada de proveedor, uso en evento…",
            modifier = Modifier.fillMaxWidth()
        )
        AppButton(
            text = "Guardar cambio",
            onClick = onConfirm,
            isLoading = isSubmitting,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.extended.textSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.extended.textPrimary)
    }
}

@Composable
private fun StockMovementRow(movement: StockMovementModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "${movement.previousQuantity} → ${movement.newQuantity}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = "${movement.reason} · ${movement.changedByUsername}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textPlaceholder
        )
    }
}
