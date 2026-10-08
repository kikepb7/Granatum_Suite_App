package com.granatum.core.data.demo

import com.granatum.core.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Demo build only: the app opens already signed in as the administrator, skipping the login
 * screen, so every feature can be tried at once. It signs in through the normal path against the
 * demo backend, so the session is real for the rest of the app. Signing out leads to the login
 * screen, which offers the demo accounts (administrator and employee) to switch.
 *
 * The demo backend forgets everything when the app closes, so a session stored by a previous run
 * would be refused: each start signs in afresh.
 */
class DemoAutoLogin(private val authRepository: AuthRepository) {

    private val _pending = MutableStateFlow(DemoMode.isEnabled)

    /** True until the automatic sign-in has finished; the gate keeps the splash meanwhile. */
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    private var started = false

    suspend fun run() {
        if (!DemoMode.isEnabled || started) return
        started = true
        try {
            authRepository.login(email = DemoMode.ADMIN.email, password = DemoMode.ADMIN.password)
        } finally {
            _pending.value = false
        }
    }
}
