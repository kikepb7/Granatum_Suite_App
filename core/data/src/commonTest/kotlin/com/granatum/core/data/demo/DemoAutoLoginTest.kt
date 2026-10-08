package com.granatum.core.data.demo

import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.OwnerRegistration
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoAutoLoginTest {

    private class Recorder : AuthRepository {
        val logins = mutableListOf<String>()
        override suspend fun login(email: String, password: String): Result<Session, AuthError> {
            logins += email
            return Result.Failure(AuthError.Unknown)
        }
        override suspend fun registerOwner(registration: OwnerRegistration): Result<Session, AuthError> = Result.Failure(AuthError.Unknown)
        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Session, AuthError> = Result.Failure(AuthError.Unknown)
        override suspend fun logout() = Unit
    }

    @Test
    fun outside_the_demo_build_it_does_nothing_and_never_holds_the_splash() = runTest {
        // Unit tests run the local build: the demo is off.
        assertFalse(DemoMode.isEnabled)
        val auth = Recorder()
        val autoLogin = DemoAutoLogin(auth)
        assertFalse(autoLogin.pending.value)
        autoLogin.run()
        assertTrue(auth.logins.isEmpty())
        assertEquals(emptyList(), DemoMode.accounts.accounts())
    }
}
