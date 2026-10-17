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

    private fun evento(id: String = "e1", estado: String = "PROGRAMADO", materiales: String = MATERIALES) =
        """{"id":"$id","nombre":"Evento 1","descripcion":"Boda de Juan y María","fecha":"2027-06-13","estado":"$estado",""" +
            """"materiales":$materiales,"creadoPor":"u1","finalizadoEn":null,"fechaAlta":"2026-10-17T09:00:00Z","fechaUltimaModificacion":"2026-10-17T09:05:00Z"}"""

    @Test
    fun `When events are refreshed then the cache holds them with their materials`() = runTest {
        val sut = sut { ok("[${evento()}]") }
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
    fun `When an event is created then the contract body is sent and the answer cached`() = runTest {
        val sut = sut { ok(evento(materiales = "[]"), HttpStatusCode.Created) }
        val expected = "POST /eventos" to """{"nombre":"Evento 1","descripcion":"Boda de Juan y María","fecha":"2027-06-13"}"""

        sut.create(EventDraft("  Evento 1 ", " Boda de Juan y María ", LocalDate(2027, 6, 13)))

        assertEquals(expected = expected, actual = requests.single())
        assertEquals(expected = 1, actual = dao.rows.value.size)
    }

    @Test
    fun `When a quantity is set then it travels as the final amount`() = runTest {
        val sut = sut { ok(evento()) }
        val expected = "PUT /eventos/e1/materiales/m1" to """{"cantidad":5}"""

        sut.setMaterial("e1", "m1", 5)

        assertEquals(expected = expected, actual = requests.single())
    }

    @Test
    fun `When there is not enough stock then the available units are reported`() = runTest {
        val sut = sut { ok("""{"code":"STOCK_INSUFICIENTE","message":"m","disponible":3}""", HttpStatusCode.Conflict) }
        val expected = Result.Failure(EventsError.InsufficientStock(available = 3))

        val actual = sut.setMaterial("e1", "m1", 4)

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When the server answers with other codes then each becomes its error`() = runTest {
        suspend fun answer(status: HttpStatusCode, code: String) = sut { ok("""{"code":"$code","message":"m"}""", status) }.finish("e1")

        assertEquals(expected = Result.Failure(EventsError.EventFinished), actual = answer(HttpStatusCode.Conflict, "EVENTO_FINALIZADO"))
        assertEquals(expected = Result.Failure(EventsError.DateInPast), actual = answer(HttpStatusCode.BadRequest, "FECHA_PASADA"))
        assertEquals(expected = Result.Failure(EventsError.MaterialNotFound), actual = answer(HttpStatusCode.NotFound, "MATERIAL_NOT_FOUND"))
        assertEquals(expected = Result.Failure(EventsError.Forbidden), actual = answer(HttpStatusCode.Forbidden, "FORBIDDEN"))
    }

    @Test
    fun `When an event was deleted elsewhere then it leaves the cache`() = runTest {
        sut { ok(evento()) }.refreshEvent("e1")
        val sut = sut { ok("""{"code":"EVENTO_NOT_FOUND","message":"m"}""", HttpStatusCode.NotFound) }

        val actual = sut.setMaterial("e1", "m1", 1)

        assertEquals(expected = Result.Failure(EventsError.EventNotFound), actual = actual)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `When there is no coverage then nothing is written`() = runTest {
        sut { ok(evento()) }.refreshEvent("e1")
        val before = dao.rows.value
        val sut = sut { throw kotlinx.io.IOException("offline") }

        val actual = sut.delete("e1")

        assertEquals(expected = Result.Failure(EventsError.NoInternet), actual = actual)
        assertEquals(expected = before, actual = dao.rows.value)
    }

    @Test
    fun `When the picker loads then materials come with their free units and location`() = runTest {
        val sut = sut { ok("""[{"id":"m1","categoria":"Jarrones","descripcion":"Jarrón","cantidad":3,"localizacion":{"almacen":"B","seccion":4},"imagenUrl":null}]""") }

        val actual = (sut.fetchAvailableMaterials() as Result.Success).data.single()

        assertEquals(expected = 3, actual = actual.quantity)
        assertEquals(expected = MaterialLocation("B", 4), actual = actual.location)
    }

    private companion object {
        const val MATERIALES =
            """[{"materialId":"m1","categoria":"Jarrones","descripcion":"Jarrón cilíndrico","cantidad":5,"localizacion":{"almacen":"A","seccion":2},"imagenUrl":"https://img?token=t"},""" +
                """{"materialId":"m2","categoria":"Cestas","descripcion":"Cesta de mimbre","cantidad":1,"localizacion":null,"imagenUrl":null}]"""
    }
}
