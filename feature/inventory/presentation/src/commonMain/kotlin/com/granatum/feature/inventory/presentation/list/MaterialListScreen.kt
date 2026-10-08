package com.granatum.feature.inventory.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppAccountButton
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.presentation.common.label
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MaterialListRoot(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToCategories: () -> Unit,
    viewModel: MaterialListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MaterialListScreen(state, viewModel, onNavigateToDetail, onNavigateToCreate, onNavigateToCategories)
}

@Composable
private fun MaterialListScreen(
    state: MaterialListState,
    viewModel: MaterialListViewModel,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onCategories: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(Res.string.inventory_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.extended.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCategories) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(Res.string.manage_categories)) }
                IconButton(onClick = { viewModel.onAction(MaterialListAction.OnRefresh) }) { Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh)) }
                AppAccountButton()
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_material)) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.isStale) {
                Text(
                    stringResource(Res.string.list_stale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.extended.yellowCardText,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
            AppTextField(
                state = viewModel.query,
                placeholder = stringResource(Res.string.search_placeholder),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Filters(state, viewModel)
            when {
                state.materials.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        stringResource(if (state.isEmptyInventory) Res.string.list_empty else Res.string.list_empty_filtered),
                        color = MaterialTheme.colorScheme.extended.textPlaceholder
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.materials, key = { it.id }) { MaterialCard(it, onClick = { onOpen(it.id) }) }
                }
            }
        }
    }
}

@Composable
private fun Filters(state: MaterialListState, viewModel: MaterialListViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.filter.categoryId == null, onClick = { viewModel.onAction(MaterialListAction.OnCategory(null)) }, label = { Text(stringResource(Res.string.filter_all_categories)) })
            state.categories.forEach { c ->
                FilterChip(selected = state.filter.categoryId == c.id, onClick = { viewModel.onAction(MaterialListAction.OnCategory(c.id)) }, label = { Text(c.name) })
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.filter.onlyOutOfStock, onClick = { viewModel.onAction(MaterialListAction.OnToggleOutOfStock) }, label = { Text(stringResource(Res.string.filter_out_of_stock)) })
            FilterChip(selected = state.filter.condition == null, onClick = { viewModel.onAction(MaterialListAction.OnCondition(null)) }, label = { Text(stringResource(Res.string.filter_any_condition)) })
            MaterialCondition.entries.forEach { c ->
                FilterChip(selected = state.filter.condition == c, onClick = { viewModel.onAction(MaterialListAction.OnCondition(c)) }, label = { Text(stringResource(c.label())) })
            }
        }
    }
}

@Composable
private fun MaterialCard(material: MaterialModel, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.extended.surfaceHigher,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    material.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.extended.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (material.isOutOfStock) stringResource(Res.string.out_of_stock) else stringResource(Res.string.stock_of, material.available, material.total),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (material.isOutOfStock) MaterialTheme.colorScheme.extended.redCardText else MaterialTheme.colorScheme.extended.success
                )
            }
            Text(
                "${material.category.name} · ${stringResource(material.condition.label())} · ${material.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textSecondary
            )
        }
    }
}
