package com.granatum.feature.staff.data

import com.granatum.core.data.auth.errorBody
import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.map
import com.granatum.feature.staff.domain.ContractType
import com.granatum.feature.staff.domain.Onboarding
import com.granatum.feature.staff.domain.StaffEdit
import com.granatum.feature.staff.domain.StaffError
import com.granatum.feature.staff.domain.StaffMember
import com.granatum.feature.staff.domain.StaffRepository
import com.granatum.feature.staff.domain.TemporaryCredentials
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate

/** `/api/employees`, `/api/auth/onboarding` and `/api/auth/accounts` (backend 93ef398), errors per research D9. */
class KtorStaffRepository(
    private val http: HttpClient,
) : StaffRepository {
    override suspend fun members() =
        call<List<EmployeeDto>> { http.get(constructRoute("/employees")) }.map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun member(id: String) = call<EmployeeDto> { http.get(constructRoute("/employees/$id")) }.map { it.toDomain() }

    override suspend fun update(
        id: String,
        edit: StaffEdit,
    ) = call<EmployeeDto> {
        http.put(constructRoute("/employees/$id")) {
            setBody(
                UpdateEmployeeRequestDto(
                    name = edit.name.trim(),
                    lastName = edit.lastName.trim(),
                    position = edit.position.trim(),
                    contractType = edit.contract.toApi(),
                    hireDate = edit.startDate.toString(),
                ),
            )
        }
    }.map { it.toDomain() }

    override suspend fun setActive(
        id: String,
        active: Boolean,
    ) = call<EmployeeDto> {
        http.patch(
            constructRoute("/employees/$id/active"),
        ) { setBody(SetEmployeeActiveRequestDto(active)) }
    }.map { it.toDomain() }

    override suspend fun onboard(onboarding: Onboarding) =
        call<OnboardingResponseDto> {
            http.post(constructRoute("/auth/onboarding")) {
                setBody(
                    OnboardingRequestDto(
                        name = onboarding.name.trim(),
                        lastName = onboarding.lastName.trim(),
                        identityDocument = onboarding.identityDocument.trim(),
                        position = onboarding.position.trim(),
                        contractType = onboarding.contract.toApi(),
                        hireDate = onboarding.startDate.toString(),
                        email = onboarding.email.trim().lowercase(),
                        role = onboarding.role.name,
                    ),
                )
            }
        }.map {
            TemporaryCredentials(
                it.employeeId,
                it.email,
                UserRole.fromBackend(it.role),
                it.temporaryPassword,
                recordCreated = it.staffRecordCreated,
            )
        }

    override suspend fun grantAccess(
        employeeId: String,
        email: String,
        role: UserRole,
    ) = call<AccountGrantResponseDto> {
        http.post(constructRoute("/auth/accounts")) { setBody(AccountGrantRequestDto(employeeId, email.trim().lowercase(), role.name)) }
    }.map { it.toCredentials() }

    override suspend fun resetPassword(employeeId: String) =
        call<AccountGrantResponseDto> { http.post(constructRoute("/auth/accounts/$employeeId/reset")) }.map { it.toCredentials() }

    private fun AccountGrantResponseDto.toCredentials() = TemporaryCredentials(employeeId, email, UserRole.fromBackend(role), temporaryPassword)

    private fun EmployeeDto.toDomain() =
        StaffMember(
            id = id,
            name = name,
            lastName = lastName,
            lastNamePending = lastNamePending,
            identityDocument = identityDocument,
            position = position,
            contract = ContractType.entries.firstOrNull { it.name == contractType } ?: ContractType.FULL_TIME,
            startDate = LocalDate.parse(hireDate),
            active = active,
        )

    /** The contract enum names match the API values (`FULL_TIME`, `PART_TIME`, `HOURLY`). */
    private fun ContractType.toApi() = name

    private suspend inline fun <reified T> call(noinline request: suspend () -> HttpResponse): Result<T, StaffError> {
        var response: HttpResponse? = null
        val sent =
            platformSafeCall(execute = request) { received ->
                response = received
                Result.Success(Unit)
            }
        val received =
            when (sent) {
                is Result.Failure -> return Result.Failure(
                    if (sent.error == DataError.Remote.NO_INTERNET ||
                        sent.error == DataError.Remote.REQUEST_TIMEOUT
                    ) {
                        StaffError.NoInternet
                    } else {
                        StaffError.Unknown
                    },
                )
                is Result.Success -> response!!
            }
        if (!received.status.isSuccess()) return Result.Failure(received.toStaffError())
        return runCatching { Result.Success(received.body<T>()) }.getOrElse { Result.Failure(StaffError.Unknown) }
    }

    private suspend fun HttpResponse.toStaffError(): StaffError =
        when (errorBody()?.code) {
            "EMPLOYEE_NOT_FOUND" -> StaffError.NotFound
            "ACCOUNT_NOT_FOUND" -> StaffError.NoAccount
            "INVALID_DOCUMENT" -> StaffError.InvalidDocument
            "DUPLICATE_DOCUMENT" -> StaffError.DuplicateDocument
            "EMAIL_ALREADY_REGISTERED" -> StaffError.EmailTaken
            "ACCOUNT_ALREADY_EXISTS" -> StaffError.AccountExists
            "INCONSISTENT_VALUES" -> StaffError.Incoherent
            "VALIDATION" -> StaffError.Invalid
            "TOO_MANY_REQUESTS" -> StaffError.RateLimited
            else ->
                when (status) {
                    HttpStatusCode.Forbidden -> StaffError.Forbidden
                    HttpStatusCode.NotFound -> StaffError.NotFound
                    HttpStatusCode.TooManyRequests -> StaffError.RateLimited
                    else -> StaffError.Unknown
                }
        }
}
