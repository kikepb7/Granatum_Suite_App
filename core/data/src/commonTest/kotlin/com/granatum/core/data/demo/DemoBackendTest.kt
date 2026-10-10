package com.granatum.core.data.demo

import com.granatum.core.data.auth.token.AccessTokenClaims
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
            val admin = AccessTokenClaims.parse(token(DemoMode.ADMIN))!!
            val employee = AccessTokenClaims.parse(token(DemoMode.EMPLOYEE))!!
            assertEquals("ADMIN", admin.role)
            assertEquals("EMPLEADO", employee.role)
            assertEquals(HttpStatusCode.Unauthorized, login(DemoMode.ADMIN.email, "wrong").status)
        }

    @Test
    fun roles_are_enforced_as_on_the_server() =
        runTest {
            val employee = token(DemoMode.EMPLOYEE)
            assertEquals(HttpStatusCode.Forbidden, get("/empleados", employee).status)
            assertEquals(HttpStatusCode.Forbidden, get("/materiales", employee).status)
            assertEquals(HttpStatusCode.Forbidden, get("/facturacion/facturas", employee).status)
            assertEquals(HttpStatusCode.Unauthorized, http.get("$base/materiales").status)
        }

    @Test
    fun the_employee_clocks_in_and_out_and_sees_their_history() =
        runTest {
            val employee = token(DemoMode.EMPLOYEE)
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
            val admin = token(DemoMode.ADMIN)
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
            val admin = token(DemoMode.ADMIN)
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
            val admin = token(DemoMode.ADMIN)
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
                setBody("""{"email":"${DemoMode.ADMIN.email}","password":"${DemoMode.ADMIN.password}"}""")
            }.bodyAsText()
        ).jsonObject["accessToken"]!!.jsonPrimitive.content

        assertEquals(HttpStatusCode.OK, get("/empleados", oldToken).status)
        assertEquals(HttpStatusCode.Unauthorized, get("/empleados", "not-a-demo-token").status)
    }
}
