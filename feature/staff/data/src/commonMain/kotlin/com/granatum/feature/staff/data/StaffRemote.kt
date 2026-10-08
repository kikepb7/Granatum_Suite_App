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

/** `/api/empleados`, `/api/auth/altas` and `/api/auth/cuentas` (backend 2ec33d0), errors per research D9. */
class KtorStaffRepository(
    private val http: HttpClient,
) : StaffRepository {
    override suspend fun members() =
        call<List<EmpleadoDto>> { http.get(constructRoute("/empleados")) }.map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun member(id: String) = call<EmpleadoDto> { http.get(constructRoute("/empleados/$id")) }.map { it.toDomain() }

    override suspend fun update(
        id: String,
        edit: StaffEdit,
    ) = call<EmpleadoDto> {
        http.put(constructRoute("/empleados/$id")) {
            setBody(ActualizarEmpleadoRequestDto(edit.name.trim(), edit.position.trim(), edit.contract.toApi(), edit.startDate.toString()))
        }
    }.map { it.toDomain() }

    override suspend fun setActive(
        id: String,
        active: Boolean,
    ) = call<EmpleadoDto> {
        http.patch(
            constructRoute("/empleados/$id/activo"),
        ) { setBody(CambiarActivoRequestDto(active)) }
    }.map { it.toDomain() }

    override suspend fun onboard(onboarding: Onboarding) =
        call<AltaPersonaResponseDto> {
            http.post(constructRoute("/auth/altas")) {
                setBody(
                    AltaPersonaRequestDto(
                        nombre = onboarding.name.trim(),
                        documentoIdentidad = onboarding.identityDocument.trim(),
                        puesto = onboarding.position.trim(),
                        tipoContrato = onboarding.contract.toApi(),
                        fechaAlta = onboarding.startDate.toString(),
                        email = onboarding.email.trim().lowercase(),
                        rol = onboarding.role.name,
                    ),
                )
            }
        }.map {
            TemporaryCredentials(
                it.empleadoId,
                it.email,
                UserRole.fromBackend(it.rol),
                it.passwordTemporal,
                recordCreated = it.fichaCreada,
            )
        }

    override suspend fun grantAccess(
        employeeId: String,
        email: String,
        role: UserRole,
    ) = call<AltaCuentaResponseDto> {
        http.post(constructRoute("/auth/cuentas")) { setBody(AltaCuentaRequestDto(employeeId, email.trim().lowercase(), role.name)) }
    }.map { it.toCredentials() }

    override suspend fun resetPassword(employeeId: String) =
        call<AltaCuentaResponseDto> { http.post(constructRoute("/auth/cuentas/$employeeId/restablecer")) }.map { it.toCredentials() }

    private fun AltaCuentaResponseDto.toCredentials() = TemporaryCredentials(empleadoId, email, UserRole.fromBackend(rol), passwordTemporal)

    private fun EmpleadoDto.toDomain() =
        StaffMember(
            id = id,
            name = nombre,
            identityDocument = documentoIdentidad,
            position = puesto,
            contract =
                when (tipoContrato) {
                    "PARCIAL" -> ContractType.PART_TIME
                    "POR_HORAS" -> ContractType.HOURLY
                    else -> ContractType.FULL_TIME
                },
            startDate = LocalDate.parse(fechaAlta),
            active = activo,
        )

    private fun ContractType.toApi() =
        when (this) {
            ContractType.FULL_TIME -> "JORNADA_COMPLETA"
            ContractType.PART_TIME -> "PARCIAL"
            ContractType.HOURLY -> "POR_HORAS"
        }

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
            "EMPLEADO_NOT_FOUND", "EMPLEADO_NO_ENCONTRADO" -> StaffError.NotFound
            "CUENTA_NO_ENCONTRADA" -> StaffError.NoAccount
            "DOCUMENTO_INVALIDO" -> StaffError.InvalidDocument
            "DOCUMENTO_DUPLICADO" -> StaffError.DuplicateDocument
            "EMAIL_YA_REGISTRADO" -> StaffError.EmailTaken
            "CUENTA_YA_EXISTE" -> StaffError.AccountExists
            "VALORES_INCOHERENTES" -> StaffError.Incoherent
            "VALIDACION" -> StaffError.Invalid
            "DEMASIADAS_PETICIONES" -> StaffError.RateLimited
            else ->
                when (status) {
                    HttpStatusCode.Forbidden -> StaffError.Forbidden
                    HttpStatusCode.NotFound -> StaffError.NotFound
                    HttpStatusCode.TooManyRequests -> StaffError.RateLimited
                    else -> StaffError.Unknown
                }
        }
}
