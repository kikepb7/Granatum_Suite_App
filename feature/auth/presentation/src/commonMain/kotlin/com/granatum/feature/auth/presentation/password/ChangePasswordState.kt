package com.granatum.feature.auth.presentation.password

import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.validation.PasswordRequirement

/** Mandatory: the server allows nothing else. Voluntary: from the account menu, cancellable. */
enum class ChangePasswordMode { MANDATORY, VOLUNTARY }

data class ChangePasswordState(
    val mode: ChangePasswordMode = ChangePasswordMode.MANDATORY,
    /** Unmet requirements of the new password, recomputed while typing (FR-013). */
    val unmetRequirements: Set<PasswordRequirement> = PasswordRequirement.entries.toSet(),
    /** What the server said is unmet, when it rejected a password the local check accepted (FR-014). */
    val serverUnmetRequirements: Set<PasswordRequirement> = emptySet(),
    val currentPasswordMissing: Boolean = false,
    val currentPasswordWrong: Boolean = false,
    val repeatMismatch: Boolean = false,
    val error: AuthError? = null,
    val isLoading: Boolean = false,
    val arePasswordsVisible: Boolean = false
) {
    val canSubmit: Boolean get() = unmetRequirements.isEmpty() && !isLoading
}

sealed interface ChangePasswordAction {
    data object OnSubmit : ChangePasswordAction
    data object OnToggleVisibility : ChangePasswordAction
    data object OnDismiss : ChangePasswordAction
}

sealed interface ChangePasswordEvent {
    /** Voluntary change done; the caller goes back. A mandatory one needs no event: the session state moves on. */
    data object Changed : ChangePasswordEvent
    data object Dismissed : ChangePasswordEvent
}
