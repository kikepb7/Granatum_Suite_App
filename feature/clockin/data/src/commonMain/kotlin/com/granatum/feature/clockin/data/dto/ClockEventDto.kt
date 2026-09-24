package com.granatum.feature.clockin.data.dto

import kotlinx.serialization.Serializable

/**
 * Contract the backend needs to implement (not yet built — this is the
 * MVP's assumption, documented here so it's easy to align once the API
 * exists): POST /attendance/events must be idempotent on `id`, i.e. pushing
 * the same [ClockEventPushDto.id] twice must not create two punches. That's
 * what makes safe retries possible after a dropped connection.
 */
@Serializable
data class ClockEventPushDto(
    val id: String,
    val type: String,
    val clientTimestamp: String
)

@Serializable
data class ClockEventDto(
    val id: String,
    val type: String,
    val clientTimestamp: String,
    val serverTimestamp: String
)

@Serializable
data class CorrectionRequestPushDto(
    val clockEventId: String,
    val reason: String
)

@Serializable
data class CorrectionRequestDto(
    val id: String,
    val clockEventId: String,
    val employeeId: String,
    val employeeName: String,
    val reason: String,
    val requestedClientTimestamp: String,
    val status: String
)

@Serializable
data class CorrectionDecisionRequestDto(
    val note: String? = null
)

@Serializable
data class EmployeeAttendanceSummaryDto(
    val employeeId: String,
    val employeeName: String,
    val currentStatus: String,
    val workedMinutesInPeriod: Int,
    val pendingCorrections: Int
)
