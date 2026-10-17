package com.granatum.feature.events.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.repository.EventsRepository

/** Creates the event, or updates it when [id] is given. */
class SaveEventUseCase(private val repository: EventsRepository) {
    suspend fun save(id: String?, draft: EventDraft): Result<Event, EventsError> =
        if (id == null) repository.create(draft) else repository.update(id, draft)
}
