package com.granatum.feature.inventory.data

import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.data.remote.InventoryRemoteDataSource
import com.granatum.feature.inventory.data.repository.OfflineFirstInventoryRepository
import com.granatum.feature.inventory.database.dao.InventoryDao
import com.granatum.feature.inventory.database.entity.CategoryEntity
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.MaterialHistoryEntity
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialSize
import com.granatum.feature.inventory.domain.model.SizeUnit
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

/** specs/006-inventario-real, contracts/inventario-api.md. */
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

    private val category = """{"id":"c1","nombre":"Cilindros","descripcion":null,"createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-01T00:00:00Z"}"""
    private fun material(id: String = "m1", available: Int = 5, fotos: String = """["https://x/1.jpg"]""") =
        """{"id":"$id","nombre":"Cilindro alto","categoria":$category,"cantidadDisponible":$available,"cantidadTotal":10,"tamano":{"alto":40,"ancho":15,"diametro":null,"unidadMedida":"CM"},"color":"Transparente","materialFisico":"Cristal","estado":"NUEVO","ubicacion":"Almacén A","precioUnitario":12.5,"proveedor":"Cristalería Sur","fotos":$fotos,"fechaAlta":"2026-10-01T00:00:00Z","fechaUltimaModificacion":"2026-10-02T00:00:00Z"}"""
    private val draft = MaterialDraft("Cilindro alto", "c1", 5, 10, MaterialSize(40.0, 15.0, null, SizeUnit.CM), "Transparente", "Cristal", MaterialCondition.NUEVO, "Almacén A", 12.5, "Cristalería Sur", listOf("https://x/1.jpg"))

    @Test
    fun a_refresh_replaces_the_cache_and_materials_come_with_their_category() = runTest {
        dao.upsertMaterial(MaterialEntity("gone", "Viejo", "c1", 0, 0, 0.0, 0.0, null, "CM", "", "", "NUEVO", "", 0.0, "", "[]", 0, 0))
        val repo = repo { request -> if (request.url.encodedPath.endsWith("/categorias")) ok("[$category]") else ok("[${material()}]") }
        assertEquals(Result.Success(Unit), repo.refresh())
        val m = repo.observeMaterials().first().single()
        assertEquals("m1" to "Cilindros", m.id to m.category.name)
        assertEquals(12.5, m.unitPrice)
        assertEquals(listOf("https://x/1.jpg"), m.photos)
    }

    @Test
    fun create_uses_the_contract_field_names() = runTest {
        val repo = repo { ok(material(), HttpStatusCode.Created) }
        repo.create(draft)
        val (call, body) = requests.single()
        assertEquals("POST /materiales", call)
        listOf(""""categoriaId":"c1"""", """"cantidadDisponible":5""", """"cantidadTotal":10""", """"unidadMedida":"CM"""", """"materialFisico":"Cristal"""", """"precioUnitario":12.5""", """"estado":"NUEVO"""")
            .forEach { assertTrue(it in body, "$it in $body") }
        assertEquals(1, dao.materials.value.size)
    }

    @Test
    fun editing_sends_the_photos_back_and_never_the_available_quantity() = runTest {
        val repo = repo { ok(material()) }
        repo.update("m1", draft)
        val (call, body) = requests.single()
        assertEquals("PUT /materiales/m1", call)
        assertTrue(""""fotos":["https://x/1.jpg"]""" in body, body)
        assertTrue("cantidadDisponible" !in body, body)
    }

    @Test
    fun an_adjustment_carries_the_reason() = runTest {
        val repo = repo { ok(material(available = 8)) }
        repo.adjustQuantity("m1", 8, "  Recibido pedido  ")
        assertEquals("PATCH /materiales/m1/cantidad" to """{"cantidadDisponible":8,"motivo":"Recibido pedido"}""", requests.single())
        assertEquals(8, dao.materials.value.single().available)
    }

    @Test
    fun a_material_deleted_elsewhere_leaves_the_cache() = runTest {
        repo { ok(material()) }.refreshMaterial("m1")
        val repo = repo { ok("""{"code":"MATERIAL_NOT_FOUND","message":"m"}""", HttpStatusCode.NotFound) }
        assertEquals(Result.Failure(InventoryError.MaterialNotFound), repo.adjustQuantity("m1", 3, "x"))
        assertTrue(dao.materials.value.isEmpty())
    }

    @Test
    fun without_coverage_nothing_is_written() = runTest {
        repo { ok(material()) }.refreshMaterial("m1")
        val before = dao.materials.value
        val repo = repo { throw kotlinx.io.IOException("offline") }
        assertEquals(Result.Failure(InventoryError.NoInternet), repo.adjustQuantity("m1", 3, "x"))
        assertEquals(Result.Failure(InventoryError.NoInternet), repo.delete("m1"))
        assertEquals(before, dao.materials.value)
    }

    @Test
    fun deleting_succeeds_on_204_and_history_is_cached() = runTest {
        repo { ok(material()) }.refreshMaterial("m1")
        val history = """[{"id":"h1","materialId":"m1","tipoCambio":"CANTIDAD","motivo":"Rotura","fecha":"2026-10-03T00:00:00Z","usuarioId":"u","valorAnterior":"5","valorNuevo":"4"}]"""
        val repo = repo { request -> if (request.url.encodedPath.endsWith("historial")) ok(history) else respond("", HttpStatusCode.NoContent) }
        repo.refreshHistory("m1")
        assertEquals("5" to "4", repo.observeHistory("m1").first().single().let { it.previousValue to it.newValue })
        assertEquals(Result.Success(Unit), repo.delete("m1"))
        assertTrue(dao.materials.value.isEmpty())
    }
}
