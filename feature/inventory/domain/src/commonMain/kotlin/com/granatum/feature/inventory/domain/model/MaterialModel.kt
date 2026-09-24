package com.granatum.feature.inventory.domain.model

import kotlinx.datetime.Instant

enum class MaterialCategory {
    FLORES,
    PLANTAS,
    ACCESORIOS,
    MOBILIARIO,
    DECORACION,
    OTROS
}

enum class MaterialStatus {
    DISPONIBLE,
    STOCK_BAJO,
    AGOTADO,
    RESERVADO,
    DANADO
}

data class MaterialModel(
    val id: String,
    val name: String,
    val category: MaterialCategory,
    val status: MaterialStatus,
    val quantity: Int,
    val size: String?,
    val color: String?,
    val location: String?,
    val photoUrls: List<String>,
    val notes: String?,
    val updatedAt: Instant
)

/**
 * Fields editable from the create/edit form. Kept separate from [MaterialModel]
 * because id/status/updatedAt are server- or quantity-derived, not user input.
 */
data class MaterialDraft(
    val id: String?,
    val name: String,
    val category: MaterialCategory,
    val quantity: Int,
    val size: String?,
    val color: String?,
    val location: String?,
    val photoUrls: List<String>,
    val notes: String?
)

data class MaterialFilter(
    val category: MaterialCategory? = null,
    val status: MaterialStatus? = null,
    val query: String = ""
) {
    fun matches(material: MaterialModel): Boolean {
        val matchesCategory = category == null || material.category == category
        val matchesStatus = status == null || material.status == status
        val matchesQuery = query.isBlank() || material.name.contains(query, ignoreCase = true)
        return matchesCategory && matchesStatus && matchesQuery
    }
}
