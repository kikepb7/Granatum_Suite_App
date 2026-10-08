package com.granatum.feature.clockin.data.sync

import kotlinx.coroutines.flow.Flow
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import kotlinx.coroutines.flow.firstOrNull
import com.granatum.core.data.networking.post
import com.granatum.core.domain.logger.AppLogger
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.feature.clockin.data.dto.ClockEventDto
import com.granatum.feature.clockin.data.dto.ClockEventPushDto
import com.granatum.feature.clockin.data.mappers.toPushDto
import com.granatum.feature.clockin.database.dao.ClockEventDao
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlin.time.Clock

/**
 * The piece that makes clock punches offline-first instead of merely
 * offline-tolerant: `ClockInRepository` writes every punch to Room and
 * returns immediately, and this class is the only thing that ever talks to
 * the network on their behalf.
 *
 * Why order matters here specifically (unlike the inventory cache): a set
 * of punches only makes sense as a sequence — CLOCK_IN before BREAK_START
 * before BREAK_END before CLOCK_OUT. Pushing them out of order, or in
 * parallel, could let the backend see an invalid transition even though the
 * device applied them correctly and in order locally. So `syncNow()`:
 *  1. Reads all PENDING/FAILED events ordered by `clientTimestamp` ASC.
 *  2. Pushes them ONE AT A TIME, in that order.
 *  3. Stops at the first failure instead of skipping ahead, so a later
 *     event never reaches the server before an earlier one.
 *
 * Idempotency: each event's client-generated id is sent as the request's
 * identity (see `ClockEventPushDto`). The backend must upsert by that id so
 * a request that actually succeeded but whose response was lost to a flaky
 * connection doesn't get double-applied on the inevitable retry. This is a
 * hard requirement on the backend contract, not just a nice-to-have.
 *
 * Triggers: `start()` reacts to connectivity regained (via
 * [ConnectivityObserver]) and otherwise polls every [POLL_INTERVAL_MS]
 * while anything is pending — cheap, since it's a no-op once the queue is
 * empty. `ClockInRepository` also calls [syncNow] eagerly, fire-and-forget,
 * right after every local write, so a punch made while online still syncs
 * immediately instead of waiting for the next poll tick.
 */
class ClockEventSyncManager(
    private val httpClient: HttpClient,
    private val dao: ClockEventDao,
    private val sessionStorage: SessionStorage,
    /** Online/offline, from [ConnectivityObserver.observe]. A flow rather than the platform class keeps this testable in common code. */
    private val connectivity: Flow<Boolean>,
    private val logger: AppLogger,
    private val clock: Clock = Clock.System
) {
    private val syncMutex = Mutex()

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            connectivity
                .distinctUntilChanged()
                .collectLatest { isOnline ->
                    if (isOnline) {
                        syncNow()
                        pollWhilePending()
                    }
                }
        }
    }

    private suspend fun pollWhilePending() {
        while (pendingForCurrentEmployee().isNotEmpty()) {
            delay(POLL_INTERVAL_MS)
            syncNow()
        }
    }

    suspend fun syncNow(): EmptyResult<DataError.Remote> {
        // A mutex, not a "skip if already running" flag: a caller awaiting this
        // result (e.g. a manual "sync now" button) must see the queue drained,
        // not silently no-op because a background pass was already in flight.
        return syncMutex.withLock {
            val pending = pendingForCurrentEmployee()
            for (event in pending) {
                dao.markSyncing(id = event.id)
                val result = httpClient.post<ClockEventPushDto, ClockEventDto>(
                    route = "/attendance/events",
                    body = event.toPushDto()
                )
                when (result) {
                    is Success -> {
                        dao.markSynced(
                            id = event.id,
                            serverTimestampEpochMillis = Instant.parse(result.data.serverTimestamp).toEpochMilliseconds()
                        )
                    }
                    is Failure -> {
                        dao.markFailed(id = event.id, attemptEpochMillis = clock.now().toEpochMilliseconds())
                        logger.error(message = "Failed to sync clock event ${event.id} (${event.type}): ${result.error}")
                        // Stop here — do not push later events out of order.
                        return@withLock Failure(error = result.error)
                    }
                }
            }
            Result.Success(data = Unit)
        }
    }

    /**
     * Only the signed-in person's punches, because the server attributes each one to whoever
     * signs the request (spec 004, FR-028). Another person's pending punches wait, untouched,
     * until they sign in again; rows with no owner are never sent.
     */
    private suspend fun pendingForCurrentEmployee(): List<ClockEventEntity> {
        val employeeId = sessionStorage.observeSession().firstOrNull()?.employeeId ?: return emptyList()
        return dao.getPendingEvents(employeeId)
    }

    private companion object {
        const val POLL_INTERVAL_MS = 30_000L
    }
}
