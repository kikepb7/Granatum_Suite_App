package com.granatum.core.demo

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** One request as the demo routes see it. */
class DemoRequest(
    val method: HttpMethod,
    /** Path after `/api`, e.g. `/materials/123`; the whole path for the storage, which has no `/api`. */
    val path: String,
    val query: Map<String, String>,
    val caller: DemoUser?,
    val bytes: ByteArray,
    val contentType: String?,
) {
    val segments: List<String> = path.trim('/').split('/')
    val body: JsonObject by lazy {
        runCatching { Json.parseToJsonElement(bytes.decodeToString()).jsonObject }.getOrDefault(JsonObject(emptyMap()))
    }
    val isAdmin: Boolean get() = caller?.role == "ADMIN"
    val isManager: Boolean get() = caller?.role == "ADMIN" || caller?.role == "MANAGER"
}

/**
 * An in-memory imitation of the Granatum API (backend 93ef398, English contract) for the demo build: same routes,
 * field names, status codes and error codes as docs/openapi.json and the backend's contracts, so
 * every screen works as it would against the server. Seeded with a small flower shop; changes live
 * until the app closes. The team-attendance screen still calls routes the real backend does not
 * have (`/attendance/…`), and here too they answer 404.
 */
class DemoBackend(
    private val state: DemoState = DemoState(),
) {
    private val mutex = Mutex()
    private val auth = DemoAuthRoutes(state)
    private val staff = DemoStaffRoutes(state)
    private val time = DemoTimeTrackingRoutes(state)
    private val inventory = DemoInventoryRoutes(state)
    private val invoicing = DemoInvoicingRoutes(state)
    private val events = DemoEventsRoutes(state, inventory)

    suspend fun handle(
        scope: MockRequestHandleScope,
        data: HttpRequestData,
    ): HttpResponseData {
        // A touch of latency, so loading states can be seen as they are against a server.
        delay(LATENCY_MILLIS)
        val token = data.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
        val request =
            DemoRequest(
                method = data.method,
                path = data.url.encodedPath.substringAfter("/api"),
                query =
                    data.url.parameters
                        .entries()
                        .associate { (key, values) -> key to values.first() },
                caller = token?.let { state.userForToken(it) },
                bytes = data.body.toByteArray(),
                contentType = data.body.contentType?.toString(),
            )
        val reply = mutex.withLock { route(request) }
        return scope.toResponse(reply)
    }

    private fun route(request: DemoRequest): DemoReply {
        // Signed image URLs carry their own token, not the session (backend 010).
        if (inventory.handlesStorage(request.path)) return inventory.routeStorage(request)
        val first = request.segments.firstOrNull()
        if (first == "auth") {
            return auth.route(request) ?: staff.routeAccounts(request) ?: notFound("RESOURCE_NOT_FOUND")
        }
        val caller = request.caller ?: return error(HttpStatusCode.Unauthorized, "NOT_AUTHENTICATED", "Inicia sesión")
        if (caller.passwordChangeRequired) return forbidden
        return when (first) {
            "employees" -> staff.route(request)
            "shifts", "corrections" -> time.route(request)
            "materials", "categories" -> inventory.route(request)
            "invoicing" -> invoicing.route(request)
            "events" -> events.route(request)
            else -> null
        } ?: notFound("RESOURCE_NOT_FOUND")
    }

    private companion object {
        const val LATENCY_MILLIS = 250L
    }
}
