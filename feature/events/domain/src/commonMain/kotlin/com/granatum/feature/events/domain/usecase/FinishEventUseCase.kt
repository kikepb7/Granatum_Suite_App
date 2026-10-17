package com.granatum.feature.events.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.repository.EventsRepository

class FinishEventUseCase(private val repository: EventsRepository) {
    suspend fun finish(id: String): Result<Event, EventsError> = repository.finish(id)
}
