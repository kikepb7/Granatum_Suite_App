package com.granatum.app.account

import com.granatum.core.domain.auth.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountStateTest {

    @Test
    fun the_initial_is_the_first_letter_of_the_email_in_upper_case() {
        assertEquals("A", AccountState(email = "ana@granatum.es").initial)
    }

    @Test
    fun without_an_email_the_initial_is_a_question_mark() {
        assertEquals("?", AccountState().initial)
    }

    @Test
    fun a_fresh_state_is_signed_in_nowhere_and_warns_nothing() {
        val state = AccountState()
        assertEquals(UserRole.UNKNOWN, state.role)
        assertEquals(0, state.pendingCount)
        assertFalse(state.showPendingWarning)
        assertFalse(state.isSigningOut)
    }

    @Test
    fun only_an_admin_sees_the_google_drive_entry() {
        assertTrue(AccountState(role = UserRole.ADMIN).showGoogleDrive)
        UserRole.entries.filter { it != UserRole.ADMIN }.forEach { role ->
            assertFalse(AccountState(role = role).showGoogleDrive, "$role must not see Google Drive")
        }
    }
}
