package com.granatum.feature.staff.domain

import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.util.Error
import com.granatum.core.domain.util.Result
import kotlinx.datetime.LocalDate

enum class ContractType { FULL_TIME, PART_TIME, HOURLY }

/** A person on the staff, as `/api/empleados` keeps them (backend 2ec33d0). */
data class StaffMember(
    val id: String,
    val name: String,
    val identityDocument: String,
    val position: String,
    val contract: ContractType,
    val startDate: LocalDate,
    val active: Boolean,
)

/** What can change on a staff record: everything but the identity document. */
data class StaffEdit(
    val name: String,
    val position: String,
    val contract: ContractType,
    val startDate: LocalDate,
)

/** Onboarding in one step (backend feature 009): staff record and account. */
data class Onboarding(
    val name: String,
    val identityDocument: String,
    val position: String,
    val contract: ContractType,
    val startDate: LocalDate,
    val email: String,
    val role: UserRole,
)

/**
 * The temporary password the server generates, shown once and handed over in person. Held only in
 * memory while on screen; never logged ([toString] hides it).
 */
class TemporaryCredentials(
    val employeeId: String,
    val email: String,
    val role: UserRole,
    val password: String,
    /** False when onboarding linked the account to a staff record that already existed. */
    val recordCreated: Boolean = true,
) {
    override fun toString(): String = "TemporaryCredentials(email=$email, role=$role, password=***)"
}

object StaffLimits {
    const val NAME = 150
    const val DOCUMENT = 20
    const val POSITION = 100
    const val EMAIL = 254

    /** The roles an account can be given; DESCONOCIDO is the app's own, never the server's. */
    val ASSIGNABLE_ROLES = listOf(UserRole.EMPLEADO, UserRole.ENCARGADO, UserRole.REPRESENTANTE, UserRole.ADMIN)
}

sealed interface StaffError : Error {
    data object NoInternet : StaffError

    data object NotFound : StaffError

    data object InvalidDocument : StaffError

    data object DuplicateDocument : StaffError

    data object EmailTaken : StaffError

    data object AccountExists : StaffError

    /** Resetting a record that has no account yet: offer to give it one. */
    data object NoAccount : StaffError

    data object Incoherent : StaffError

    data object Invalid : StaffError

    data object Forbidden : StaffError

    data object RateLimited : StaffError

    data object Unknown : StaffError
}

/** Straight to the server, nothing kept on the device: personal data (research D3). */
interface StaffRepository {
    suspend fun members(): Result<List<StaffMember>, StaffError>

    suspend fun member(id: String): Result<StaffMember, StaffError>

    suspend fun update(
        id: String,
        edit: StaffEdit,
    ): Result<StaffMember, StaffError>

    suspend fun setActive(
        id: String,
        active: Boolean,
    ): Result<StaffMember, StaffError>

    suspend fun onboard(onboarding: Onboarding): Result<TemporaryCredentials, StaffError>

    suspend fun grantAccess(
        employeeId: String,
        email: String,
        role: UserRole,
    ): Result<TemporaryCredentials, StaffError>

    suspend fun resetPassword(employeeId: String): Result<TemporaryCredentials, StaffError>
}

/** Single entry point for the staff screens, as in inventory and invoicing. */
class StaffUseCases(
    val repository: StaffRepository,
)

/** Search on the device: the server does not page or search staff. Accent-insensitive. */
fun List<StaffMember>.search(
    text: String,
    active: Boolean?,
): List<StaffMember> {
    val needle = text.trim().fold()
    return filter { member ->
        (active == null || member.active == active) &&
            (needle.isEmpty() || listOf(member.name, member.identityDocument, member.position).any { it.fold().contains(needle) })
    }.sortedBy { it.name.fold() }
}

private fun String.fold(): String =
    lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('ü', 'u')
