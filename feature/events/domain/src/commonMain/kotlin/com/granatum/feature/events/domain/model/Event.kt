package com.granatum.feature.events.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

// Events and the inventory material they take (specs/012-eventos, backend feature 011).

enum class EventStatus { SCHEDULED, FINISHED }

/** Where a material is in the inventory. Its own type: features do not share models. */
data class MaterialLocation(val warehouse: String, val section: Int)

data class Event(
    val id: String,
    val name: String,
    val description: String?,
    val date: LocalDate,
    val status: EventStatus,
    val materials: List<EventMaterial>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val finishedAt: Instant?
) {
    val isFinished: Boolean get() = status == EventStatus.FINISHED
    val totalUnits: Int get() = materials.sumOf { it.quantity }
}

/**
 * A material the event takes. [category] and [description] are those it had when assigned;
 * [location] and [imageUrl] are the inventory's current ones, `null` if it no longer exists.
 */
data class EventMaterial(
    val materialId: String,
    val category: String,
    val description: String,
    val quantity: Int,
    val location: MaterialLocation?,
    val imageUrl: String?
)

/** A material that can be taken to an event, with its free units. */
data class AvailableMaterial(
    val id: String,
    val category: String,
    val description: String,
    val quantity: Int,
    val location: MaterialLocation,
    val imageUrl: String?
)

/** What a person writes when creating or editing an event. */
data class EventDraft(
    val name: String,
    val description: String?,
    val date: LocalDate
)
