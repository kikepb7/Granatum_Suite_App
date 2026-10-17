package com.granatum.feature.clockin.data.dto

import kotlinx.serialization.Serializable

// Field names match docs/openapi.json letter for letter (constitution principle VI). No
// `location` on the punch requests: optional in the contract and not sent by this app
// (spec 005, FR-024).

@Serializable
data class ClockInRequestDto(val clientEventId: String, val occurredAt: String)

@Serializable
data class BreakStartRequestDto(val clientEventId: String, val occurredAt: String, val type: String)

@Serializable
data class BreakEndRequestDto(val clientEventId: String, val occurredAt: String)

@Serializable
data class ClockOutRequestDto(val clientEventId: String, val occurredAt: String)

@Serializable
data class ShiftLocationDto(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Int? = null
)

@Serializable
data class ShiftBreakDto(
    val id: String,
    val start: String,
    val type: String,
    val end: String? = null
)

@Serializable
data class ShiftDto(
    val id: String,
    val employeeId: String,
    val clockIn: String,
    val status: String,
    val wasIncomplete: Boolean,
    val breaks: List<ShiftBreakDto> = emptyList(),
    val clockOut: String? = null,
    val workedMinutes: Int? = null,
    val clockInLocation: ShiftLocationDto? = null,
    val clockOutLocation: ShiftLocationDto? = null
)

@Serializable
data class ShiftSummaryDayDto(
    val date: String,
    val clockIn: String,
    val corrected: Boolean,
    val reconstructed: Boolean,
    val breakMinutes: Int,
    val clockOut: String? = null,
    val workedMinutes: Int? = null
)

@Serializable
data class MonthlyShiftSummaryDto(
    val year: Int,
    val month: Int,
    val employeeId: String,
    val contractType: String,
    val totalWorkedMinutes: Int,
    val days: List<ShiftSummaryDayDto> = emptyList()
)

@Serializable
data class BreakValuesDto(val type: String, val start: String, val end: String)

@Serializable
data class ShiftValuesDto(
    val clockIn: String,
    val clockOut: String,
    val breaks: List<BreakValuesDto>,
    val location: ShiftLocationDto? = null
)

@Serializable
data class CreateShiftCorrectionRequestDto(val reason: String, val proposedValues: ShiftValuesDto)

@Serializable
data class ShiftCorrectionDto(
    val id: String,
    val shiftId: String,
    val requesterId: String,
    val status: String,
    val reason: String,
    val proposedValues: ShiftValuesDto,
    val createdAt: String,
    val originalValues: ShiftValuesDto? = null,
    val resolutionReason: String? = null,
    val resolvedAt: String? = null,
    val resolvedById: String? = null
)
