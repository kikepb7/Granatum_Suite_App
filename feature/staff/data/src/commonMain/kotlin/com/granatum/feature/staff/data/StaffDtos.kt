package com.granatum.feature.staff.data

import kotlinx.serialization.Serializable

// Field for field with docs/openapi.json (backend 93ef398). Personal data: no toString leaks.

@Serializable
data class EmployeeDto(
    val id: String,
    val name: String,
    val identityDocument: String,
    val position: String,
    val contractType: String,
    val hireDate: String,
    val active: Boolean,
) {
    override fun toString(): String = "EmployeeDto(id=$id)"
}

/** The identity document cannot change: it is not sent (the server refuses a non-null one). */
@Serializable
data class UpdateEmployeeRequestDto(
    val name: String,
    val position: String,
    val contractType: String,
    val hireDate: String,
) {
    override fun toString(): String = "UpdateEmployeeRequestDto(contractType=$contractType)"
}

@Serializable
data class SetEmployeeActiveRequestDto(
    val active: Boolean,
)

@Serializable
data class OnboardingRequestDto(
    val name: String,
    val identityDocument: String,
    val position: String,
    val contractType: String,
    val hireDate: String,
    val email: String,
    val role: String,
) {
    override fun toString(): String = "OnboardingRequestDto(contractType=$contractType, role=$role)"
}

@Serializable
data class OnboardingResponseDto(
    val employeeId: String,
    val accountId: String,
    val email: String,
    val role: String,
    val staffRecordCreated: Boolean,
    val temporaryPassword: String,
) {
    override fun toString(): String = "OnboardingResponseDto(employeeId=$employeeId, temporaryPassword=***)"
}

@Serializable
data class AccountGrantRequestDto(
    val employeeId: String,
    val email: String,
    val role: String,
) {
    override fun toString(): String = "AccountGrantRequestDto(employeeId=$employeeId, role=$role)"
}

@Serializable
data class AccountGrantResponseDto(
    val accountId: String,
    val employeeId: String,
    val email: String,
    val role: String,
    val temporaryPassword: String,
) {
    override fun toString(): String = "AccountGrantResponseDto(employeeId=$employeeId, temporaryPassword=***)"
}
