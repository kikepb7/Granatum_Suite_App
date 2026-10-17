package com.granatum.feature.events.data

import com.granatum.core.domain.util.Result
import com.granatum.feature.events.data.mapper.EventMapper
import com.granatum.feature.events.data.remote.EventsRemoteDataSource
import com.granatum.feature.events.data.repository.EventsRepositoryImpl
import com.granatum.feature.events.database.dao.EventsDao
import com.granatum.feature.events.database.entity.EventEntity
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.model.EventMaterial
import com.granatum.feature.events.domain.model.EventStatus
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.model.MaterialLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** specs/012-eventos against the backend 011 contract. */
class EventsRepositoryTest {

    private class MemoryDao : EventsDao {
        val rows = MutableStateFlow<List<EventEntity>>(emptyList())
        override fun observeEvents(): Flow<List<EventEntity>> = rows
        override fun observeEvent(id: String): Flow<EventEntity?> = rows.map { all -> all.firstOrNull { it.id == id } }
        override suspend fun upsert(event: EventEntity) { rows.value = rows.value.filterNot { it.id == event.id } + event }
        override suspend fun upsertAll(events: List<EventEntity>) = events.forEach { upsert(it) }
        override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
        override suspend fun clear() { rows.value = emptyList() }
    }

    private val dao = MemoryDao()
    private val requests = mutableListOf<Pair<String, String>>()

    private fun sut(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): EventsRepositoryImpl {
        val client = HttpClient(MockEngine { request ->
            requests += "${request.method.value} ${request.url.encodedPath.substringAfter("/api")}" to request.body.toByteArray().decodeToString()
            handler(request)
        }) {
            install(ContentNegotiation) { json() }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
        return EventsRepositoryImpl(EventsRemoteDataSource(client), dao, EventMapper())
    }

    private fun MockRequestHandleScope.ok(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun event(id: String = "e1", status: String = "SCHEDULED", materials: String = MATERIALS) =
        """{"id":"$id","name":"Evento 1","description":"Boda de Juan y María","date":"2027-06-13","status":"$status",""" +
            """"materials":$materials,"createdBy":"u1","finishedAt":null,"createdAt":"2026-10-17T09:00:00Z","updatedAt":"2026-10-17T09:05:00Z"}"""

    @Test
    fun `When events are refreshed then the cache holds them with their materials`() = runTest {
        val sut = sut { ok("[${event()}]") }
        val expected = listOf(
            EventMaterial("m2", "Cestas", "Cesta de mimbre", 1, null, null),
            EventMaterial("m1", "Jarrones", "Jarrón cilíndrico", 5, MaterialLocation("A", 2), "https://img?token=t")
        )

        sut.refresh()
        val actual = sut.observeEvents().first().single()

        assertEquals(expected = expected, actual = actual.materials)
        assertEquals(expected = LocalDate(2027, 6, 13), actual = actual.date)
        assertEquals(expected = EventStatus.SCHEDULED, actual = actual.status)
        assertEquals(expected = 6, actual = actual.totalUnits)
    }

    @Test
    fun `When an event is finished then it is cached as finished`() = runTest {
        val sut = sut { ok(event(status = "FINISHED")) }
        val expected = EventStatus.FINISHED

        val actual = (sut.finish("e1") as Result.Success).data.status

        assertEquals(expected = expected, actual = actual)
        assertEquals(expected = "POST /events/e1/finish", actual = requests.single().first)
    }

    @Test
    fun `When an event is created then the contract body is sent and the answer cached`() = runTest {
        val sut = sut { ok(event(materials = "[]"), HttpStatusCode.Created) }
        val expected = "POST /events" to """{"name":"Evento 1","description":"Boda de Juan y María","date":"2027-06-13"}"""

        sut.create(EventDraft("  Evento 1 ", " Boda de Juan y María ", LocalDate(2027, 6, 13)))

        assertEquals(expected = expected, actual = requests.single())
        assertEquals(expected = 1, actual = dao.rows.value.size)
    }

    @Test
    fun `When a quantity is set then it travels as the final amount`() = runTest {
        val sut = sut { ok(event()) }
        val expected = "PUT /events/e1/materials/m1" to """{"quantity":5}"""

        sut.setMaterial("e1", "m1", 5)

        assertEquals(expected = expected, actual = requests.single())
    }

    @Test
    fun `When there is not enough stock then the available units are reported`() = runTest {
        val sut = sut { ok("""{"code":"INSUFFICIENT_STOCK","message":"m","available":3}""", HttpStatusCode.Conflict) }
        val expected = Result.Failure(EventsError.InsufficientStock(available = 3))

        val actual = sut.setMaterial("e1", "m1", 4)

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When the server answers with other codes then each becomes its error`() = runTest {
        suspend fun answer(status: HttpStatusCode, code: String) = sut { ok("""{"code":"$code","message":"m"}""", status) }.finish("e1")

        assertEquals(expected = Result.Failure(EventsError.EventFinished), actual = answer(HttpStatusCode.Conflict, "EVENT_FINISHED"))
        assertEquals(expected = Result.Failure(EventsError.DateInPast), actual = answer(HttpStatusCode.BadRequest, "DATE_IN_PAST"))
        assertEquals(expected = Result.Failure(EventsError.MaterialNotFound), actual = answer(HttpStatusCode.NotFound, "MATERIAL_NOT_FOUND"))
        assertEquals(expected = Result.Failure(EventsError.Forbidden), actual = answer(HttpStatusCode.Forbidden, "FORBIDDEN"))
        assertEquals(expected = Result.Failure(EventsError.Invalid), actual = answer(HttpStatusCode.BadRequest, "VALIDATION"))
    }

    @Test
    fun `When an event was deleted elsewhere then it leaves the cache`() = runTest {
        sut { ok(event()) }.refreshEvent("e1")
        val sut = sut { ok("""{"code":"EVENT_NOT_FOUND","message":"m"}""", HttpStatusCode.NotFound) }

        val actual = sut.setMaterial("e1", "m1", 1)

        assertEquals(expected = Result.Failure(EventsError.EventNotFound), actual = actual)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `When there is no coverage then nothing is written`() = runTest {
        sut { ok(event()) }.refreshEvent("e1")
        val before = dao.rows.value
        val sut = sut { throw kotlinx.io.IOException("offline") }

        val actual = sut.delete("e1")

        assertEquals(expected = Result.Failure(EventsError.NoInternet), actual = actual)
        assertEquals(expected = before, actual = dao.rows.value)
    }

    @Test
    fun `When the picker loads then materials come with their free units and location`() = runTest {
        val sut = sut { ok("""[{"id":"m1","category":"Jarrones","description":"Jarrón","quantity":3,"location":{"warehouse":"B","section":4},"imageUrl":null}]""") }

        val actual = (sut.fetchAvailableMaterials() as Result.Success).data.single()

        assertEquals(expected = 3, actual = actual.quantity)
        assertEquals(expected = MaterialLocation("B", 4), actual = actual.location)
    }

    private companion object {
        const val MATERIALS =
            """[{"materialId":"m1","category":"Jarrones","description":"Jarrón cilíndrico","quantity":5,"location":{"warehouse":"A","section":2},"imageUrl":"https://img?token=t"},""" +
                """{"materialId":"m2","category":"Cestas","description":"Cesta de mimbre","quantity":1,"location":null,"imageUrl":null}]"""
    }
}
