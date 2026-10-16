package com.granatum.feature.inventory.domain.model

enum class StockFilter { ALL, IN_STOCK, OUT_OF_STOCK }

enum class ProductSort { RECENT, CATEGORY, QUANTITY_DESC, QUANTITY_ASC }

/** The server neither pages nor filters, so the device does (specs/011, FR-004). */
data class ProductFilter(
    val query: String = "",
    val warehouse: Warehouse? = null,
    val section: Int? = null,
    val categoryId: String? = null,
    val stock: StockFilter = StockFilter.ALL,
    val sort: ProductSort = ProductSort.RECENT
) {
    /** Filters that narrow the list, as shown by the filters button (text and sort are not). */
    val activeCount: Int
        get() = listOf(warehouse != null, section != null, categoryId != null, stock != StockFilter.ALL).count { it }

    val isNarrowed: Boolean get() = query.isNotBlank() || activeCount > 0

    fun cleared(): ProductFilter = ProductFilter(query = query, sort = sort)
}

fun List<ProductModel>.filterBy(filter: ProductFilter): List<ProductModel> {
    val q = filter.query.trim().fold()
    val matching = filter { p ->
        (q.isEmpty() || listOfNotNull(p.category.name, p.description, p.location?.toApi()).any { q in it.fold() }) &&
            (filter.warehouse == null || p.location?.warehouse == filter.warehouse) &&
            (filter.section == null || p.location?.section == filter.section) &&
            (filter.categoryId == null || p.category.id == filter.categoryId) &&
            when (filter.stock) {
                StockFilter.ALL -> true
                StockFilter.IN_STOCK -> !p.isOutOfStock
                StockFilter.OUT_OF_STOCK -> p.isOutOfStock
            }
    }
    return when (filter.sort) {
        ProductSort.RECENT -> matching.sortedByDescending { it.updatedAt }
        ProductSort.CATEGORY -> matching.sortedWith(compareBy({ it.category.name.fold() }, { it.description.fold() }))
        ProductSort.QUANTITY_DESC -> matching.sortedByDescending { it.quantity }
        ProductSort.QUANTITY_ASC -> matching.sortedBy { it.quantity }
    }
}

/**
 * Lower case without accents, so "jarron" finds "Jarrón" and "árbol" sorts with the a's instead of
 * after the z's, as a Spanish reader expects.
 */
internal fun String.fold(): String = lowercase().map { ACCENTS[it] ?: it }.joinToString("")

private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'à' to 'a', 'è' to 'e', 'ò' to 'o')
