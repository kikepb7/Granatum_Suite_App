package com.granatum.feature.events.domain.usecase

import com.granatum.core.domain.util.EmptyResult
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.repository.EventsRepository

class RefreshEventsUseCase(private val repository: EventsRepository) {
    suspend fun refresh(): EmptyResult<EventsError> = repository.refresh()

    suspend fun refresh(id: String): EmptyResult<EventsError> = repository.refreshEvent(id)
}
