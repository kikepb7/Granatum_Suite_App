package com.granatum.feature.events.domain.usecase

import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.repository.EventsRepository
import kotlinx.coroutines.flow.Flow

class ObserveEventsUseCase(private val repository: EventsRepository) {
    fun observe(): Flow<List<Event>> = repository.observeEvents()
}
