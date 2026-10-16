package com.granatum.feature.inventory.data

import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.data.remote.InventoryRemoteDataSource
import com.granatum.feature.inventory.data.repository.OfflineFirstInventoryRepository
import com.granatum.feature.inventory.database.dao.InventoryDao
import com.granatum.feature.inventory.database.entity.CategoryEntity
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.MaterialHistoryEntity
import com.granatum.feature.inventory.domain.model.Dimensions
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductDraft
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** specs/011-inventario-productos, research D1 (mapping) and D2 (quantity with history). */
class InventoryRepositoryTest {

    private class MemoryDao : InventoryDao {
        val materials = MutableStateFlow<List<MaterialEntity>>(emptyList())
        val categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
        val history = MutableStateFlow<List<MaterialHistoryEntity>>(emptyList())
        override fun observeMaterials(): Flow<List<MaterialEntity>> = materials
        override fun observeMaterial(id: String) = materials.map { all -> all.firstOrNull { it.id == id } }
        override fun observeCategories(): Flow<List<CategoryEntity>> = categories
        override fun observeHistory(materialId: String) = history.map { all -> all.filter { it.materialId == materialId } }
        override suspend fun upsertMaterial(material: MaterialEntity) { materials.value = materials.value.filterNot { it.id == material.id } + material }
        override suspend fun upsertCategory(category: CategoryEntity) { categories.value = categories.value.filterNot { it.id == category.id } + category }
        override suspend fun deleteMaterial(id: String) { materials.value = materials.value.filterNot { it.id == id } }
        override suspend fun deleteCategory(id: String) { categories.value = categories.value.filterNot { it.id == id } }
        override suspend fun clearMaterials() { materials.value = emptyList() }
        override suspend fun clearCategories() { categories.value = emptyList() }
        override suspend fun clearHistory(materialId: String) { history.value = history.value.filterNot { it.materialId == materialId } }
        override suspend fun upsertMaterials(materials: List<MaterialEntity>) = materials.forEach { upsertMaterial(it) }
        override suspend fun upsertCategories(categories: List<CategoryEntity>) = categories.forEach { upsertCategory(it) }
        override suspend fun upsertHistory(entries: List<MaterialHistoryEntity>) { history.value = history.value + entries }
    }

    private val dao = MemoryDao()
    private val requests = mutableListOf<Pair<String, String>>()

