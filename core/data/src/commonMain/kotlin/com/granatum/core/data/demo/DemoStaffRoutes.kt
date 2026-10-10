package com.granatum.core.data.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** `/api/empleados`, `/api/auth/altas` and `/api/auth/cuentas` (ADMIN only, as on the server). */
class DemoStaffRoutes(
    private val state: DemoState,
) {
    fun route(request: DemoRequest): DemoReply? {
        if (!request.isAdmin) return forbidden
        val s = request.segments
        return when {
            s.size == 1 && request.method == HttpMethod.Get -> {
                val active = request.query["activo"]?.toBooleanStrictOrNull()
                json(JsonArray(state.employees.filter { active == null || it.active == active }.map { it.toJson() }))
            }
            s.size == 2 && request.method == HttpMethod.Get ->
                state.employee(s[1])?.let { json(it.toJson()) }
                    ?: notFound("EMPLEADO_NOT_FOUND")
            s.size == 2 && request.method == HttpMethod.Put -> {
                val employee = state.employee(s[1]) ?: return notFound("EMPLEADO_NOT_FOUND")
                if (request.body.str("documentoIdentidad") != null) {
                    return error(HttpStatusCode.UnprocessableEntity, "VALORES_INCOHERENTES", "El documento no se puede cambiar")
                }
                employee.name = request.body.str("nombre") ?: employee.name
                employee.position = request.body.str("puesto") ?: employee.position
                employee.contract = request.body.str("tipoContrato") ?: employee.contract
                employee.startDate = request.body.str("fechaAlta")?.let { LocalDate.parse(it) } ?: employee.startDate
                json(employee.toJson())
            }
            s.size == 3 && s[2] == "activo" && request.method == HttpMethod.Patch -> {
                val employee = state.employee(s[1]) ?: return notFound("EMPLEADO_NOT_FOUND")
                employee.active = request.body.bool("activo") ?: employee.active
                json(employee.toJson())
            }
            else -> null
        }
    }

    /** `/api/auth/altas` and `/api/auth/cuentas…`, reached through the auth prefix. */
    fun routeAccounts(request: DemoRequest): DemoReply? {
        val s = request.segments
        if (s.size < 2 || (s[1] != "altas" && s[1] != "cuentas") || request.method != HttpMethod.Post) return null
        if (request.caller == null) return error(HttpStatusCode.Unauthorized, "NO_AUTENTICADO", "Inicia sesión")
        if (!request.isAdmin) return forbidden
        return when {
            s.size == 2 && s[1] == "altas" -> onboard(request.body)
            s.size == 2 && s[1] == "cuentas" -> grant(request.body)
            s.size == 4 && s[3] == "restablecer" -> {
                val user = state.users.firstOrNull { it.employeeId == s[2] } ?: return notFound("CUENTA_NO_ENCONTRADA")
                user.password = temporaryPassword()
                user.mustChangePassword = true
                state.revokeAll(user.employeeId)
                json(user.toCredentials())
            }
            else -> null
        }
    }

    private fun onboard(body: JsonObject): DemoReply {
        val document =
            body
                .str("documentoIdentidad")
                ?.uppercase()
                ?.filter { it.isLetterOrDigit() }
                .orEmpty()
        if (!validDocument(document)) return error(HttpStatusCode.UnprocessableEntity, "DOCUMENTO_INVALIDO", "DNI o NIE no válido")
        val email =
            body
                .str("email")
                ?.trim()
                ?.lowercase()
                .orEmpty()
        if (state.users.any { it.email == email }) return error(HttpStatusCode.Conflict, "EMAIL_YA_REGISTRADO", "El correo ya tiene cuenta")
        val existing = state.employees.firstOrNull { it.document == document }
        if (existing != null && state.users.any { it.employeeId == existing.id }) {
            return error(HttpStatusCode.Conflict, "CUENTA_YA_EXISTE", "Esa ficha ya tiene cuenta")
        }
        val employee =
            existing ?: DemoEmployee(
                id = state.newId(),
                name = body.str("nombre").orEmpty(),
                document = document,
                position = body.str("puesto").orEmpty(),
                contract = body.str("tipoContrato") ?: "JORNADA_COMPLETA",
                startDate = body.str("fechaAlta")?.let { LocalDate.parse(it) } ?: state.today(),
            ).also { state.employees += it }
        val user = DemoUser(employee.id, email, temporaryPassword(), body.str("rol") ?: "EMPLEADO", mustChangePassword = true)
        state.users += user
        return json(
            buildJsonObject {
                put("empleadoId", employee.id)
                put("cuentaId", state.newId())
                put("email", user.email)
                put("rol", user.role)
                put("fichaCreada", existing == null)
                put("passwordTemporal", user.password)
            },
            HttpStatusCode.Created,
        )
    }

    private fun grant(body: JsonObject): DemoReply {
        val employeeId = body.str("empleadoId").orEmpty()
        state.employee(employeeId) ?: return notFound("EMPLEADO_NO_ENCONTRADO")
        if (state.users.any { it.employeeId == employeeId }) return error(HttpStatusCode.Conflict, "CUENTA_YA_EXISTE", "Ya tiene cuenta")
        val email =
            body
                .str("email")
                ?.trim()
                ?.lowercase()
                .orEmpty()
        if (state.users.any { it.email == email }) return error(HttpStatusCode.Conflict, "EMAIL_YA_REGISTRADO", "El correo ya tiene cuenta")
        val user = DemoUser(employeeId, email, temporaryPassword(), body.str("rol") ?: "EMPLEADO", mustChangePassword = true)
        state.users += user
        return json(user.toCredentials(), HttpStatusCode.Created)
    }

    private fun DemoUser.toCredentials() =
        buildJsonObject {
            put("cuentaId", state.newId())
            put("empleadoId", employeeId)
            put("email", email)
            put("rol", role)
            put("passwordTemporal", password)
        }

    private fun DemoEmployee.toJson() =
        buildJsonObject {
            put("id", id)
            put("nombre", name)
            put("documentoIdentidad", document)
            put("puesto", position)
            put("tipoContrato", contract)
            put("fechaAlta", startDate.toString())
            put("activo", active)
        }

    /** Like the server's: 16 characters, unambiguous, with symbols. */
    private fun temporaryPassword(): String {
        val letters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
        return (1..13).map { letters.random() }.joinToString("") + "#7k"
    }

    /** DNI or NIE with its check letter, as the server validates them. */
    private fun validDocument(document: String): Boolean {
        val letters = "TRWAGMYFPDXBNJZSQVHLCKE"
        val normalised =
            when (document.firstOrNull()) {
                'X' -> "0" + document.drop(1)
                'Y' -> "1" + document.drop(1)
                'Z' -> "2" + document.drop(1)
                else -> document
            }
        if (normalised.length != 9) return false
        val number = normalised.take(8).toIntOrNull() ?: return false
        return letters[number % 23] == normalised.last()
    }
}
