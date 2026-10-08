package com.granatum.feature.clockin.data.sync

import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.data.mappers.toEntity
import com.granatum.feature.clockin.data.remote.FichajeRemoteDataSource
import com.granatum.feature.clockin.data.remote.SendOutcome
import com.granatum.feature.clockin.database.dao.ClockEventDao
import com.granatum.feature.clockin.database.dao.ServerShiftDao
import com.granatum.feature.clockin.database.dao.ShiftDao
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.domain.model.ClockRejection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Registers the signed-in person's pending punches with the server
 * (specs/005-fichaje-real, research D2-D4):
 *
 * - In the order they happened. A punch that needs its shift's server id waits until the entry
 *   has been registered.
 * - A temporary failure stops the pass: what comes after must not overtake it.
 * - A definitive refusal marks the punch REJECTED and is never retried; if it was a shift's
 *   entry, the rest of that shift goes with it. Other shifts carry on.
 * - FICHAJE_YA_EN_CURSO on an entry means the shift already exists — opened from another
 *   device, or by an earlier attempt whose answer was lost — so it is adopted, not duplicated.
 *
 * Retrying is safe because every punch keeps the same `clientEventId` and the same body.
 */
class ShiftSyncEngine(
    private val remote: FichajeRemoteDataSource,
    private val eventDao: ClockEventDao,
    private val shiftDao: ShiftDao,
    private val serverShiftDao: ServerShiftDao,
    private val sessionStorage: SessionStorage,
    /** Online/offline, from [ConnectivityObserver.observe]. */
    private val connectivity: Flow<Boolean>,
    private val logger: AppLogger,
    private val clock: Clock = Clock.System
) {
    private val mutex = Mutex()

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            connectivity.distinctUntilChanged().collectLatest { online ->
                if (online) {
                    syncNow()
                    while (hasPending()) {
                        delay(POLL_INTERVAL_MS)
                        syncNow()
                    }
                }
            }
        }
    }

    /** True when the queue is empty or only waits on things that need no retry. */
    suspend fun syncNow(): Boolean = mutex.withLock {
        val employeeId = sessionStorage.observeSession().firstOrNull()?.employeeId ?: return@withLock true
        val rejectedShifts = mutableSetOf<String>()

        for (event in eventDao.getPendingEvents(employeeId)) {
            val shiftLocalId = event.shiftLocalId
            val shift = shiftLocalId?.let { shiftDao.get(it) }
            if (shift == null || shiftLocalId in rejectedShifts) {
                eventDao.markRejected(event.id, ClockRejection.SHIFT_NOT_REGISTERED)
                continue
            }
            if (event.type == CLOCK_IN && shift.serverId != null) {
                // Adopted from the server: the entry already exists there.
                eventDao.markSynced(event.id, clock.now().toEpochMilliseconds())
                continue
            }
            if (event.type != CLOCK_IN && shift.serverId == null) {
                // Its entry is not registered yet; it is earlier in the queue, so this only
                // happens when that entry is waiting too. Leave it for the next pass.
                continue
            }

            eventDao.markSyncing(event.id)
            when (val outcome = remote.send(event, shift.serverId)) {
                is SendOutcome.Registered -> {
                    if (event.type == CLOCK_IN) shiftDao.setServerId(shift.localId, outcome.shift.id)
                    eventDao.markSynced(event.id, clock.now().toEpochMilliseconds())
                    serverShiftDao.upsert(outcome.shift.toEntity(fetchedAtEpochMillis = clock.now().toEpochMilliseconds()))
                }
                SendOutcome.Temporary -> {
                    eventDao.markRetry(event.id, clock.now().toEpochMilliseconds())
                    return@withLock false
                }
                is SendOutcome.Refused -> when {
                    event.type == CLOCK_IN && outcome.code == SHIFT_ALREADY_OPEN -> {
                        if (!adoptOpenShift(employeeId, shift.localId, event)) {
                            eventDao.markRetry(event.id, clock.now().toEpochMilliseconds())
                            return@withLock false
                        }
                    }
                    outcome.code == EMPLOYEE_INACTIVE -> {
                        // Nothing else will go through either; stop rather than burn the queue.
                        eventDao.markRejected(event.id, outcome.code)
                        return@withLock true
                    }
                    else -> {
                        if (outcome.code == KEY_REUSED) {
                            logger.error("clientEventId reused with a different body for ${event.id}: a serialisation bug")
                        }
                        eventDao.markRejected(event.id, outcome.code)
                        if (event.type == CLOCK_IN) {
                            eventDao.rejectPendingOfShift(shift.localId, ClockRejection.SHIFT_NOT_REGISTERED)
                            rejectedShifts += shift.localId
                        }
                    }
                }
            }
        }
        true
    }

    /** research D3: take the open shift the server already has. */
    private suspend fun adoptOpenShift(employeeId: String, shiftLocalId: String, entry: ClockEventEntity): Boolean {
        val today = clock.todayIn(MADRID)
        val found = remote.fetchShifts(employeeId, today.minus(DatePeriod(days = 3)), today)
        val open = (found as? Result.Success)?.data?.firstOrNull { it.estado == "EN_CURSO" } ?: return false
        shiftDao.setServerId(shiftLocalId, open.id)
        eventDao.markSynced(entry.id, clock.now().toEpochMilliseconds())
        serverShiftDao.upsert(open.toEntity(fetchedAtEpochMillis = clock.now().toEpochMilliseconds()))
        logger.info("Adopted the server's open shift instead of opening a second one")
        return true
    }

    private suspend fun hasPending(): Boolean {
        val employeeId = sessionStorage.observeSession().firstOrNull()?.employeeId ?: return false
        return eventDao.getPendingEvents(employeeId).isNotEmpty()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L
        private const val CLOCK_IN = "CLOCK_IN"
        const val SHIFT_ALREADY_OPEN = "FICHAJE_YA_EN_CURSO"
        const val EMPLOYEE_INACTIVE = "EMPLEADO_INACTIVO"
        const val KEY_REUSED = "CLIENT_EVENT_ID_REUTILIZADO"

        /** The server reads civil dates in Madrid (backend contract, "Tiempo"). */
        val MADRID: TimeZone = TimeZone.of("Europe/Madrid")
    }
}
