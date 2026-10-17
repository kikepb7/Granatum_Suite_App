package com.granatum.app.account

import com.granatum.core.domain.environment.AppEnvironment
import com.granatum.core.domain.environment.EnvironmentError
import com.granatum.core.domain.environment.EnvironmentRepository
import com.granatum.core.domain.util.Result
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.feature.clockin.domain.usecase.ObservePendingSyncCountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountState(
    val email: String = "",
    val role: UserRole = UserRole.UNKNOWN,
    val pendingCount: Int = 0,
    val showPendingWarning: Boolean = false,
    val isSigningOut: Boolean = false,
    /** PRE build only (specs/010-entornos). */
    val environment: AppEnvironment = AppEnvironment.PRO,
    val environments: List<AppEnvironment> = emptyList(),
    val environmentError: EnvironmentError? = null
) {
    /** The app cannot show the person's name: the backend offers no route for it (spec 004). */
    val initial: String get() = email.firstOrNull()?.uppercase() ?: "?"
}

sealed interface AccountAction {
    data object OnSignOutClick : AccountAction
    data object OnConfirmSignOut : AccountAction
    data object OnDismissWarning : AccountAction
    data class OnEnvironment(val environment: AppEnvironment) : AccountAction
}

class AccountViewModel(
    sessionStorage: SessionStorage,
    observePendingSyncCount: ObservePendingSyncCountUseCase,
    private val authRepository: AuthRepository,
    private val environments: EnvironmentRepository
) : ViewModel() {

    private val ui = MutableStateFlow(
        AccountState(environments = if (environments.canSwitch) environments.available else emptyList())
    )

    val state = combine(sessionStorage.observeSession(), observePendingSyncCount(), environments.current, ui) { session, pending, environment, ui ->
        ui.copy(
            environment = environment,
            email = session?.email.orEmpty(),
            role = session?.role ?: UserRole.UNKNOWN,
            pendingCount = pending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AccountState())

    fun onAction(action: AccountAction) {
        when (action) {
            AccountAction.OnSignOutClick -> viewModelScope.launch {
                // Pending punches are kept, but nobody can send them until this person signs in
                // again. They should know before they go (FR-027).
                if (state.first().pendingCount > 0) ui.update { it.copy(showPendingWarning = true) } else signOut()
            }
            AccountAction.OnConfirmSignOut -> signOut()
            AccountAction.OnDismissWarning -> ui.update { it.copy(showPendingWarning = false) }
            // Switching signs out: the gate then shows the login of the new environment.
            is AccountAction.OnEnvironment -> viewModelScope.launch {
                val result = environments.switchTo(action.environment)
                ui.update { it.copy(environmentError = (result as? Result.Failure)?.error) }
            }
        }
    }

    private fun signOut() {
        if (ui.value.isSigningOut) return
        ui.update { it.copy(showPendingWarning = false, isSigningOut = true) }
        // Always ends the session here, with or without coverage; the gate then shows login.
        viewModelScope.launch { authRepository.logout() }
    }
}
