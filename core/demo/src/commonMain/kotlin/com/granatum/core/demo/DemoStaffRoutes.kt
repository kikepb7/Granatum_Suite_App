package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** `/api/employees`, `/api/auth/onboarding` and `/api/auth/accounts` (ADMIN only, as on the server). */
class DemoStaffRoutes(
    private val state: DemoState,
) {
    fun route(request: DemoRequest): DemoReply? {
        if (!request.isAdmin) return forbidden
        val s = request.segments
        return when {
            s.size == 1 && request.method == HttpMethod.Get -> {
                val active = request.query["active"]?.toBooleanStrictOrNull()
                json(JsonArray(state.employees.filter { active == null || it.active == active }.map { it.toJson() }))
            }
            s.size == 2 && request.method == HttpMethod.Get ->
                state.employee(s[1])?.let { json(it.toJson()) }
                    ?: notFound("EMPLOYEE_NOT_FOUND")
            s.size == 2 && request.method == HttpMethod.Put -> {
                val employee = state.employee(s[1]) ?: return notFound("EMPLOYEE_NOT_FOUND")
                if (request.body.str("identityDocument") != null) {
                    return error(HttpStatusCode.UnprocessableEntity, "INCONSISTENT_VALUES", "El documento no se puede cambiar")
                }
                employee.name = request.body.str("name") ?: employee.name
                employee.position = request.body.str("position") ?: employee.position
                employee.contractType = request.body.str("contractType") ?: employee.contractType
                employee.hireDate = request.body.str("hireDate")?.let { LocalDate.parse(it) } ?: employee.hireDate
                json(employee.toJson())
            }
            s.size == 3 && s[2] == "active" && request.method == HttpMethod.Patch -> {
                val employee = state.employee(s[1]) ?: return notFound("EMPLOYEE_NOT_FOUND")
                employee.active = request.body.bool("active") ?: employee.active
                json(employee.toJson())
            }
            else -> null
        }
    }

    /** `/api/auth/onboarding` and `/api/auth/accounts…`, reached through the auth prefix. */
    fun routeAccounts(request: DemoRequest): DemoReply? {
        val s = request.segments
        if (s.size < 2 || (s[1] != "onboarding" && s[1] != "accounts")) return null
        val orphans = s.size == 3 && s[2] == "orphans" && request.method == HttpMethod.Get
        if (request.method != HttpMethod.Post && !orphans) return null
        if (request.caller == null) return error(HttpStatusCode.Unauthorized, "NOT_AUTHENTICATED", "Inicia sesión")
        if (!request.isAdmin) return forbidden
        return when {
            orphans ->
                // Every demo account has its staff record.
                json(buildJsonObject {
                    put("accounts", JsonArray(emptyList()))
                    put("total", 0)
                })
            s.size == 2 && s[1] == "onboarding" -> onboard(request.body)
            s.size == 2 && s[1] == "accounts" -> grant(request.body)
            s.size == 4 && s[3] == "reset" -> {
                val user = state.users.firstOrNull { it.employeeId == s[2] } ?: return notFound("ACCOUNT_NOT_FOUND")
                user.password = temporaryPassword()
                user.passwordChangeRequired = true
                state.revokeAll(user.employeeId)
                json(user.toCredentials())
            }
            else -> null
        }
    }

    private fun onboard(body: JsonObject): DemoReply {
        val document =
            body
                .str("identityDocument")
                ?.uppercase()
                ?.filter { it.isLetterOrDigit() }
                .orEmpty()
        if (!validDocument(document)) return error(HttpStatusCode.UnprocessableEntity, "INVALID_DOCUMENT", "DNI o NIE no válido")
        val email =
            body
                .str("email")
                ?.trim()
                ?.lowercase()
                .orEmpty()
        if (state.users.any { it.email == email }) return error(HttpStatusCode.Conflict, "EMAIL_ALREADY_REGISTERED", "El correo ya tiene cuenta")
        val existing = state.employees.firstOrNull { it.identityDocument == document }
        if (existing != null && state.users.any { it.employeeId == existing.id }) {
            return error(HttpStatusCode.Conflict, "ACCOUNT_ALREADY_EXISTS", "Esa ficha ya tiene cuenta")
        }
        val employee =
            existing ?: DemoEmployee(
                id = state.newId(),
                name = body.str("name").orEmpty(),
                identityDocument = document,
                position = body.str("position").orEmpty(),
                contractType = body.str("contractType") ?: "FULL_TIME",
                hireDate = body.str("hireDate")?.let { LocalDate.parse(it) } ?: state.today(),
            ).also { state.employees += it }
        val user = DemoUser(employee.id, email, temporaryPassword(), body.str("role") ?: "EMPLOYEE", passwordChangeRequired = true)
        state.users += user
        return json(
            buildJsonObject {
                put("employeeId", employee.id)
                put("accountId", state.newId())
                put("email", user.email)
                put("role", user.role)
                put("staffRecordCreated", existing == null)
                put("temporaryPassword", user.password)
            },
            HttpStatusCode.Created,
        )
    }

    private fun grant(body: JsonObject): DemoReply {
        val employeeId = body.str("employeeId").orEmpty()
        state.employee(employeeId) ?: return notFound("EMPLOYEE_NOT_FOUND")
        if (state.users.any { it.employeeId == employeeId }) return error(HttpStatusCode.Conflict, "ACCOUNT_ALREADY_EXISTS", "Ya tiene cuenta")
        val email =
            body
                .str("email")
                ?.trim()
                ?.lowercase()
                .orEmpty()
        if (state.users.any { it.email == email }) return error(HttpStatusCode.Conflict, "EMAIL_ALREADY_REGISTERED", "El correo ya tiene cuenta")
        val user = DemoUser(employeeId, email, temporaryPassword(), body.str("role") ?: "EMPLOYEE", passwordChangeRequired = true)
        state.users += user
        return json(user.toCredentials(), HttpStatusCode.Created)
    }

    private fun DemoUser.toCredentials() =
        buildJsonObject {
            put("accountId", state.newId())
            put("employeeId", employeeId)
            put("email", email)
            put("role", role)
            put("temporaryPassword", password)
        }

    private fun DemoEmployee.toJson() =
        buildJsonObject {
            put("id", id)
            put("name", name)
            put("identityDocument", identityDocument)
            put("position", position)
            put("contractType", contractType)
            put("hireDate", hireDate.toString())
            put("active", active)
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
