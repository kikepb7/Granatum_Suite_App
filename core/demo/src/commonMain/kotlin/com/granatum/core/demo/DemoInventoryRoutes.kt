package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/**
 * `/api/materials` and `/api/categories` (ADMIN and MANAGER, as on the server), with the
 * material of backend feature 010: description, dimensions in cm, one quantity, a warehouse/section
 * location and an image the server stores itself and hands out as a temporary signed URL.
 */
class DemoInventoryRoutes(
    private val state: DemoState,
    private val images: DemoStorageRoutes = DemoStorageRoutes(),
) {
    private class Category(
        val id: String,
        var name: String,
        var description: String?,
        val createdAt: Instant,
        var updatedAt: Instant,
    )

    private class Material(
        val id: String,
        var categoryId: String,
        var description: String,
        var height: Double,
        var width: Double,
        var quantity: Int,
        var warehouse: String,
        var section: Int,
        var imagePath: String?,
        val createdAt: Instant,
        var updatedAt: Instant,
    )

    private class Change(
        val id: String,
        val materialId: String,
        val type: String,
        val reason: String,
        val at: Instant,
        val userId: String,
        val before: String?,
        val after: String?,
    )

    private val categories = mutableListOf<Category>()
    private val materials = mutableListOf<Material>()
    private val history = mutableListOf<Change>()

    init {
        val t = state.now()
        val vases = Category(state.newId(), "Jarrones", "Cristal y cerámica", t, t).also { categories += it }
        val ribbons = Category(state.newId(), "Cintas y lazos", null, t, t).also { categories += it }
        val baskets = Category(state.newId(), "Cestas", "Mimbre y fibra", t, t).also { categories += it }
        seed(vases, "Jarrón cilíndrico alto de cristal", 40.0, 12.0, 8, "A", 2, DemoArtwork.Kind.TALL_GLASS_VASE)
        seed(vases, "Jarrón de cerámica blanca", 25.0, 18.0, 0, "A", 5, DemoArtwork.Kind.CERAMIC_VASE)
        seed(ribbons, "Cinta de raso roja", 1.0, 2.5, 30, "B", 4, DemoArtwork.Kind.RIBBON_SPOOL)
        seed(ribbons, "Lazo de arpillera", 1.0, 5.0, 12, "B", 5, DemoArtwork.Kind.BURLAP_BOW)
        seed(baskets, "Cesta de mimbre mediana", 25.0, 35.0, 5, "C", 1, DemoArtwork.Kind.WICKER_BASKET)
        history += Change(state.newId(), materials.first().id, "QUANTITY", "Boda del sábado", t, DemoState.MANAGER_ID, "12", "8")
    }

    private fun seed(
        category: Category,
        description: String,
        height: Double,
        width: Double,
        quantity: Int,
        warehouse: String,
        section: Int,
        artwork: DemoArtwork.Kind,
    ) {
        val t = state.now()
        val id = state.newId()
        val image = images.storeLazily("materiales/$id/seed.png", "image/png") { DemoArtwork.png(artwork) }
        materials += Material(id, category.id, description, height, width, quantity, warehouse, section, image, t, t)
    }

    // The demo's side of backend 011's material stock and material usage ports: what the events routes
    // use to take units out of this inventory and put them back.

    /** Set by [DemoBackend]: whether a material is out at a scheduled event. */
    var materialInUse: (String) -> Boolean = { false }

    /** Every material as the events picker shows it (`AvailableMaterialDto`). */
    fun availableJson(): JsonArray = JsonArray(materials.map { it.stockJson() })

    /** Category, description, location and image of a material, or `null` if it is gone. */
    fun summaryJson(id: String): JsonObject? = materials.firstOrNull { it.id == id }?.stockJson()

    /** How many units a material has, or `null` if it does not exist. */
    fun quantityOf(id: String): Int? = materials.firstOrNull { it.id == id }?.quantity

    /** Takes units out with a history entry; the caller has checked there are enough. */
    fun withdraw(id: String, units: Int, userId: String, reason: String) = move(id, -units, userId, reason)

    /** Puts units back; nothing happens if the material is gone. */
    fun putBack(id: String, units: Int, userId: String, reason: String) = move(id, units, userId, reason)

    private fun move(id: String, delta: Int, userId: String, reason: String) {
        val material = materials.firstOrNull { it.id == id } ?: return
        val before = material.quantity
        material.quantity = before + delta
        material.updatedAt = state.now()
        history += Change(state.newId(), id, "QUANTITY", reason, state.now(), userId, before.toString(), material.quantity.toString())
    }

    private fun Material.stockJson() =
        buildJsonObject {
            put("id", id)
            put("category", categories.first { it.id == categoryId }.name)
            put("description", description)
            put("quantity", quantity)
            put(
                "location",
                buildJsonObject {
                    put("warehouse", warehouse)
                    put("section", section)
                },
            )
            put("imageUrl", imagePath?.let(images::signedUrl))
        }

    /** The storage routes of the demo, which serve the images handed out here. */
    fun handlesStorage(path: String) = images.handles(path)

    fun routeStorage(request: DemoRequest) = images.route(request)

    fun route(request: DemoRequest): DemoReply? {
        if (!request.isManager) return forbidden
        val s = request.segments
        return if (s[0] == "categories") categories(request, s) else materials(request, s)
    }

    private fun categories(
        request: DemoRequest,
        s: List<String>,
    ): DemoReply? = categoryReply(request, s)

    private fun categoryReply(
        request: DemoRequest,
        s: List<String>,
    ): DemoReply? {
        if (s.size == 2) {
            val category = categories.firstOrNull { it.id == s[1] } ?: return notFound("CATEGORY_NOT_FOUND")
            return when (request.method) {
                HttpMethod.Get -> json(category.toJson())
                HttpMethod.Put -> {
                    category.name = request.body.str("name") ?: category.name
                    category.description = request.body.str("description")
                    category.updatedAt = state.now()
                    json(category.toJson())
                }
                HttpMethod.Delete -> {
                    // The real server answers 500 here (unhandled FK); the app never sends it.
                    if (materials.any { it.categoryId == category.id }) {
                        return error(
                            HttpStatusCode.InternalServerError,
                            "INTERNAL_ERROR",
                            "En uso",
                        )
                    }
                    categories -= category
                    DemoReply.NoContent
                }
                else -> null
            }
        }
        return when {
            s.size == 1 && request.method == HttpMethod.Get -> json(JsonArray(categories.map { it.toJson() }))
            s.size == 1 && request.method == HttpMethod.Post -> {
                val t = state.now()
                val category = Category(state.newId(), request.body.str("name").orEmpty(), request.body.str("description"), t, t)
                categories += category
                json(category.toJson(), HttpStatusCode.Created)
            }
            else -> null
        }
    }

    private fun materials(
        request: DemoRequest,
        s: List<String>,
    ): DemoReply? {
        if (s.size == 1) {
            return when (request.method) {
                HttpMethod.Get -> {
                    val warehouse = request.query["warehouse"]
                    val section = request.query["section"]?.toIntOrNull()
                    if (section != null && warehouse == null) return error(HttpStatusCode.BadRequest, "VALIDATION", "La sección exige el almacén")
                    json(
                        JsonArray(
                            materials
                                .filter { (warehouse == null || it.warehouse == warehouse) && (section == null || it.section == section) }
                                .map { it.toJson() },
                        ),
                    )
                }
                HttpMethod.Post -> {
                    invalid(request.body, withQuantity = true)?.let { return it }
                    val categoryId = request.body.str("categoryId").orEmpty()
                    if (categories.none { it.id == categoryId }) return notFound("CATEGORY_NOT_FOUND")
                    val t = state.now()
                    val body = request.body
                    val material =
                        Material(
                            state.newId(),
                            categoryId,
                            body.str("description")!!.trim(),
                            body.dimension("height")!!,
                            body.dimension("width")!!,
                            body.int("quantity")!!,
                            body.location("warehouse")!!,
                            body.location("section")!!.toInt(),
                            null,
                            t,
                            t,
                        )
                    materials += material
                    json(material.toJson(), HttpStatusCode.Created)
                }
                else -> null
            }
        }
        val material = materials.firstOrNull { it.id == s[1] } ?: return notFound("MATERIAL_NOT_FOUND")
        return when {
            s.size == 2 && request.method == HttpMethod.Get -> json(material.toJson())
            s.size == 2 && request.method == HttpMethod.Put -> {
                invalid(request.body, withQuantity = false)?.let { return it }
                val categoryId = request.body.str("categoryId").orEmpty()
                if (categories.none { it.id == categoryId }) return notFound("CATEGORY_NOT_FOUND")
                val body = request.body
                material.categoryId = categoryId
                material.description = body.str("description")!!.trim()
                material.height = body.dimension("height")!!
                material.width = body.dimension("width")!!
                material.warehouse = body.location("warehouse")!!
                material.section = body.location("section")!!.toInt()
                material.updatedAt = state.now()
                json(material.toJson())
            }
            s.size == 2 && request.method == HttpMethod.Delete -> {
                if (materialInUse(material.id)) {
                    return error(HttpStatusCode.Conflict, "MATERIAL_IN_USE", "El material está asignado a un evento programado")
                }
                materials -= material
                material.imagePath?.let(images::remove)
                DemoReply.NoContent
            }
            s.size == 3 && s[2] == "quantity" -> {
                val value = request.body.int("quantity")
                val reason = request.body.str("reason")
                if (value == null || value < 0 || reason.isNullOrBlank()) {
                    return error(HttpStatusCode.BadRequest, "VALIDATION", "La cantidad (>= 0) y el motivo son obligatorios")
                }
                history +=
                    Change(
                        state.newId(),
                        material.id,
                        "QUANTITY",
                        reason,
                        state.now(),
                        request.caller!!.employeeId,
                        material.quantity.toString(),
                        value.toString(),
                    )
                material.quantity = value
                material.updatedAt = state.now()
                json(material.toJson())
            }
            s.size == 3 && s[2] == "image" && request.method == HttpMethod.Put -> {
                val part =
                    DemoDocuments.multipart(request.bytes, request.contentType).firstOrNull { it.name == "image" }
                        ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Falta la imagen")
                if (part.bytes.size > MAX_IMAGE_BYTES) {
                    return error(HttpStatusCode.PayloadTooLarge, "IMAGE_TOO_LARGE", "La imagen supera el tamano maximo de 10 MB")
                }
                val type =
                    DemoDocuments.sniff(part.bytes)?.takeIf { it.startsWith("image/") }
                        ?: return error(HttpStatusCode.UnsupportedMediaType, "INVALID_IMAGE", "La imagen debe ser JPEG, PNG o WebP")
                material.imagePath?.let(images::remove)
                material.imagePath = images.store("materiales/${material.id}/${state.newId()}.${DemoDocuments.extension(type)}", part.bytes, type)
                material.updatedAt = state.now()
                json(material.toJson())
            }
            s.size == 3 && s[2] == "image" && request.method == HttpMethod.Delete -> {
                material.imagePath?.let(images::remove)
                material.imagePath = null
                json(material.toJson())
            }
            s.size == 3 && s[2] == "history" ->
                json(
                    JsonArray(
                        history
                            .filter {
                                it.materialId == material.id
                            }.sortedByDescending { it.at }
                            .map { it.toJson() },
                    ),
                )
            else -> null
        }
    }

    /** The server's bean validation, field by field (docs/openapi.json, backend 010). */
    private fun invalid(
        body: JsonObject,
        withQuantity: Boolean,
    ): DemoReply? {
        val description = body.str("description")
        val ok =
            body.str("categoryId") != null &&
                !description.isNullOrBlank() && description.length <= 500 &&
                (body.dimension("height") ?: 0.0) > 0 && (body.dimension("width") ?: 0.0) > 0 &&
                body.location("warehouse") in listOf("A", "B", "C") &&
                body.location("section")?.toIntOrNull() in 1..5 &&
                (!withQuantity || (body.int("quantity") ?: -1) >= 0)
        return if (ok) null else error(HttpStatusCode.BadRequest, "VALIDATION", "Datos del material no válidos")
    }

    private fun JsonObject.dimension(key: String) = (this["dimensions"] as? JsonObject)?.double(key)

    private fun JsonObject.location(key: String) = (this["location"] as? JsonObject)?.str(key)

    private fun Category.toJson() =
        buildJsonObject {
            put("id", id)
            put("name", name)
            description?.let { put("description", it) }
            put("createdAt", createdAt.toString())
            put("updatedAt", updatedAt.toString())
        }

    private fun Material.toJson() =
        buildJsonObject {
            put("id", id)
            put("category", categories.first { it.id == categoryId }.toJson())
            put("description", description)
            put(
                "dimensions",
                buildJsonObject {
                    put("height", height)
                    put("width", width)
                },
            )
            put("quantity", quantity)
            put(
                "location",
                buildJsonObject {
                    put("warehouse", warehouse)
                    put("section", section)
                },
            )
            put("imageUrl", imagePath?.let(images::signedUrl))
            put("createdAt", createdAt.toString())
            put("updatedAt", updatedAt.toString())
        }

    private fun Change.toJson() =
        buildJsonObject {
            put("id", id)
            put("materialId", materialId)
            put("changeType", type)
            put("reason", reason)
            put("date", at.toString())
            put("userId", userId)
            before?.let { put("previousValue", it) }
            after?.let { put("newValue", it) }
        }

    private companion object {
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
    }
}
