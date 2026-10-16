package com.granatum.feature.inventory.presentation.list

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.brand.AppGradientText
import com.granatum.core.designsystem.components.brand.rememberCountUp
import com.granatum.core.designsystem.components.buttons.AppFloatingActionButton
import com.granatum.core.designsystem.components.cards.AppHeroCard
import com.granatum.core.designsystem.components.cards.AppStatCard
import com.granatum.core.designsystem.components.chips.AppFilterChip
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.feedback.AppBanner
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.icons.AppTabIcons
import com.granatum.core.designsystem.components.inputs.AppSearchField
import com.granatum.core.designsystem.components.lists.AppIconBadge
import com.granatum.core.designsystem.components.lists.AppListItem
import com.granatum.core.designsystem.components.lists.AppProgressBar
import com.granatum.core.designsystem.components.motion.appEntrance
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.components.topbar.TopBarIconButton
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.designsystem.theme.largeTitle
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
                    TopBarIconButton(onClick = { viewModel.onAction(MaterialListAction.OnRefresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
                    }
                }
            )
        },
        floatingActionButton = {
            AppFloatingActionButton(onClick = onCreate) { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_material)) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTheme.colors.brand,
                    trackColor = AppTheme.colors.brandSoft
                )
            }
            if (state.isStale) {
                AppBanner(
                    title = stringResource(Res.string.list_stale),
                    tone = AppTone.WARNING,
                    modifier = Modifier.padding(horizontal = horizontal, vertical = 4.dp)
                )
            }
            AppSearchField(
                state = viewModel.query,
                placeholder = stringResource(Res.string.search_placeholder),
                modifier = Modifier.padding(horizontal = horizontal, vertical = 8.dp)
            )
            Filters(state, viewModel)
            when {
                state.materials.isEmpty() -> AppEmptyState(
                    title = stringResource(if (state.isEmptyInventory) Res.string.list_empty else Res.string.list_empty_filtered)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "stats") { Bento(state.materials, Modifier.appEntrance()) }
                    itemsIndexed(state.materials, key = { _, material -> material.id }) { index, material ->
                        MaterialCard(material, onClick = { onOpen(material.id) }, modifier = Modifier.appEntrance(index + 1))
                    }
                }
            }
        }
    }
}

@Composable
private fun Filters(state: MaterialListState, viewModel: MaterialListViewModel) {
    val horizontal = AppTheme.spacing.screenHorizontal
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = horizontal), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFilterChip(selected = state.filter.categoryId == null, onClick = { viewModel.onAction(MaterialListAction.OnCategory(null)) }, text = stringResource(Res.string.filter_all_categories))
            state.categories.forEach { c ->
                AppFilterChip(selected = state.filter.categoryId == c.id, onClick = { viewModel.onAction(MaterialListAction.OnCategory(c.id)) }, text = c.name)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = horizontal), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFilterChip(selected = state.filter.onlyOutOfStock, onClick = { viewModel.onAction(MaterialListAction.OnToggleOutOfStock) }, text = stringResource(Res.string.filter_out_of_stock))
            AppFilterChip(selected = state.filter.condition == null, onClick = { viewModel.onAction(MaterialListAction.OnCondition(null)) }, text = stringResource(Res.string.filter_any_condition))
            MaterialCondition.entries.forEach { c ->
                AppFilterChip(selected = state.filter.condition == c, onClick = { viewModel.onAction(MaterialListAction.OnCondition(c)) }, text = stringResource(c.label()))
            }
        }
    }
}

/** Asymmetric summary: the count big on the left, the out-of-stock and units tiles stacked on the right. */
@Composable
private fun Bento(materials: List<MaterialModel>, modifier: Modifier = Modifier) {
    val outOfStock = materials.count { it.isOutOfStock }
    val available = materials.sumOf { it.available }
    val total = materials.sumOf { it.total }
    val count = rememberCountUp(materials.size)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.height(IntrinsicSize.Min)) {
        AppHeroCard(modifier = Modifier.weight(1.15f).fillMaxHeight(), contentPadding = 20.dp) {
            Text(
                text = stringResource(Res.string.stat_materials),
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.onHeroMuted
            )
            AppGradientText(
                text = count.toString(),
                style = MaterialTheme.typography.largeTitle.copy(fontSize = 72.sp, lineHeight = 76.sp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            AppStatCard(
                label = stringResource(Res.string.stat_out_of_stock),
                value = outOfStock.toString(),
                valueColor = if (outOfStock > 0) AppTheme.colors.danger else AppTheme.colors.success
            )
            AppStatCard(label = stringResource(Res.string.stat_units), value = "$available/$total")
        }
    }
}

@Composable
private fun MaterialCard(material: MaterialModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tone = if (material.isOutOfStock) AppTone.DANGER else AppTone.SUCCESS
    AppListItem(
        title = material.name,
        subtitle = "${material.category.name} · ${stringResource(material.condition.label())} · ${material.location}",
        onClick = onClick,
        modifier = modifier,
        tone = tone,
        leading = { AppIconBadge(icon = AppTabIcons.Inventory, tone = tone) },
        trailing = {
            AppStatusChip(
                text = if (material.isOutOfStock) stringResource(Res.string.out_of_stock) else "${material.available}/${material.total}",
                tone = tone
            )
        },
        footer = {
            if (material.total > 0) {
                AppProgressBar(progress = material.available.toFloat() / material.total, tone = tone)
            }
        }
    )
}
