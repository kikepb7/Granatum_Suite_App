package com.granatum.feature.auth.presentation.testing

import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.OwnerRegistration
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.util.Result
import kotlinx.coroutines.CompletableDeferred

class FakeAuthRepository : AuthRepository {
    val loginCalls = mutableListOf<Pair<String, String>>()
    val changeCalls = mutableListOf<Pair<String, String>>()
    var logoutCalls = 0

    /** Completed by the test to release the pending call; lets a test see the in-flight state. */
    var gate: CompletableDeferred<Unit>? = null
    var nextResult: Result<Session, AuthError> = Result.Success(session())

    override suspend fun login(email: String, password: String): Result<Session, AuthError> {
        loginCalls += email to password
        gate?.await()
        return nextResult
    }

    val registrations = mutableListOf<OwnerRegistration>()

    override suspend fun registerOwner(registration: OwnerRegistration): Result<Session, AuthError> {
        registrations += registration
        gate?.await()
        return nextResult
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Session, AuthError> {
        changeCalls += currentPassword to newPassword
        gate?.await()
        return nextResult
    }

    override suspend fun logout() {
        logoutCalls++
    }

    companion object {
        fun session() = Session("a", "r", "e-1", UserRole.EMPLEADO, false, "ana@granatum.es")
    }
}
