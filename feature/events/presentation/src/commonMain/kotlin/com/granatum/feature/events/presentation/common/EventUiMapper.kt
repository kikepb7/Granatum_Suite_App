package com.granatum.feature.events.presentation.common

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventMaterial
import com.granatum.feature.events.domain.model.MaterialLocation
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.location_short
import granatumsuite.feature.events.presentation.generated.resources.materials_none
import granatumsuite.feature.events.presentation.generated.resources.materials_summary
import granatumsuite.feature.events.presentation.generated.resources.materials_summary_one
import kotlinx.datetime.LocalDate

/** From the domain's events to what the screens render (android-patterns, section 9). */
internal class EventUiMapper(private val dates: EventDateFormatter) {

    fun toCard(event: Event, today: LocalDate) = EventCardUiState(
        id = event.id,
        name = event.name,
        description = event.description,
        dayLabel = dates.day(event.date),
        monthLabel = dates.monthBadge(event.date),
        dateLabel = dates.long(event.date),
        relativeLabel = dates.relative(event.date, today),
        materialsLabel = materialsLabel(event),
        isFinished = event.isFinished,
        isToday = event.date == today
    )

    fun toMaterial(material: EventMaterial, quantity: Int = material.quantity, isSaving: Boolean = false) = EventMaterialUiState(
        materialId = material.materialId,
        category = material.category,
        description = material.description,
        quantity = quantity,
        locationLabel = material.location?.let(::locationLabel),
        imageUrl = material.imageUrl,
        isMissing = material.location == null,
        isSaving = isSaving
    )

    fun locationLabel(location: MaterialLocation): UiText =
        UiText.Resource(Res.string.location_short, arrayOf(location.warehouse, location.section))

    fun materialsLabel(event: Event): UiText = when (event.materials.size) {
        0 -> UiText.Resource(Res.string.materials_none)
        1 -> UiText.Resource(Res.string.materials_summary_one, arrayOf(event.totalUnits))
        else -> UiText.Resource(Res.string.materials_summary, arrayOf(event.materials.size, event.totalUnits))
    }
}
