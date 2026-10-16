package com.granatum.feature.inventory.presentation.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.brand.AppGradientText
import com.granatum.core.designsystem.components.brand.rememberCountUp
import com.granatum.core.designsystem.components.buttons.AppFloatingActionButton
import com.granatum.core.designsystem.components.cards.AppHeroCard
import com.granatum.core.designsystem.components.cards.AppStatCard
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.controls.AppFilterButton
import com.granatum.core.designsystem.components.controls.AppRemovablePill
import com.granatum.core.designsystem.components.controls.AppSegmentedControl
import com.granatum.core.designsystem.components.feedback.AppBanner
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.components.inputs.AppSearchField
import com.granatum.core.designsystem.components.motion.appEntrance
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.components.topbar.TopBarIconButton
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.designsystem.theme.chipLabel
import com.granatum.core.designsystem.theme.largeTitle
import com.granatum.feature.inventory.domain.model.StockFilter
import com.granatum.feature.inventory.domain.model.Warehouse
import com.granatum.feature.inventory.presentation.common.label
import com.granatum.feature.inventory.presentation.components.InventoryFiltersSheet
import com.granatum.feature.inventory.presentation.components.ProductCard
import granatumsuite.feature.inventory.presentation.generated.resources.*
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProductListRoot(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToCategories: () -> Unit,
    viewModel: ProductListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProductListScreen(
        state = state,
        query = viewModel.query,
        onAction = viewModel::onAction,
        onOpen = onNavigateToDetail,
        onCreate = onNavigateToCreate,
        onCategories = onNavigateToCategories
    )
}

@Composable
private fun ProductListScreen(
    state: ProductListState,
    query: androidx.compose.foundation.text.input.TextFieldState,
    onAction: (ProductListAction) -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onCategories: () -> Unit
) {
    val horizontal = AppTheme.spacing.screenHorizontal
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.inventory_title),
                actions = {
                    TopBarIconButton(onClick = onCategories) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(Res.string.manage_categories))
                    }
                    TopBarIconButton(onClick = { onAction(ProductListAction.OnRefresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
                    }
                }
            )
        },
        floatingActionButton = {
            AppFloatingActionButton(onClick = onCreate) { Icon(AppIcons.Plus, contentDescription = stringResource(Res.string.add_product)) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AppTheme.colors.brand, trackColor = AppTheme.colors.brandSoft)
            }
            if (state.isStale) {
                AppBanner(title = stringResource(Res.string.list_stale), tone = AppTone.WARNING, modifier = Modifier.padding(horizontal = horizontal, vertical = 4.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = horizontal, vertical = 8.dp)
            ) {
                AppSearchField(state = query, placeholder = stringResource(Res.string.search_placeholder), modifier = Modifier.weight(1f))
                AppFilterButton(
                    activeCount = state.filter.activeCount - (if (state.filter.warehouse != null) 1 else 0),
                    onClick = { onAction(ProductListAction.OnOpenFilters) },
                    contentDescription = stringResource(Res.string.filters)
                )
            }
            AppSegmentedControl(
                options = listOf<Warehouse?>(null) + Warehouse.entries,
                selected = state.filter.warehouse,
                onSelect = { onAction(ProductListAction.OnWarehouse(it)) },
                label = { it?.name ?: stringResource(Res.string.filter_all) },
                modifier = Modifier.padding(horizontal = horizontal)
            )
            ActivePills(state, onAction)
            when {
                state.products.isEmpty() -> AppEmptyState(
                    title = stringResource(if (state.isEmptyInventory) Res.string.list_empty else Res.string.list_empty_filtered),
                    icon = { Icon(AppIcons.Box, contentDescription = null, tint = AppTheme.colors.brand) },
                    action = if (state.filter.isNarrowed && !state.isEmptyInventory) {
                        { TextButton(onClick = { onAction(ProductListAction.OnClearFilters) }) { Text(stringResource(Res.string.filters_clear), color = AppTheme.colors.brand) } }
                    } else {
                        null
                    }
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "stats") { Bento(state, Modifier.appEntrance()) }
                    itemsIndexed(state.products, key = { _, product -> product.id }) { index, product ->
                        ProductCard(product, onClick = { onOpen(product.id) }, modifier = Modifier.animateItem().appEntrance(index + 1))
                    }
                }
            }
        }
    }
    state.sheet?.let { sheet ->
        InventoryFiltersSheet(
            draft = sheet.draft,
            matching = sheet.matching,
            categories = state.categories,
            locationCounts = state.locationCounts,
            onChange = { onAction(ProductListAction.OnDraftChange(it)) },
            onReset = { onAction(ProductListAction.OnResetDraft) },
            onApply = { onAction(ProductListAction.OnApplyFilters) },
            onDismiss = { onAction(ProductListAction.OnDismissFilters) }
        )
    }
}

/** What narrows the list besides the warehouse (always visible above), each removable on its own. */
@Composable
private fun ActivePills(state: ProductListState, onAction: (ProductListAction) -> Unit) {
    val filter = state.filter
    val visible = filter.section != null || filter.categoryId != null || filter.stock != StockFilter.ALL
    AnimatedVisibility(visible = visible) {
        val remove = stringResource(Res.string.filters_remove)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = 8.dp)
        ) {
            filter.section?.let { section ->
                AppRemovablePill(
                    text = stringResource(Res.string.section_label, section),
                    onRemove = { onAction(ProductListAction.OnRemove(FilterPill.SECTION)) },
                    removeDescription = remove,
                    leadingIcon = AppIcons.Location
                )
            }
            state.categories.firstOrNull { it.id == filter.categoryId }?.let { category ->
                AppRemovablePill(text = category.name, onRemove = { onAction(ProductListAction.OnRemove(FilterPill.CATEGORY)) }, removeDescription = remove)
            }
            if (filter.stock != StockFilter.ALL) {
                AppRemovablePill(
                    text = stringResource(filter.stock.label()),
                    onRemove = { onAction(ProductListAction.OnRemove(FilterPill.STOCK)) },
                    removeDescription = remove,
                    leadingIcon = AppIcons.Box
                )
            }
            TextButton(onClick = { onAction(ProductListAction.OnClearFilters) }) {
                Text(stringResource(Res.string.filters_clear), style = MaterialTheme.typography.chipLabel, color = AppTheme.colors.brand)
            }
        }
    }
}

/** Asymmetric summary: the product count big on the left, out-of-stock and units stacked on the right. */
@Composable
private fun Bento(state: ProductListState, modifier: Modifier = Modifier) {
    val count = rememberCountUp(state.products.size)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.height(IntrinsicSize.Min)) {
        AppHeroCard(modifier = Modifier.weight(1.15f).fillMaxHeight(), contentPadding = 20.dp) {
            Text(stringResource(Res.string.stat_products), style = MaterialTheme.typography.caption, color = AppTheme.colors.onHeroMuted)
            AppGradientText(text = count.toString(), style = MaterialTheme.typography.largeTitle.copy(fontSize = 72.sp, lineHeight = 76.sp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            AppStatCard(
                label = stringResource(Res.string.stat_out_of_stock),
                value = state.outOfStock.toString(),
                valueColor = if (state.outOfStock > 0) AppTheme.colors.danger else AppTheme.colors.success
            )
            AppStatCard(label = stringResource(Res.string.stat_units), value = state.totalUnits.toString())
        }
    }
}
