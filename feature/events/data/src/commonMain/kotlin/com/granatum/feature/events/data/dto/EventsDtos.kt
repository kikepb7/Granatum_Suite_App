package com.granatum.feature.events.data.dto

import kotlinx.serialization.Serializable

// Field names match docs/openapi.json (backend 93ef398) letter for letter (constitution principle VI).

@Serializable
internal data class EventDto(
    val id: String,
    val name: String,
    val description: String? = null,
    /** `2027-06-13`. */
    val date: String,
    /** `SCHEDULED` or `FINISHED`. */
    val status: String,
    val materials: List<EventMaterialDto> = emptyList(),
    val createdBy: String,
    val finishedAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
internal data class EventMaterialDto(
    val materialId: String,
    val category: String,
    val description: String,
    val quantity: Int,
    val location: EventLocationDto? = null,
    val imageUrl: String? = null
)

@Serializable
internal data class AvailableMaterialDto(
    val id: String,
    val category: String,
    val description: String,
    val quantity: Int,
    val location: EventLocationDto,
    val imageUrl: String? = null
)

@Serializable
internal data class EventLocationDto(val warehouse: String, val section: Int)

@Serializable
internal data class EventRequestDto(val name: String, val description: String?, val date: String)

@Serializable
internal data class EventMaterialQuantityRequestDto(val quantity: Int)

/** The error body; `available` only comes with `INSUFFICIENT_STOCK`. */
@Serializable
internal data class EventsErrorBodyDto(val code: String? = null, val message: String? = null, val available: Int? = null)
