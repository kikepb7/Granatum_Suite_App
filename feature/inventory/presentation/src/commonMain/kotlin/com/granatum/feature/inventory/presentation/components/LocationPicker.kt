package com.granatum.feature.inventory.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.designsystem.theme.chipLabel
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.section_label
import granatumsuite.feature.inventory.presentation.generated.resources.warehouse_label
import org.jetbrains.compose.resources.stringResource

/**
 * A plan of the three warehouses side by side, each with its five sections stacked like shelves:
 * one tap on a section chooses warehouse and section at once. [counts] (optional) shows how many
 * products each section holds, which turns the same plan into a map of the stock.
 */
@Composable
fun LocationPicker(
    selected: StorageLocation?,
    onSelect: (StorageLocation) -> Unit,
    modifier: Modifier = Modifier,
    counts: Map<StorageLocation, Int> = emptyMap(),
    enabled: Boolean = true,
    /** A whole warehouse chosen without a section (filters only). */
    selectedWarehouse: Warehouse? = selected?.warehouse,
    /** When set, tapping a warehouse's header chooses the whole warehouse. */
    onWarehouseClick: ((Warehouse) -> Unit)? = null
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        Warehouse.entries.forEach { warehouse ->
            val warehouseSelected = selectedWarehouse == warehouse
            Surface(
                shape = AppTheme.shapes.medium,
                color = if (warehouseSelected) AppTheme.colors.brandSoft else AppTheme.colors.surfaceSunken,
                border = BorderStroke(1.dp, if (warehouseSelected) AppTheme.colors.brand.copy(alpha = 0.4f) else AppTheme.colors.border),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val headerClick = onWarehouseClick?.let { click -> Modifier.clickable(enabled = enabled, role = Role.Button) { click(warehouse) } } ?: Modifier
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().then(headerClick).padding(vertical = 2.dp)) {
                        Icon(
                            AppIcons.Warehouse,
                            contentDescription = null,
                            tint = if (warehouseSelected) AppTheme.colors.brand else AppTheme.colors.textTertiary,
                            modifier = Modifier.height(16.dp)
                        )
                        Text(
                            text = warehouse.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (warehouseSelected) AppTheme.colors.brand else AppTheme.colors.textPrimary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    StorageLocation.SECTIONS.forEach { section ->
                        val location = StorageLocation(warehouse, section)
                        SectionCell(
                            location = location,
                            selected = selected == location,
                            count = counts[location],
                            enabled = enabled,
                            onClick = { onSelect(location) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCell(location: StorageLocation, selected: Boolean, count: Int?, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val container = animateColorAsState(if (selected) colors.brand else colors.surface, label = "cellFill").value
    val content = animateColorAsState(if (selected) colors.onBrand else colors.textSecondary, label = "cellText").value
    val description = "${stringResource(Res.string.warehouse_label, location.warehouse.name)}, ${stringResource(Res.string.section_label, location.section)}"
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppTheme.shapes.small,
        color = container,
        border = if (selected) null else BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.2f)
            .semantics {
                contentDescription = description
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(location.section.toString(), style = MaterialTheme.typography.chipLabel, fontWeight = FontWeight.Bold, color = content)
                if (count != null && count > 0) {
                    Text("· $count", style = MaterialTheme.typography.caption, color = content.copy(alpha = 0.8f))
                }
            }
        }
    }
}
