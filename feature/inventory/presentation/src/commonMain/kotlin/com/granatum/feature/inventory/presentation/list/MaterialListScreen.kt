package com.granatum.feature.inventory.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.granatum.core.designsystem.components.buttons.AppFloatingActionButton
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.inventory.domain.model.MaterialCategory
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.MaterialStatus
import com.granatum.feature.inventory.presentation.list.MaterialListAction.OnAddClick
import com.granatum.feature.inventory.presentation.list.MaterialListAction.OnCategorySelected
import com.granatum.feature.inventory.presentation.list.MaterialListAction.OnMaterialClick
import com.granatum.feature.inventory.presentation.list.MaterialListAction.OnRefresh
import com.granatum.feature.inventory.presentation.list.MaterialListAction.OnStatusSelected
import com.granatum.feature.inventory.presentation.list.MaterialListEvent.Error
import com.granatum.feature.inventory.presentation.list.MaterialListEvent.NavigateToCreate
import com.granatum.feature.inventory.presentation.list.MaterialListEvent.NavigateToDetail
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MaterialListRoot(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: MaterialListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is NavigateToDetail -> onNavigateToDetail(event.materialId)
            NavigateToCreate -> onNavigateToCreate()
            is Error -> scope.launch { snackbarHostState.showSnackbar(event.message.asStringAsync()) }
        }
    }

    MaterialListScreen(
        state = state,
        queryState = viewModel.queryState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun MaterialListScreen(
    state: MaterialListUiState,
    queryState: TextFieldState,
    onAction: (MaterialListAction) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = "Inventario") },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AppFloatingActionButton(onClick = { onAction(OnAddClick) }) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppTextField(
                    state = queryState,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Buscar material…",
                    singleLine = true
                )

                FilterRow(
                    label = "Categoría",
                    options = MaterialCategory.entries,
                    optionLabel = { it.toDisplayName() },
                    selected = state.filter.category,
                    onSelected = { onAction(OnCategorySelected(it)) }
                )

                FilterRow(
                    label = "Estado",
                    options = MaterialStatus.entries,
                    optionLabel = { it.toDisplayName() },
                    selected = state.filter.status,
                    onSelected = { onAction(OnStatusSelected(it)) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.extended.surfaceOutline)

            if (state.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        text = if (state.isRefreshing) {
                            "Cargando materiales…"
                        } else {
                            "No hay materiales que coincidan con el filtro."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.extended.textPlaceholder
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(items = state.items, key = { it.id }) { material ->
                        MaterialRow(material = material, onClick = { onAction(OnMaterialClick(material.id)) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.extended.surfaceOutline)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> FilterRow(
    label: String,
    options: List<T>,
    optionLabel: (T) -> String,
    selected: T?,
    onSelected: (T?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.extended.textSecondary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                AppButton(
                    text = "Todos",
                    onClick = { onSelected(null) },
                    style = if (selected == null) AppButtonStyle.PRIMARY else AppButtonStyle.SECONDARY
                )
            }
            items(items = options) { option ->
                AppButton(
                    text = optionLabel(option),
                    onClick = { onSelected(option) },
                    style = if (selected == option) AppButtonStyle.PRIMARY else AppButtonStyle.SECONDARY
                )
            }
        }
    }
}

@Composable
private fun MaterialRow(material: MaterialModel, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = material.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
            Text(
                text = "${material.category.toDisplayName()} · ${material.quantity} uds. · ${material.location ?: "sin ubicación"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textPlaceholder
            )
            StatusBadge(status = material.status)
        }
    }
}

@Composable
private fun StatusBadge(status: MaterialStatus) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = status.toBadgeColor()
    ) {
        Text(
            text = status.toDisplayName(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MaterialStatus.toBadgeColor() = when (this) {
    MaterialStatus.DISPONIBLE -> MaterialTheme.colorScheme.primaryContainer
    MaterialStatus.STOCK_BAJO -> MaterialTheme.colorScheme.tertiaryContainer
    MaterialStatus.AGOTADO -> MaterialTheme.colorScheme.errorContainer
    MaterialStatus.RESERVADO -> MaterialTheme.colorScheme.secondaryContainer
    MaterialStatus.DANADO -> MaterialTheme.colorScheme.errorContainer
}

fun MaterialCategory.toDisplayName(): String = when (this) {
    MaterialCategory.FLORES -> "Flores"
    MaterialCategory.PLANTAS -> "Plantas"
    MaterialCategory.ACCESORIOS -> "Accesorios"
    MaterialCategory.MOBILIARIO -> "Mobiliario"
    MaterialCategory.DECORACION -> "Decoración"
    MaterialCategory.OTROS -> "Otros"
}

fun MaterialStatus.toDisplayName(): String = when (this) {
    MaterialStatus.DISPONIBLE -> "Disponible"
    MaterialStatus.STOCK_BAJO -> "Stock bajo"
    MaterialStatus.AGOTADO -> "Agotado"
    MaterialStatus.RESERVADO -> "Reservado"
    MaterialStatus.DANADO -> "Dañado"
}

@org.jetbrains.compose.ui.tooling.preview.Preview
@Composable
private fun MaterialListScreenPreview() {
    AppTheme {
        MaterialListScreen(
            state = MaterialListUiState(),
            queryState = TextFieldState(),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
