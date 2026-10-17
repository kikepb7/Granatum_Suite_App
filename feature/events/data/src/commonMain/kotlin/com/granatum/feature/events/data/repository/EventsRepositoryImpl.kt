package com.granatum.feature.events.data.repository

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.events.data.dto.EventDto
import com.granatum.feature.events.data.mapper.EventMapper
import com.granatum.feature.events.data.remote.EventsRemoteDataSource
import com.granatum.feature.events.database.dao.EventsDao
import com.granatum.feature.events.domain.model.AvailableMaterial
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.repository.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads from the local copy, which every successful call keeps up to date; writes go to the
 * server first and touch the copy only with the server's answer. Without coverage nothing is
 * written anywhere: assigning material moves shared stock, which cannot wait to be replayed.
 */
internal class EventsRepositoryImpl(
    private val remote: EventsRemoteDataSource,
    private val dao: EventsDao,
    private val mapper: EventMapper
) : EventsRepository {

    override fun observeEvents(): Flow<List<Event>> = dao.observeEvents().map { list -> list.map(mapper::toDomain) }

    override fun observeEvent(id: String): Flow<Event?> = dao.observeEvent(id).map { it?.let(mapper::toDomain) }

    override suspend fun refresh(): EmptyResult<EventsError> = when (val r = remote.events()) {
        is Result.Success -> { dao.replaceAll(r.data.map(mapper::toEntity)); Result.Success(Unit) }
        is Result.Failure -> r
    }

    override suspend fun refreshEvent(id: String): EmptyResult<EventsError> = when (val r = saved(remote.event(id), id)) {
        is Result.Success -> Result.Success(Unit)
        is Result.Failure -> r
    }

    override suspend fun create(draft: EventDraft): Result<Event, EventsError> = saved(remote.create(mapper.toRequest(draft)))

    override suspend fun update(id: String, draft: EventDraft): Result<Event, EventsError> =
        saved(remote.update(id, mapper.toRequest(draft)), id)

    override suspend fun delete(id: String): EmptyResult<EventsError> = when (val r = remote.delete(id)) {
        is Result.Success -> { dao.delete(id); r }
        is Result.Failure -> { forgetIfGone(id, r.error); r }
    }

    override suspend fun finish(id: String): Result<Event, EventsError> = saved(remote.finish(id), id)

    override suspend fun setMaterial(eventId: String, materialId: String, quantity: Int): Result<Event, EventsError> =
        saved(remote.setMaterial(eventId, materialId, quantity), eventId)

    override suspend fun removeMaterial(eventId: String, materialId: String): Result<Event, EventsError> =
        saved(remote.removeMaterial(eventId, materialId), eventId)

    override suspend fun fetchAvailableMaterials(): Result<List<AvailableMaterial>, EventsError> =
        when (val r = remote.availableMaterials()) {
            is Result.Success -> Result.Success(r.data.map(mapper::toDomain))
            is Result.Failure -> r
        }

    private suspend fun saved(result: Result<EventDto, EventsError>, id: String? = null): Result<Event, EventsError> =
        when (result) {
            is Result.Success -> {
                val entity = mapper.toEntity(result.data)
                dao.upsert(entity)
                Result.Success(mapper.toDomain(entity))
            }
            is Result.Failure -> { id?.let { forgetIfGone(it, result.error) }; result }
        }

    /** Deleted by someone else: stop showing it. */
    private suspend fun forgetIfGone(id: String, error: EventsError) {
        if (error == EventsError.EventNotFound) dao.delete(id)
    }
}
