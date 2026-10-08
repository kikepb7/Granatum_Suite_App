package com.granatum.feature.clockin.data

import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.data.datasource.local.OfflineFirstClockInRepositoryImpl
import com.granatum.feature.clockin.data.remote.FichajeRemoteDataSource
import com.granatum.feature.clockin.data.sync.ShiftSyncEngine
import com.granatum.feature.clockin.data.testing.FakeSessions
import com.granatum.feature.clockin.data.testing.InMemoryEventDao
import com.granatum.feature.clockin.data.testing.InMemoryServerShiftDao
import com.granatum.feature.clockin.data.testing.InMemoryShiftDao
import com.granatum.feature.clockin.data.testing.SilentLogger
import com.granatum.feature.clockin.data.testing.error
import com.granatum.feature.clockin.data.testing.json
import com.granatum.feature.clockin.data.testing.mockClient
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.CorrectionDraft
import com.granatum.feature.clockin.domain.model.CorrectionError
import com.granatum.feature.clockin.domain.model.CorrectionState
import com.granatum.feature.clockin.domain.model.CorrectionValues
import com.granatum.feature.clockin.domain.model.ProposedBreak
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/** spec 005, US5: contract field names and error mapping of a correction request. */
class CorrectionRequestTest {
    private fun t(hhmm: String) = Instant.parse("2026-10-08T$hhmm:00Z")
    private val draft = CorrectionDraft("Olvidé la salida", CorrectionValues(t("08:00"), t("16:00"), listOf(ProposedBreak(BreakType.COMIDA, t("12:00"), t("12:30")))))

    private fun kotlinx.coroutines.test.TestScope.repo(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): OfflineFirstClockInRepositoryImpl {
        val (client, _) = mockClient(handler)
        val remote = FichajeRemoteDataSource(client)
        val events = InMemoryEventDao(); val shifts = InMemoryShiftDao(); val server = InMemoryServerShiftDao()
        val sessions = FakeSessions().apply { signIn("ana") }
        val engine = ShiftSyncEngine(remote, events, shifts, server, sessions, emptyFlow(), SilentLogger)
        return OfflineFirstClockInRepositoryImpl(events, shifts, server, remote, sessions, engine, backgroundScope)
    }

    @Test
    fun the_request_uses_the_contract_shape() = runTest {
        var body = ""
        var path = ""
        val repo = repo { request ->
            body = request.body.toByteArray().decodeToString(); path = request.url.encodedPath
            json(HttpStatusCode.Created, """{"id":"c1","fichajeId":"srv","solicitanteId":"ana","estado":"PENDIENTE","motivo":"Olvidé la salida","valoresPropuestos":{"entrada":"2026-10-08T08:00:00Z","salida":"2026-10-08T16:00:00Z","pausas":[]},"creadaEn":"2026-10-08T18:00:00Z"}""")
        }
        val result = repo.requestCorrection("srv", draft)
        assertTrue(path.endsWith("/fichajes/srv/correcciones"), path)
        assertTrue(""""motivo":"Olvidé la salida"""" in body)
        assertTrue(""""pausas":[{"tipo":"COMIDA","inicio":"2026-10-08T12:00:00Z","fin":"2026-10-08T12:30:00Z"}]""" in body, body)
        assertEquals(CorrectionState.PENDIENTE, (result as Result.Success).data.state)
    }

    @Test
    fun server_codes_map_to_errors() = runTest {
        assertEquals(Result.Failure(CorrectionError.ShiftNotFinished), repo { json(HttpStatusCode.Conflict, error("FICHAJE_NO_FINALIZADO")) }.requestCorrection("srv", draft))
        assertEquals(Result.Failure(CorrectionError.Inactive), repo { json(HttpStatusCode.Conflict, error("EMPLEADO_INACTIVO")) }.requestCorrection("srv", draft))
        assertEquals(Result.Failure(CorrectionError.Invalid(emptySet())), repo { json(HttpStatusCode.UnprocessableEntity, error("VALORES_INCOHERENTES")) }.requestCorrection("srv", draft))
    }
}
