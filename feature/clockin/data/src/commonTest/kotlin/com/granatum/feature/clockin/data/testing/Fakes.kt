package com.granatum.feature.clockin.data.testing

import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import com.granatum.feature.clockin.database.dao.ClockEventDao
import com.granatum.feature.clockin.database.dao.ServerShiftDao
import com.granatum.feature.clockin.database.dao.ShiftDao
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.database.entity.ServerShiftEntity
import com.granatum.feature.clockin.database.entity.ShiftEntity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
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
import kotlinx.coroutines.flow.map

class InMemoryEventDao : ClockEventDao {
    val rows = MutableStateFlow<List<ClockEventEntity>>(emptyList())
    private fun pending(e: ClockEventEntity) = e.syncState == "PENDING" || e.syncState == "SYNCING"
    private fun update(pred: (ClockEventEntity) -> Boolean, change: (ClockEventEntity) -> ClockEventEntity) {
        rows.value = rows.value.map { if (pred(it)) change(it) else it }
    }
    fun byId(id: String) = rows.value.single { it.id == id }

    override fun observeAllEvents(employeeId: String) = rows.map { all -> all.filter { it.employeeId == employeeId }.sortedBy { it.clientTimestampEpochMillis } }
    override fun observePendingCount(employeeId: String) = rows.map { all -> all.count { it.employeeId == employeeId && pending(it) } }
    override suspend fun getPendingEvents(employeeId: String) = rows.value.filter { it.employeeId == employeeId && pending(it) }.sortedBy { it.clientTimestampEpochMillis }
    override suspend fun getEventsOfShift(shiftLocalId: String) = rows.value.filter { it.shiftLocalId == shiftLocalId }
    override suspend fun upsertEvent(event: ClockEventEntity) { rows.value = rows.value.filterNot { it.id == event.id } + event }
    override suspend fun markSynced(id: String, serverTimestampEpochMillis: Long) = update({ it.id == id }) { it.copy(syncState = "SYNCED") }
    override suspend fun markRejected(id: String, code: String) = update({ it.id == id }) { it.copy(syncState = "REJECTED", rejectionCode = code) }
    override suspend fun rejectPendingOfShift(shiftLocalId: String, code: String) =
        update({ it.shiftLocalId == shiftLocalId && pending(it) }) { it.copy(syncState = "REJECTED", rejectionCode = code) }
    override suspend fun markRetry(id: String, attemptEpochMillis: Long) = update({ it.id == id }) { it.copy(syncState = "PENDING", retryCount = it.retryCount + 1) }
    override suspend fun markSyncing(id: String) = update({ it.id == id }) { it.copy(syncState = "SYNCING") }
}

class InMemoryShiftDao : ShiftDao {
    val rows = MutableStateFlow<List<ShiftEntity>>(emptyList())
    override suspend fun upsert(shift: ShiftEntity) { rows.value = rows.value.filterNot { it.localId == shift.localId } + shift }
    override suspend fun get(localId: String) = rows.value.firstOrNull { it.localId == localId }
    override suspend fun getByServerId(employeeId: String, serverId: String) = rows.value.firstOrNull { it.employeeId == employeeId && it.serverId == serverId }
    override fun observeAll(employeeId: String) = rows.map { all -> all.filter { it.employeeId == employeeId } }
    override suspend fun setServerId(localId: String, serverId: String) {
        rows.value = rows.value.map { if (it.localId == localId) it.copy(serverId = serverId) else it }
    }
}

class InMemoryServerShiftDao : ServerShiftDao {
    val rows = MutableStateFlow<List<ServerShiftEntity>>(emptyList())
    override suspend fun upsert(shift: ServerShiftEntity) { rows.value = rows.value.filterNot { it.id == shift.id } + shift }
    override suspend fun upsertAll(shifts: List<ServerShiftEntity>) = shifts.forEach { upsert(it) }
    override fun observeAll(employeeId: String) = rows.map { all -> all.filter { it.employeeId == employeeId } }
    override suspend fun deleteRange(employeeId: String, fromEpochMillis: Long, toEpochMillis: Long) {
        rows.value = rows.value.filterNot { it.employeeId == employeeId && it.entradaEpochMillis in fromEpochMillis..toEpochMillis }
    }
    override suspend fun replaceRange(employeeId: String, fromEpochMillis: Long, toEpochMillis: Long, shifts: List<ServerShiftEntity>) {
        deleteRange(employeeId, fromEpochMillis, toEpochMillis); upsertAll(shifts)
    }
}

class FakeSessions : SessionStorage {
    val session = MutableStateFlow<Session?>(null)
    override fun observeSession(): Flow<Session?> = session
    override suspend fun set(session: Session?) { this.session.value = session }
    fun signIn(employeeId: String) { session.value = Session("a", "r", employeeId, UserRole.EMPLEADO, false, "$employeeId@granatum.es") }
}

object SilentLogger : AppLogger {
    val errors = mutableListOf<String>()
    override fun debug(message: String) = Unit
    override fun info(message: String) = Unit
    override fun warn(message: String) = Unit
    override fun error(message: String, throwable: Throwable?) { errors += message }
}

/** The app's client config that matters here: JSON in and out (HttpClientFactory sets the same). */
fun mockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Pair<HttpClient, MockEngine> {
    val engine = MockEngine(handler)
    return HttpClient(engine) {
        install(ContentNegotiation) { json() }
        defaultRequest { contentType(ContentType.Application.Json) }
    } to engine
}

fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

fun fichajeJson(id: String, employeeId: String = "ana", estado: String = "EN_CURSO", entrada: String = "2026-10-08T08:00:00Z", pausas: String = "[]", salida: String? = null) =
    """{"id":"$id","empleadoId":"$employeeId","entrada":"$entrada","estado":"$estado","fueIncompleto":false,"pausas":$pausas${salida?.let { ",\"salida\":\"$it\"" } ?: ""}}"""

fun error(code: String) = """{"code":"$code","message":"m"}"""
