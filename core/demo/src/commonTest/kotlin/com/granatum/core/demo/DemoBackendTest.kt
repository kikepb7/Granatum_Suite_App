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
            assertEquals("EMPLOYEE", employee.role)
            val manager = login("encargada@demo.granatum.es", "Demo-Encargada-2026!").json().jsonObject["accessToken"]!!.jsonPrimitive.content
            assertEquals("MANAGER", AccessTokenClaims.parse(manager)!!.role)
            assertEquals(HttpStatusCode.Unauthorized, login(DemoAccountsCatalog.ADMIN.email, "wrong").status)
        }

    @Test
    fun roles_are_enforced_as_on_the_server() =
        runTest {
            val employee = token(DemoAccountsCatalog.EMPLOYEE)
            assertEquals(HttpStatusCode.Forbidden, get("/employees", employee).status)
            assertEquals(HttpStatusCode.Forbidden, get("/materials", employee).status)
            assertEquals(HttpStatusCode.Forbidden, get("/invoicing/invoices", employee).status)
            assertEquals(HttpStatusCode.Unauthorized, http.get("$base/materials").status)
        }

    @Test
    fun the_employee_clocks_in_and_out_and_sees_their_history() =
        runTest {
            val employee = token(DemoAccountsCatalog.EMPLOYEE)
            val history = get("/shifts/employee/${DemoState.EMPLOYEE_ID}", employee).json().jsonArray
            assertTrue(history.isNotEmpty(), "seeded shifts")

            val entry = post("/shifts/clock-in", employee, """{"clientEventId":"e1","occurredAt":"2026-10-09T07:00:00Z"}""")
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
                    "/shifts/clock-in",
                    employee,
                    """{"clientEventId":"e1","occurredAt":"2026-10-09T07:00:00Z"}""",
                ).json().jsonObject["id"]!!.jsonPrimitive.content,
            )
            post(
                "/shifts/$shiftId/break/start",
                employee,
                """{"clientEventId":"e2","occurredAt":"2026-10-09T09:00:00Z","type":"REST"}""",
            )
            post("/shifts/$shiftId/break/end", employee, """{"clientEventId":"e3","occurredAt":"2026-10-09T09:15:00Z"}""")
            val closed =
                post(
                    "/shifts/$shiftId/clock-out",
                    employee,
                    """{"clientEventId":"e4","occurredAt":"2026-10-09T11:00:00Z"}""",
                ).json().jsonObject
            assertEquals("CLOSED", closed["status"]!!.jsonPrimitive.content)
            assertEquals(225, closed["workedMinutes"]!!.jsonPrimitive.content.toInt())
        }

    @Test
    fun the_admin_sees_inventory_invoices_and_staff() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            assertTrue(get("/materials", admin).json().jsonArray.size >= 5)
            assertTrue(get("/categories", admin).json().jsonArray.size >= 3)
            val page = get("/invoicing/invoices?page=0&size=50", admin).json().jsonObject
            assertTrue((page["items"] as JsonArray).size >= 4)
            assertEquals(
                "Granatum Flores SL",
                get("/invoicing/company", admin)
                    .json()
                    .jsonObject["legalName"]!!
                    .jsonPrimitive.content,
            )
            assertTrue(get("/employees", admin).json().jsonArray.size >= 4)
            val report = get("/invoicing/reports?period=YEARLY&year=2026", admin)
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
                        "$base/invoicing/invoices",
                        formData {
                            files.forEach {
                                append(
                                    "files",
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
                    .map { (it as JsonObject)["result"]!!.jsonPrimitive.content }

            assertEquals(listOf("ACCEPTED", "UNSUPPORTED_FORMAT"), upload(pdf, "hello".encodeToByteArray()))
            assertEquals(listOf("DUPLICATE"), upload(pdf))
        }

    @Test
    fun onboarding_gives_a_temporary_password_that_must_be_changed() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val created =
                post(
                    "/auth/onboarding",
                    admin,
                    """{"name":"Marta","lastName":"Gil Navarro","identityDocument":"12345678Z","position":"Florista","contractType":"PART_TIME","hireDate":"2026-10-09","email":"marta@demo.granatum.es","role":"EMPLOYEE"}""",
                )
            // That DNI is the admin's own record, which already has an account.
            assertEquals(HttpStatusCode.Conflict, created.status)

            val ok =
                post(
                    "/auth/onboarding",
                    admin,
                    """{"name":"Marta","lastName":"Gil Navarro","identityDocument":"00000001R","position":"Florista","contractType":"PART_TIME","hireDate":"2026-10-09","email":"marta@demo.granatum.es","role":"EMPLOYEE"}""",
                ).json().jsonObject
            val temporary = ok["temporaryPassword"]!!.jsonPrimitive.content
            val session = login("marta@demo.granatum.es", temporary).json().jsonObject
            assertEquals("true", session["passwordChangeRequired"]!!.jsonPrimitive.content)
            val marta = get("/employees/${ok["employeeId"]!!.jsonPrimitive.content}", admin).json().jsonObject
            assertEquals("Gil Navarro", marta["lastName"]!!.jsonPrimitive.content)
            assertEquals("false", marta["lastNamePending"]!!.jsonPrimitive.content)
        }

    @Test
    fun onboarding_without_last_names_is_refused_like_the_server() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            listOf("", ""","lastName":"   """", ""","lastName":null""").forEach { lastName ->
                val refused =
                    post(
                        "/auth/onboarding",
                        admin,
                        """{"name":"Marta"$lastName,"identityDocument":"00000001R","position":"Florista","contractType":"PART_TIME","hireDate":"2026-10-09","email":"marta@demo.granatum.es","role":"EMPLOYEE"}""",
                    )
                assertEquals(HttpStatusCode.BadRequest, refused.status)
                assertEquals("VALIDATION", refused.json().jsonObject["code"]!!.jsonPrimitive.content)
            }
        }

    @Test
    fun seeded_people_have_last_names_but_one_is_still_pending() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val people = get("/employees", admin).json().jsonArray.map { it.jsonObject }
            val pending = people.filter { it["lastNamePending"]!!.jsonPrimitive.content == "true" }
            assertEquals(1, pending.size)
            assertEquals(JsonNull, pending.single()["lastName"])
            val withLastNames = people - pending.toSet()
            assertTrue(withLastNames.all { it["lastName"]!!.jsonPrimitive.content.isNotBlank() })
            assertEquals(
                "Ferrer Montes",
                people.single { it["id"]!!.jsonPrimitive.content == DemoState.ADMIN_ID }["lastName"]!!.jsonPrimitive.content,
            )
        }

    @Test
    fun editing_a_pending_person_requires_and_saves_the_last_names() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val pending =
                get("/employees", admin).json().jsonArray.map { it.jsonObject }.single {
                    it["lastNamePending"]!!.jsonPrimitive.content == "true"
                }
            val id = pending["id"]!!.jsonPrimitive.content

            suspend fun edit(lastName: String) =
                http.put("$base/employees/$id") {
                    header(HttpHeaders.Authorization, "Bearer $admin")
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Elena","position":"Encargada de tienda","contractType":"FULL_TIME","hireDate":"2024-01-08"$lastName}""")
                }

            val refused = edit("")
            assertEquals(HttpStatusCode.BadRequest, refused.status)
            assertEquals("VALIDATION", refused.json().jsonObject["code"]!!.jsonPrimitive.content)

            val saved = edit(""","lastName":" Ríos Campos """").json().jsonObject
            assertEquals("Ríos Campos", saved["lastName"]!!.jsonPrimitive.content)
            assertEquals("false", saved["lastNamePending"]!!.jsonPrimitive.content)
        }

    @Test
    fun the_owner_sign_up_asks_for_last_names_too() =
        runTest {
            suspend fun signUp(body: String) =
                http.post("$base/auth/sign-up") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            val withoutLastName =
                signUp("""{"email":"a@b.es","password":"Clave-2026!","name":"Ana","identityDocument":"00000001R","bootstrapCode":"x"}""")
            assertEquals(HttpStatusCode.BadRequest, withoutLastName.status)
            assertEquals("VALIDATION", withoutLastName.json().jsonObject["code"]!!.jsonPrimitive.content)
            // The demo installation already has its owner.
            val complete =
                signUp(
                    """{"email":"a@b.es","password":"Clave-2026!","name":"Ana","lastName":"Martín","identityDocument":"00000001R","bootstrapCode":"x"}""",
                )
            assertEquals(HttpStatusCode.Forbidden, complete.status)
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

        assertEquals(HttpStatusCode.OK, get("/employees", oldToken).status)
        assertEquals(HttpStatusCode.Unauthorized, get("/employees", "not-a-demo-token").status)
    }

    @Test
    fun a_material_image_is_uploaded_to_the_server_and_read_from_its_signed_url() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val id = get("/materials", admin).json().jsonArray.first().jsonObject["id"]!!.jsonPrimitive.content
            val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 1, 2)
            fun part(bytes: ByteArray) =
                formData {
                    append("image", bytes, Headers.build { append(HttpHeaders.ContentDisposition, "filename=\"a.png\"") })
                }
            val uploaded =
                http.put("$base/materials/$id/image") {
                    header(HttpHeaders.Authorization, "Bearer $admin")
                    setBody(MultiPartFormDataContent(part(png)))
                }
            assertEquals(HttpStatusCode.OK, uploaded.status)
            val url = uploaded.json().jsonObject["imageUrl"]!!.jsonPrimitive.content
            assertTrue("/storage/v1/object/sign/" in url, url)
            assertEquals(png.toList(), http.get(url).readRawBytes().toList())

            val notAnImage =
                http.put("$base/materials/$id/image") {
                    header(HttpHeaders.Authorization, "Bearer $admin")
                    setBody(MultiPartFormDataContent(part("hello".encodeToByteArray())))
                }
            assertEquals(HttpStatusCode.UnsupportedMediaType, notAnImage.status)

            val removed = http.delete("$base/materials/$id/image") { header(HttpHeaders.Authorization, "Bearer $admin") }
            assertEquals(JsonNull, removed.json().jsonObject["imageUrl"])
            assertEquals(HttpStatusCode.NotFound, http.get(url).status)
        }

    @Test
    fun materials_follow_the_010_contract() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val first = get("/materials", admin).json().jsonArray.first().jsonObject
            listOf("description", "dimensions", "quantity", "location", "createdAt").forEach { assertTrue(it in first, it) }
            assertEquals(2, get("/materials?warehouse=B", admin).json().jsonArray.size)
            assertEquals(HttpStatusCode.BadRequest, get("/materials?section=2", admin).status)
            val category = first["category"]!!.jsonObject["id"]!!.jsonPrimitive.content
            val zeroWidth = """{"categoryId":"$category","description":"x","dimensions":{"height":1,"width":0},"quantity":1,"location":{"warehouse":"A","section":1}}"""
            assertEquals(HttpStatusCode.BadRequest, post("/materials", admin, zeroWidth).status)
            val noReason = http.patch("$base/materials/${first["id"]!!.jsonPrimitive.content}/quantity") {
                header(HttpHeaders.Authorization, "Bearer $admin")
                contentType(ContentType.Application.Json)
                setBody("""{"quantity":3,"reason":" "}""")
            }
            assertEquals(HttpStatusCode.BadRequest, noReason.status)
        }

    @Test
    fun seeded_products_come_with_an_illustration() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            val urls = get("/materials", admin).json().jsonArray.map { it.jsonObject["imageUrl"]!!.jsonPrimitive.content }
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
            val material = get("/events/materials", employee).json().jsonArray.map { it.jsonObject }
                .first { it["description"]!!.jsonPrimitive.content.startsWith("Lazo") }
            val id = material["id"]!!.jsonPrimitive.content
            val free = material["quantity"]!!.jsonPrimitive.content.toInt()

            val created = post("/events", employee, """{"name":"Evento 9","description":"Prueba","date":"2099-01-01"}""")
            assertEquals(HttpStatusCode.Created, created.status)
            val event = created.json().jsonObject["id"]!!.jsonPrimitive.content

            val reserved =
                http.put("$base/events/$event/materials/$id") {
                    header(HttpHeaders.Authorization, "Bearer $employee")
                    contentType(ContentType.Application.Json)
                    setBody("""{"quantity":2}""")
                }
            assertEquals(HttpStatusCode.OK, reserved.status)
            val after = get("/events/materials", employee).json().jsonArray.map { it.jsonObject }.first { it["id"]!!.jsonPrimitive.content == id }
            assertEquals(free - 2, after["quantity"]!!.jsonPrimitive.content.toInt())

            val tooMany =
                http.put("$base/events/$event/materials/$id") {
                    header(HttpHeaders.Authorization, "Bearer $employee")
                    contentType(ContentType.Application.Json)
                    setBody("""{"quantity":${free + 5}}""")
                }
            assertEquals(HttpStatusCode.Conflict, tooMany.status)
            assertEquals("INSUFFICIENT_STOCK", tooMany.json().jsonObject["code"]!!.jsonPrimitive.content)
            assertEquals((free - 2).toString(), tooMany.json().jsonObject["available"]!!.jsonPrimitive.content)

            val delete = http.delete("$base/materials/$id") { header(HttpHeaders.Authorization, "Bearer $admin") }
            assertEquals(HttpStatusCode.Conflict, delete.status)
            assertEquals(HttpStatusCode.Forbidden, get("/materials", employee).status)

            assertEquals(HttpStatusCode.OK, post("/events/$event/finish", employee, "{}").status)
            val back = get("/events/materials", employee).json().jsonArray.map { it.jsonObject }.first { it["id"]!!.jsonPrimitive.content == id }
            assertEquals(free, back["quantity"]!!.jsonPrimitive.content.toInt())
        }

    @Test
    fun `When the demo starts then it has upcoming and finished events with reserved material`() =
        runTest {
            val events = get("/events", token(DemoAccountsCatalog.EMPLOYEE)).json().jsonArray.map { it.jsonObject }
            assertTrue(events.any { it["status"]!!.jsonPrimitive.content == "SCHEDULED" && it["materials"]!!.jsonArray.isNotEmpty() })
            assertTrue(events.any { it["status"]!!.jsonPrimitive.content == "FINISHED" })
        }

    @Test
    fun a_weak_new_password_is_refused_with_its_requirements() =
        runTest {
            val employee = token(DemoAccountsCatalog.EMPLOYEE)
            val weak =
                post(
                    "/auth/change-password",
                    employee,
                    """{"currentPassword":"${DemoAccountsCatalog.EMPLOYEE.password}","newPassword":"abc"}""",
                )
            assertEquals(HttpStatusCode.UnprocessableEntity, weak.status)
            val body = weak.json().jsonObject
            assertEquals("WEAK_PASSWORD", body["code"]!!.jsonPrimitive.content)
            assertTrue("MIN_LENGTH" in body["requirements"]!!.jsonArray.map { it.jsonPrimitive.content })
            assertEquals(HttpStatusCode.BadRequest, http.post("$base/auth/sign-up") { contentType(ContentType.Application.Json); setBody("{}") }.status)
        }

    @Test
    fun a_correction_is_requested_and_approved_by_the_admin() =
        runTest {
            val employee = token(DemoAccountsCatalog.EMPLOYEE)
            val admin = token(DemoAccountsCatalog.ADMIN)
            val shift = get("/shifts/employee/${DemoState.EMPLOYEE_ID}", employee).json().jsonArray.first().jsonObject
            val id = shift["id"]!!.jsonPrimitive.content
            val clockIn = shift["clockIn"]!!.jsonPrimitive.content
            val clockOut = shift["clockOut"]!!.jsonPrimitive.content
            val requested =
                post(
                    "/shifts/$id/corrections",
                    employee,
                    """{"reason":"Olvidé fichar","proposedValues":{"clockIn":"$clockIn","clockOut":"$clockOut","breaks":[]}}""",
                )
            assertEquals(HttpStatusCode.Created, requested.status)
            val correction = requested.json().jsonObject["id"]!!.jsonPrimitive.content
            assertEquals(1, get("/corrections?status=PENDING", admin).json().jsonArray.size)
            val approved = post("/corrections/$correction/approve", admin, "{}").json().jsonObject
            assertEquals("APPROVED", approved["status"]!!.jsonPrimitive.content)
        }
}
