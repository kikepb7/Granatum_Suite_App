package com.granatum.feature.clockin.data

import com.granatum.feature.clockin.data.remote.FichajeRemoteDataSource
import com.granatum.feature.clockin.data.sync.ShiftSyncEngine
import com.granatum.feature.clockin.data.testing.FakeSessions
import com.granatum.feature.clockin.data.testing.InMemoryEventDao
import com.granatum.feature.clockin.data.testing.InMemoryServerShiftDao
import com.granatum.feature.clockin.data.testing.InMemoryShiftDao
import com.granatum.feature.clockin.data.testing.SilentLogger
import com.granatum.feature.clockin.data.testing.error
import com.granatum.feature.clockin.data.testing.fichajeJson
import com.granatum.feature.clockin.data.testing.json
import com.granatum.feature.clockin.data.testing.mockClient
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.database.entity.ShiftEntity
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** specs/005-fichaje-real, research D2-D4. */
class ShiftSyncEngineTest {

    private val events = InMemoryEventDao()
    private val shifts = InMemoryShiftDao()
    private val serverShifts = InMemoryServerShiftDao()
    private val sessions = FakeSessions().apply { signIn("ana") }

    private fun engine(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Pair<ShiftSyncEngine, MutableList<String>> {
        val log = mutableListOf<String>()
        val (client, _) = mockClient { request ->
            log += "${request.method.value} ${request.url.encodedPath.substringAfter("/api")}"
            handler(request)
        }
        return ShiftSyncEngine(FichajeRemoteDataSource(client), events, shifts, serverShifts, sessions, emptyFlow(), SilentLogger) to log
    }

    private suspend fun shift(localId: String, employee: String = "ana", serverId: String? = null) =
        shifts.upsert(ShiftEntity(localId, employee, serverId, 0))

    private suspend fun punch(id: String, type: String, at: Long, shift: String, employee: String = "ana", breakType: String? = null) =
        events.upsertEvent(ClockEventEntity(id, type, at, null, "PENDING", 0, null, employee, shift, breakType))

    private fun path(request: HttpRequestData) = request.url.encodedPath.substringAfter("/api")

    /** A server that registers everything, opening shift `srv-<n>` on each entry. */
    private val happyServer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        if (path(request) == "/fichajes/entrada") json(HttpStatusCode.Created, fichajeJson("srv-1"))
        else json(HttpStatusCode.OK, fichajeJson("srv-1"))
    }

    @Test
    fun a_whole_shift_goes_in_order_on_the_server_shift() = runTest {
        shift("s1")
        punch("e1", "CLOCK_IN", 1, "s1"); punch("e2", "BREAK_START", 2, "s1", breakType = "COMIDA")
        punch("e3", "BREAK_END", 3, "s1"); punch("e4", "CLOCK_OUT", 4, "s1")
        val (engine, log) = engine(happyServer)

        assertTrue(engine.syncNow())

        assertEquals(
            listOf("POST /fichajes/entrada", "POST /fichajes/srv-1/pausa/inicio", "POST /fichajes/srv-1/pausa/fin", "POST /fichajes/srv-1/salida"),
            log
        )
        assertTrue(events.rows.value.all { it.syncState == "SYNCED" })
        assertEquals("srv-1", shifts.get("s1")?.serverId)
        assertEquals(listOf("srv-1"), serverShifts.rows.value.map { it.id })
    }

