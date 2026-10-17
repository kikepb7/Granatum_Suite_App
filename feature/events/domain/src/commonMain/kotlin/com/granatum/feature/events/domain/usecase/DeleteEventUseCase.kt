package com.granatum.feature.events.domain.usecase

import com.granatum.core.domain.util.EmptyResult
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.repository.EventsRepository

class DeleteEventUseCase(private val repository: EventsRepository) {
    suspend fun delete(id: String): EmptyResult<EventsError> = repository.delete(id)
}
