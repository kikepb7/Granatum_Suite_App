package com.granatum.core.data.auth

import com.granatum.core.domain.auth.model.SignOutReason
import com.granatum.core.domain.auth.repository.SessionStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one way a session ends without the person ending it: the server rejected it for good.
 * Clears the stored session and keeps the reason for the login screen (FR-021).
 *
 * Only definitive rejections come through here. Losing coverage, a timeout or a 5xx never do —
 * that is the whole point of having a single entry point (research D4).
 */
class SessionEvents(
    private val sessionStorage: SessionStorage
) {
    private val reason = MutableStateFlow<SignOutReason?>(null)

    /** The reason for the last forced sign-out, until someone signs in again. */
    val lastSignOutReason: StateFlow<SignOutReason?> = reason.asStateFlow()

    suspend fun forceSignOut(why: SignOutReason) {
        reason.value = why
        sessionStorage.set(null)
    }

    /** A deliberate sign-out or a new sign-in leaves no stale reason behind. */
    fun clearReason() {
        reason.value = null
    }
}
