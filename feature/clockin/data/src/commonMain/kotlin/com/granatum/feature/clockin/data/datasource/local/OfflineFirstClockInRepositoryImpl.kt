package com.granatum.feature.clockin.data.datasource.local

import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.data.dto.CrearCorreccionRequestDto
import com.granatum.feature.clockin.data.mappers.toDomain
import com.granatum.feature.clockin.data.mappers.toDto
import com.granatum.feature.clockin.data.mappers.toEntity
import com.granatum.feature.clockin.data.merge.ShiftMerger
import com.granatum.feature.clockin.data.remote.FichajeRemoteDataSource
import com.granatum.feature.clockin.data.remote.RemoteFailure
import com.granatum.feature.clockin.data.sync.ShiftSyncEngine
import com.granatum.feature.clockin.database.dao.ClockEventDao
import com.granatum.feature.clockin.database.dao.ServerShiftDao
import com.granatum.feature.clockin.database.dao.ShiftDao
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.database.entity.ShiftEntity
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.CorrectionDraft
import com.granatum.feature.clockin.domain.model.CorrectionError
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.ShiftState
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.SyncState
import com.granatum.feature.clockin.domain.model.TodayState
import com.granatum.feature.clockin.domain.model.currentShiftStatus
import com.granatum.feature.clockin.domain.repository.ClockInRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Every punch lands on the device first and returns at once (offline-first); [ShiftSyncEngine]
 * registers it later. Reads combine the server's copy with what is still pending
 * (specs/005-fichaje-real). Everything is scoped to the signed-in person (spec 004, FR-028).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstClockInRepositoryImpl(
    private val eventDao: ClockEventDao,
    private val shiftDao: ShiftDao,
    private val serverShiftDao: ServerShiftDao,
    private val remote: FichajeRemoteDataSource,
    private val sessionStorage: SessionStorage,
    private val syncEngine: ShiftSyncEngine,
    private val appScope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ClockInRepository {

    private val currentEmployeeId: Flow<String?> =
        sessionStorage.observeSession().map { it?.employeeId }.distinctUntilChanged()

    private val allShifts: Flow<List<ShiftModel>> = currentEmployeeId.flatMapLatest { employeeId ->
        if (employeeId == null) {
            flowOf(emptyList())
        } else {
            combine(
                serverShiftDao.observeAll(employeeId),
                shiftDao.observeAll(employeeId),
                eventDao.observeAllEvents(employeeId)
            ) { server, local, events -> ShiftMerger.merge(server, local, events, clock.now()) }
        }
    }

    override fun observeToday(): Flow<TodayState> = allShifts.map { shifts ->
        val today = clock.now().toLocalDateTime(timeZone).date
        val ofToday = shifts.filter { it.start.toLocalDateTime(timeZone).date == today || it.state == ShiftState.IN_PROGRESS }
        val recent = shifts.filter { clock.now() - it.start < RECENT_REJECTIONS }
        TodayState(
            status = ofToday.lastOrNull()?.takeIf { it.state == ShiftState.IN_PROGRESS }?.items?.currentShiftStatus()
                ?: ShiftStatus.CLOCKED_OUT,
            shifts = ofToday,
            pendingCount = shifts.sumOf { s -> s.items.count { it.syncState == SyncState.PENDING || it.syncState == SyncState.SYNCING } },
            rejected = recent.flatMap { it.rejections }
        )
    }

    override fun observeMonth(year: Int, month: Int): Flow<List<ShiftModel>> = allShifts.map { shifts ->
        shifts.filter {
            val date = it.start.toLocalDateTime(timeZone).date
            date.year == year && date.month.number == month
        }
    }

    override fun observeShift(key: String): Flow<ShiftModel?> = allShifts.map { shifts -> shifts.firstOrNull { it.key == key } }

    override fun observePendingSyncCount(): Flow<Int> = currentEmployeeId.flatMapLatest { employeeId ->
        if (employeeId == null) flowOf(0) else eventDao.observePendingCount(employeeId)
    }

    override suspend fun refreshMonth(year: Int, month: Int) {
        val employeeId = currentEmployeeId.first() ?: return
        val first = LocalDate(year, month, 1)
        val last = first.plus(DatePeriod(months = 1)).plus(DatePeriod(days = -1))
        val shifts = (remote.fetchShifts(employeeId, first, last) as? Result.Success)?.data ?: return
        val flags = (remote.fetchMonthSummary(employeeId, year, month) as? Result.Success)?.data?.dias
            ?.associate { it.fecha to (it.corregido to it.reconstruido) }
            .orEmpty()
        val fetchedAt = clock.now().toEpochMilliseconds()
        val madrid = ShiftSyncEngine.MADRID
        serverShiftDao.replaceRange(
            employeeId = employeeId,
            fromEpochMillis = first.atStartOfDayIn(madrid).toEpochMilliseconds(),
            toEpochMillis = last.plus(DatePeriod(days = 1)).atStartOfDayIn(madrid).toEpochMilliseconds() - 1,
            shifts = shifts.map { dto ->
                val date = kotlin.time.Instant.parse(dto.entrada).toLocalDateTime(madrid).date.toString()
                val (corrected, reconstructed) = flags[date] ?: (false to false)
                dto.toEntity(fetchedAt, corrected, reconstructed)
            }
        )
    }

    override suspend fun clockIn(): Result<Unit, ClockActionError> {
        val employeeId = currentEmployeeId.first() ?: return Result.Failure(ClockActionError.NoSession)
        if (status() != ShiftStatus.CLOCKED_OUT) return Result.Failure(ClockActionError.AlreadyClockedIn)
        val now = clock.now().toEpochMilliseconds()
        val shift = ShiftEntity(localId = newId(), employeeId = employeeId, serverId = null, startedAtEpochMillis = now)
        shiftDao.upsert(shift)
        record(employeeId, shift.localId, ClockEventType.CLOCK_IN, now)
        return Result.Success(Unit)
    }

    override suspend fun startBreak(type: BreakType): Result<Unit, ClockActionError> =
        onOpenShift(ClockEventType.BREAK_START, allowedFrom = ShiftStatus.CLOCKED_IN, error = ClockActionError.AlreadyOnBreak, breakType = type)

    override suspend fun endBreak(): Result<Unit, ClockActionError> =
        onOpenShift(ClockEventType.BREAK_END, allowedFrom = ShiftStatus.ON_BREAK, error = ClockActionError.NotOnBreak)

    /** FR-004: an open break has to be ended before clocking out, as the server requires. */
    override suspend fun clockOut(): Result<Unit, ClockActionError> = when (status()) {
        ShiftStatus.ON_BREAK -> Result.Failure(ClockActionError.OnBreak)
        else -> onOpenShift(ClockEventType.CLOCK_OUT, allowedFrom = ShiftStatus.CLOCKED_IN, error = ClockActionError.NotClockedIn)
    }

    override suspend fun syncNow() {
        syncEngine.syncNow()
    }

    override suspend fun getCorrections(shiftServerId: String): Result<List<CorrectionModel>, CorrectionError> =
        when (val result = remote.fetchCorrections(shiftServerId)) {
            is Result.Success -> Result.Success(result.data.map { it.toDomain() }.sortedByDescending { it.createdAt })
            is Result.Failure -> Result.Failure(result.error.toCorrectionError())
        }

    override suspend fun requestCorrection(shiftServerId: String, draft: CorrectionDraft): Result<CorrectionModel, CorrectionError> {
        val request = CrearCorreccionRequestDto(motivo = draft.reason, valoresPropuestos = draft.values.toDto())
        return when (val result = remote.createCorrection(shiftServerId, request)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Failure -> Result.Failure(result.error.toCorrectionError())
        }
    }

    private suspend fun status(): ShiftStatus = observeToday().first().status

    /**
     * Adds a punch to the shift in progress, whether the device opened it or it was opened on
     * another device and only exists on the server (FR-018). In the latter case a local shift is
     * created that points at the server's.
     */
    private suspend fun onOpenShift(
        type: ClockEventType,
        allowedFrom: ShiftStatus,
        error: ClockActionError,
        breakType: BreakType? = null
    ): Result<Unit, ClockActionError> {
        val employeeId = currentEmployeeId.first() ?: return Result.Failure(ClockActionError.NoSession)
        val today = observeToday().first()
        if (today.status != allowedFrom) return Result.Failure(error)
        val open = today.currentShift ?: return Result.Failure(error)

        val shiftLocalId = when (val serverId = open.serverId) {
            null -> open.key
            else -> shiftDao.getByServerId(employeeId, serverId)?.localId
                ?: newId().also { id ->
                    shiftDao.upsert(ShiftEntity(id, employeeId, serverId, open.start.toEpochMilliseconds()))
                }
        }
        record(employeeId, shiftLocalId, type, clock.now().toEpochMilliseconds(), breakType)
        return Result.Success(Unit)
    }

    private suspend fun record(employeeId: String, shiftLocalId: String, type: ClockEventType, at: Long, breakType: BreakType? = null) {
        eventDao.upsertEvent(
            ClockEventEntity(
                id = newId(),
                type = type.name,
                clientTimestampEpochMillis = at,
                serverTimestampEpochMillis = null,
                syncState = SyncState.PENDING.name,
                retryCount = 0,
                lastSyncAttemptEpochMillis = null,
                employeeId = employeeId,
                shiftLocalId = shiftLocalId,
                breakType = breakType?.name
            )
        )
        // The screen already shows the new state from the database; sending happens in the
        // background and, offline, simply waits for the connectivity trigger.
        appScope.launch(Dispatchers.Default) { syncEngine.syncNow() }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newId(): String = Uuid.random().toString()

    private fun RemoteFailure.toCorrectionError(): CorrectionError = when {
        transport == DataError.Remote.NO_INTERNET || transport == DataError.Remote.REQUEST_TIMEOUT -> CorrectionError.NoInternet
        code == "FICHAJE_NO_FINALIZADO" -> CorrectionError.ShiftNotFinished
        code == "EMPLEADO_INACTIVO" -> CorrectionError.Inactive
        code == "VALORES_INCOHERENTES" -> CorrectionError.Invalid(emptySet())
        else -> CorrectionError.Unknown
    }

    private companion object {
        /** Rejections stay on the clock-in screen for a week; after that, the history shows them. */
        val RECENT_REJECTIONS = 7.days
    }
}
