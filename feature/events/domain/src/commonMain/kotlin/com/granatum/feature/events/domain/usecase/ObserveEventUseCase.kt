package com.granatum.feature.events.domain.usecase

import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.repository.EventsRepository
import kotlinx.coroutines.flow.Flow

class ObserveEventUseCase(private val repository: EventsRepository) {
    fun observe(id: String): Flow<Event?> = repository.observeEvent(id)
}
