package com.granatum.core.domain.auth

import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.SessionState
import com.granatum.core.domain.auth.model.SignOutReason
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.model.sessionStateOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionStateTest {

    private fun session(role: UserRole = UserRole.EMPLEADO, mustChange: Boolean = false) = Session(
        accessToken = "a",
        refreshToken = "r",
        employeeId = "e-1",
        role = role,
        mustChangePassword = mustChange,
        email = "ana@granatum.es"
    )

    @Test
    fun nothing_is_decided_until_the_store_has_been_read() {
        assertEquals(SessionState.Loading, sessionStateOf(session(), loaded = false, reason = null))
    }

    @Test
    fun no_session_means_signed_out() {
        assertEquals(SessionState.SignedOut(null), sessionStateOf(null, loaded = true, reason = null))
    }

    @Test
    fun a_forced_sign_out_keeps_its_reason() {
        assertEquals(
            SessionState.SignedOut(SignOutReason.INACTIVE),
            sessionStateOf(null, loaded = true, reason = SignOutReason.INACTIVE)
        )
    }

    @Test
    fun a_pending_password_change_blocks_everything_else() {
        assertEquals(
            SessionState.PasswordChangeRequired,
            sessionStateOf(session(role = UserRole.ADMIN, mustChange = true), loaded = true, reason = null)
        )
    }

    @Test
    fun a_valid_session_is_active_with_its_role() {
        assertEquals(
            SessionState.Active(UserRole.REPRESENTANTE),
            sessionStateOf(session(role = UserRole.REPRESENTANTE), loaded = true, reason = null)
        )
    }

    @Test
    fun a_stale_reason_is_ignored_once_someone_signs_in() {
        assertEquals(
            SessionState.Active(UserRole.EMPLEADO),
            sessionStateOf(session(), loaded = true, reason = SignOutReason.SESSION_REJECTED)
        )
    }
}
