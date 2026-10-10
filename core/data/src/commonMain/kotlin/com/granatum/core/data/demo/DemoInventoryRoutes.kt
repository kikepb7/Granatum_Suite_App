package com.granatum.core.data.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/** `/api/materiales` and `/api/categorias` (ADMIN and ENCARGADO, as on the server). */
class DemoInventoryRoutes(
    private val state: DemoState,
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
        var data: JsonObject,
        var categoryId: String,
        var available: Int,
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
        seed(
            "Jarrón cilíndrico alto",
            vases,
            8,
            12,
            "Transparente",
            "Cristal",
            "NUEVO",
            "Almacén A · estante 2",
            14.5,
            "Vidrios del Sur",
            40.0,
            12.0,
        )
        seed(
            "Jarrón de cerámica blanca",
            vases,
            0,
            6,
            "Blanco",
            "Cerámica",
            "USADO",
            "Tienda · escaparate",
            22.0,
            "Cerámicas Levante",
            25.0,
            18.0,
        )
        seed("Cinta de raso roja", ribbons, 30, 40, "Rojo", "Raso", "NUEVO", "Almacén B · cajón 4", 3.2, "Mercería Granada", 1.0, 2.5)
        seed(
            "Lazo de arpillera",
            ribbons,
            12,
            20,
            "Natural",
            "Arpillera",
            "NUEVO",
            "Almacén B · cajón 5",
            1.8,
            "Mercería Granada",
            1.0,
            5.0,
        )
        seed(
            "Cesta de mimbre mediana",
            baskets,
            5,
            10,
            "Natural",
            "Mimbre",
            "DANADO",
            "Almacén A · suelo",
            18.9,
            "Cestería Sur",
            25.0,
            35.0,
        )
        history += Change(state.newId(), materials.first().id, "CANTIDAD", "Boda del sábado", t, DemoState.MANAGER_ID, "12", "8")
    }

    private fun seed(
        name: String,
        category: Category,
        available: Int,
        total: Int,
        color: String,
        physical: String,
        condition: String,
        location: String,
        price: Double,
        supplier: String,
        height: Double,
        width: Double,
    ) {
        val t = state.now()
        materials +=
            Material(
                state.newId(),
                buildJsonObject {
                    put("nombre", name)
                    put("cantidadTotal", total)
                    put(
                        "tamano",
                        buildJsonObject {
                            put("alto", height)
                            put("ancho", width)
                            put("unidadMedida", "CM")
                        },
                    )
                    put("color", color)
                    put("materialFisico", physical)
                    put("estado", condition)
                    put("ubicacion", location)
                    put("precioUnitario", price)
                    put("proveedor", supplier)
                    put("fotos", JsonArray(emptyList()))
                },
                category.id,
                available,
                t,
                t,
            )
    }

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
                HttpMethod.Get -> json(JsonArray(materials.map { it.toJson() }))
                HttpMethod.Post -> {
                    val categoryId = request.body.str("categoriaId").orEmpty()
                    if (categories.none { it.id == categoryId }) return notFound("CATEGORIA_NOT_FOUND")
                    val t = state.now()
                    val material =
                        Material(state.newId(), strip(request.body), categoryId, request.body.int("cantidadDisponible") ?: 0, t, t)
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
                val categoryId = request.body.str("categoriaId") ?: material.categoryId
                if (categories.none { it.id == categoryId }) return notFound("CATEGORIA_NOT_FOUND")
                val before = material.data
                material.data = strip(request.body)
                material.categoryId = categoryId
                material.updatedAt = state.now()
                listOf(
                    "estado" to "ESTADO",
                    "ubicacion" to "UBICACION",
                    "precioUnitario" to "PRECIO_UNITARIO",
                    "proveedor" to "PROVEEDOR",
                ).forEach { (field, type) ->
                    if (before.str(field) != material.data.str(field)) {
                        history +=
                            Change(
                                state.newId(),
                                material.id,
                                type,
                                "Edición",
                                state.now(),
                                request.caller!!.employeeId,
                                before.str(field),
                                material.data.str(field),
                            )
                    }
                }
                json(material.toJson())
            }
            s.size == 2 && request.method == HttpMethod.Delete -> {
                materials -= material
                DemoReply.NoContent
            }
            s.size == 3 && s[2] == "cantidad" -> {
                val value = request.body.int("cantidadDisponible") ?: -1
                if (value < 0 || value > (material.data.int("cantidadTotal") ?: 0)) {
                    return error(HttpStatusCode.BadRequest, "INVALID_OPERATION", "Cantidad fuera de rango")
                }
                history +=
                    Change(
                        state.newId(),
                        material.id,
                        "CANTIDAD",
                        request.body.str("motivo").orEmpty(),
                        state.now(),
                        request.caller!!.employeeId,
                        material.available.toString(),
                        value.toString(),
                    )
                material.available = value
                material.updatedAt = state.now()
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

    private fun strip(body: JsonObject) = JsonObject(body.filterKeys { it != "categoriaId" && it != "cantidadDisponible" })

    private fun Category.toJson() =
        buildJsonObject {
            put("id", id)
            put("nombre", name)
            description?.let { put("descripcion", it) }
            put("createdAt", createdAt.toString())
            put("updatedAt", updatedAt.toString())
        }

    private fun Material.toJson() =
        JsonObject(
            data +
                buildJsonObject {
                    put("id", id)
                    put("categoria", categories.first { it.id == categoryId }.toJson())
                    put("cantidadDisponible", available)
                    put("fechaAlta", createdAt.toString())
                    put("fechaUltimaModificacion", updatedAt.toString())
                },
        )

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
}
