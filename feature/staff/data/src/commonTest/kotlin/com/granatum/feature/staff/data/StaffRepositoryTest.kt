package com.granatum.feature.staff.data

import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.util.Result
import com.granatum.feature.staff.domain.ContractType
import com.granatum.feature.staff.domain.Onboarding
import com.granatum.feature.staff.domain.StaffEdit
import com.granatum.feature.staff.domain.StaffError
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
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** specs/009-personal, contracts/personal-api.md. */
class StaffRepositoryTest {
    private val sent = mutableListOf<Triple<String, String, String>>()

    private fun repo(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        KtorStaffRepository(
            HttpClient(
                MockEngine { request ->
                    sent +=
                        Triple(
                            request.method.value,
                            request.url.encodedPath.substringAfter("/api"),
                            request.body.toByteArray().decodeToString(),
                        )
                    handler(request)
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                defaultRequest { contentType(ContentType.Application.Json) }
            },
        )

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val ana = """{"id":"e1","name":"Ana","identityDocument":"12345678Z","position":"Florista","contractType":"PART_TIME","hireDate":"2026-10-01","active":true}"""

    @Test
    fun the_list_reads_every_field() =
        runTest {
            val members = (repo { json("[$ana]") }.members() as Result.Success).data
            val member = members.single()
            assertEquals(ContractType.PART_TIME, member.contract)
            assertEquals(LocalDate(2026, 10, 1), member.startDate)
            assertEquals("/employees", sent.single().second)
        }

    @Test
    fun an_edit_never_sends_the_identity_document() =
        runTest {
            repo { json(ana) }.update("e1", StaffEdit(" Ana Martín ", "Encargada de tienda", ContractType.FULL_TIME, LocalDate(2026, 9, 1)))
            val (method, path, body) = sent.single()
            assertEquals("PUT /employees/e1", "$method $path")
            assertEquals(
                """{"name":"Ana Martín","position":"Encargada de tienda","contractType":"FULL_TIME","hireDate":"2026-09-01"}""",
                body,
            )
        }

    @Test
    fun activation_and_reset_use_their_routes() =
        runTest {
            val repository =
                repo { request ->
                    if (request.url.encodedPath.endsWith("reset")) {
                        json(
                            """{"accountId":"c1","employeeId":"e1","email":"ana@granatum.es","role":"EMPLOYEE","temporaryPassword":"Tmp-1234-abcd"}""",
                        )
                    } else {
                        json(ana.replace("true", "false"))
                    }
                }
            assertFalse((repository.setActive("e1", false) as Result.Success).data.active)
            assertEquals("PATCH /employees/e1/active" to """{"active":false}""", "${sent[0].first} ${sent[0].second}" to sent[0].third)
            val credentials = (repository.resetPassword("e1") as Result.Success).data
            assertEquals("Tmp-1234-abcd", credentials.password)
            assertEquals("/auth/accounts/e1/reset", sent[1].second)
        }

    @Test
    fun onboarding_sends_record_and_account_together() =
        runTest {
            val credentials =
                (
                    repo {
                        json(
                            """{"employeeId":"e9","accountId":"c9","email":"luis@granatum.es","role":"MANAGER","staffRecordCreated":false,"temporaryPassword":"Tmp-9"}""",
                            HttpStatusCode.Created,
                        )
                    }.onboard(
                        Onboarding(
                            "Luis",
                            "X1234567L",
                            "Encargado",
                            ContractType.HOURLY,
                            LocalDate(2026, 10, 9),
                            " Luis@Granatum.es ",
                            UserRole.MANAGER,
                        ),
                    ) as Result.Success
                ).data
            assertEquals(
                """{"name":"Luis","identityDocument":"X1234567L","position":"Encargado","contractType":"HOURLY","hireDate":"2026-10-09","email":"luis@granatum.es","role":"MANAGER"}""",
                sent.single().third,
            )
            assertEquals("/auth/onboarding", sent.single().second)
            assertEquals(UserRole.MANAGER, credentials.role)
            assertFalse(credentials.recordCreated)
            assertEquals("TemporaryCredentials(email=luis@granatum.es, role=MANAGER, password=***)", credentials.toString())
        }

    @Test
    fun granting_access_sends_the_record_email_and_role() =
        runTest {
            repo {
                json(
                    """{"accountId":"c1","employeeId":"e1","email":"a@b.es","role":"EMPLOYEE","temporaryPassword":"x"}""",
                    HttpStatusCode.Created,
                )
            }.grantAccess("e1", "A@b.es", UserRole.EMPLOYEE)
            assertEquals("""{"employeeId":"e1","email":"a@b.es","role":"EMPLOYEE"}""", sent.single().third)
        }

    @Test
    fun server_codes_become_typed_errors() =
        runTest {
            suspend fun errorFor(
                code: String,
                status: HttpStatusCode,
            ) = (repo { json("""{"code":"$code","message":"m"}""", status) }.resetPassword("e1") as Result.Failure).error
            assertEquals(StaffError.NoAccount, errorFor("ACCOUNT_NOT_FOUND", HttpStatusCode.NotFound))
            assertEquals(StaffError.InvalidDocument, errorFor("INVALID_DOCUMENT", HttpStatusCode.UnprocessableEntity))
            assertEquals(StaffError.EmailTaken, errorFor("EMAIL_ALREADY_REGISTERED", HttpStatusCode.Conflict))
            assertEquals(StaffError.AccountExists, errorFor("ACCOUNT_ALREADY_EXISTS", HttpStatusCode.Conflict))
            assertEquals(StaffError.NotFound, errorFor("EMPLOYEE_NOT_FOUND", HttpStatusCode.NotFound))
            assertEquals(StaffError.DuplicateDocument, errorFor("DUPLICATE_DOCUMENT", HttpStatusCode.Conflict))
            assertEquals(StaffError.Invalid, errorFor("VALIDATION", HttpStatusCode.BadRequest))
            assertEquals(StaffError.Forbidden, errorFor("FORBIDDEN", HttpStatusCode.Forbidden))
        }
}
