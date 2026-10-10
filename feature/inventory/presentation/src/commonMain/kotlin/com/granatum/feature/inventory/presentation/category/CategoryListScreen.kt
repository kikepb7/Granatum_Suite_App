package com.granatum.feature.inventory.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.buttons.AppFloatingActionButton
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.dialogs.AppBottomSheet
import com.granatum.core.designsystem.components.dialogs.AppDestructiveConfirmationDialog
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.inventory.domain.validation.CategoryIssue
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoryListRoot(onNavigateBack: () -> Unit, viewModel: CategoryListViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.categories_title), onBackClick = onNavigateBack) },
        floatingActionButton = {
            AppFloatingActionButton(onClick = { viewModel.onAction(CategoryAction.OnNew) }) { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.category_new)) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.extended.redCardText, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
            if (state.rows.isEmpty()) {
                Text(stringResource(Res.string.categories_empty), color = MaterialTheme.colorScheme.extended.textPlaceholder, modifier = Modifier.padding(24.dp))
            }
            LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.rows, key = { it.category.id }) { row ->
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(row.category.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.extended.textPrimary)
                            row.category.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.extended.textSecondary) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppButton(stringResource(Res.string.edit), onClick = { viewModel.onAction(CategoryAction.OnEdit(row.category)) }, style = AppButtonStyle.SECONDARY)
                                if (row.canDelete) {
                                    AppButton(stringResource(Res.string.delete), onClick = { viewModel.onAction(CategoryAction.OnDelete(row.category)) }, style = AppButtonStyle.DESTRUCTIVE_SECONDARY)
                                } else {
                                    Text(stringResource(Res.string.category_in_use, row.materialCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.extended.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (state.editingId != null) {
        AppBottomSheet(onDismiss = { viewModel.onAction(CategoryAction.OnDismissEditor) }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (state.editingId.isNullOrEmpty()) Res.string.category_new else Res.string.category_edit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.extended.textPrimary)
                val nameError = when {
                    CategoryIssue.NAME_REQUIRED in state.issues -> stringResource(Res.string.category_name_required)
                    CategoryIssue.NAME_TOO_LONG in state.issues -> stringResource(Res.string.category_name_too_long)
                    else -> null
                }
                AppTextField(state = viewModel.name, title = stringResource(Res.string.category_name), singleLine = true, isError = nameError != null, supportingText = nameError, modifier = Modifier.fillMaxWidth())
                val descError = if (CategoryIssue.DESCRIPTION_TOO_LONG in state.issues) stringResource(Res.string.category_description_too_long) else null
                AppTextField(state = viewModel.description, title = stringResource(Res.string.category_description), isError = descError != null, supportingText = descError, modifier = Modifier.fillMaxWidth())
                state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.extended.redCardText, style = MaterialTheme.typography.bodySmall) }
                AppButton(stringResource(Res.string.category_save), onClick = { viewModel.onAction(CategoryAction.OnSave) }, isLoading = state.isBusy, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    state.deleting?.let { c ->
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.category_delete_title),
            description = stringResource(Res.string.category_delete_description, c.name),
            confirmButtonText = stringResource(Res.string.delete_confirm),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = { viewModel.onAction(CategoryAction.OnConfirmDelete) },
            onCancelClick = { viewModel.onAction(CategoryAction.OnDismissDelete) },
            onDismiss = { viewModel.onAction(CategoryAction.OnDismissDelete) }
        )
    }
}
