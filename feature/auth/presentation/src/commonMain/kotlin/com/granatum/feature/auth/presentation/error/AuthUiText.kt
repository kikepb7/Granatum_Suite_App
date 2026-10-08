package com.granatum.feature.auth.presentation.error

import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.SignOutReason
import com.granatum.core.domain.validation.PasswordRequirement
import com.granatum.core.presentation.util.UiText
import granatumsuite.feature.auth.presentation.generated.resources.Res
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_invalid_credentials
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_invalid_session
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_no_internet
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_service_busy
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_service_busy_no_wait
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_timeout
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_too_many_attempts
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_too_many_attempts_no_wait
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_unknown
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_validation
import granatumsuite.feature.auth.presentation.generated.resources.auth_error_weak_password
import granatumsuite.feature.auth.presentation.generated.resources.password_req_digit
import granatumsuite.feature.auth.presentation.generated.resources.password_req_lowercase
import granatumsuite.feature.auth.presentation.generated.resources.password_req_max_length
import granatumsuite.feature.auth.presentation.generated.resources.password_req_min_length
import granatumsuite.feature.auth.presentation.generated.resources.password_req_symbol
import granatumsuite.feature.auth.presentation.generated.resources.password_req_uppercase
import granatumsuite.feature.auth.presentation.generated.resources.signout_reason_inactive
import granatumsuite.feature.auth.presentation.generated.resources.signout_reason_rejected

/** Text for the person lives here, in presentation, never in domain (constitution IV). */
fun AuthError.toUiText(): UiText = when (this) {
    // One message for unknown email, wrong password, inactive person and locked account (FR-003).
    AuthError.InvalidCredentials -> UiText.Resource(Res.string.auth_error_invalid_credentials)
    is AuthError.TooManyAttempts -> retryAfterSeconds
        ?.let { UiText.Resource(Res.string.auth_error_too_many_attempts, arrayOf(it.toInt())) }
        ?: UiText.Resource(Res.string.auth_error_too_many_attempts_no_wait)
    is AuthError.ServiceBusy -> retryAfterSeconds
        ?.let { UiText.Resource(Res.string.auth_error_service_busy, arrayOf(it.toInt())) }
        ?: UiText.Resource(Res.string.auth_error_service_busy_no_wait)
    is AuthError.WeakPassword -> UiText.Resource(Res.string.auth_error_weak_password)
    AuthError.Validation -> UiText.Resource(Res.string.auth_error_validation)
    AuthError.NoInternet -> UiText.Resource(Res.string.auth_error_no_internet)
    AuthError.Timeout -> UiText.Resource(Res.string.auth_error_timeout)
    AuthError.InvalidSession -> UiText.Resource(Res.string.auth_error_invalid_session)
    AuthError.Unknown -> UiText.Resource(Res.string.auth_error_unknown)
}

fun SignOutReason.toUiText(): UiText = when (this) {
    SignOutReason.INACTIVE -> UiText.Resource(Res.string.signout_reason_inactive)
    SignOutReason.SESSION_REJECTED -> UiText.Resource(Res.string.signout_reason_rejected)
}

fun PasswordRequirement.toUiText(): UiText = UiText.Resource(
    when (this) {
        PasswordRequirement.LONGITUD_MINIMA -> Res.string.password_req_min_length
        PasswordRequirement.LONGITUD_MAXIMA -> Res.string.password_req_max_length
        PasswordRequirement.FALTA_MAYUSCULA -> Res.string.password_req_uppercase
        PasswordRequirement.FALTA_MINUSCULA -> Res.string.password_req_lowercase
        PasswordRequirement.FALTA_DIGITO -> Res.string.password_req_digit
        PasswordRequirement.FALTA_SIMBOLO -> Res.string.password_req_symbol
    }
)
