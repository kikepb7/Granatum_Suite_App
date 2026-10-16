package com.granatum.feature.auth.presentation.error

import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.SignOutReason
import com.granatum.core.domain.validation.PasswordRequirement
import com.granatum.core.presentation.util.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthUiTextTest {

    private fun resource(text: UiText) = assertIs<UiText.Resource>(text)

    @Test
    fun plain_auth_errors_each_have_their_own_message() {
        val errors = listOf(
            AuthError.InvalidCredentials, AuthError.Validation, AuthError.InvalidBootstrapCode,
            AuthError.EmailTaken, AuthError.AccountExists, AuthError.InvalidDocument, AuthError.NoInternet,
            AuthError.Timeout, AuthError.InvalidSession, AuthError.Unknown,
            AuthError.WeakPassword(emptySet())
        )
        val keys = errors.map { resource(it.toUiText()).id.key }
        assertEquals(keys.size, keys.toSet().size, "keys: $keys")
        assertEquals("auth_error_invalid_credentials", resource(AuthError.InvalidCredentials.toUiText()).id.key)
    }

    @Test
    fun a_known_wait_is_passed_to_the_message_as_whole_seconds() {
        val tooMany = resource(AuthError.TooManyAttempts(30L).toUiText())
        assertEquals("auth_error_too_many_attempts", tooMany.id.key)
        assertEquals(listOf<Any>(30), tooMany.args.toList())

        val busy = resource(AuthError.ServiceBusy(5L).toUiText())
        assertEquals("auth_error_service_busy", busy.id.key)
        assertEquals(listOf<Any>(5), busy.args.toList())
    }

    @Test
    fun an_unknown_wait_uses_the_message_without_a_number() {
        val tooMany = resource(AuthError.TooManyAttempts(null).toUiText())
        assertEquals("auth_error_too_many_attempts_no_wait", tooMany.id.key)
        assertEquals(0, tooMany.args.size)
        assertEquals("auth_error_service_busy_no_wait", resource(AuthError.ServiceBusy(null).toUiText()).id.key)
    }

    @Test
    fun sign_out_reasons_and_password_requirements_have_distinct_messages() {
        val reasons = SignOutReason.entries.map { resource(it.toUiText()).id.key }
        assertEquals(setOf("signout_reason_inactive", "signout_reason_rejected"), reasons.toSet())

        val requirements = PasswordRequirement.entries.map { resource(it.toUiText()).id.key }
        assertEquals(PasswordRequirement.entries.size, requirements.toSet().size)
    }
}
