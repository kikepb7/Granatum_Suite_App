package com.granatum.core.data.demo

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** What a demo route answers; turned into a mock response by [DemoBackend]. */
sealed interface DemoReply {
    data class Json(
        val body: JsonElement,
        val status: HttpStatusCode = HttpStatusCode.OK,
    ) : DemoReply

    data class Bytes(
        val body: ByteArray,
        val contentType: String,
        val fileName: String,
    ) : DemoReply

    data class Error(
        val status: HttpStatusCode,
        val code: String,
        val message: String,
    ) : DemoReply

    data object NoContent : DemoReply

    data object Accepted : DemoReply
}

fun json(
    body: JsonElement,
    status: HttpStatusCode = HttpStatusCode.OK,
) = DemoReply.Json(body, status)

fun error(
    status: HttpStatusCode,
    code: String,
    message: String,
) = DemoReply.Error(status, code, message)

fun notFound(code: String) = error(HttpStatusCode.NotFound, code, "No existe")

val forbidden = error(HttpStatusCode.Forbidden, "FORBIDDEN", "El rol no permite esta operación")

internal fun MockRequestHandleScope.toResponse(reply: DemoReply): HttpResponseData =
    when (reply) {
        is DemoReply.Json -> respond(reply.body.toString(), reply.status, headersOf(HttpHeaders.ContentType, "application/json"))
        is DemoReply.Error ->
            respond(
                buildJsonObject {
                    put("code", reply.code)
                    put("message", reply.message)
                }.toString(),
                reply.status,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        is DemoReply.Bytes ->
            respond(
                reply.body,
                HttpStatusCode.OK,
                headersOf(
                    HttpHeaders.ContentType to listOf(reply.contentType),
                    HttpHeaders.ContentDisposition to listOf("attachment; filename=\"${reply.fileName}\""),
                ),
            )
        DemoReply.NoContent -> respond(ByteArray(0), HttpStatusCode.NoContent)
        DemoReply.Accepted -> respond(ByteArray(0), HttpStatusCode.Accepted)
    }

internal fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

internal fun JsonObject.int(key: String): Int? = str(key)?.toIntOrNull()

internal fun JsonObject.double(key: String): Double? = str(key)?.toDoubleOrNull()

internal fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()

internal fun JsonElement?.textOrNull(): String? =
    (this as? JsonPrimitive)
        ?.takeUnless {
            it.toString() == "null"
        }?.jsonPrimitive
        ?.contentOrNull
