package com.granatum.feature.inventory.presentation

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.HistoryChangeType
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductSort
import com.granatum.feature.inventory.domain.model.StockFilter
import com.granatum.feature.inventory.presentation.common.dateLabel
import com.granatum.feature.inventory.presentation.common.label
import com.granatum.feature.inventory.presentation.common.measureLabel
import com.granatum.feature.inventory.presentation.common.toMeasureOrNull
import com.granatum.feature.inventory.presentation.common.toUiText
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

class InventoryUiTest {

    @Test
    fun measures_read_the_spanish_way_without_a_pointless_decimal() {
        assertEquals("40", 40.0.measureLabel())
        assertEquals("12,5", 12.5.measureLabel())
        assertEquals("0,3", 0.3.measureLabel())
        assertEquals("-1,5", (-1.5).measureLabel())
    }

    @Test
    fun typed_measures_accept_comma_or_point_and_refuse_the_rest() {
        assertEquals(12.5, "12,5".toMeasureOrNull())
        assertEquals(12.5, " 12.5 ".toMeasureOrNull())
        assertEquals(0.0, "0".toMeasureOrNull())
        listOf("", "alto", "-3", "NaN").forEach { assertNull(it.toMeasureOrNull(), it) }
    }

    @Test
    fun dates_are_day_month_year_in_the_given_zone() {
        val instant = Instant.parse("2026-10-01T23:30:00Z")
        assertEquals("01/10/2026", instant.dateLabel(TimeZone.UTC))
        assertEquals("02/10/2026", instant.dateLabel(TimeZone.of("Europe/Madrid")))
    }

    @Test
    fun every_error_has_its_own_message() {
        val errors = listOf(
            InventoryError.NoInternet, InventoryError.ProductNotFound, InventoryError.ProductInUse, InventoryError.CategoryNotFound,
            InventoryError.QuantityOutOfRange, InventoryError.Invalid, InventoryError.Forbidden,
            InventoryError.StorageUnavailable, InventoryError.ImageTooLarge, InventoryError.ImageInvalid, InventoryError.Unknown
        )
        val messages = errors.map { assertIs<UiText.Resource>(it.toUiText()).id }
        assertEquals(errors.size, messages.toSet().size)
    }

    @Test
    fun every_option_has_its_own_label() {
        assertEquals(StockFilter.entries.size, StockFilter.entries.map { it.label() }.toSet().size)
        assertEquals(ProductSort.entries.size, ProductSort.entries.map { it.label() }.toSet().size)
        assertEquals(HistoryChangeType.entries.size, HistoryChangeType.entries.map { it.label() }.toSet().size)
    }
}
