package com.granatum.feature.inventory.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.inventory.domain.model.MaterialCategory
import com.granatum.feature.inventory.presentation.form.MaterialFormAction.OnCategorySelected
import com.granatum.feature.inventory.presentation.form.MaterialFormAction.OnSaveClick
import com.granatum.feature.inventory.presentation.form.MaterialFormEvent.Error
import com.granatum.feature.inventory.presentation.form.MaterialFormEvent.Saved
import com.granatum.feature.inventory.presentation.list.toDisplayName
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MaterialFormRoot(
    materialId: String?,
    onSaved: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MaterialFormViewModel = koinViewModel(parameters = { parametersOf(materialId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is Saved -> onSaved(event.materialId)
            is Error -> scope.launch { snackbarHostState.showSnackbar(event.message.asStringAsync()) }
        }
    }

    MaterialFormScreen(
        state = state,
        nameState = viewModel.nameState,
        sizeState = viewModel.sizeState,
        colorState = viewModel.colorState,
        locationState = viewModel.locationState,
        notesState = viewModel.notesState,
        quantityState = viewModel.quantityState,
        onAction = viewModel::onAction,
        onBackClick = onNavigateBack,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun MaterialFormScreen(
    state: MaterialFormUiState,
    nameState: TextFieldState,
    sizeState: TextFieldState,
    colorState: TextFieldState,
    locationState: TextFieldState,
    notesState: TextFieldState,
    quantityState: TextFieldState,
    onAction: (MaterialFormAction) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(
                title = if (state.isEditing) "Editar material" else "Nuevo material",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppTextField(state = nameState, title = "Nombre", singleLine = true, modifier = Modifier.fillMaxWidth())

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = MaterialCategory.entries.toList()) { category ->
                    AppButton(
                        text = category.toDisplayName(),
                        onClick = { onAction(OnCategorySelected(category)) },
                        style = if (state.category == category) AppButtonStyle.PRIMARY else AppButtonStyle.SECONDARY
                    )
                }
            }

            AppTextField(
                state = quantityState,
                title = "Cantidad",
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )
            AppTextField(state = sizeState, title = "Tamaño", singleLine = true, modifier = Modifier.fillMaxWidth())
            AppTextField(state = colorState, title = "Color", singleLine = true, modifier = Modifier.fillMaxWidth())
            AppTextField(state = locationState, title = "Ubicación", singleLine = true, modifier = Modifier.fillMaxWidth())
            AppTextField(state = notesState, title = "Notas", modifier = Modifier.fillMaxWidth())

            AppButton(
                text = "Guardar",
                onClick = { onAction(OnSaveClick) },
                isLoading = state.isSaving,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
