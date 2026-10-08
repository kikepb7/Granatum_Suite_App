package com.granatum.feature.clockin.data.dto

import kotlinx.serialization.Serializable

// What remains here backs the team screen, which moves to the real contract in the team
// phase. The punch DTOs moved to FichajeDtos.kt (spec 005, FR-025).

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
