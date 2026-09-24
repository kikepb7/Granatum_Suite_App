package com.granatum.feature.clockin.data.mappers

import com.granatum.feature.clockin.data.dto.ClockEventPushDto
import com.granatum.feature.clockin.data.dto.CorrectionRequestDto
import com.granatum.feature.clockin.data.dto.EmployeeAttendanceSummaryDto
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.domain.model.ClockEventModel
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.CorrectionRequestModel
import com.granatum.feature.clockin.domain.model.CorrectionStatus
import com.granatum.feature.clockin.domain.model.EmployeeAttendanceSummaryModel
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.SyncState
import kotlinx.datetime.Instant

fun ClockEventEntity.toDomain(): ClockEventModel = ClockEventModel(
    id = id,
    type = ClockEventType.valueOf(type),
    clientTimestamp = Instant.fromEpochMilliseconds(clientTimestampEpochMillis),
    serverTimestamp = serverTimestampEpochMillis?.let { Instant.fromEpochMilliseconds(it) },
    syncState = SyncState.valueOf(syncState)
)

fun ClockEventEntity.toPushDto(): ClockEventPushDto = ClockEventPushDto(
    id = id,
    type = type,
    clientTimestamp = Instant.fromEpochMilliseconds(clientTimestampEpochMillis).toString()
)

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
