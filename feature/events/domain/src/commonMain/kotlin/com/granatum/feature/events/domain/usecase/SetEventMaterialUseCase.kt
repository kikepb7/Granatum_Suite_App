package com.granatum.feature.events.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.repository.EventsRepository

/** Sets how many units of a material the event takes; zero removes it from the event. */
class SetEventMaterialUseCase(private val repository: EventsRepository) {
    suspend fun set(eventId: String, materialId: String, quantity: Int): Result<Event, EventsError> =
        if (quantity <= 0) repository.removeMaterial(eventId, materialId) else repository.setMaterial(eventId, materialId, quantity)
}
