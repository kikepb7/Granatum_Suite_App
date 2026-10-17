package com.granatum.feature.events.presentation.common

import androidx.compose.runtime.Immutable
import com.granatum.core.presentation.util.UiText

/** One event as a card of the list. Everything already formatted for display. */
@Immutable
internal data class EventCardUiState(
    val id: String,
    val name: String,
    val description: String?,
    val dayLabel: String,
    val monthLabel: String,
    val dateLabel: String,
    val relativeLabel: UiText,
    val materialsLabel: UiText,
    val isFinished: Boolean,
    val isToday: Boolean
)

/** A material of an event, as a row of its detail. */
@Immutable
internal data class EventMaterialUiState(
    val materialId: String,
    val category: String,
    val description: String,
    val quantity: Int,
    val locationLabel: UiText?,
    val imageUrl: String?,
    /** No longer in the inventory: shown for the record, not editable. */
    val isMissing: Boolean,
    /** A change to its quantity is on its way to the server. */
    val isSaving: Boolean
)