    private fun repo(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): OfflineFirstInventoryRepository {
        val client = HttpClient(MockEngine { request ->
            requests += "${request.method.value} ${request.url.encodedPath.substringAfter("/api")}" to request.body.toByteArray().decodeToString()
            handler(request)
        }) {
            install(ContentNegotiation) { json() }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
        return OfflineFirstInventoryRepository(InventoryRemoteDataSource(client), dao)
    }

    private fun MockRequestHandleScope.ok(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val category = """{"id":"c1","nombre":"Jarrones","descripcion":null,"createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-01T00:00:00Z"}"""
    private fun material(
        id: String = "m1",
        available: Int = 5,
        total: Int = 5,
        location: String = "B-3",
        size: String = "\"alto\":40,\"ancho\":15,\"diametro\":null,\"unidadMedida\":\"CM\"",
        fotos: String = "[\"https://x/1.jpg\",\"https://x/2.jpg\"]"
    ) = """{"id":"$id","nombre":"Cilindro alto","categoria":$category,"cantidadDisponible":$available,"cantidadTotal":$total,"tamano":{$size},"color":"Transparente","materialFisico":"Cristal","estado":"USADO","ubicacion":"$location","precioUnitario":12.5,"proveedor":"Cristalería Sur","fotos":$fotos,"fechaAlta":"2026-10-01T00:00:00Z","fechaUltimaModificacion":"2026-10-02T00:00:00Z"}"""

    private val draft = ProductDraft("c1", "  Cilindro alto  ", Dimensions(40.0, 15.0), 7, "https://x/new.jpg", StorageLocation(Warehouse.A, 3))

    @Test
    fun a_refresh_maps_materials_onto_products() = runTest {
        dao.upsertMaterial(MaterialEntity("gone", "Viejo", "c1", 0, 0, 0.0, 0.0, null, "CM", "", "", "NUEVO", "", 0.0, "", "[]", 0, 0))
        val repo = repo { request -> if (request.url.encodedPath.endsWith("/categorias")) ok("[$category]") else ok("[${material()}]") }
        assertEquals(Result.Success(Unit), repo.refresh())
        val p = repo.observeProducts().first().single()
        assertEquals("Jarrones" to "Cilindro alto", p.category.name to p.description)
        assertEquals(StorageLocation(Warehouse.B, 3), p.location)
        assertEquals("https://x/1.jpg", p.imageUrl)
        assertEquals(5, p.quantity)
    }

    @Test
    fun older_units_become_centimetres_and_free_text_locations_stay_unlocated() = runTest {
        repo { ok(material(location = "Almacén A · estante 2", size = "\"alto\":0.4,\"ancho\":0.15,\"diametro\":null,\"unidadMedida\":\"M\"")) }.refreshProduct("m1")
        val p = repo { ok("[]") }.observeProduct("m1").first()!!
        assertEquals(Dimensions(40.0, 15.0), p.dimensions)
        assertEquals(null, p.location)
    }

    @Test
    fun creating_fills_what_the_server_still_requires_with_neutral_values() = runTest {
        val repo = repo { ok(material(), HttpStatusCode.Created) }
        repo.create(draft)
        val (call, body) = requests.single()
        assertEquals("POST /materiales", call)
        listOf(
            """"nombre":"Cilindro alto"""", """"categoriaId":"c1"""", """"cantidadDisponible":7""", """"cantidadTotal":7""",
            """"ubicacion":"A-3"""", """"unidadMedida":"CM"""", """"fotos":["https://x/new.jpg"]""", """"estado":"NUEVO"""",
            """"color":"—"""", """"precioUnitario":0.0"""
        ).forEach { assertTrue(it in body, "$it in $body") }
        assertEquals(1, dao.materials.value.size)
    }

    @Test
    fun editing_keeps_the_legacy_fields_and_the_other_photos_and_never_the_quantity() = runTest {
        repo { ok(material()) }.refreshProduct("m1")
        requests.clear()
        val repo = repo { ok(material()) }
        repo.update("m1", draft)
        val (call, body) = requests.single()
        assertEquals("PUT /materiales/m1", call)
        listOf(
            """"color":"Transparente"""", """"estado":"USADO"""", """"proveedor":"Cristalería Sur"""", """"precioUnitario":12.5""",
            """"fotos":["https://x/new.jpg","https://x/2.jpg"]""", """"cantidadTotal":5""", """"ubicacion":"A-3""""
        ).forEach { assertTrue(it in body, "$it in $body") }
        assertTrue("cantidadDisponible" !in body, body)
    }

    @Test
    fun raising_the_quantity_sets_the_total_first_then_records_the_change() = runTest {
        repo { ok(material()) }.refreshProduct("m1")
        requests.clear()
        val repo = repo { request -> if (request.url.encodedPath.endsWith("cantidad")) ok(material(available = 9, total = 9)) else ok(material(available = 5, total = 9)) }
        assertTrue(repo.setQuantity("m1", 9, "  Pedido recibido ") is Result.Success)
        assertEquals(listOf("PUT /materiales/m1", "PATCH /materiales/m1/cantidad"), requests.map { it.first })
        assertTrue(""""cantidadTotal":9""" in requests[0].second)
        assertEquals("""{"cantidadDisponible":9,"motivo":"Pedido recibido"}""", requests[1].second)
        assertEquals(9, dao.materials.value.single().available)
    }

    @Test
    fun lowering_the_quantity_records_the_change_first_then_shrinks_the_total() = runTest {
        repo { ok(material()) }.refreshProduct("m1")
        requests.clear()
        val repo = repo { request -> if (request.url.encodedPath.endsWith("cantidad")) ok(material(available = 2, total = 5)) else ok(material(available = 2, total = 2)) }
        assertTrue(repo.setQuantity("m1", 2, null) is Result.Success)
        assertEquals(listOf("PATCH /materiales/m1/cantidad", "PUT /materiales/m1"), requests.map { it.first })
        assertEquals("""{"cantidadDisponible":2,"motivo":"Ajuste de cantidad"}""", requests[0].second)
        assertTrue(""""cantidadTotal":2""" in requests[1].second)
    }

    @Test
    fun a_product_deleted_elsewhere_leaves_the_cache() = runTest {
        repo { ok(material()) }.refreshProduct("m1")
        val repo = repo { ok("""{"code":"MATERIAL_NOT_FOUND","message":"m"}""", HttpStatusCode.NotFound) }
        assertEquals(Result.Failure(InventoryError.ProductNotFound), repo.setQuantity("m1", 3, "x"))
        assertTrue(dao.materials.value.isEmpty())
    }

    @Test
    fun without_coverage_nothing_is_written() = runTest {
        repo { ok(material()) }.refreshProduct("m1")
        val before = dao.materials.value
        val repo = repo { throw kotlinx.io.IOException("offline") }
        assertEquals(Result.Failure(InventoryError.NoInternet), repo.setQuantity("m1", 3, "x"))
        assertEquals(Result.Failure(InventoryError.NoInternet), repo.delete("m1"))
        assertEquals(before, dao.materials.value)
    }

    @Test
    fun deleting_succeeds_on_204_and_history_comes_newest_first() = runTest {
        repo { ok(material()) }.refreshProduct("m1")
        val history = """[{"id":"h1","materialId":"m1","tipoCambio":"CANTIDAD","motivo":"Rotura","fecha":"2026-10-03T00:00:00Z","usuarioId":"u","valorAnterior":"5","valorNuevo":"4"},""" +
            """{"id":"h2","materialId":"m1","tipoCambio":"UBICACION","motivo":"Orden","fecha":"2026-10-05T00:00:00Z","usuarioId":"u","valorAnterior":"A-1","valorNuevo":"B-3"}]"""
        val repo = repo { request -> if (request.url.encodedPath.endsWith("historial")) ok(history) else respond("", HttpStatusCode.NoContent) }
        repo.refreshHistory("m1")
        assertEquals(listOf("h2", "h1"), repo.observeHistory("m1").first().map { it.id })
        assertEquals(Result.Success(Unit), repo.delete("m1"))
        assertTrue(dao.materials.value.isEmpty())
    }
}
