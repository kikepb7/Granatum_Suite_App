package com.granatum.feature.auth.presentation.password

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.validation.PasswordValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Both the mandatory change after a temporary password and the voluntary one from the account
 * menu: same rules, same messages (FR-017). Only what the secondary button does differs, and
 * the caller decides that from [ChangePasswordEvent.Dismissed].
 */
class ChangePasswordViewModel(
    private val authRepository: AuthRepository,
    mode: ChangePasswordMode
) : ViewModel() {

    val currentState = TextFieldState()
    val newState = TextFieldState()
    val repeatState = TextFieldState()

    private val _state = MutableStateFlow(ChangePasswordState(mode = mode))
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    private val eventChannel = Channel<ChangePasswordEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        snapshotFlow { newState.text.toString() }
            .onEach { password ->
                _state.update {
                    it.copy(
                        unmetRequirements = PasswordValidator.validate(password),
                        serverUnmetRequirements = emptySet(),
                        error = null
                    )
                }
            }
            .launchIn(viewModelScope)

        combine(
            snapshotFlow { currentState.text.toString() },
            snapshotFlow { repeatState.text.toString() }
        ) { _, _ -> }
            .onEach {
                _state.update { it.copy(currentPasswordMissing = false, currentPasswordWrong = false, repeatMismatch = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: ChangePasswordAction) {
        when (action) {
            ChangePasswordAction.OnSubmit -> submit()
            ChangePasswordAction.OnToggleVisibility -> _state.update { it.copy(arePasswordsVisible = !it.arePasswordsVisible) }
            ChangePasswordAction.OnDismiss -> viewModelScope.launch { eventChannel.send(ChangePasswordEvent.Dismissed) }
        }
    }

    private fun submit() {
        val snapshot = _state.value
        if (snapshot.isLoading) return

        val current = currentState.text.toString()
        val new = newState.text.toString()
        val repeat = repeatState.text.toString()
        if (current.isEmpty() || new != repeat || snapshot.unmetRequirements.isNotEmpty()) {
            _state.update { it.copy(currentPasswordMissing = current.isEmpty(), repeatMismatch = new != repeat) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.changePassword(currentPassword = current, newPassword = new)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    eventChannel.send(ChangePasswordEvent.Changed)
                }
                is Result.Failure -> _state.update { failed(it, result.error) }
            }
        }
    }

    /** What is typed in the new-password fields is kept in every case (FR-015). */
    private fun failed(state: ChangePasswordState, error: AuthError): ChangePasswordState = when (error) {
        AuthError.InvalidCredentials -> state.copy(isLoading = false, currentPasswordWrong = true)
        is AuthError.WeakPassword -> state.copy(isLoading = false, serverUnmetRequirements = error.requirements, error = error)
        else -> state.copy(isLoading = false, error = error)
    }
}
