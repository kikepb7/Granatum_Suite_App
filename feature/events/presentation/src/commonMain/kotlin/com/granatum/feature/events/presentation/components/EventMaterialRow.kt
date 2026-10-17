package com.granatum.feature.events.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.components.media.AppItemImage
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.designsystem.theme.statValue
import com.granatum.feature.events.presentation.common.EventMaterialUiState
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.decrease
import granatumsuite.feature.events.presentation.generated.resources.increase
import granatumsuite.feature.events.presentation.generated.resources.material_missing
import granatumsuite.feature.events.presentation.generated.resources.material_remove
import granatumsuite.feature.events.presentation.generated.resources.units_short
import org.jetbrains.compose.resources.stringResource

/**
 * A material of the event: what it is and where it is, with − / + while the event can change.
 * A change on its way to the server shows dimmed until it is saved.
 */
@Composable
internal fun EventMaterialRow(
    material: EventMaterialUiState,
    editable: Boolean,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(contentPadding = 12.dp, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppItemImage(material.imageUrl, material.category, Modifier.size(56.dp), corner = 14.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(material.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(material.description, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(AppIcons.Location, contentDescription = null, tint = if (material.isMissing) AppTheme.colors.warning else AppTheme.colors.brand, modifier = Modifier.size(12.dp))
                    Text(
                        material.locationLabel?.asString() ?: stringResource(Res.string.material_missing),
                        style = MaterialTheme.typography.caption,
                        color = if (material.isMissing) AppTheme.colors.warning else AppTheme.colors.textTertiary
                    )
                }
            }
            if (editable && !material.isMissing) {
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(AppIcons.Close, contentDescription = stringResource(Res.string.material_remove), tint = AppTheme.colors.textTertiary, modifier = Modifier.size(14.dp))
                    }
                    CompactQuantityStepper(
                        value = material.quantity,
                        onChange = onQuantityChange,
                        decreaseDescription = stringResource(Res.string.decrease),
                        increaseDescription = stringResource(Res.string.increase),
                        modifier = Modifier.alpha(if (material.isSaving) 0.6f else 1f)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(material.quantity.toString(), style = MaterialTheme.typography.statValue, color = AppTheme.colors.textPrimary)
                    Text(stringResource(Res.string.units_short), style = MaterialTheme.typography.caption, color = AppTheme.colors.textTertiary)
                }
            }
        }
    }
}