    @Test
    fun the_break_type_travels_with_the_break_start() = runTest {
        shift("s1", serverId = "srv-1")
        punch("e2", "BREAK_START", 2, "s1", breakType = "DESCANSO")
        var body = ""
        val (engine, _) = engine { request -> body = request.body.toByteArray().decodeToString(); json(HttpStatusCode.OK, fichajeJson("srv-1")) }
        engine.syncNow()
        assertTrue(""""tipo":"DESCANSO"""" in body, body)
    }

    @Test
    fun a_temporary_failure_stops_the_pass_and_keeps_everything_pending() = runTest {
        listOf(HttpStatusCode.ServiceUnavailable, HttpStatusCode.TooManyRequests).forEach { status ->
            events.rows.value = emptyList(); shifts.rows.value = emptyList()
            shift("s1"); punch("e1", "CLOCK_IN", 1, "s1"); punch("e2", "CLOCK_OUT", 2, "s1")
            val (engine, log) = engine { json(status, error("SERVICIO_SATURADO")) }
            assertFalse(engine.syncNow())
            assertEquals(1, log.size, "the exit must not overtake the entry ($status)")
            assertTrue(events.rows.value.all { it.syncState == "PENDING" })
        }
    }

    @Test
    fun no_coverage_is_temporary_too() = runTest {
        shift("s1"); punch("e1", "CLOCK_IN", 1, "s1")
        val (engine, _) = engine { throw kotlinx.io.IOException("offline") }
        assertFalse(engine.syncNow())
        assertEquals("PENDING", events.byId("e1").syncState)
    }

    @Test
    fun an_already_open_shift_is_adopted_not_duplicated() = runTest {
        shift("s1"); punch("e1", "CLOCK_IN", 1, "s1"); punch("e2", "BREAK_START", 2, "s1", breakType = "COMIDA")
        val (engine, log) = engine { request ->
            when {
                path(request) == "/fichajes/entrada" -> json(HttpStatusCode.Conflict, error("FICHAJE_YA_EN_CURSO"))
                request.method == HttpMethod.Get -> json(HttpStatusCode.OK, "[${fichajeJson("srv-other", estado = "CERRADO")},${fichajeJson("srv-open")}]")
                else -> json(HttpStatusCode.OK, fichajeJson("srv-open"))
            }
        }
        assertTrue(engine.syncNow())
        assertEquals("srv-open", shifts.get("s1")?.serverId)
        assertEquals("POST /fichajes/srv-open/pausa/inicio", log.last())
        assertTrue(events.rows.value.all { it.syncState == "SYNCED" })
    }

    @Test
    fun a_refused_entry_takes_its_shift_down_but_other_shifts_carry_on() = runTest {
        shift("old"); punch("o1", "CLOCK_IN", 1, "old"); punch("o2", "CLOCK_OUT", 2, "old")
        shift("new"); punch("n1", "CLOCK_IN", 3, "new")
        val (engine, log) = engine { request ->
            val body = request.body.toByteArray().decodeToString()
            if ("\"o1\"" in body) json(HttpStatusCode.UnprocessableEntity, error("DESVIACION_RELOJ"))
            else json(HttpStatusCode.Created, fichajeJson("srv-new"))
        }
        assertTrue(engine.syncNow())
        assertEquals("REJECTED" to "DESVIACION_RELOJ", events.byId("o1").let { it.syncState to it.rejectionCode })
        assertEquals("REJECTED" to "JORNADA_NO_REGISTRADA", events.byId("o2").let { it.syncState to it.rejectionCode })
        assertEquals("SYNCED", events.byId("n1").syncState)
        assertEquals(2, log.size, "the refused shift's exit is never sent")
    }

    @Test
    fun an_invalid_transition_is_rejected_and_never_retried() = runTest {
        shift("s1", serverId = "srv-1"); punch("e1", "BREAK_END", 1, "s1")
        val (engine, log) = engine { json(HttpStatusCode.Conflict, error("PAUSA_NO_ABIERTA")) }
        engine.syncNow(); engine.syncNow()
        assertEquals("REJECTED", events.byId("e1").syncState)
        assertEquals(1, log.size)
    }

    @Test
    fun an_inactive_person_stops_everything() = runTest {
        shift("s1"); punch("e1", "CLOCK_IN", 1, "s1")
        shift("s2"); punch("e2", "CLOCK_IN", 2, "s2")
        val (engine, log) = engine { json(HttpStatusCode.Conflict, error("EMPLEADO_INACTIVO")) }
        engine.syncNow()
        assertEquals(1, log.size)
        assertEquals("PENDING", events.byId("e2").syncState)
    }

    @Test
    fun a_lost_answer_is_retried_with_the_same_body() = runTest {
        shift("s1"); punch("e1", "CLOCK_IN", 1, "s1")
        val bodies = mutableListOf<String>()
        var first = true
        val (engine, _) = engine { request ->
            bodies += request.body.toByteArray().decodeToString()
            if (first) { first = false; throw kotlinx.io.IOException("answer lost") }
            json(HttpStatusCode.Created, fichajeJson("srv-1"))
        }
        engine.syncNow(); engine.syncNow()
        assertEquals(2, bodies.size)
        assertEquals(bodies[0], bodies[1])
        assertEquals("SYNCED", events.byId("e1").syncState)
    }

    @Test
    fun only_the_signed_in_persons_punches_are_sent() = runTest {
        shift("a", employee = "ana"); punch("e1", "CLOCK_IN", 1, "a", employee = "ana")
        shift("b", employee = "bea"); punch("e2", "CLOCK_IN", 2, "b", employee = "bea")
        val (engine, log) = engine(happyServer)
        engine.syncNow()
        assertEquals(1, log.size)
        assertEquals("PENDING", events.byId("e2").syncState)
    }

    @Test
    fun a_punch_without_shift_is_rejected() = runTest {
        punch("e1", "CLOCK_OUT", 1, "missing")
        val (engine, log) = engine(happyServer)
        engine.syncNow()
        assertEquals("JORNADA_NO_REGISTRADA", events.byId("e1").rejectionCode)
        assertTrue(log.isEmpty())
    }
}
