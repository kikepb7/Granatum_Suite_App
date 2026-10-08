package com.granatum.feature.clockin.domain.model

import kotlinx.datetime.LocalDate

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
    val requestedClientTimestamp: kotlin.time.Instant,
    val status: CorrectionStatus
)

data class EmployeeAttendanceSummaryModel(
    val employeeId: String,
    val employeeName: String,
    val currentStatus: ShiftStatus,
    val workedMinutesInPeriod: Int,
    val pendingCorrections: Int
)
