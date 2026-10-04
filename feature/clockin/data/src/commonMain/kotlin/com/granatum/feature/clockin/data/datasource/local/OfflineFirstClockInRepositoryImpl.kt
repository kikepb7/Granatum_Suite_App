package com.granatum.feature.clockin.data.datasource.local

import com.granatum.core.data.networking.post
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.feature.clockin.data.dto.CorrectionRequestPushDto
import com.granatum.feature.clockin.data.sync.ClockEventSyncManager
import com.granatum.feature.clockin.database.dao.ClockEventDao
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.ClockEventModel
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.DailyAttendanceSummary
import com.granatum.feature.clockin.domain.model.RequestCorrectionError
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.SyncState
import com.granatum.feature.clockin.domain.model.currentShiftStatus
import com.granatum.feature.clockin.domain.repository.ClockInRepository
import com.granatum.feature.clockin.data.mappers.toDomain
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class OfflineFirstClockInRepositoryImpl(
    private val httpClient: HttpClient,
    private val dao: ClockEventDao,
    private val syncManager: ClockEventSyncManager,
    private val appScope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ClockInRepository {

    override fun observeCurrentStatus(): Flow<ShiftStatus> =
        dao.observeLatestEvent().map { latest ->
            listOfNotNull(latest?.toDomain()).currentShiftStatus()
        }

    override fun observeTodayEvents(): Flow<List<ClockEventModel>> {
        val (start, end) = todayRangeEpochMillis()
        return dao.observeEventsBetween(fromEpochMillis = start, toEpochMillis = end)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun observePendingSyncCount(): Flow<Int> = dao.observePendingCount()

    override fun observeHistory(from: LocalDate, to: LocalDate): Flow<List<DailyAttendanceSummary>> {
        val fromMillis = from.atStartOfDayEpochMillis(timeZone)
        val toMillis = to.plusDaysAtStartOfDayEpochMillis(timeZone, days = 1)
        return dao.observeEventsBetween(fromEpochMillis = fromMillis, toEpochMillis = toMillis - 1)
            .map { entities -> entities.map { it.toDomain() }.groupByDay(timeZone) }
    }

    override suspend fun clockIn(): Result<Unit, ClockActionError> =
        transition(
            newType = ClockEventType.CLOCK_IN,
            isAllowed = { it == ShiftStatus.CLOCKED_OUT },
            error = ClockActionError.AlreadyClockedIn
        )

    override suspend fun clockOut(): Result<Unit, ClockActionError> =
        transition(
            newType = ClockEventType.CLOCK_OUT,
            isAllowed = { it == ShiftStatus.CLOCKED_IN || it == ShiftStatus.ON_BREAK },
            error = ClockActionError.NotClockedIn
        )

    override suspend fun startBreak(): Result<Unit, ClockActionError> =
        transition(
            newType = ClockEventType.BREAK_START,
            isAllowed = { it == ShiftStatus.CLOCKED_IN },
            error = ClockActionError.AlreadyOnBreak
        )

    override suspend fun endBreak(): Result<Unit, ClockActionError> =
        transition(
            newType = ClockEventType.BREAK_END,
            isAllowed = { it == ShiftStatus.ON_BREAK },
            error = ClockActionError.NotOnBreak
        )

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun transition(
        newType: ClockEventType,
        isAllowed: (ShiftStatus) -> Boolean,
        error: ClockActionError
    ): Result<Unit, ClockActionError> {
        val currentStatus = observeCurrentStatus().first()
        if (!isAllowed(currentStatus)) return Failure(error = error)

        dao.upsertEvent(
            ClockEventEntity(
                id = Uuid.random().toString(),
                type = newType.name,
                clientTimestampEpochMillis = clock.now().toEpochMilliseconds(),
                serverTimestampEpochMillis = null,
                syncState = SyncState.PENDING.name,
                retryCount = 0,
                lastSyncAttemptEpochMillis = null
            )
        )

        // Fire-and-forget: the UI already reflects the new state from Room.
        // If we're offline this simply fails silently and the connectivity
        // observer in ClockEventSyncManager will pick the queue back up.
        appScope.launch(Dispatchers.Default) { syncManager.syncNow() }

        return Success(data = Unit)
    }

    override suspend fun syncPendingEvents(): EmptyResult<DataError.Remote> = syncManager.syncNow()

    override suspend fun requestCorrection(
        clockEventId: String,
        reason: String
    ): Result<Unit, RequestCorrectionError> {
        val result = httpClient.post<CorrectionRequestPushDto, Unit>(
            route = "/attendance/corrections",
            body = CorrectionRequestPushDto(clockEventId = clockEventId, reason = reason)
        )
        return when (result) {
            is Success -> Success(data = Unit)
            is Failure -> Failure(error = RequestCorrectionError.Remote(dataError = result.error))
        }
    }

    private fun todayRangeEpochMillis(): Pair<Long, Long> {
        val today = clock.now().toLocalDateTime(timeZone).date
        val start = today.atStartOfDayEpochMillis(timeZone)
        val end = today.plusDaysAtStartOfDayEpochMillis(timeZone, days = 1) - 1
        return start to end
    }
}

private fun LocalDate.atStartOfDayEpochMillis(timeZone: TimeZone): Long =
    atStartOfDayIn(timeZone).toEpochMilliseconds()

private fun LocalDate.plusDaysAtStartOfDayEpochMillis(timeZone: TimeZone, days: Int): Long =
    atStartOfDayIn(timeZone).plus(days, DateTimeUnit.DAY, timeZone).toEpochMilliseconds()

private fun List<ClockEventModel>.groupByDay(timeZone: TimeZone): List<DailyAttendanceSummary> =
    groupBy { it.clientTimestamp.toLocalDateTime(timeZone).date }
        .map { (date, events) -> date.toDailySummary(events) }
        .sortedByDescending { it.date }

private fun LocalDate.toDailySummary(events: List<ClockEventModel>): DailyAttendanceSummary {
    val sorted = events.sortedBy { it.clientTimestamp }
    var workedMillis = 0L
    var breakMillis = 0L
    var lastClockIn: Instant? = null
    var lastBreakStart: Instant? = null

    for (event in sorted) {
        when (event.type) {
            ClockEventType.CLOCK_IN -> lastClockIn = event.clientTimestamp
            ClockEventType.BREAK_START -> {
                lastBreakStart = event.clientTimestamp
                lastClockIn?.let { workedMillis += (event.clientTimestamp - it).inWholeMilliseconds }
                lastClockIn = null
            }
            ClockEventType.BREAK_END -> {
                lastBreakStart?.let { breakMillis += (event.clientTimestamp - it).inWholeMilliseconds }
                lastBreakStart = null
                lastClockIn = event.clientTimestamp
            }
            ClockEventType.CLOCK_OUT -> {
                lastClockIn?.let { workedMillis += (event.clientTimestamp - it).inWholeMilliseconds }
                lastClockIn = null
            }
        }
    }

    return DailyAttendanceSummary(
        date = this,
        events = sorted,
        workedMinutes = (workedMillis / 60_000L).toInt(),
        breakMinutes = (breakMillis / 60_000L).toInt(),
        hasPendingSync = sorted.any { it.syncState != SyncState.SYNCED }
    )
}
