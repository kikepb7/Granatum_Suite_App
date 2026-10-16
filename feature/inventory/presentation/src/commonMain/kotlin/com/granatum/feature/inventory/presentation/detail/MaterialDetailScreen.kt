package com.granatum.feature.inventory.presentation.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.cards.AppInsetCard
import com.granatum.core.designsystem.components.dialogs.AppDestructiveConfirmationDialog
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.inventory.domain.model.MaterialHistoryEntry
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.presentation.common.label
import com.granatum.feature.inventory.presentation.common.measureLabel
import com.granatum.feature.inventory.presentation.common.priceLabel
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@Composable
fun MaterialDetailRoot(
    materialId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: MaterialDetailViewModel = koinViewModel(key = materialId) { parametersOf(materialId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.eventFlow) { event ->
        when (event) {
            MaterialDetailEvent.Deleted -> onNavigateBack()
            is MaterialDetailEvent.Message -> scope.launch { snackbar.showSnackbar(event.text.asStringAsync()) }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.detail_title), onBackClick = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val material = state.material
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (material == null) {
                if (state.loaded) Text(stringResource(Res.string.detail_gone), color = MaterialTheme.colorScheme.extended.textPlaceholder)
                return@Column
            }
            Text(material.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.extended.textPrimary)
            Text(
                stringResource(Res.string.stock_of, material.available, material.total),
                style = MaterialTheme.typography.titleMedium,
                color = if (material.isOutOfStock) MaterialTheme.colorScheme.extended.redCardText else MaterialTheme.colorScheme.extended.success
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(stringResource(Res.string.adjust_quantity), onClick = { viewModel.onAction(MaterialDetailAction.OnAdjust) }, enabled = !state.isBusy, modifier = Modifier.weight(1f))
                AppButton(stringResource(Res.string.edit), onClick = { onNavigateToEdit(material.id) }, style = AppButtonStyle.SECONDARY, enabled = !state.isBusy, modifier = Modifier.weight(1f))
            }
            Photos(material.photos)
            Facts(material)
            History(state.history)
            AppButton(
                stringResource(Res.string.delete),
                onClick = { viewModel.onAction(MaterialDetailAction.OnDelete) },
                style = AppButtonStyle.DESTRUCTIVE_SECONDARY,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
    val material = state.material
    if (state.isAdjusting && material != null) {
        AdjustQuantitySheet(material, state, viewModel)
    }
    if (state.isConfirmingDelete && material != null) {
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.delete_title),
            description = stringResource(Res.string.delete_description, material.name),
            confirmButtonText = stringResource(Res.string.delete_confirm),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = { viewModel.onAction(MaterialDetailAction.OnDeleteConfirm) },
            onCancelClick = { viewModel.onAction(MaterialDetailAction.OnDeleteDismiss) },
            onDismiss = { viewModel.onAction(MaterialDetailAction.OnDeleteDismiss) }
        )
    }
}

@Composable
private fun Photos(photos: List<String>) {
    if (photos.isEmpty()) return
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        photos.forEachIndexed { i, url ->
            AsyncImage(
                model = url,
                contentDescription = stringResource(Res.string.detail_photo, i + 1, photos.size.toString()),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(20.dp))
            )
        }
    }
}

@Composable
private fun Facts(m: MaterialModel) {
    val size = if (m.size.diameter != null) {
        stringResource(Res.string.detail_size_value_diameter, m.size.height.measureLabel(), m.size.width.measureLabel(), m.size.unit.name.lowercase(), m.size.diameter!!.measureLabel())
    } else {
        stringResource(Res.string.detail_size_value, m.size.height.measureLabel(), m.size.width.measureLabel(), m.size.unit.name.lowercase())
    }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Fact(Res.string.detail_category, m.category.name)
            Fact(Res.string.detail_condition, stringResource(m.condition.label()))
            Fact(Res.string.detail_location, m.location)
            Fact(Res.string.detail_size, size)
            Fact(Res.string.detail_color, m.color)
            Fact(Res.string.detail_material, m.physicalMaterial)
            Fact(Res.string.detail_price, stringResource(Res.string.detail_price_value, m.unitPrice.priceLabel()))
            Fact(Res.string.detail_supplier, m.supplier)
            Fact(Res.string.detail_created, m.createdAt.dateLabel())
            Fact(Res.string.detail_updated, m.updatedAt.dateLabel())
        }
    }
}

@Composable
private fun Fact(label: org.jetbrains.compose.resources.StringResource, value: String) {
    Row {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.extended.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.extended.textPrimary, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun History(entries: List<MaterialHistoryEntry>) {
    Text(
        stringResource(Res.string.history_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.extended.textPrimary,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
    )
    if (entries.isEmpty()) {
        Text(stringResource(Res.string.history_empty), color = MaterialTheme.colorScheme.extended.textPlaceholder)
        return
    }
    entries.forEach { e ->
        AppInsetCard {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row {
                    Text(stringResource(e.type.label()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.extended.textPrimary, modifier = Modifier.weight(1f))
                    Text(e.at.dateLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.extended.textSecondary)
                }
                if (e.previousValue != null || e.newValue != null) {
                    Text(stringResource(Res.string.history_change, e.previousValue ?: "–", e.newValue ?: "–"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.extended.textPrimary)
                }
                Text(e.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.extended.textSecondary)
            }
        }
    }
}

private fun Instant.dateLabel(): String {
    val d = toLocalDateTime(TimeZone.currentSystemDefault())
    fun two(n: Int) = n.toString().padStart(2, '0')
    return "${two(d.day)}/${two(d.month.ordinal + 1)}/${d.year} ${two(d.hour)}:${two(d.minute)}"
}
