package com.granatum.feature.events.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.buttons.AppPrimaryButton
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.controls.AppQuantityStepper
import com.granatum.core.designsystem.components.dialogs.AppSheetPanel
import com.granatum.core.designsystem.components.inputs.AppSearchField
import com.granatum.core.designsystem.components.media.AppItemImage
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.feature.events.presentation.detail.MaterialPickerUiState
import com.granatum.feature.events.presentation.detail.PickerItemUiState
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.decrease
import granatumsuite.feature.events.presentation.generated.resources.increase
import granatumsuite.feature.events.presentation.generated.resources.picker_add
import granatumsuite.feature.events.presentation.generated.resources.picker_empty
import granatumsuite.feature.events.presentation.generated.resources.picker_free
import granatumsuite.feature.events.presentation.generated.resources.picker_in_event
import granatumsuite.feature.events.presentation.generated.resources.picker_max
import granatumsuite.feature.events.presentation.generated.resources.picker_out_of_stock
import granatumsuite.feature.events.presentation.generated.resources.picker_search
import granatumsuite.feature.events.presentation.generated.resources.picker_title
import granatumsuite.feature.events.presentation.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

/**
 * Choosing material for the event: search, pick one with free units, set how many - never more
 * than are free - and add. Sold-out materials stay listed, greyed, so nobody wonders where they went.
 */
@Composable
internal fun MaterialPickerSheet(
    state: MaterialPickerUiState,
    query: TextFieldState,
    onSelect: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AppSheetPanel(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp).semantics { heading() }
            )
            AppSearchField(state = query, placeholder = stringResource(Res.string.picker_search), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            Box(Modifier.weight(1f, fill = false).heightIn(min = 160.dp)) {
                when {
                    state.isLoading -> CircularProgressIndicator(color = AppTheme.colors.brand, modifier = Modifier.align(Alignment.Center).size(32.dp))
                    state.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).padding(24.dp)) {
                        Text(state.error.asString(), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
                        TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry), color = AppTheme.colors.brand) }
                    }
                    state.items.isEmpty() -> Text(
                        stringResource(Res.string.picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textTertiary,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            PickerRow(item, selected = state.selection?.materialId == item.id, onClick = { onSelect(item.id) })
                        }
                    }
                }
            }
            state.selection?.let { selection ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(selection.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AppQuantityStepper(
                        value = selection.quantity,
                        onChange = { onQuantityChange(it.coerceAtMost(selection.max)) },
                        decreaseDescription = stringResource(Res.string.decrease),
                        increaseDescription = stringResource(Res.string.increase),
                        min = 1
                    )
                    Text(stringResource(Res.string.picker_max, selection.max), style = MaterialTheme.typography.caption, color = AppTheme.colors.textTertiary)
                    AppPrimaryButton(
                        text = stringResource(Res.string.picker_add, selection.quantity),
                        onClick = onConfirm,
                        isLoading = state.isSaving,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(item: PickerItemUiState, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        enabled = item.isSelectable,
        shape = AppTheme.shapes.medium,
        color = if (selected) colors.brandSoft else colors.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.brand else colors.border),
        modifier = Modifier.fillMaxWidth().alpha(if (item.isSelectable) 1f else 0.55f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(10.dp)) {
            AppItemImage(item.imageUrl, item.category, Modifier.size(48.dp), corner = 12.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.locationLabel.asString(), style = MaterialTheme.typography.caption, color = colors.textTertiary)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.isSelectable) {
                    AppStatusChip(text = stringResource(Res.string.picker_free, item.freeUnits), tone = AppTone.SUCCESS)
                } else {
                    AppStatusChip(text = stringResource(Res.string.picker_out_of_stock), tone = AppTone.DANGER)
                }
                if (item.inEvent > 0) {
                    Text(stringResource(Res.string.picker_in_event, item.inEvent), style = MaterialTheme.typography.caption, color = colors.brand)
                }
            }
        }
    }
}
