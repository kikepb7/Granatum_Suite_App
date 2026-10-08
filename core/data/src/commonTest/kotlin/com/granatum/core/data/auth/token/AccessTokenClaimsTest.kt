package com.granatum.core.data.auth.token

import com.granatum.core.data.testing.TestTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccessTokenClaimsTest {

    @Test
    fun reads_subject_role_and_pending_change() {
        val claims = AccessTokenClaims.parse(TestTokens.access(sub = "e-42", role = "ENCARGADO", pwdChange = true))!!
        assertEquals("e-42", claims.employeeId)
        assertEquals("ENCARGADO", claims.role)
        assertTrue(claims.pwdChange)
    }

    @Test
    fun an_absent_pending_change_claim_means_false() {
        assertFalse(AccessTokenClaims.parse(TestTokens.access())!!.pwdChange)
    }

    @Test
    fun payloads_with_and_without_padding_both_decode() {
        // Lengths chosen so the base64 payload would need one or two padding characters.
        listOf("e", "e1", "e12", "e123").forEach { sub ->
            assertEquals(sub, AccessTokenClaims.parse(TestTokens.access(sub = sub))?.employeeId)
        }
    }

    @Test
    fun an_unknown_role_is_passed_through_for_the_caller_to_judge() {
        assertEquals("AUDITOR", AccessTokenClaims.parse(TestTokens.access(role = "AUDITOR"))?.role)
    }

    @Test
    fun a_token_without_subject_is_rejected() {
        assertNull(AccessTokenClaims.parse(TestTokens.jwt("""{"role":"ADMIN"}""")))
        assertNull(AccessTokenClaims.parse(TestTokens.jwt("""{"sub":"","role":"ADMIN"}""")))
    }

    @Test
    fun anything_that_is_not_a_jwt_is_rejected() {
        assertNull(AccessTokenClaims.parse("not-a-token"))
        assertNull(AccessTokenClaims.parse("a.b"))
        assertNull(AccessTokenClaims.parse("a.%%%.c"))
        assertNull(AccessTokenClaims.parse(TestTokens.jwt("this is not json")))
    }
}
