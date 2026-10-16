package com.granatum.feature.inventory.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.designsystem.theme.statValue
import com.granatum.feature.inventory.domain.model.ProductModel
import com.granatum.feature.inventory.presentation.common.label
import com.granatum.feature.inventory.presentation.common.measureLabel
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.dimensions_value
import granatumsuite.feature.inventory.presentation.generated.resources.out_of_stock
import granatumsuite.feature.inventory.presentation.generated.resources.units_short
import org.jetbrains.compose.resources.stringResource

/** Image, what it is, where it is and how many: everything needed to find a product at a glance. */
@Composable
fun ProductCard(product: ProductModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(
        onClick = onClick,
        onClickLabel = product.category.name,
        tone = if (product.isOutOfStock) AppTone.DANGER else null,
        contentPadding = 12.dp,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProductImage(product.imageUrl, product.category.name, Modifier.size(68.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    product.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier) {
                    Meta(AppIcons.Location, product.location.label(), AppTheme.colors.brand)
                    Meta(
                        AppIcons.Ruler,
                        stringResource(Res.string.dimensions_value, product.dimensions.height.measureLabel(), product.dimensions.width.measureLabel()),
                        AppTheme.colors.textTertiary
                    )
                }
            }
            if (product.isOutOfStock) {
                AppStatusChip(text = stringResource(Res.string.out_of_stock), tone = AppTone.DANGER)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(product.quantity.toString(), style = MaterialTheme.typography.statValue, color = AppTheme.colors.textPrimary)
                    Text(
                        stringResource(Res.string.units_short),
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun Meta(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.caption, color = AppTheme.colors.textSecondary, maxLines = 1)
    }
}
