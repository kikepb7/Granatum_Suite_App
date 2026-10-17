package com.granatum.core.demo

import com.granatum.core.data.auth.token.AccessTokenClaims
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The demo backend speaks the contract the app expects, for both demo roles. */
class DemoBackendTest {
    private val backend = DemoBackend()
    private val http = HttpClient(MockEngine { request -> backend.handle(this, request) })
    private val base = "http://demo/api"

    private suspend fun login(
        email: String,
        password: String,
    ): HttpResponse =
        http.post("$base/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }

    private suspend fun token(account: com.granatum.core.domain.auth.model.DemoAccount): String =
        Json
            .parseToJsonElement(login(account.email, account.password).bodyAsText())
            .jsonObject["accessToken"]!!
            .jsonPrimitive.content

    private suspend fun get(
        path: String,
        token: String,
    ) = http.get("$base$path") { header(HttpHeaders.Authorization, "Bearer $token") }

    private suspend fun post(
        path: String,
        token: String,
        body: String,
    ) = http.post("$base$path") {
        header(HttpHeaders.Authorization, "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun HttpResponse.json() = Json.parseToJsonElement(bodyAsText())

    @Test
    fun both_demo_accounts_sign_in_with_their_role_in_the_token() =
        runTest {
            val admin = AccessTokenClaims.parse(token(DemoAccountsCatalog.ADMIN))!!
            val employee = AccessTokenClaims.parse(token(DemoAccountsCatalog.EMPLOYEE))!!
            assertEquals("ADMIN", admin.role)
            assertEquals("EMPLEADO", employee.role)
            assertEquals(HttpStatusCode.Unauthorized, login(DemoAccountsCatalog.ADMIN.email, "wrong").status)
        }

    @Test
    fun roles_are_enforced_as_on_the_server() =
        runTest {
            val employee = token(DemoAccountsCatalog.EMPLOYEE)
            assertEquals(HttpStatusCode.Forbidden, get("/empleados", employee).status)
            assertEquals(HttpStatusCode.Forbidden, get("/materiales", employee).status)
            assertEquals(HttpStatusCode.Forbidden, get("/facturacion/facturas", employee).status)
            assertEquals(HttpStatusCode.Unauthorized, http.get("$base/materiales").status)
        }

    @Test
    fun the_employee_clocks_in_and_out_and_sees_their_history() =
        runTest {
            val employee = token(DemoAccountsCatalog.EMPLOYEE)
            val history = get("/fichajes/empleado/${DemoState.EMPLOYEE_ID}", employee).json().jsonArray
            assertTrue(history.isNotEmpty(), "seeded shifts")

            val entry = post("/fichajes/entrada", employee, """{"clientEventId":"e1","occurredAt":"2026-10-09T07:00:00Z"}""")
            assertEquals(HttpStatusCode.Created, entry.status)
            val shiftId =
                entry
                    .json()
                    .jsonObject["id"]!!
                    .jsonPrimitive.content
            // Resent: same shift, no duplicate.
            assertEquals(
                shiftId,
                post(
                    "/fichajes/entrada",
                    employee,
                    """{"clientEventId":"e1","occurredAt":"2026-10-09T07:00:00Z"}""",
                ).json().jsonObject["id"]!!.jsonPrimitive.content,
            )
            post(
                "/fichajes/$shiftId/pausa/inicio",
                employee,
                """{"clientEventId":"e2","occurredAt":"2026-10-09T09:00:00Z","tipo":"DESCANSO"}""",
            )
            post("/fichajes/$shiftId/pausa/fin", employee, """{"clientEventId":"e3","occurredAt":"2026-10-09T09:15:00Z"}""")
            val closed =
                post(
                    "/fichajes/$shiftId/salida",
                    employee,
                    """{"clientEventId":"e4","occurredAt":"2026-10-09T11:00:00Z"}""",
                ).json().jsonObject
            assertEquals("CERRADO", closed["estado"]!!.jsonPrimitive.content)
            assertEquals(225, closed["minutosTrabajados"]!!.jsonPrimitive.content.toInt())
        }

    @Test
    fun the_admin_sees_inventory_invoices_and_staff() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            assertTrue(get("/materiales", admin).json().jsonArray.size >= 5)
            assertTrue(get("/categorias", admin).json().jsonArray.size >= 3)
            val page = get("/facturacion/facturas?pagina=0&tamano=50", admin).json().jsonObject
            assertTrue((page["elementos"] as JsonArray).size >= 4)
            assertEquals(
                "Granatum Flores SL",
                get("/facturacion/empresa", admin)
                    .json()
                    .jsonObject["razonSocial"]!!
                    .jsonPrimitive.content,
            )
            assertTrue(get("/empleados", admin).json().jsonArray.size >= 4)
            val report = get("/facturacion/reportes?periodo=ANUAL&anio=2026", admin)
            assertEquals(HttpStatusCode.OK, report.status)
        }

    @Test
    fun an_upload_is_sniffed_and_a_duplicate_detected() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val pdf = DemoDocuments.pdf(listOf("Factura"))

            suspend fun upload(vararg files: ByteArray) =
                http
                    .submitFormWithBinaryData(
                        "$base/facturacion/facturas",
                        formData {
                            files.forEach {
                                append(
                                    "ficheros",
                                    it,
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "application/pdf")
                                        append(HttpHeaders.ContentDisposition, "filename=\"f.pdf\"")
                                    },
                                )
                            }
                        },
                    ) {
                        header(
                            HttpHeaders.Authorization,
                            "Bearer $admin",
                        )
                    }.json()
                    .jsonArray
                    .map { (it as JsonObject)["resultado"]!!.jsonPrimitive.content }

