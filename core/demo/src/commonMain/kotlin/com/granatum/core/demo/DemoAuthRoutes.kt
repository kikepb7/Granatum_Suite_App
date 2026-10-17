package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** `/api/auth/login|refresh|logout|change-password|sign-up`. */
class DemoAuthRoutes(
    private val state: DemoState,
) {
    fun route(request: DemoRequest): DemoReply? {
        if (request.method != HttpMethod.Post || request.segments.size != 2) return null
        return when (request.segments[1]) {
            "login" -> login(request.body)
            "refresh" ->
                state
                    .userForRefresh(request.body.str("refreshToken").orEmpty())
                    ?.let { tokens(it) } ?: error(HttpStatusCode.Unauthorized, "INVALID_REFRESH_TOKEN", "Sesión caducada")
            "logout" -> DemoReply.NoContent
            "change-password" -> changePassword(request)
            // The demo installation already has its owner: exactly what the server says then.
            "sign-up" -> error(HttpStatusCode.Forbidden, "INVALID_BOOTSTRAP_CODE", "Código de arranque no válido")
            else -> null
        }
    }

    private fun login(body: JsonObject): DemoReply {
        val email = body.str("email")?.trim()?.lowercase()
        val user = state.users.firstOrNull { it.email == email && it.password == body.str("password") }
        val active = user?.let { state.employee(it.employeeId)?.active } == true
        return if (user == null || !active) {
            error(HttpStatusCode.Unauthorized, "INVALID_CREDENTIALS", "Credenciales no válidas")
        } else {
            tokens(user)
        }
    }

    private fun changePassword(request: DemoRequest): DemoReply {
        val user = request.caller ?: return error(HttpStatusCode.Unauthorized, "NOT_AUTHENTICATED", "Inicia sesión")
        if (request.body.str("currentPassword") != user.password) {
            return error(HttpStatusCode.Unauthorized, "INVALID_CREDENTIALS", "La contraseña actual no es correcta")
        }
        val new = request.body.str("newPassword").orEmpty()
        val missing =
            buildList {
                if (new.length < 8) add("MIN_LENGTH")
                if (new.none { it.isUpperCase() }) add("MISSING_UPPERCASE")
                if (new.none { it.isLowerCase() }) add("MISSING_LOWERCASE")
                if (new.none { it.isDigit() }) add("MISSING_DIGIT")
                if (new.all { it.isLetterOrDigit() }) add("MISSING_SYMBOL")
            }
        if (missing.isNotEmpty()) {
            return json(
                buildJsonObject {
                    put("code", "WEAK_PASSWORD")
                    put("message", "La contraseña no cumple la política")
                    put("requirements", buildJsonArray { missing.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                },
                HttpStatusCode.UnprocessableEntity,
            )
        }
        user.password = new
        user.passwordChangeRequired = false
        state.revokeAll(user.employeeId)
        return tokens(user)
    }

    private fun tokens(user: DemoUser): DemoReply {
        val (access, refresh) = state.issueTokens(user)
        return json(
            buildJsonObject {
                put("accessToken", access)
                put("refreshToken", refresh)
                put("expiresIn", 86_400)
                put("passwordChangeRequired", user.passwordChangeRequired)
            },
        )
    }
}
