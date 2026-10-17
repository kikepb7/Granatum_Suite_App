package com.granatum.feature.events.domain

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.events.domain.model.AvailableMaterial
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.model.EventStatus
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.repository.EventsRepository
import com.granatum.feature.events.domain.usecase.SetEventMaterialUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SetEventMaterialUseCaseTest {

    /** Records which repository call the use case makes. */
    private class RecordingRepository : EventsRepository {
        val calls = mutableListOf<String>()
        private val event = Event("e1", "Evento 1", null, LocalDate(2027, 6, 13), EventStatus.SCHEDULED, emptyList(), Instant.DISTANT_PAST, Instant.DISTANT_PAST, null)

        override fun observeEvents(): Flow<List<Event>> = emptyFlow()
        override fun observeEvent(id: String): Flow<Event?> = emptyFlow()
        override suspend fun refresh(): EmptyResult<EventsError> = Result.Success(Unit)
        override suspend fun refreshEvent(id: String): EmptyResult<EventsError> = Result.Success(Unit)
        override suspend fun create(draft: EventDraft): Result<Event, EventsError> = Result.Success(event)
        override suspend fun update(id: String, draft: EventDraft): Result<Event, EventsError> = Result.Success(event)
        override suspend fun delete(id: String): EmptyResult<EventsError> = Result.Success(Unit)
        override suspend fun finish(id: String): Result<Event, EventsError> = Result.Success(event)
        override suspend fun setMaterial(eventId: String, materialId: String, quantity: Int): Result<Event, EventsError> {
            calls += "set $materialId $quantity"
            return Result.Success(event)
        }
        override suspend fun removeMaterial(eventId: String, materialId: String): Result<Event, EventsError> {
            calls += "remove $materialId"
            return Result.Success(event)
        }
        override suspend fun fetchAvailableMaterials(): Result<List<AvailableMaterial>, EventsError> = Result.Success(emptyList())
    }

    private val repository = RecordingRepository()
    private val sut = SetEventMaterialUseCase(repository)

    @Test
    fun `When the quantity is positive then it is set on the event`() = runTest {
        val expected = listOf("set m1 5")

        sut.set("e1", "m1", 5)

        assertEquals(expected = expected, actual = repository.calls)
    }

    @Test
    fun `When the quantity drops to zero then the material is removed from the event`() = runTest {
        val expected = listOf("remove m1")

        sut.set("e1", "m1", 0)

        assertEquals(expected = expected, actual = repository.calls)
    }
}
