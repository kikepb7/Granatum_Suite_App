package com.granatum.core.demo

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The demo imitates `/api/sheets` as the backend spec 012 contract describes it. */
class DemoSheetsRoutesTest {
    private val state = DemoState()
    private val backend = DemoBackend(state)
    private val http = HttpClient(MockEngine { request -> backend.handle(this, request) })
    private val base = "http://demo/api"

    private suspend fun token(account: com.granatum.core.domain.auth.model.DemoAccount): String {
        val login =
            http.post("$base/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"${account.email}","password":"${account.password}"}""")
            }
        return Json
            .parseToJsonElement(login.bodyAsText())
            .jsonObject["accessToken"]!!
            .jsonPrimitive.content
    }

    private suspend fun get(
        path: String,
        token: String,
    ) = http.get("$base$path") { header(HttpHeaders.Authorization, "Bearer $token") }

    private suspend fun post(
        path: String,
        token: String,
    ) = http.post("$base$path") { header(HttpHeaders.Authorization, "Bearer $token") }

    private suspend fun putSettings(
        token: String,
        shifts: String,
        invoices: String,
    ) = http.put("$base/sheets/settings") {
        header(HttpHeaders.Authorization, "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody("""{"shiftsSpreadsheet":"$shifts","invoicesSpreadsheet":"$invoices"}""")
    }

    private suspend fun HttpResponse.obj(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

    private fun JsonObject.text(key: String) = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

    private val today get() = state.today()
    private val currentPath get() = "${today.year}/${today.month.number}"

    @Test
    fun the_settings_start_available_with_the_demo_account_and_no_spreadsheets() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)

            val actual = get("/sheets/settings", admin).obj()

            assertEquals("true", actual.text("available"))
            assertEquals("granatum-demo@demo.iam.gserviceaccount.com", actual.text("serviceAccountEmail"))
            assertEquals(null, actual.text("shiftsSpreadsheetId"))
            assertEquals(null, actual.text("invoicesSpreadsheetUrl"))
        }

    @Test
    fun only_an_admin_reaches_the_sheets_routes() =
        runTest {
            val employee = token(DemoAccountsCatalog.EMPLOYEE)

            assertEquals(HttpStatusCode.Forbidden, get("/sheets/settings", employee).status)
            assertEquals(HttpStatusCode.Forbidden, get("/sheets/status", employee).status)
        }

    @Test
    fun saving_two_links_keeps_their_ids_and_links() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)

            val saved = putSettings(admin, "https://docs.google.com/spreadsheets/d/1AbCdEf/edit#gid=0", "1XyZaBc")

            assertEquals(HttpStatusCode.OK, saved.status)
            val actual = get("/sheets/settings", admin).obj()
            assertEquals("1AbCdEf", actual.text("shiftsSpreadsheetId"))
            assertEquals("https://docs.google.com/spreadsheets/d/1XyZaBc/edit", actual.text("invoicesSpreadsheetUrl"))
            assertTrue(actual.text("configuredAt") != null)
        }

    @Test
    fun a_link_that_is_not_shared_or_read_only_is_refused_on_its_spreadsheet() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)

            val notShared = putSettings(admin, "https://docs.google.com/spreadsheets/d/no-compartida/edit", "1XyZaBc")
            val readOnly = putSettings(admin, "1AbCdEf", "https://docs.google.com/spreadsheets/d/solo-lectura/edit")

            assertEquals(HttpStatusCode.UnprocessableEntity, notShared.status)
            with(notShared.obj()) {
                assertEquals("SPREADSHEET_NOT_ACCESSIBLE", text("code"))
                assertEquals("shifts", text("spreadsheet"))
                assertEquals("NOT_SHARED", text("reason"))
            }
            with(readOnly.obj()) {
                assertEquals("invoices", text("spreadsheet"))
                assertEquals("READ_ONLY", text("reason"))
            }
            assertEquals(null, get("/sheets/settings", admin).obj().text("shiftsSpreadsheetId"))
        }

    @Test
    fun the_same_spreadsheet_twice_or_an_invalid_link_is_refused() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)

            val same = putSettings(admin, "https://docs.google.com/spreadsheets/d/1AbCdEf/edit", "1AbCdEf")
            val invalid = putSettings(admin, "", "1XyZaBc")

            assertEquals(HttpStatusCode.Conflict, same.status)
            assertEquals("SAME_SPREADSHEET", same.obj().text("code"))
            assertEquals(HttpStatusCode.BadRequest, invalid.status)
            assertEquals("VALIDATION", invalid.obj().text("code"))
        }

    @Test
    fun exporting_without_a_spreadsheet_asks_to_configure_it_and_future_months_are_refused() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)

            val notConfigured = post("/sheets/shifts/$currentPath", admin)
            putSettings(admin, "1AbCdEf", "1XyZaBc")
            val future = post("/sheets/shifts/${today.year + 1}/1", admin)
            val noMonth = post("/sheets/shifts/${today.year}/13", admin)

            assertEquals(HttpStatusCode.Conflict, notConfigured.status)
            assertEquals("SHEETS_NOT_CONFIGURED", notConfigured.obj().text("code"))
            assertEquals("INVALID_PERIOD", future.obj().text("code"))
            assertEquals(HttpStatusCode.BadRequest, noMonth.status)
        }

    @Test
    fun an_exported_month_is_done_on_the_next_read_with_its_rows_and_pending_last_names() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            putSettings(admin, "1AbCdEf", "1XyZaBc")
            val expectedRows =
                state.shifts.count {
                    val day = it.clockIn.toLocalDateTime(state.madrid).date
                    day.year == today.year && day.month == today.month
                }

            val created = post("/sheets/shifts/$currentPath", admin)
            val taskId = created.obj().text("taskId")!!
            val task = get("/sheets/tasks/$taskId", admin).obj()

            assertEquals(HttpStatusCode.Accepted, created.status)
            assertEquals("SHIFT_MONTH", task.text("kind"))
            assertEquals("${today.year}-${today.month.number.toString().padStart(2, '0')}", task.text("reference"))
            assertEquals("DONE", task.text("status"))
            assertEquals(expectedRows, task["rowCount"]!!.jsonPrimitive.int)
            // Elena Ríos was onboarded before last names existed.
            assertEquals(1, task["lastNamesPending"]!!.jsonPrimitive.int)
            assertTrue(get("/sheets/status", admin).obj().text("lastSuccessAt") != null)
        }

    @Test
    fun a_missing_task_is_not_found() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)

            val actual = get("/sheets/tasks/nope", admin)

            assertEquals(HttpStatusCode.NotFound, actual.status)
            assertEquals("TASK_NOT_FOUND", actual.obj().text("code"))
        }

    @Test
    fun the_status_starts_with_a_failed_receipt_write_that_can_be_retried() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)

            val status = get("/sheets/status", admin).obj()
            val failed = status["recent"]!!.jsonArray.single().jsonObject
            val id = failed.text("id")!!

            assertEquals("1", status.text("failed"))
            assertEquals("FAILED", failed.text("status"))
            assertEquals("INVOICE", failed.text("kind"))
            assertEquals("La hoja está compartida con la cuenta técnica de Google solo como lectora", failed.text("lastError"))

            val retried = post("/sheets/tasks/$id/retry", admin)
            assertEquals(HttpStatusCode.Accepted, retried.status)
            assertEquals("PENDING", retried.obj().text("status"))
            assertEquals("TASK_NOT_FAILED", post("/sheets/tasks/$id/retry", admin).obj().text("code"))

            val after = get("/sheets/status", admin).obj()
            assertEquals("0", after.text("failed"))
            assertEquals(
                "DONE",
                after["recent"]!!
                    .jsonArray
                    .single()
                    .jsonObject
                    .text("status"),
            )
        }

    @Test
    fun retrying_all_failed_says_how_many_went_back_to_the_queue() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)

            val actual = post("/sheets/tasks/retry-failed", admin)

            assertEquals(HttpStatusCode.Accepted, actual.status)
            assertEquals("1", actual.obj().text("requeued"))
            assertEquals("0", post("/sheets/tasks/retry-failed", admin).obj().text("requeued"))
        }

    @Test
    fun resyncing_a_month_of_receipts_rewrites_it_with_its_confirmed_invoices() =
        runTest {
            val admin = token(DemoAccountsCatalog.ADMIN)
            putSettings(admin, "1AbCdEf", "1XyZaBc")

            val created = post("/sheets/invoices/$currentPath/resync", admin)
            val task = get("/sheets/tasks/${created.obj().text("taskId")}", admin).obj()

            assertEquals(HttpStatusCode.Accepted, created.status)
            assertEquals("INVOICE_MONTH", task.text("kind"))
            assertEquals("DONE", task.text("status"))
            assertTrue(task["rowCount"]!!.jsonPrimitive.int >= 0)
        }
}
