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

    private val ana = """{"id":"e1","nombre":"Ana","documentoIdentidad":"12345678Z","puesto":"Florista","tipoContrato":"PARCIAL","fechaAlta":"2026-10-01","activo":true}"""

    @Test
    fun the_list_reads_every_field() =
        runTest {
            val members = (repo { json("[$ana]") }.members() as Result.Success).data
            val member = members.single()
            assertEquals(ContractType.PART_TIME, member.contract)
            assertEquals(LocalDate(2026, 10, 1), member.startDate)
            assertEquals("/empleados", sent.single().second)
        }

    @Test
    fun an_edit_never_sends_the_identity_document() =
        runTest {
            repo { json(ana) }.update("e1", StaffEdit(" Ana Martín ", "Encargada de tienda", ContractType.FULL_TIME, LocalDate(2026, 9, 1)))
            val (method, path, body) = sent.single()
            assertEquals("PUT /empleados/e1", "$method $path")
            assertEquals(
                """{"nombre":"Ana Martín","puesto":"Encargada de tienda","tipoContrato":"JORNADA_COMPLETA","fechaAlta":"2026-09-01"}""",
                body,
            )
        }

    @Test
    fun activation_and_reset_use_their_routes() =
        runTest {
            val repository =
                repo { request ->
                    if (request.url.encodedPath.endsWith("restablecer")) {
                        json(
                            """{"cuentaId":"c1","empleadoId":"e1","email":"ana@granatum.es","rol":"EMPLEADO","passwordTemporal":"Tmp-1234-abcd"}""",
                        )
                    } else {
                        json(ana.replace("true", "false"))
                    }
                }
            assertFalse((repository.setActive("e1", false) as Result.Success).data.active)
            assertEquals("PATCH /empleados/e1/activo" to """{"activo":false}""", "${sent[0].first} ${sent[0].second}" to sent[0].third)
            val credentials = (repository.resetPassword("e1") as Result.Success).data
            assertEquals("Tmp-1234-abcd", credentials.password)
            assertEquals("/auth/cuentas/e1/restablecer", sent[1].second)
        }

    @Test
    fun onboarding_sends_record_and_account_together() =
        runTest {
            val credentials =
                (
                    repo {
                        json(
                            """{"empleadoId":"e9","cuentaId":"c9","email":"luis@granatum.es","rol":"ENCARGADO","fichaCreada":false,"passwordTemporal":"Tmp-9"}""",
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
                            UserRole.ENCARGADO,
                        ),
                    ) as Result.Success
                ).data
            assertEquals(
                """{"nombre":"Luis","documentoIdentidad":"X1234567L","puesto":"Encargado","tipoContrato":"POR_HORAS","fechaAlta":"2026-10-09","email":"luis@granatum.es","rol":"ENCARGADO"}""",
                sent.single().third,
            )
            assertEquals("/auth/altas", sent.single().second)
            assertEquals(UserRole.ENCARGADO, credentials.role)
            assertFalse(credentials.recordCreated)
            assertEquals("TemporaryCredentials(email=luis@granatum.es, role=ENCARGADO, password=***)", credentials.toString())
        }

    @Test
    fun granting_access_sends_the_record_email_and_role() =
        runTest {
            repo {
                json(
                    """{"cuentaId":"c1","empleadoId":"e1","email":"a@b.es","rol":"EMPLEADO","passwordTemporal":"x"}""",
                    HttpStatusCode.Created,
                )
            }.grantAccess("e1", "A@b.es", UserRole.EMPLEADO)
            assertEquals("""{"empleadoId":"e1","email":"a@b.es","rol":"EMPLEADO"}""", sent.single().third)
        }

    @Test
    fun server_codes_become_typed_errors() =
        runTest {
            suspend fun errorFor(
                code: String,
                status: HttpStatusCode,
            ) = (repo { json("""{"code":"$code","message":"m"}""", status) }.resetPassword("e1") as Result.Failure).error
            assertEquals(StaffError.NoAccount, errorFor("CUENTA_NO_ENCONTRADA", HttpStatusCode.NotFound))
            assertEquals(StaffError.InvalidDocument, errorFor("DOCUMENTO_INVALIDO", HttpStatusCode.UnprocessableEntity))
            assertEquals(StaffError.EmailTaken, errorFor("EMAIL_YA_REGISTRADO", HttpStatusCode.Conflict))
            assertEquals(StaffError.AccountExists, errorFor("CUENTA_YA_EXISTE", HttpStatusCode.Conflict))
            assertEquals(StaffError.NotFound, errorFor("EMPLEADO_NOT_FOUND", HttpStatusCode.NotFound))
            assertEquals(StaffError.Forbidden, errorFor("FORBIDDEN", HttpStatusCode.Forbidden))
        }
}
