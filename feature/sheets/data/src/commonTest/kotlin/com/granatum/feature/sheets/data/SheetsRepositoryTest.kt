package com.granatum.feature.sheets.data

import com.granatum.core.domain.util.Result
import com.granatum.feature.sheets.data.remote.SheetsRemoteDataSource
import com.granatum.feature.sheets.data.repository.SheetsRepositoryImpl
import com.granatum.feature.sheets.domain.model.SheetAccessProblem
import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.model.SheetTaskKind
import com.granatum.feature.sheets.domain.model.SheetTaskStatus
import com.granatum.feature.sheets.domain.model.SheetsError
import com.granatum.feature.sheets.domain.model.SpreadsheetField
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** The app's side of the backend spec 012 contract: the `/api/sheets` routes. */
class SheetsRepositoryTest {
    private val requests = mutableListOf<Pair<String, String>>()

    private fun sut(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): SheetsRepositoryImpl {
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += "${request.method.value} ${request.url.encodedPath.substringAfter("/api")}" to
                        request.body.toByteArray().decodeToString()
                    handler(request)
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                defaultRequest { contentType(ContentType.Application.Json) }
            }
        return SheetsRepositoryImpl(SheetsRemoteDataSource(client))
    }

    private fun MockRequestHandleScope.ok(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.fail(
        status: HttpStatusCode,
        code: String,
        extra: String = "",
    ) = ok("""{"code":"$code","message":"m"$extra}""", status)

    private val configured =
        """{"available":true,"serviceAccountEmail":"granatum@p.iam.gserviceaccount.com",""" +
            """"shiftsSpreadsheetId":"1AbC","shiftsSpreadsheetUrl":"https://docs.google.com/spreadsheets/d/1AbC/edit",""" +
            """"invoicesSpreadsheetId":"1XyZ","invoicesSpreadsheetUrl":"https://docs.google.com/spreadsheets/d/1XyZ/edit",""" +
            """"configuredAt":"2026-10-18T10:00:00Z","configuredBy":"u1","somethingNew":1}"""

    private fun task(
        status: String = "DONE",
        error: String? = null,
    ) = """{"id":"t1","kind":"SHIFT_MONTH","reference":"2026-10","status":"$status","attempts":1,"rowCount":92,""" +
        """"lastNamesPending":1,"lastError":${error?.let { "\"$it\"" } ?: "null"},"createdAt":"2026-10-18T10:00:00Z",""" +
        """"updatedAt":"2026-10-18T10:00:05Z","completedAt":"2026-10-18T10:00:05Z"}"""

    @Test
    fun `When the settings are configured then both spreadsheets come with their links`() =
        runTest {
            val actual = (sut { ok(configured) }.settings() as Result.Success).data

            assertTrue(actual.available)
            assertEquals("granatum@p.iam.gserviceaccount.com", actual.serviceAccountEmail)
            assertEquals("1AbC", actual.shifts?.id)
            assertEquals("https://docs.google.com/spreadsheets/d/1XyZ/edit", actual.invoices?.url)
            assertEquals(Instant.parse("2026-10-18T10:00:00Z"), actual.configuredAt)
            assertEquals("GET /sheets/settings", requests.single().first)
        }

    @Test
    fun `When the server has no credentials then the settings are unavailable and empty`() =
        runTest {
            val body =
                """{"available":false,"serviceAccountEmail":null,"shiftsSpreadsheetId":null,"shiftsSpreadsheetUrl":null,""" +
                    """"invoicesSpreadsheetId":null,"invoicesSpreadsheetUrl":null,"configuredAt":null,"configuredBy":null}"""
            val actual = (sut { ok(body) }.settings() as Result.Success).data

            assertFalse(actual.available)
            assertNull(actual.serviceAccountEmail)
            assertNull(actual.shifts)
            assertFalse(actual.isConfigured)
        }

    @Test
    fun `When the settings are saved then both links are sent and the saved settings come back`() =
        runTest {
            val actual = sut { ok(configured) }.saveSettings("https://docs.google.com/spreadsheets/d/1AbC/edit#gid=0", "1XyZ")

            assertTrue((actual as Result.Success).data.isConfigured)
            val (line, body) = requests.single()
            assertEquals("PUT /sheets/settings", line)
            assertEquals(
                """{"shiftsSpreadsheet":"https://docs.google.com/spreadsheets/d/1AbC/edit#gid=0","invoicesSpreadsheet":"1XyZ"}""",
                body,
            )
        }

    @Test
    fun `When a spreadsheet is not accessible then the error names the field and the reason`() =
        runTest {
            val actual =
                sut {
                    fail(
                        HttpStatusCode.UnprocessableEntity,
                        "SPREADSHEET_NOT_ACCESSIBLE",
                        ""","spreadsheet":"invoices","reason":"READ_ONLY"""",
                    )
                }.saveSettings("a", "b")

            assertEquals(
                SheetsError.NotAccessible(SpreadsheetField.INVOICES, SheetAccessProblem.READ_ONLY),
                (actual as Result.Failure).error,
            )
        }

    @Test
    fun `When the shifts spreadsheet is not shared then the error is on the shifts field`() =
        runTest {
            val actual =
                sut {
                    fail(
                        HttpStatusCode.UnprocessableEntity,
                        "SPREADSHEET_NOT_ACCESSIBLE",
                        ""","spreadsheet":"shifts","reason":"NOT_SHARED"""",
                    )
                }.saveSettings("a", "b")

            assertEquals(
                SheetsError.NotAccessible(SpreadsheetField.SHIFTS, SheetAccessProblem.NOT_SHARED),
                (actual as Result.Failure).error,
            )
        }

    @Test
    fun `The other settings errors map to their own types`() =
        runTest {
            val cases =
                listOf(
                    Triple(HttpStatusCode.Conflict, "SAME_SPREADSHEET", SheetsError.SameSpreadsheet),
                    Triple(HttpStatusCode.ServiceUnavailable, "SHEETS_UNAVAILABLE", SheetsError.Unavailable),
                    Triple(HttpStatusCode.BadGateway, "SHEETS_UNREACHABLE", SheetsError.Unreachable),
                    Triple(HttpStatusCode.BadRequest, "VALIDATION", SheetsError.InvalidLink),
                    Triple(HttpStatusCode.Forbidden, "FORBIDDEN", SheetsError.Forbidden),
                )
            cases.forEach { (status, code, expected) ->
                val actual = sut { fail(status, code) }.saveSettings("a", "b")
                assertEquals(expected, (actual as Result.Failure).error, code)
            }
        }

    @Test
    fun `When a month is exported then the task id of the 202 comes back`() =
        runTest {
            val actual = sut { ok("""{"taskId":"t1"}""", HttpStatusCode.Accepted) }.exportShiftMonth(SheetMonth(2026, 3))

            assertEquals("t1", (actual as Result.Success).data)
            assertEquals("POST /sheets/shifts/2026/3", requests.single().first)
        }

    @Test
    fun `The export errors map to their own types`() =
        runTest {
            val notConfigured = sut { fail(HttpStatusCode.Conflict, "SHEETS_NOT_CONFIGURED") }.exportShiftMonth(SheetMonth(2026, 10))
            val invalid = sut { fail(HttpStatusCode.BadRequest, "INVALID_PERIOD") }.exportShiftMonth(SheetMonth(2030, 1))

            assertEquals(SheetsError.NotConfigured, (notConfigured as Result.Failure).error)
            assertEquals(SheetsError.InvalidPeriod, (invalid as Result.Failure).error)
        }

    @Test
    fun `When the receipts of a month are resynced then the resync route is called`() =
        runTest {
            val actual = sut { ok("""{"taskId":"t9"}""", HttpStatusCode.Accepted) }.resyncInvoiceMonth(SheetMonth(2026, 9))

            assertEquals("t9", (actual as Result.Success).data)
            assertEquals("POST /sheets/invoices/2026/9/resync", requests.single().first)
        }

    @Test
    fun `When a task is read then its rows and pending last names come back`() =
        runTest {
            val actual = (sut { ok(task()) }.task("t1") as Result.Success).data

            assertEquals(SheetTaskKind.SHIFT_MONTH, actual.kind)
            assertEquals(SheetTaskStatus.DONE, actual.status)
            assertEquals(92, actual.rowCount)
            assertEquals(1, actual.lastNamesPending)
            assertEquals(SheetMonth(2026, 10), actual.month)
            assertEquals("GET /sheets/tasks/t1", requests.single().first)
        }

    @Test
    fun `When a task does not exist then the error is task not found`() =
        runTest {
            val actual = sut { fail(HttpStatusCode.NotFound, "TASK_NOT_FOUND") }.task("t1")

            assertEquals(SheetsError.TaskNotFound, (actual as Result.Failure).error)
        }

    @Test
    fun `When the status is read then the failed tasks come with their reason`() =
        runTest {
            val body =
                """{"lastSuccessAt":"2026-10-18T09:00:00Z","pending":2,"failed":1,""" +
                    """"recent":[${task("FAILED", "La hoja está compartida con la cuenta técnica de Google solo como lectora")},${task(
                        "PENDING",
                    )}]}"""
            val actual = (sut { ok(body) }.status() as Result.Success).data

            assertEquals(Instant.parse("2026-10-18T09:00:00Z"), actual.lastSuccessAt)
            assertEquals(2, actual.pending)
            assertEquals(1, actual.failed)
            assertEquals(
                com.granatum.feature.sheets.domain.model.SheetFailureReason.READ_ONLY,
                actual.failedTasks
                    .single()
                    .failureReason,
            )
        }

    @Test
    fun `When a failed task is retried then the requeued task comes back`() =
        runTest {
            val actual = sut { ok(task("PENDING"), HttpStatusCode.Accepted) }.retry("t1")

            assertEquals(SheetTaskStatus.PENDING, (actual as Result.Success).data.status)
            assertEquals("POST /sheets/tasks/t1/retry", requests.single().first)
        }

    @Test
    fun `When a task that has not failed is retried then the error says so`() =
        runTest {
            val actual = sut { fail(HttpStatusCode.Conflict, "TASK_NOT_FAILED") }.retry("t1")

            assertEquals(SheetsError.TaskNotFailed, (actual as Result.Failure).error)
        }

    @Test
    fun `When all failed tasks are retried then the number requeued comes back`() =
        runTest {
            val actual = sut { ok("""{"requeued":3}""", HttpStatusCode.Accepted) }.retryAllFailed()

            assertEquals(3, (actual as Result.Success).data)
            assertEquals("POST /sheets/tasks/retry-failed", requests.single().first)
        }
}
