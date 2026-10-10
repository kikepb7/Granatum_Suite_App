package com.granatum.core.data.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** `/api/auth/login|refresh|logout|change-password|registro`. */
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
                    ?.let { tokens(it) } ?: error(HttpStatusCode.Unauthorized, "TOKEN_RENOVACION_INVALIDO", "Sesión caducada")
            "logout" -> DemoReply.NoContent
            "change-password" -> changePassword(request)
            // The demo installation already has its owner: exactly what the server says then.
            "registro" -> error(HttpStatusCode.Forbidden, "CODIGO_ARRANQUE_INVALIDO", "Código de arranque no válido")
            else -> null
        }
    }

    private fun login(body: JsonObject): DemoReply {
        val email = body.str("email")?.trim()?.lowercase()
        val user = state.users.firstOrNull { it.email == email && it.password == body.str("password") }
        val active = user?.let { state.employee(it.employeeId)?.active } == true
        return if (user == null || !active) {
            error(HttpStatusCode.Unauthorized, "CREDENCIALES_INVALIDAS", "Credenciales no válidas")
        } else {
            tokens(user)
        }
    }

    private fun changePassword(request: DemoRequest): DemoReply {
        val user = request.caller ?: return error(HttpStatusCode.Unauthorized, "NO_AUTENTICADO", "Inicia sesión")
        if (request.body.str("passwordActual") != user.password) {
            return error(HttpStatusCode.Unauthorized, "CREDENCIALES_INVALIDAS", "La contraseña actual no es correcta")
        }
        val new = request.body.str("passwordNueva").orEmpty()
        val missing =
            buildList {
                if (new.length < 8) add("LONGITUD_MINIMA")
                if (new.none { it.isUpperCase() }) add("FALTA_MAYUSCULA")
                if (new.none { it.isLowerCase() }) add("FALTA_MINUSCULA")
                if (new.none { it.isDigit() }) add("FALTA_DIGITO")
                if (new.all { it.isLetterOrDigit() }) add("FALTA_SIMBOLO")
            }
        if (missing.isNotEmpty()) {
            return json(
                buildJsonObject {
                    put("code", "PASSWORD_DEBIL")
                    put("message", "La contraseña no cumple la política")
                    put("requisitos", buildJsonArray { missing.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                },
                HttpStatusCode.UnprocessableEntity,
            )
        }
        user.password = new
        user.mustChangePassword = false
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
                put("requiereCambioPassword", user.mustChangePassword)
            },
        )
    }
}