            assertEquals(listOf("ACEPTADA", "FORMATO_NO_ADMITIDO"), upload(pdf, "hola".encodeToByteArray()))
            assertEquals(listOf("DUPLICADA"), upload(pdf))
        }

    @Test
    fun onboarding_gives_a_temporary_password_that_must_be_changed() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val created =
                post(
                    "/auth/altas",
                    admin,
                    """{"nombre":"Marta Gil","documentoIdentidad":"12345678Z","puesto":"Florista","tipoContrato":"PARCIAL","fechaAlta":"2026-10-09","email":"marta@demo.granatum.es","rol":"EMPLEADO"}""",
                )
            // That DNI is the admin's own record, which already has an account.
            assertEquals(HttpStatusCode.Conflict, created.status)

            val ok =
                post(
                    "/auth/altas",
                    admin,
                    """{"nombre":"Marta Gil","documentoIdentidad":"00000001R","puesto":"Florista","tipoContrato":"PARCIAL","fechaAlta":"2026-10-09","email":"marta@demo.granatum.es","rol":"EMPLEADO"}""",
                ).json().jsonObject
            val temporary = ok["passwordTemporal"]!!.jsonPrimitive.content
            val session = login("marta@demo.granatum.es", temporary).json().jsonObject
            assertEquals("true", session["requiereCambioPassword"]!!.jsonPrimitive.content)
        }

    @Test
    fun a_token_from_an_earlier_run_still_works() = runTest {
        val earlier = DemoBackend()
        val oldToken = Json.parseToJsonElement(
            HttpClient(MockEngine { request -> earlier.handle(this, request) }).post("$base/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"${DemoAccountsCatalog.ADMIN.email}","password":"${DemoAccountsCatalog.ADMIN.password}"}""")
            }.bodyAsText()
        ).jsonObject["accessToken"]!!.jsonPrimitive.content

        assertEquals(HttpStatusCode.OK, get("/empleados", oldToken).status)
        assertEquals(HttpStatusCode.Unauthorized, get("/empleados", "not-a-demo-token").status)
    }

    @Test
    fun a_material_image_is_uploaded_to_the_server_and_read_from_its_signed_url() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val id = get("/materiales", admin).json().jsonArray.first().jsonObject["id"]!!.jsonPrimitive.content
            val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 1, 2)
            fun part(bytes: ByteArray) =
                formData {
                    append("imagen", bytes, Headers.build { append(HttpHeaders.ContentDisposition, "filename=\"a.png\"") })
                }
            val uploaded =
                http.put("$base/materiales/$id/imagen") {
                    header(HttpHeaders.Authorization, "Bearer $admin")
                    setBody(MultiPartFormDataContent(part(png)))
                }
            assertEquals(HttpStatusCode.OK, uploaded.status)
            val url = uploaded.json().jsonObject["imagenUrl"]!!.jsonPrimitive.content
            assertTrue("/storage/v1/object/sign/" in url, url)
            assertEquals(png.toList(), http.get(url).readRawBytes().toList())

            val notAnImage =
                http.put("$base/materiales/$id/imagen") {
                    header(HttpHeaders.Authorization, "Bearer $admin")
                    setBody(MultiPartFormDataContent(part("hola".encodeToByteArray())))
                }
            assertEquals(HttpStatusCode.UnsupportedMediaType, notAnImage.status)

            val removed = http.delete("$base/materiales/$id/imagen") { header(HttpHeaders.Authorization, "Bearer $admin") }
            assertEquals(JsonNull, removed.json().jsonObject["imagenUrl"])
            assertEquals(HttpStatusCode.NotFound, http.get(url).status)
        }

    @Test
    fun materials_follow_the_010_contract() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val first = get("/materiales", admin).json().jsonArray.first().jsonObject
            listOf("descripcion", "dimensiones", "cantidad", "localizacion", "fechaAlta").forEach { assertTrue(it in first, it) }
            assertEquals(2, get("/materiales?almacen=B", admin).json().jsonArray.size)
            assertEquals(HttpStatusCode.BadRequest, get("/materiales?seccion=2", admin).status)
            val category = first["categoria"]!!.jsonObject["id"]!!.jsonPrimitive.content
            val zeroWidth = """{"categoriaId":"$category","descripcion":"x","dimensiones":{"alto":1,"ancho":0},"cantidad":1,"localizacion":{"almacen":"A","seccion":1}}"""
            assertEquals(HttpStatusCode.BadRequest, post("/materiales", admin, zeroWidth).status)
            val noReason = http.patch("$base/materiales/${first["id"]!!.jsonPrimitive.content}/cantidad") {
                header(HttpHeaders.Authorization, "Bearer $admin")
                contentType(ContentType.Application.Json)
                setBody("""{"cantidad":3,"motivo":" "}""")
            }
            assertEquals(HttpStatusCode.BadRequest, noReason.status)
        }

    @Test
    fun seeded_products_come_with_an_illustration() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val urls = get("/materiales", admin).json().jsonArray.map { it.jsonObject["imagenUrl"]!!.jsonPrimitive.content }
            assertEquals(5, urls.toSet().size)
            val image = http.get(urls.first())
            assertEquals(HttpStatusCode.OK, image.status)
            val bytes = image.readRawBytes()
            assertEquals(listOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()), bytes.take(4))
            assertEquals("image/png", image.headers[HttpHeaders.ContentType])
        }

    @Test
    fun `When an employee runs an event then the demo inventory follows its material`() =
        runTest {
            val employee = token(DemoAccountsCatalog.EMPLOYEE)
            val admin = token(DemoAccountsCatalog.ADMIN)
            val material = get("/eventos/materiales", employee).json().jsonArray.map { it.jsonObject }
                .first { it["descripcion"]!!.jsonPrimitive.content.startsWith("Lazo") }
            val id = material["id"]!!.jsonPrimitive.content
            val free = material["cantidad"]!!.jsonPrimitive.content.toInt()

            val created = post("/eventos", employee, """{"nombre":"Evento 9","descripcion":"Prueba","fecha":"2099-01-01"}""")
            assertEquals(HttpStatusCode.Created, created.status)
            val evento = created.json().jsonObject["id"]!!.jsonPrimitive.content

            val reserved =
                http.put("$base/eventos/$evento/materiales/$id") {
                    header(HttpHeaders.Authorization, "Bearer $employee")
                    contentType(ContentType.Application.Json)
                    setBody("""{"cantidad":2}""")
                }
            assertEquals(HttpStatusCode.OK, reserved.status)
            val after = get("/eventos/materiales", employee).json().jsonArray.map { it.jsonObject }.first { it["id"]!!.jsonPrimitive.content == id }
            assertEquals(free - 2, after["cantidad"]!!.jsonPrimitive.content.toInt())

            val tooMany =
                http.put("$base/eventos/$evento/materiales/$id") {
                    header(HttpHeaders.Authorization, "Bearer $employee")
                    contentType(ContentType.Application.Json)
                    setBody("""{"cantidad":${free + 5}}""")
                }
            assertEquals(HttpStatusCode.Conflict, tooMany.status)
            assertEquals((free - 2).toString(), tooMany.json().jsonObject["disponible"]!!.jsonPrimitive.content)

            val delete = http.delete("$base/materiales/$id") { header(HttpHeaders.Authorization, "Bearer $admin") }
            assertEquals(HttpStatusCode.Conflict, delete.status)
            assertEquals(HttpStatusCode.Forbidden, get("/materiales", employee).status)

            assertEquals(HttpStatusCode.OK, post("/eventos/$evento/finalizar", employee, "{}").status)
            val back = get("/eventos/materiales", employee).json().jsonArray.map { it.jsonObject }.first { it["id"]!!.jsonPrimitive.content == id }
            assertEquals(free, back["cantidad"]!!.jsonPrimitive.content.toInt())
        }

    @Test
    fun `When the demo starts then it has upcoming and finished events with reserved material`() =
        runTest {
            val events = get("/eventos", token(DemoAccountsCatalog.EMPLOYEE)).json().jsonArray.map { it.jsonObject }
            assertTrue(events.any { it["estado"]!!.jsonPrimitive.content == "PROGRAMADO" && it["materiales"]!!.jsonArray.isNotEmpty() })
            assertTrue(events.any { it["estado"]!!.jsonPrimitive.content == "FINALIZADO" })
        }
}
