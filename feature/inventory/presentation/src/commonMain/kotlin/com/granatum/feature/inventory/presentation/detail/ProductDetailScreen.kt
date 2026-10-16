package com.granatum.feature.inventory.presentation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.buttons.AppPrimaryButton
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.chips.AppFilterChip
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.controls.AppQuantityStepper
import com.granatum.core.designsystem.components.dialogs.AppDestructiveConfirmationDialog
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.feedback.AppLoadingState
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.components.lists.AppIconBadge
import com.granatum.core.designsystem.components.lists.AppSectionHeader
import com.granatum.core.designsystem.components.lists.AppTimelineItem
import com.granatum.core.designsystem.components.motion.appEntrance
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.components.topbar.TopBarIconButton
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.inventory.domain.model.HistoryChangeType
import com.granatum.feature.inventory.domain.model.ProductHistoryEntry
import com.granatum.feature.inventory.domain.model.ProductModel
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import com.granatum.feature.inventory.domain.validation.QuantityIssue
import com.granatum.feature.inventory.presentation.common.dateLabel
import com.granatum.feature.inventory.presentation.common.label
import com.granatum.feature.inventory.presentation.common.measureLabel
import com.granatum.feature.inventory.presentation.components.ProductImage
import granatumsuite.feature.inventory.presentation.generated.resources.*
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProductDetailRoot(
    productId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(key = productId) { parametersOf(productId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.eventFlow) { event ->
        when (event) {
            ProductDetailEvent.Deleted -> onNavigateBack()
            is ProductDetailEvent.Message -> scope.launch { snackbar.showSnackbar(event.text.asStringAsync()) }
        }
    }
    ProductDetailScreen(
        state = state,
        reason = viewModel.reason,
        snackbar = snackbar,
        onAction = viewModel::onAction,
        onBack = onNavigateBack,
        onEdit = onNavigateToEdit
    )
}

@Composable
private fun ProductDetailScreen(
    state: ProductDetailState,
    reason: TextFieldState,
    snackbar: SnackbarHostState,
    onAction: (ProductDetailAction) -> Unit,
    onBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    val product = state.product
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(
                title = product?.category?.name ?: stringResource(Res.string.product_title),
                onBackClick = onBack,
                actions = {
                    if (product != null) {
                        TopBarIconButton(onClick = { onEdit(product.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when {
            product == null && !state.loaded -> AppLoadingState(Modifier.padding(padding))
            product == null -> AppEmptyState(
                title = stringResource(Res.string.product_not_found),
                icon = { Icon(AppIcons.Box, contentDescription = null, tint = AppTheme.colors.textTertiary) },
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = 12.dp)
            ) {
                Hero(product, Modifier.appEntrance(0))
                Facts(product, Modifier.appEntrance(1))
                QuantityCard(state, reason, onAction, Modifier.appEntrance(2))
                History(state.history, Modifier.appEntrance(3))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                    AppButton(
                        stringResource(Res.string.edit),
                        onClick = { onEdit(product.id) },
                        style = AppButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                    AppButton(
                        stringResource(Res.string.delete),
                        onClick = { onAction(ProductDetailAction.OnDelete) },
                        style = AppButtonStyle.DESTRUCTIVE_SECONDARY,
                        isLoading = state.isDeleting,
                        enabled = !state.isDeleting,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    if (state.isConfirmingDelete && product != null) {
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.delete_title),
            description = stringResource(Res.string.delete_description),
            confirmButtonText = stringResource(Res.string.delete_confirm),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = { onAction(ProductDetailAction.OnDeleteConfirm) },
            onCancelClick = { onAction(ProductDetailAction.OnDeleteDismiss) },
            onDismiss = { onAction(ProductDetailAction.OnDeleteDismiss) }
        )
    }
}

/** The image large, with the category, description and stock status over it. */
@Composable
private fun Hero(product: ProductModel, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        Box {
            ProductImage(
                image = product.imageUrl,
                categoryName = product.category.name,
                corner = 24.dp,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
            )
            AppStatusChip(
                text = if (product.isOutOfStock) stringResource(Res.string.out_of_stock) else stringResource(Res.string.units_count, product.quantity),
                tone = if (product.isOutOfStock) AppTone.DANGER else AppTone.SUCCESS,
                showDot = true,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(product.category.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
            Text(product.description, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary)
        }
    }
}

/** Location, dimensions and dates as a 2 × 2 grid of tiles. */
@Composable
private fun Facts(product: ProductModel, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(AppIcons.Location, stringResource(Res.string.field_location), product.location.label(), Modifier.weight(1f))
            Fact(
                AppIcons.Ruler,
                stringResource(Res.string.field_dimensions),
                stringResource(Res.string.dimensions_value, product.dimensions.height.measureLabel(), product.dimensions.width.measureLabel()),
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact(AppIcons.Calendar, stringResource(Res.string.created_at), product.createdAt.dateLabel(), Modifier.weight(1f), AppTone.NEUTRAL)
            Fact(AppIcons.Calendar, stringResource(Res.string.updated_at), product.updatedAt.dateLabel(), Modifier.weight(1f), AppTone.NEUTRAL)
        }
    }
}

@Composable
private fun Fact(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, tone: AppTone = AppTone.BRAND) {
    AppCard(modifier = modifier, contentPadding = 14.dp) {
        AppIconBadge(icon = icon, tone = tone)
        Text(label, style = MaterialTheme.typography.caption, color = AppTheme.colors.textTertiary, modifier = Modifier.padding(top = 10.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary)
    }
}

/**
 * The stock, changed right here: − and + (or ±5 / ±10) prepare the new figure and nothing is sent
 * until it is saved, with an optional reason that ends up in the history.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuantityCard(state: ProductDetailState, reason: TextFieldState, onAction: (ProductDetailAction) -> Unit, modifier: Modifier = Modifier) {
    val product = state.product ?: return
    val value = state.shownQuantity
    AppCard(modifier = modifier) {
        Text(stringResource(Res.string.quantity_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
        AppQuantityStepper(
            value = value,
            onChange = { onAction(ProductDetailAction.OnQuantityChange(it)) },
            decreaseDescription = stringResource(Res.string.quantity_decrease),
            increaseDescription = stringResource(Res.string.quantity_increase),
            enabled = !state.isSavingQuantity,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 14.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(-10, -5, 5, 10).forEach { delta ->
                AppFilterChip(
                    text = if (delta > 0) "+$delta" else "−${-delta}",
                    selected = false,
                    onClick = { onAction(ProductDetailAction.OnQuantityChange(value + delta)) }
                )
            }
        }
        AnimatedVisibility(visible = state.hasPendingQuantity) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                Text(
                    stringResource(Res.string.quantity_pending, product.quantity, value),
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.brand
                )
                val reasonError = if (QuantityIssue.REASON_TOO_LONG in state.quantityIssues) stringResource(Res.string.too_long, InventoryValidation.REASON_MAX) else null
                AppTextField(
                    state = reason,
                    title = stringResource(Res.string.quantity_reason),
                    singleLine = true,
                    isError = reasonError != null,
                    supportingText = reasonError,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAction(ProductDetailAction.OnQuantityReset) }, enabled = !state.isSavingQuantity) {
                        Text(stringResource(Res.string.cancel), color = AppTheme.colors.textSecondary)
                    }
                    AppPrimaryButton(
                        text = stringResource(Res.string.quantity_save),
                        onClick = { onAction(ProductDetailAction.OnQuantitySave) },
                        isLoading = state.isSavingQuantity,
                        enabled = !state.isSavingQuantity,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun History(entries: List<ProductHistoryEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 6.dp)) {
        AppSectionHeader(title = stringResource(Res.string.history_title))
        if (entries.isEmpty()) {
            Text(
                stringResource(Res.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textTertiary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            return@Column
        }
        entries.forEachIndexed { index, entry ->
            AppTimelineItem(
                tone = if (entry.type == HistoryChangeType.CANTIDAD) AppTone.BRAND else AppTone.NEUTRAL,
                isFirst = index == 0,
                isLast = index == entries.lastIndex
            ) {
                AppCard(contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(entry.type.label()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(AppIcons.Calendar, contentDescription = null, tint = AppTheme.colors.textTertiary, modifier = Modifier.size(12.dp))
                        Text(entry.at.dateLabel(), style = MaterialTheme.typography.caption, color = AppTheme.colors.textTertiary, modifier = Modifier.padding(start = 4.dp))
                    }
                    if (entry.previousValue != null || entry.newValue != null) {
                        Text(
                            stringResource(Res.string.history_change, entry.previousValue ?: "–", entry.newValue ?: "–"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.brand,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (entry.reason.isNotBlank()) {
                        Text(entry.reason, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
