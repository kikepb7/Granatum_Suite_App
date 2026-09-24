package com.granatum.feature.clockin.domain.model

import kotlinx.datetime.LocalDate

data class DailyAttendanceSummary(
    val date: LocalDate,
    val events: List<ClockEventModel>,
    val workedMinutes: Int,
    val breakMinutes: Int,
    val hasPendingSync: Boolean
)

enum class CorrectionStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class CorrectionRequestModel(
    val id: String,
    val clockEventId: String,
    val employeeId: String,
    val employeeName: String,
    val reason: String,
    val requestedClientTimestamp: kotlinx.datetime.Instant,
    val status: CorrectionStatus
)

data class EmployeeAttendanceSummaryModel(
    val employeeId: String,
    val employeeName: String,
    val currentStatus: ShiftStatus,
    val workedMinutesInPeriod: Int,
    val pendingCorrections: Int
)
