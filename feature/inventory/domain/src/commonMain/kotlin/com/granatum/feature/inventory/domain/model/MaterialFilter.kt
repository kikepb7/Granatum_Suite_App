package com.granatum.feature.inventory.domain.model

/** The server neither pages nor filters, so the device does (research D4). */
data class MaterialFilter(
    val query: String = "",
    val categoryId: String? = null,
    val condition: MaterialCondition? = null,
    val onlyOutOfStock: Boolean = false
) {
    val isActive: Boolean get() = query.isNotBlank() || categoryId != null || condition != null || onlyOutOfStock
}

fun List<MaterialModel>.filterBy(filter: MaterialFilter): List<MaterialModel> {
    val q = filter.query.trim().fold()
    return filter { m ->
        (q.isEmpty() || listOf(m.name, m.location, m.supplier, m.category.name).any { q in it.fold() }) &&
            (filter.categoryId == null || m.category.id == filter.categoryId) &&
            (filter.condition == null || m.condition == filter.condition) &&
            (!filter.onlyOutOfStock || m.isOutOfStock)
    }.sortedBy { it.name.fold() }
}

/**
 * Lower case without accents, so "almacen" finds "Almacén" and "árbol" sorts with the a's
 * instead of after the z's, as a Spanish reader expects.
 */
internal fun String.fold(): String = lowercase().map { ACCENTS[it] ?: it }.joinToString("")

private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'à' to 'a', 'è' to 'e', 'ò' to 'o')
