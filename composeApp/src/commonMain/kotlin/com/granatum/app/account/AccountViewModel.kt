package com.granatum.app.account

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
    val role: UserRole = UserRole.DESCONOCIDO,
    val pendingCount: Int = 0,
    val showPendingWarning: Boolean = false,
    val isSigningOut: Boolean = false
) {
    /** The app cannot show the person's name: the backend offers no route for it (spec 004). */
    val initial: String get() = email.firstOrNull()?.uppercase() ?: "?"
}

sealed interface AccountAction {
    data object OnSignOutClick : AccountAction
    data object OnConfirmSignOut : AccountAction
    data object OnDismissWarning : AccountAction
}

class AccountViewModel(
    sessionStorage: SessionStorage,
    observePendingSyncCount: ObservePendingSyncCountUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val ui = MutableStateFlow(AccountState())

    val state = combine(sessionStorage.observeSession(), observePendingSyncCount(), ui) { session, pending, ui ->
        ui.copy(
            email = session?.email.orEmpty(),
            role = session?.role ?: UserRole.DESCONOCIDO,
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
        }
    }

    private fun signOut() {
        if (ui.value.isSigningOut) return
        ui.update { it.copy(showPendingWarning = false, isSigningOut = true) }
        // Always ends the session here, with or without coverage; the gate then shows login.
        viewModelScope.launch { authRepository.logout() }
    }
}
