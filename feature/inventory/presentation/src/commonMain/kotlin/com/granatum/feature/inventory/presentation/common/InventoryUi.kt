package com.granatum.feature.inventory.presentation.common

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.HistoryChangeType
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.MaterialCondition
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.*
import kotlin.math.abs
import kotlin.math.roundToLong

fun MaterialCondition.label() = when (this) {
    MaterialCondition.NUEVO -> Res.string.condition_nuevo
    MaterialCondition.USADO -> Res.string.condition_usado
    MaterialCondition.DANADO -> Res.string.condition_danado
    MaterialCondition.ROTO -> Res.string.condition_roto
    MaterialCondition.EN_REPARACION -> Res.string.condition_en_reparacion
}

fun HistoryChangeType.label() = when (this) {
    HistoryChangeType.CANTIDAD -> Res.string.change_cantidad
    HistoryChangeType.ESTADO -> Res.string.change_estado
    HistoryChangeType.UBICACION -> Res.string.change_ubicacion
    HistoryChangeType.PRECIO_UNITARIO -> Res.string.change_precio
    HistoryChangeType.PROVEEDOR -> Res.string.change_proveedor
    HistoryChangeType.OTRO -> Res.string.change_otro
}

fun InventoryError.toUiText(): UiText = UiText.Resource(
    when (this) {
        InventoryError.NoInternet -> Res.string.error_no_internet
        InventoryError.MaterialNotFound -> Res.string.error_material_not_found
        InventoryError.CategoryNotFound -> Res.string.error_category_not_found
        InventoryError.QuantityOutOfRange -> Res.string.error_quantity_out_of_range
        InventoryError.Invalid -> Res.string.error_invalid
        InventoryError.Forbidden -> Res.string.error_forbidden
        InventoryError.Unknown -> Res.string.error_unknown
    }
)

/** "12,50": two decimals with a comma, as prices are written in Spain. No String.format in common code. */
fun Double.priceLabel(): String {
    val cents = (abs(this) * 100).roundToLong()
    val sign = if (this < 0) "-" else ""
    return "$sign${cents / 100},${(cents % 100).toString().padStart(2, '0')}"
}

/** "40" or "12,5": a measure without a pointless ",0". */
fun Double.measureLabel(): String {
    val rounded = (this * 100).roundToLong() / 100.0
    val text = if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    return text.replace('.', ',')
}

/** Accepts "12,5" or "12.5" (research D7). */
fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()
