package com.granatum.feature.inventory.data

import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.data.remote.InventoryRemoteDataSource
import com.granatum.feature.inventory.data.repository.OfflineFirstInventoryRepository
import com.granatum.feature.inventory.database.dao.InventoryDao
import com.granatum.feature.inventory.database.entity.CategoryEntity
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.MaterialHistoryEntity
import com.granatum.feature.inventory.domain.model.Dimensions
import com.granatum.feature.inventory.domain.model.HistoryChangeType
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductDraft
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
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

/** specs/011-inventario-productos against backend 010 (contracts/README.md). */
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

    private val category = """{"id":"c1","name":"Jarrones","description":null,"createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-01T00:00:00Z"}"""
    private fun material(
        id: String = "m1",
        quantity: Int = 5,
        warehouse: String = "B",
        section: Int = 3,
        image: String? = "https://s.test/storage/v1/object/sign/materiales/m1/a.jpg?token=t"
    ) = """{"id":"$id","category":$category,"description":"Cilindro alto","dimensions":{"height":40.0,"width":15.5},"quantity":$quantity,""" +
        """"location":{"warehouse":"$warehouse","section":$section},"imageUrl":${image?.let { "\"$it\"" } ?: "null"},""" +
        """"createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-02T00:00:00Z"}"""

    private val draft = ProductDraft("c1", "  Cilindro alto  ", Dimensions(40.0, 15.555), 7, StorageLocation(Warehouse.A, 3))

    @Test
    fun a_refresh_maps_materials_onto_products() = runTest {
        dao.upsertMaterial(MaterialEntity("gone", "c1", "Viejo", 1.0, 1.0, 0, "A", 1, null, 0, 0))
        val repo = repo { request -> if (request.url.encodedPath.endsWith("/categories")) ok("[$category]") else ok("[${material()}]") }
        assertEquals(Result.Success(Unit), repo.refresh())
        val p = repo.observeProducts().first().single()
        assertEquals("Jarrones" to "Cilindro alto", p.category.name to p.description)
        assertEquals(StorageLocation(Warehouse.B, 3), p.location)
        assertEquals(Dimensions(40.0, 15.5), p.dimensions)
        assertEquals("https://s.test/storage/v1/object/sign/materiales/m1/a.jpg?token=t", p.imageUrl)
        assertEquals(5, p.quantity)
    }

    @Test
    fun a_row_with_an_unknown_location_does_not_hide_the_rest() = runTest {
        val repo = repo { request ->
            if (request.url.encodedPath.endsWith("/categories")) ok("[$category]") else ok("[${material()},${material(id = "m2", warehouse = "D")}]")
        }
        repo.refresh()
        assertEquals(listOf("m1"), repo.observeProducts().first().map { it.id })
    }

    @Test
    fun creating_sends_the_contract_body() = runTest {
        val repo = repo { ok(material(), HttpStatusCode.Created) }
        repo.create(draft)
        val (call, body) = requests.single()
        assertEquals("POST /materials", call)
        assertEquals(
            """{"categoryId":"c1","description":"Cilindro alto","dimensions":{"height":40.0,"width":15.56},"quantity":7,"location":{"warehouse":"A","section":3}}""",
            body
        )
        assertEquals(1, dao.materials.value.size)
    }

    @Test
    fun editing_never_sends_the_quantity() = runTest {
        val repo = repo { ok(material()) }
        repo.update("m1", draft)
        val (call, body) = requests.single()
        assertEquals("PUT /materials/m1", call)
        assertEquals("""{"categoryId":"c1","description":"Cilindro alto","dimensions":{"height":40.0,"width":15.56},"location":{"warehouse":"A","section":3}}""", body)
    }

    @Test
    fun a_quantity_change_always_carries_a_reason() = runTest {
        val repo = repo { ok(material(quantity = 9)) }
        repo.setQuantity("m1", 9, "  Pedido recibido ")
        repo.setQuantity("m1", 2, "  ")
        assertEquals(
            listOf(
                "PATCH /materials/m1/quantity" to """{"quantity":9,"reason":"Pedido recibido"}""",
                "PATCH /materials/m1/quantity" to """{"quantity":2,"reason":"Ajuste de cantidad"}"""
            ),
            requests
        )
        assertEquals(9, dao.materials.value.single().quantity)
    }

    @Test
    fun the_image_goes_as_one_multipart_part_named_image() = runTest {
        var method: HttpMethod? = null
        val repo = repo { request -> method = request.method; ok(material()) }
        val result = repo.setImage("m1", byteArrayOf(1, 2, 3), "foto.jpg", "image/jpeg")
        val (call, body) = requests.single()
        assertEquals(HttpMethod.Put, method)
        assertEquals("PUT /materials/m1/image", call)
        assertTrue("name=image" in body && "filename=\"foto.jpg\"" in body && "Content-Type: image/jpeg" in body, body)
        assertTrue(result is Result.Success)
    }

    @Test
    fun removing_the_image_updates_the_cache() = runTest {
        repo { ok(material()) }.refreshProduct("m1")
        val repo = repo { ok(material(image = null)) }
        assertTrue(repo.removeImage("m1") is Result.Success)
        assertEquals("DELETE /materials/m1/image", requests.last().first)
        assertEquals(null, dao.materials.value.single().imageUrl)
    }

    @Test
    fun image_errors_are_told_apart() = runTest {
        suspend fun upload(status: HttpStatusCode, code: String) =
            repo { ok("""{"code":"$code","message":"m"}""", status) }.setImage("m1", byteArrayOf(1), "a.jpg", "image/jpeg")
        assertEquals(Result.Failure(InventoryError.ImageInvalid), upload(HttpStatusCode.UnsupportedMediaType, "INVALID_IMAGE"))
        assertEquals(Result.Failure(InventoryError.ImageTooLarge), upload(HttpStatusCode.PayloadTooLarge, "IMAGE_TOO_LARGE"))
        assertEquals(Result.Failure(InventoryError.StorageUnavailable), upload(HttpStatusCode.ServiceUnavailable, "IMAGE_STORAGE_UNAVAILABLE"))
    }

    @Test
    fun `When a product is out at a scheduled event then deleting it is refused and it stays cached`() = runTest {
        repo { ok(material()) }.refreshProduct("m1")
        val repo = repo { ok("""{"code":"MATERIAL_IN_USE","message":"m"}""", HttpStatusCode.Conflict) }

        val actual = repo.delete("m1")

        assertEquals(expected = Result.Failure(InventoryError.ProductInUse), actual = actual)
        assertEquals(expected = 1, actual = dao.materials.value.size)
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
        val history = """[{"id":"h1","materialId":"m1","changeType":"QUANTITY","reason":"Rotura","date":"2026-10-03T00:00:00Z","userId":"u","previousValue":"5","newValue":"4"},""" +
            """{"id":"h2","materialId":"m1","changeType":"QUANTITY","reason":"Pedido","date":"2026-10-05T00:00:00Z","userId":"u","previousValue":"4","newValue":"9"}]"""
        val repo = repo { request -> if (request.url.encodedPath.endsWith("history")) ok(history) else respond("", HttpStatusCode.NoContent) }
        repo.refreshHistory("m1")
        assertEquals(listOf("h2", "h1"), repo.observeHistory("m1").first().map { it.id })
        assertEquals(HistoryChangeType.QUANTITY, repo.observeHistory("m1").first().first().type)
        assertEquals(Result.Success(Unit), repo.delete("m1"))
        assertTrue(dao.materials.value.isEmpty())
    }
}
