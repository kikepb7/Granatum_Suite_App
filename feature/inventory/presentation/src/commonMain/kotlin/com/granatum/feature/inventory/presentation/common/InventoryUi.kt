package com.granatum.feature.inventory.presentation.common

import androidx.compose.runtime.Composable
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.HistoryChangeType
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductSort
import com.granatum.feature.inventory.domain.model.StockFilter
import com.granatum.feature.inventory.domain.model.StorageLocation
import granatumsuite.feature.inventory.presentation.generated.resources.*
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Instant

fun HistoryChangeType.label() = when (this) {
    HistoryChangeType.CANTIDAD -> Res.string.change_cantidad
    HistoryChangeType.ESTADO -> Res.string.change_estado
    HistoryChangeType.UBICACION -> Res.string.change_ubicacion
    HistoryChangeType.PRECIO_UNITARIO -> Res.string.change_precio
    HistoryChangeType.PROVEEDOR -> Res.string.change_proveedor
    HistoryChangeType.OTRO -> Res.string.change_otro
}

fun StockFilter.label() = when (this) {
    StockFilter.ALL -> Res.string.stock_all
    StockFilter.IN_STOCK -> Res.string.stock_in
    StockFilter.OUT_OF_STOCK -> Res.string.stock_out
}

fun ProductSort.label() = when (this) {
    ProductSort.RECENT -> Res.string.sort_recent
    ProductSort.CATEGORY -> Res.string.sort_category
    ProductSort.QUANTITY_DESC -> Res.string.sort_quantity_desc
    ProductSort.QUANTITY_ASC -> Res.string.sort_quantity_asc
}

fun InventoryError.toUiText(): UiText = UiText.Resource(
    when (this) {
        InventoryError.NoInternet -> Res.string.error_no_internet
        InventoryError.ProductNotFound -> Res.string.error_product_not_found
        InventoryError.ProductInUse -> Res.string.error_product_in_use
        InventoryError.CategoryNotFound -> Res.string.error_category_not_found
        InventoryError.QuantityOutOfRange -> Res.string.error_quantity_out_of_range
        InventoryError.Invalid -> Res.string.error_invalid
        InventoryError.Forbidden -> Res.string.error_forbidden
        InventoryError.StorageUnavailable -> Res.string.error_storage_unavailable
        InventoryError.ImageTooLarge -> Res.string.error_image_too_large
        InventoryError.ImageInvalid -> Res.string.error_image_invalid
        InventoryError.Unknown -> Res.string.error_unknown
    }
)

/** «A · 3». */
@Composable
fun StorageLocation.label(): String = stringResource(Res.string.location_short, warehouse.name, section)

/** «40» or «12,5»: a measure without a pointless «,0». No String.format in common code. */
fun Double.measureLabel(): String {
    val tenths = (abs(this) * 10).roundToLong()
    val sign = if (this < 0) "-" else ""
    return if (tenths % 10 == 0L) "$sign${tenths / 10}" else "$sign${tenths / 10},${tenths % 10}"
}

/** Accepts «12,5» and «12.5»; null when it is not a non-negative number (zero is left to validation). */
fun String.toMeasureOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }

/** `01/10/2026`, as dates are written in Spain, in the device's zone. */
fun Instant.dateLabel(zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val d = toLocalDateTime(zone).date
    return "${d.day.toString().padStart(2, '0')}/${d.month.number.toString().padStart(2, '0')}/${d.year}"
}
