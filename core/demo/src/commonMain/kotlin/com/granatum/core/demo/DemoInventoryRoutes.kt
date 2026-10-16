package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/**
 * `/api/materiales` and `/api/categorias` (ADMIN and ENCARGADO, as on the server), with the
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
        seed(vases, "Jarrón cilíndrico alto de cristal", 40.0, 12.0, 8, "A", 2)
        seed(vases, "Jarrón de cerámica blanca", 25.0, 18.0, 0, "A", 5)
        seed(ribbons, "Cinta de raso roja", 1.0, 2.5, 30, "B", 4)
        seed(ribbons, "Lazo de arpillera", 1.0, 5.0, 12, "B", 5)
        seed(baskets, "Cesta de mimbre mediana", 25.0, 35.0, 5, "C", 1)
        history += Change(state.newId(), materials.first().id, "CANTIDAD", "Boda del sábado", t, DemoState.MANAGER_ID, "12", "8")
    }

    private fun seed(
        category: Category,
        description: String,
        height: Double,
        width: Double,
        quantity: Int,
        warehouse: String,
        section: Int,
    ) {
        val t = state.now()
        materials += Material(state.newId(), category.id, description, height, width, quantity, warehouse, section, null, t, t)
    }

    /** The storage routes of the demo, which serve the images handed out here. */
    fun handlesStorage(path: String) = images.handles(path)

    fun routeStorage(request: DemoRequest) = images.route(request)

    fun route(request: DemoRequest): DemoReply? {
        if (!request.isManager) return forbidden
        val s = request.segments
        return if (s[0] == "categorias") categories(request, s) else materials(request, s)
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
            val category = categories.firstOrNull { it.id == s[1] } ?: return notFound("CATEGORIA_NOT_FOUND")
            return when (request.method) {
                HttpMethod.Get -> json(category.toJson())
                HttpMethod.Put -> {
                    category.name = request.body.str("nombre") ?: category.name
                    category.description = request.body.str("descripcion")
                    category.updatedAt = state.now()
                    json(category.toJson())
                }
                HttpMethod.Delete -> {
                    // The real server answers 500 here (unhandled FK); the app never sends it.
                    if (materials.any { it.categoryId == category.id }) {
                        return error(
                            HttpStatusCode.InternalServerError,
                            "ERROR_INTERNO",
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
                val category = Category(state.newId(), request.body.str("nombre").orEmpty(), request.body.str("descripcion"), t, t)
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
                    val warehouse = request.query["almacen"]
                    val section = request.query["seccion"]?.toIntOrNull()
                    if (section != null && warehouse == null) return error(HttpStatusCode.BadRequest, "VALIDACION", "seccion exige almacen")
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
                    val categoryId = request.body.str("categoriaId").orEmpty()
                    if (categories.none { it.id == categoryId }) return notFound("CATEGORIA_NOT_FOUND")
                    val t = state.now()
                    val body = request.body
                    val material =
                        Material(
                            state.newId(),
                            categoryId,
                            body.str("descripcion")!!.trim(),
                            body.dimension("alto")!!,
                            body.dimension("ancho")!!,
                            body.int("cantidad")!!,
                            body.location("almacen")!!,
                            body.location("seccion")!!.toInt(),
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
                val categoryId = request.body.str("categoriaId").orEmpty()
                if (categories.none { it.id == categoryId }) return notFound("CATEGORIA_NOT_FOUND")
                val body = request.body
                material.categoryId = categoryId
                material.description = body.str("descripcion")!!.trim()
                material.height = body.dimension("alto")!!
                material.width = body.dimension("ancho")!!
                material.warehouse = body.location("almacen")!!
                material.section = body.location("seccion")!!.toInt()
                material.updatedAt = state.now()
                json(material.toJson())
            }
            s.size == 2 && request.method == HttpMethod.Delete -> {
                materials -= material
                material.imagePath?.let(images::remove)
                DemoReply.NoContent
            }
            s.size == 3 && s[2] == "cantidad" -> {
                val value = request.body.int("cantidad")
                val reason = request.body.str("motivo")
                if (value == null || value < 0 || reason.isNullOrBlank()) {
                    return error(HttpStatusCode.BadRequest, "VALIDACION", "cantidad >= 0 y motivo obligatorios")
                }
                history +=
                    Change(
                        state.newId(),
                        material.id,
                        "CANTIDAD",
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
            s.size == 3 && s[2] == "imagen" && request.method == HttpMethod.Put -> {
                val part =
                    DemoDocuments.multipart(request.bytes, request.contentType).firstOrNull()
                        ?: return error(HttpStatusCode.BadRequest, "VALIDACION", "Falta la parte imagen")
                if (part.bytes.size > MAX_IMAGE_BYTES) {
                    return error(HttpStatusCode.PayloadTooLarge, "IMAGEN_DEMASIADO_GRANDE", "La imagen supera el tamano maximo de 10 MB")
                }
                val type =
                    DemoDocuments.sniff(part.bytes)?.takeIf { it.startsWith("image/") }
                        ?: return error(HttpStatusCode.UnsupportedMediaType, "IMAGEN_NO_VALIDA", "La imagen debe ser JPEG, PNG o WebP")
                material.imagePath?.let(images::remove)
                material.imagePath = images.store("materiales/${material.id}/${state.newId()}.${DemoDocuments.extension(type)}", part.bytes, type)
                material.updatedAt = state.now()
                json(material.toJson())
            }
            s.size == 3 && s[2] == "imagen" && request.method == HttpMethod.Delete -> {
                material.imagePath?.let(images::remove)
                material.imagePath = null
                json(material.toJson())
            }
            s.size == 3 && s[2] == "historial" ->
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
        val description = body.str("descripcion")
        val ok =
            body.str("categoriaId") != null &&
                !description.isNullOrBlank() && description.length <= 500 &&
                (body.dimension("alto") ?: 0.0) > 0 && (body.dimension("ancho") ?: 0.0) > 0 &&
                body.location("almacen") in listOf("A", "B", "C") &&
                body.location("seccion")?.toIntOrNull() in 1..5 &&
                (!withQuantity || (body.int("cantidad") ?: -1) >= 0)
        return if (ok) null else error(HttpStatusCode.BadRequest, "VALIDACION", "Datos del material no validos")
    }

    private fun JsonObject.dimension(key: String) = (this["dimensiones"] as? JsonObject)?.double(key)

    private fun JsonObject.location(key: String) = (this["localizacion"] as? JsonObject)?.str(key)

    private fun Category.toJson() =
        buildJsonObject {
            put("id", id)
            put("nombre", name)
            description?.let { put("descripcion", it) }
            put("createdAt", createdAt.toString())
            put("updatedAt", updatedAt.toString())
        }

    private fun Material.toJson() =
        buildJsonObject {
            put("id", id)
            put("categoria", categories.first { it.id == categoryId }.toJson())
            put("descripcion", description)
            put(
                "dimensiones",
                buildJsonObject {
                    put("alto", height)
                    put("ancho", width)
                },
            )
            put("cantidad", quantity)
            put(
                "localizacion",
                buildJsonObject {
                    put("almacen", warehouse)
                    put("seccion", section)
                },
            )
            put("imagenUrl", imagePath?.let(images::signedUrl))
            put("fechaAlta", createdAt.toString())
            put("fechaUltimaModificacion", updatedAt.toString())
        }

    private fun Change.toJson() =
        buildJsonObject {
            put("id", id)
            put("materialId", materialId)
            put("tipoCambio", type)
            put("motivo", reason)
            put("fecha", at.toString())
            put("usuarioId", userId)
            before?.let { put("valorAnterior", it) }
            after?.let { put("valorNuevo", it) }
        }

    private companion object {
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
    }
}
