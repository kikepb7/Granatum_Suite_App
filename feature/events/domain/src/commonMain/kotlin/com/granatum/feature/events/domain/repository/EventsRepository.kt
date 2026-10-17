package com.granatum.feature.events.domain.repository

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.events.domain.model.AvailableMaterial
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.model.EventsError
import kotlinx.coroutines.flow.Flow

/**
 * Reads come from a local copy refreshed from the server and work offline; changes go straight
 * to the server and need a connection, because they move shared inventory stock.
 */
interface EventsRepository {
    fun observeEvents(): Flow<List<Event>>
    fun observeEvent(id: String): Flow<Event?>

    suspend fun refresh(): EmptyResult<EventsError>
    suspend fun refreshEvent(id: String): EmptyResult<EventsError>

    suspend fun create(draft: EventDraft): Result<Event, EventsError>
    suspend fun update(id: String, draft: EventDraft): Result<Event, EventsError>
    suspend fun delete(id: String): EmptyResult<EventsError>
    suspend fun finish(id: String): Result<Event, EventsError>

    /** Sets how many units of the material the event takes; the server moves only the difference. */
    suspend fun setMaterial(eventId: String, materialId: String, quantity: Int): Result<Event, EventsError>
    suspend fun removeMaterial(eventId: String, materialId: String): Result<Event, EventsError>

    /** What can be taken to an event, with its free units. Online only: stock changes all the time. */
    suspend fun fetchAvailableMaterials(): Result<List<AvailableMaterial>, EventsError>
}
