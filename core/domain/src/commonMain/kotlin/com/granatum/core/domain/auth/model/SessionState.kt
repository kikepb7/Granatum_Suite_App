package com.granatum.core.domain.auth.model

/** What navigation shows. Derived, never stored: see [sessionStateOf]. */
sealed interface SessionState {
    /** The secure store has not been read yet. Shown as a splash so login never flashes. */
    data object Loading : SessionState

    data class SignedOut(val reason: SignOutReason?) : SessionState

    /** The server allows this session to change its password and nothing else. */
    data object PasswordChangeRequired : SessionState

    data class Active(val role: UserRole) : SessionState
}

/**
 * The single place that decides what navigation shows.
 *
 * Network state is not an input on purpose: losing coverage never changes what the person sees
 * (offline-first). Only the stored session and the reason for the last forced sign-out do.
 */
fun sessionStateOf(session: Session?, loaded: Boolean, reason: SignOutReason?): SessionState = when {
    !loaded -> SessionState.Loading
    session == null -> SessionState.SignedOut(reason)
    session.mustChangePassword -> SessionState.PasswordChangeRequired
    else -> SessionState.Active(session.role)
}
