package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The object storage of the Demo environment, imitating Supabase Storage's REST API: uploads to
 * `/storage/v1/object/{bucket}/{path}` and public reads from `/storage/v1/object/public/{bucket}/{path}`.
 * Images live in memory, like everything else in the demo.
 */
internal class DemoStorageRoutes {
    private class StoredObject(val bytes: ByteArray, val contentType: String)

    private val objects = mutableMapOf<String, StoredObject>()

    fun handles(path: String): Boolean = path.startsWith(PREFIX)

    fun route(request: DemoRequest): DemoReply {
        val rest = request.path.removePrefix(PREFIX)
        return when {
            request.method == HttpMethod.Get && rest.startsWith(PUBLIC) -> {
                val key = rest.removePrefix(PUBLIC)
                val stored = objects[key] ?: return error(HttpStatusCode.NotFound, "not_found", "Object not found")
                DemoReply.Bytes(stored.bytes, stored.contentType, key.substringAfterLast('/'))
            }
            request.method == HttpMethod.Post && rest.isNotEmpty() -> {
                if (objects.containsKey(rest)) return error(HttpStatusCode.Conflict, "Duplicate", "The resource already exists")
                if (request.bytes.size > MAX_BYTES) return error(HttpStatusCode.PayloadTooLarge, "Payload too large", "The object exceeded the maximum allowed size")
                objects[rest] = StoredObject(request.bytes, request.contentType ?: "application/octet-stream")
                json(buildJsonObject { put("Key", rest) })
            }
            else -> notFound("not_found")
        }
    }

    private companion object {
        const val PREFIX = "/storage/v1/object/"
        const val PUBLIC = "public/"
        const val MAX_BYTES = 10 * 1024 * 1024
    }
}
