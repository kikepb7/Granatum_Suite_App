package com.granatum.feature.inventory.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.buttons.AppPrimaryButton
import com.granatum.core.designsystem.components.chips.AppFilterChip
import com.granatum.core.designsystem.components.controls.AppSegmentedControl
import com.granatum.core.designsystem.components.dialogs.AppSheetPanel
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.chipLabel
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.ProductFilter
import com.granatum.feature.inventory.domain.model.ProductSort
import com.granatum.feature.inventory.domain.model.StockFilter
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.presentation.common.label
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.filter_all
import granatumsuite.feature.inventory.presentation.generated.resources.filter_all_categories
import granatumsuite.feature.inventory.presentation.generated.resources.filter_category
import granatumsuite.feature.inventory.presentation.generated.resources.field_location
import granatumsuite.feature.inventory.presentation.generated.resources.filter_sort
import granatumsuite.feature.inventory.presentation.generated.resources.filter_stock
import granatumsuite.feature.inventory.presentation.generated.resources.filters_reset
import granatumsuite.feature.inventory.presentation.generated.resources.filters_show
import granatumsuite.feature.inventory.presentation.generated.resources.filters_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Every filter in one place, grouped from the broadest to the most specific. Changes apply to a
 * draft and the button says how many products it leaves, so nobody applies a filter that empties
 * the list without knowing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryFiltersSheet(
    draft: ProductFilter,
    matching: Int,
    categories: List<CategoryModel>,
    locationCounts: Map<StorageLocation, Int>,
    onChange: (ProductFilter) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    // A panel rather than Material's sheet, which on iOS opens tall content half way: the groups
    // scroll and the apply button stays pinned at the bottom.
    AppSheetPanel(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
            ) {
                Text(
                    stringResource(Res.string.filters_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f).semantics { heading() }
                )
                TextButton(onClick = onReset, enabled = draft != ProductFilter(query = draft.query)) {
                    Text(stringResource(Res.string.filters_reset), style = MaterialTheme.typography.chipLabel, color = AppTheme.colors.brand)
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Group(Res.string.filter_sort) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProductSort.entries.forEach { sort ->
                            AppFilterChip(text = stringResource(sort.label()), selected = draft.sort == sort, onClick = { onChange(draft.copy(sort = sort)) })
                        }
                    }
                }
                Group(Res.string.filter_stock) {
                    AppSegmentedControl(
                        options = StockFilter.entries,
                        selected = draft.stock,
                        onSelect = { onChange(draft.copy(stock = it)) },
                        label = { stringResource(it.label()) }
                    )
                }
                Group(Res.string.field_location) {
                    AppFilterChip(
                        text = stringResource(Res.string.filter_all),
                        selected = draft.warehouse == null && draft.section == null,
                        onClick = { onChange(draft.copy(warehouse = null, section = null)) }
                    )
                    // Header = the whole warehouse; a cell = that section. Tapping the chosen one again clears it.
                    LocationPicker(
                        selected = draft.warehouse?.let { w -> draft.section?.let { s -> StorageLocation(w, s) } },
                        selectedWarehouse = draft.warehouse,
                        onSelect = { location ->
                            val same = draft.warehouse == location.warehouse && draft.section == location.section
                            onChange(if (same) draft.copy(section = null) else draft.copy(warehouse = location.warehouse, section = location.section))
                        },
                        onWarehouseClick = { warehouse ->
                            val same = draft.warehouse == warehouse && draft.section == null
                            onChange(draft.copy(warehouse = if (same) null else warehouse, section = null))
                        },
                        counts = locationCounts
                    )
                }
                if (categories.isNotEmpty()) {
                    Group(Res.string.filter_category) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppFilterChip(
                                text = stringResource(Res.string.filter_all_categories),
                                selected = draft.categoryId == null,
                                onClick = { onChange(draft.copy(categoryId = null)) }
                            )
                            categories.forEach { category ->
                                AppFilterChip(
                                    text = category.name,
                                    selected = draft.categoryId == category.id,
                                    onClick = { onChange(draft.copy(categoryId = category.id)) }
                                )
                            }
                        }
                    }
                }
            }
            AppPrimaryButton(
                text = stringResource(Res.string.filters_show, matching),
                onClick = onApply,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun Group(title: StringResource, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(title).uppercase(),
            style = MaterialTheme.typography.chipLabel,
            color = AppTheme.colors.textTertiary,
            modifier = Modifier.semantics { heading() }
        )
        content()
    }
}

