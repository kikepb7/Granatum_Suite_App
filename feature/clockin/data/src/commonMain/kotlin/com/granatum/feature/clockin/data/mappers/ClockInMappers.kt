package com.granatum.feature.clockin.data.mappers

import com.granatum.feature.clockin.data.dto.BreakValuesDto
import com.granatum.feature.clockin.data.dto.CorrectionRequestDto
import com.granatum.feature.clockin.data.dto.EmployeeAttendanceSummaryDto
import com.granatum.feature.clockin.data.dto.ShiftBreakDto
import com.granatum.feature.clockin.data.dto.ShiftCorrectionDto
import com.granatum.feature.clockin.data.dto.ShiftDto
import com.granatum.feature.clockin.data.dto.ShiftValuesDto
import com.granatum.feature.clockin.database.entity.ServerShiftEntity
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.CorrectionRequestModel
import com.granatum.feature.clockin.domain.model.CorrectionState
import com.granatum.feature.clockin.domain.model.CorrectionStatus
import com.granatum.feature.clockin.domain.model.CorrectionValues
import com.granatum.feature.clockin.domain.model.EmployeeAttendanceSummaryModel
import com.granatum.feature.clockin.domain.model.ProposedBreak
import com.granatum.feature.clockin.domain.model.ShiftStatus
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal val clockInJson = Json { ignoreUnknownKeys = true }

fun ShiftDto.toEntity(fetchedAtEpochMillis: Long, corrected: Boolean = false, reconstructed: Boolean = false) = ServerShiftEntity(
    id = id,
    employeeId = employeeId,
    clockInEpochMillis = Instant.parse(clockIn).toEpochMilliseconds(),
    clockOutEpochMillis = clockOut?.let { Instant.parse(it).toEpochMilliseconds() },
    status = status,
    workedMinutes = workedMinutes,
    wasIncomplete = wasIncomplete,
    breaksJson = clockInJson.encodeToString(breaks),
    corrected = corrected,
    reconstructed = reconstructed,
    fetchedAtEpochMillis = fetchedAtEpochMillis
)

fun ServerShiftEntity.breaks(): List<ShiftBreakDto> =
    runCatching { clockInJson.decodeFromString<List<ShiftBreakDto>>(breaksJson) }.getOrDefault(emptyList())

fun String.toBreakType(): BreakType = runCatching { BreakType.valueOf(this) }.getOrDefault(BreakType.OTHER)

fun ShiftCorrectionDto.toDomain(): CorrectionModel = CorrectionModel(
    id = id,
    shiftServerId = shiftId,
    state = runCatching { CorrectionState.valueOf(status) }.getOrDefault(CorrectionState.PENDING),
    reason = reason,
    proposed = proposedValues.toDomain(),
    resolutionReason = resolutionReason,
    createdAt = Instant.parse(createdAt),
    resolvedAt = resolvedAt?.let(Instant::parse)
)

fun ShiftValuesDto.toDomain() = CorrectionValues(
    entry = Instant.parse(clockIn),
    exit = Instant.parse(clockOut),
    breaks = breaks.map { ProposedBreak(it.type.toBreakType(), Instant.parse(it.start), Instant.parse(it.end)) }
)

fun CorrectionValues.toDto() = ShiftValuesDto(
    clockIn = entry.toString(),
    clockOut = exit.toString(),
    breaks = breaks.map { BreakValuesDto(type = it.type.name, start = it.start.toString(), end = it.end.toString()) }
)

// Team screen — unchanged until the team phase.

fun CorrectionRequestDto.toDomain(): CorrectionRequestModel = CorrectionRequestModel(
    id = id,
    clockEventId = clockEventId,
    employeeId = employeeId,
    employeeName = employeeName,
    reason = reason,
    requestedClientTimestamp = Instant.parse(requestedClientTimestamp),
    status = runCatching { CorrectionStatus.valueOf(status) }.getOrDefault(CorrectionStatus.PENDING)
)

fun EmployeeAttendanceSummaryDto.toDomain(): EmployeeAttendanceSummaryModel = EmployeeAttendanceSummaryModel(
    employeeId = employeeId,
    employeeName = employeeName,
    currentStatus = runCatching { ShiftStatus.valueOf(currentStatus) }.getOrDefault(ShiftStatus.CLOCKED_OUT),
    workedMinutesInPeriod = workedMinutesInPeriod,
    pendingCorrections = pendingCorrections
)
