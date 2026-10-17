package com.granatum.feature.events.presentation

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.events.domain.model.AvailableMaterial
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.model.EventMaterial
import com.granatum.feature.events.domain.model.EventStatus
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.model.MaterialLocation
import com.granatum.feature.events.domain.provider.TodayProvider
import com.granatum.feature.events.domain.repository.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal val TODAY = LocalDate(2027, 6, 13)
internal val todayProvider = TodayProvider { TODAY }

internal fun event(
    id: String,
    name: String = "Evento $id",
    date: LocalDate = TODAY,
    status: EventStatus = EventStatus.SCHEDULED,
    materials: List<EventMaterial> = emptyList()
) = Event(id, name, "Boda de Juan y María", date, status, materials, Instant.DISTANT_PAST, Instant.DISTANT_PAST, null)

internal fun eventMaterial(id: String, quantity: Int) =
    EventMaterial(id, "Jarrones", "Jarrón $id", quantity, MaterialLocation("A", 2), null)

internal fun available(id: String, quantity: Int) =
    AvailableMaterial(id, "Jarrones", "Jarrón $id", quantity, MaterialLocation("A", 2), null)

/**
 * An inventory-aware stand-in for the server: setting a material moves units between [stock] and
 * the event, and refuses with InsufficientStock as the backend does.
 */
internal class FakeEventsRepository(events: List<Event> = emptyList()) : EventsRepository {
    val events = MutableStateFlow(events)
    val stock = mutableMapOf<String, Int>()
    var failure: EventsError? = null
    var refreshes = 0
    val saved = mutableListOf<Pair<String?, EventDraft>>()
    val setCalls = mutableListOf<Triple<String, String, Int>>()

    private fun <T> answer(value: () -> T): Result<T, EventsError> = failure?.let { Result.Failure(it) } ?: Result.Success(value())

    override fun observeEvents(): Flow<List<Event>> = events
    override fun observeEvent(id: String): Flow<Event?> = events.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun refresh(): EmptyResult<EventsError> { refreshes++; return answer { } }
    override suspend fun refreshEvent(id: String): EmptyResult<EventsError> = answer { }

    override suspend fun create(draft: EventDraft) = answer {
        saved += null to draft
        event("new", draft.name, draft.date).also { e -> events.update { it + e } }
    }

    override suspend fun update(id: String, draft: EventDraft) = answer {
        saved += id to draft
        events.value.first { it.id == id }.copy(name = draft.name, date = draft.date)
    }

    override suspend fun delete(id: String): EmptyResult<EventsError> = answer { events.update { list -> list.filterNot { it.id == id } } }

    override suspend fun finish(id: String) = answer {
        val finished = events.value.first { it.id == id }.copy(status = EventStatus.FINISHED)
        events.update { list -> list.map { if (it.id == id) finished else it } }
        finished
    }

    override suspend fun setMaterial(eventId: String, materialId: String, quantity: Int): Result<Event, EventsError> {
        failure?.let { return Result.Failure(it) }
        setCalls += Triple(eventId, materialId, quantity)
        val event = events.value.first { it.id == eventId }
        val current = event.materials.firstOrNull { it.materialId == materialId }?.quantity ?: 0
        val free = stock[materialId] ?: 0
        if (quantity - current > free) return Result.Failure(EventsError.InsufficientStock(free))
        stock[materialId] = free - (quantity - current)
        val updated = event.copy(materials = event.materials.filterNot { it.materialId == materialId } + eventMaterial(materialId, quantity))
        events.update { list -> list.map { if (it.id == eventId) updated else it } }
        return Result.Success(updated)
    }

    override suspend fun removeMaterial(eventId: String, materialId: String): Result<Event, EventsError> {
        failure?.let { return Result.Failure(it) }
        setCalls += Triple(eventId, materialId, 0)
        val event = events.value.first { it.id == eventId }
        val current = event.materials.firstOrNull { it.materialId == materialId }?.quantity ?: 0
        stock[materialId] = (stock[materialId] ?: 0) + current
        val updated = event.copy(materials = event.materials.filterNot { it.materialId == materialId })
        events.update { list -> list.map { if (it.id == eventId) updated else it } }
        return Result.Success(updated)
    }

    override suspend fun fetchAvailableMaterials(): Result<List<AvailableMaterial>, EventsError> =
        answer { stock.map { (id, quantity) -> available(id, quantity) } }
}
