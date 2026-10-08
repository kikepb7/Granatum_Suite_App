package com.granatum.core.data.auth

import com.granatum.core.data.testing.AuthTestHarness
import com.granatum.core.data.testing.TestTokens
import com.granatum.core.data.testing.errorJson
import com.granatum.core.data.testing.json
import com.granatum.core.data.testing.tokensJson
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.OwnerRegistration
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.util.Result
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorAuthRepositoryImplTest {

    private fun OutgoingContent.text() = (this as TextContent).text

    @Test
    fun login_sends_the_trimmed_email_and_the_password_as_typed_and_stores_the_session() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.OK, tokensJson(TestTokens.access(sub = "e-7", role = "ADMIN"), "r1")) }

        val result = h.repository.login("  Ana@Granatum.es ", " pass word ")

        val sent = h.requestsTo("/auth/login").single().body.text()
        assertEquals("""{"email":"ana@granatum.es","password":" pass word "}""", sent)
        val session = (result as Result.Success).data
        assertEquals("e-7", session.employeeId)
        assertEquals(UserRole.ADMIN, session.role)
        assertEquals("ana@granatum.es", session.email)
        assertEquals(session, h.storage.observeSession().first())
    }

    @Test
    fun login_never_sends_an_authorization_header() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.OK, tokensJson(TestTokens.access(), "r1")) }
        h.repository.login("a@b.es", "x")
        h.repository.login("a@b.es", "x") // second time a session exists
        assertTrue(h.requestsTo("/auth/login").none { it.headers.contains("Authorization") })
    }

    @Test
    fun rejected_credentials_store_nothing() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.Unauthorized, errorJson("CREDENCIALES_INVALIDAS")) }
        assertEquals(Result.Failure(AuthError.InvalidCredentials), h.repository.login("a@b.es", "x"))
        assertNull(h.storage.observeSession().first())
    }

    @Test
    fun a_token_without_identity_is_a_failed_sign_in() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.OK, tokensJson(TestTokens.jwt("""{"role":"ADMIN"}"""), "r1")) }
        assertEquals(Result.Failure(AuthError.InvalidSession), h.repository.login("a@b.es", "x"))
        assertNull(h.storage.observeSession().first())
    }

    @Test
    fun the_pending_change_flag_is_kept() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.OK, tokensJson(TestTokens.access(pwdChange = true), "r1", mustChange = true)) }
        assertTrue((h.repository.login("a@b.es", "x") as Result.Success).data.mustChangePassword)
    }

    @Test
    fun change_password_uses_the_contract_field_names_and_replaces_the_session() = runTest {
        val h = AuthTestHarness { request ->
            if (request.url.encodedPath.endsWith("/auth/login")) {
                json(HttpStatusCode.OK, tokensJson(TestTokens.access(pwdChange = true), "r1", mustChange = true))
            } else {
                json(HttpStatusCode.OK, tokensJson(TestTokens.access(marker = "new"), "r2"))
            }
        }
        h.repository.login("ana@granatum.es", "Temp0ral!")

        val result = h.repository.changePassword("Temp0ral!", "Mi-Clave-2026!")

        val sent = h.requestsTo("/auth/change-password").single()
        assertEquals("""{"passwordActual":"Temp0ral!","passwordNueva":"Mi-Clave-2026!"}""", sent.body.text())
        assertTrue(sent.headers["Authorization"]!!.startsWith("Bearer "))
        val session = (result as Result.Success).data
        assertEquals(false, session.mustChangePassword)
        assertEquals("ana@granatum.es", session.email)
        assertEquals("r2", h.storage.observeSession().first()?.refreshToken)
    }

    @Test
    fun a_wrong_current_password_neither_refreshes_nor_signs_out() = runTest {
        val h = AuthTestHarness { request ->
            if (request.url.encodedPath.endsWith("/auth/login")) json(HttpStatusCode.OK, tokensJson(TestTokens.access(), "r1"))
            else json(HttpStatusCode.Unauthorized, errorJson("CREDENCIALES_INVALIDAS"))
        }
        h.repository.login("a@b.es", "x")

        assertEquals(Result.Failure(AuthError.InvalidCredentials), h.repository.changePassword("bad", "Granatum1!"))
        assertTrue(h.requestsTo("/auth/refresh").isEmpty())
        assertEquals("r1", h.storage.observeSession().first()?.refreshToken)
    }

    @Test
    fun a_weak_password_returns_the_servers_requirements() = runTest {
        val h = AuthTestHarness { request ->
            if (request.url.encodedPath.endsWith("/auth/login")) json(HttpStatusCode.OK, tokensJson(TestTokens.access(), "r1"))
            else json(HttpStatusCode.UnprocessableEntity, """{"code":"PASSWORD_DEBIL","message":"m","requisitos":["FALTA_SIMBOLO"]}""")
        }
        h.repository.login("a@b.es", "x")
        assertIs<AuthError.WeakPassword>((h.repository.changePassword("x", "y") as Result.Failure).error)
    }

    @Test
    fun logout_without_coverage_still_ends_the_session_here() = runTest {
        var online = true
        val h = AuthTestHarness { request ->
            if (!online) throw kotlinx.io.IOException("offline")
            json(HttpStatusCode.OK, tokensJson(TestTokens.access(), "r1"))
        }
        h.repository.login("a@b.es", "x")
        online = false

        h.repository.logout()

        assertNull(h.storage.observeSession().first())
    }

    @Test
    fun logout_revokes_the_refresh_token_on_the_server() = runTest {
        val h = AuthTestHarness { request ->
            if (request.url.encodedPath.endsWith("/auth/logout")) json(HttpStatusCode.NoContent, "")
            else json(HttpStatusCode.OK, tokensJson(TestTokens.access(), "r1"))
        }
        h.repository.login("a@b.es", "x")
        h.repository.logout()
        assertEquals("""{"refreshToken":"r1"}""", h.requestsTo("/auth/logout").single().body.text())
    }

    @Test
    fun the_owner_signs_up_with_every_field_and_is_signed_in_right_after() = runTest {
        val h = AuthTestHarness { request ->
            if (request.url.encodedPath.endsWith("/auth/registro")) {
                json(HttpStatusCode.Created, """{"estado":"ACTIVA","mensaje":"ok"}""")
            } else {
                json(HttpStatusCode.OK, tokensJson(TestTokens.access(sub = "e-1", role = "ADMIN"), "r1"))
            }
        }

        val result = h.repository.registerOwner(
            OwnerRegistration(name = " Ana Martín ", identityDocument = "12345678Z", email = " Ana@Granatum.es", password = "Clave-2026!", bootstrapCode = " codigo ")
        )

        val sent = h.requestsTo("/auth/registro").single()
        assertEquals(
            """{"email":"ana@granatum.es","password":"Clave-2026!","nombre":"Ana Martín","documentoIdentidad":"12345678Z","codigoArranque":"codigo"}""",
            sent.body.text()
        )
        assertTrue(!sent.headers.contains("Authorization"), "a public route")
        assertEquals("""{"email":"ana@granatum.es","password":"Clave-2026!"}""", h.requestsTo("/auth/login").single().body.text())
        assertEquals(UserRole.ADMIN, (result as Result.Success).data.role)
    }

    @Test
    fun a_rejected_sign_up_does_not_try_to_sign_in() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.Forbidden, errorJson("CODIGO_ARRANQUE_INVALIDO")) }
        val result = h.repository.registerOwner(OwnerRegistration("Ana", "12345678Z", "a@b.es", "Clave-2026!", "mal"))
        assertEquals(Result.Failure(AuthError.InvalidBootstrapCode), result)
        assertTrue(h.requestsTo("/auth/login").isEmpty())
        assertNull(h.storage.observeSession().first())
    }
}
