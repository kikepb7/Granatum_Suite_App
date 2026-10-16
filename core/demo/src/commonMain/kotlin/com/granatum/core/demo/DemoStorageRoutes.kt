package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

/**
 * The private image bucket of the Demo environment. The demo backend stores what is uploaded to
 * `PUT /materiales/{id}/imagen` here and hands out addresses in the shape of Supabase Storage's
 * signed URLs, which the app loads like any other (backend 010, research D-005).
 */
class DemoStorageRoutes {
    private class StoredObject(
        val bytes: ByteArray,
        val contentType: String,
    )

    private val objects = mutableMapOf<String, StoredObject>()

    fun store(
        path: String,
        bytes: ByteArray,
        contentType: String,
    ): String {
        objects[path] = StoredObject(bytes, contentType)
        return path
    }

    fun remove(path: String) {
        objects -= path
    }

    fun signedUrl(path: String) = "$BASE_URL$PREFIX$SIGN$path?token=demo"

    fun handles(path: String): Boolean = path.startsWith(PREFIX)

    fun route(request: DemoRequest): DemoReply {
        val rest = request.path.removePrefix(PREFIX)
        if (request.method != HttpMethod.Get || !rest.startsWith(SIGN)) return notFound("not_found")
        val stored = objects[rest.removePrefix(SIGN)] ?: return error(HttpStatusCode.NotFound, "not_found", "Object not found")
        return DemoReply.Bytes(stored.bytes, stored.contentType, rest.substringAfterLast('/'))
    }

    companion object {
        /** Never resolved: in the Demo environment every request goes to the demo engine. */
        const val BASE_URL = "https://demo.granatum.invalid"
        private const val PREFIX = "/storage/v1/object/"
        private const val SIGN = "sign/"
    }
}
