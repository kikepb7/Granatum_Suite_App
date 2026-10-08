package com.granatum.feature.staff.presentation.detail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.staff.domain.ContractType
import com.granatum.feature.staff.domain.StaffEdit
import com.granatum.feature.staff.domain.StaffError
import com.granatum.feature.staff.domain.StaffLimits
import com.granatum.feature.staff.domain.StaffMember
import com.granatum.feature.staff.domain.StaffUseCases
import com.granatum.feature.staff.domain.TemporaryCredentials
import com.granatum.feature.staff.presentation.common.FieldIssue
import com.granatum.feature.staff.presentation.common.StaffField
import com.granatum.feature.staff.presentation.common.StaffValidation
import com.granatum.feature.staff.presentation.common.toUiText
import granatumsuite.feature.staff.presentation.generated.resources.Res
import granatumsuite.feature.staff.presentation.generated.resources.detail_deactivated
import granatumsuite.feature.staff.presentation.generated.resources.detail_reactivated
import granatumsuite.feature.staff.presentation.generated.resources.detail_saved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class StaffDetailState(
    val member: StaffMember? = null,
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val contract: ContractType = ContractType.FULL_TIME,
    val startDate: LocalDate? = null,
    val issues: Map<StaffField, FieldIssue> = emptyMap(),
    val isSelf: Boolean = false,
    val isBusy: Boolean = false,
    val confirmingDeactivate: Boolean = false,
    val confirmingReset: Boolean = false,
    /** The record has no account: ask for an email and a role to give it one (research D5). */
    val grantingAccess: Boolean = false,
    val grantRole: UserRole = UserRole.EMPLEADO,
    val credentials: TemporaryCredentials? = null,
    val message: UiText? = null,
)

sealed interface StaffDetailAction {
    data class OnContract(
        val contract: ContractType,
    ) : StaffDetailAction

    data class OnStartDate(
        val date: LocalDate,
    ) : StaffDetailAction

    data object OnSave : StaffDetailAction

    data object OnToggleActive : StaffDetailAction

    data object OnDeactivateConfirm : StaffDetailAction

    data object OnReset : StaffDetailAction

    data object OnResetConfirm : StaffDetailAction

    data class OnGrantRole(
        val role: UserRole,
    ) : StaffDetailAction

    data object OnGrantConfirm : StaffDetailAction

    data object OnDismiss : StaffDetailAction

    data object OnCredentialsDismissed : StaffDetailAction

    data object OnRetry : StaffDetailAction

    data object OnMessageShown : StaffDetailAction
}

class StaffDetailViewModel(
    private val memberId: String,
    private val staff: StaffUseCases,
    private val sessionStorage: SessionStorage,
) : ViewModel() {
    val name = TextFieldState()
    val position = TextFieldState()
    val grantEmail = TextFieldState()
    private val _state = MutableStateFlow(StaffDetailState())
    val state = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            val ownId = sessionStorage.observeSession().firstOrNull()?.employeeId
            _state.update { it.copy(isSelf = ownId == memberId) }
        }
    }

    fun onAction(action: StaffDetailAction) {
        when (action) {
            is StaffDetailAction.OnContract -> _state.update { it.copy(contract = action.contract) }
            is StaffDetailAction.OnStartDate -> _state.update { it.copy(startDate = action.date) }
            StaffDetailAction.OnSave -> save()
            StaffDetailAction.OnToggleActive -> {
                val member = _state.value.member ?: return
                // Reactivating needs no warning; leaving does.
                if (member.active) _state.update { it.copy(confirmingDeactivate = true) } else setActive(true)
            }
            StaffDetailAction.OnDeactivateConfirm -> {
                _state.update { it.copy(confirmingDeactivate = false) }
                setActive(false)
            }
            StaffDetailAction.OnReset -> _state.update { it.copy(confirmingReset = true) }
            StaffDetailAction.OnResetConfirm -> reset()
            is StaffDetailAction.OnGrantRole -> _state.update { it.copy(grantRole = action.role) }
            StaffDetailAction.OnGrantConfirm -> grant()
            StaffDetailAction.OnDismiss ->
                _state.update {
                    it.copy(
                        confirmingDeactivate = false,
                        confirmingReset = false,
                        grantingAccess = false,
                    )
                }
            StaffDetailAction.OnCredentialsDismissed -> _state.update { it.copy(credentials = null) }
            StaffDetailAction.OnRetry -> load()
            StaffDetailAction.OnMessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            when (val result = staff.repository.member(memberId)) {
                is Result.Success -> show(result.data)
                is Result.Failure -> _state.update { it.copy(isLoading = false, loadError = result.error.toUiText()) }
            }
        }
    }

    private fun show(member: StaffMember) {
        name.setTextAndPlaceCursorAtEnd(member.name)
        position.setTextAndPlaceCursorAtEnd(member.position)
        _state.update {
            it.copy(
                member = member,
                isLoading = false,
                contract = member.contract,
                startDate = member.startDate,
                issues = emptyMap(),
            )
        }
    }

    private fun busy(block: suspend () -> Unit) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            try {
                block()
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun save() {
        val issues =
            buildMap {
                StaffValidation.text(name, StaffLimits.NAME)?.let { put(StaffField.NAME, it) }
                StaffValidation.text(position, StaffLimits.POSITION)?.let { put(StaffField.POSITION, it) }
            }
        val date = _state.value.startDate
        if (issues.isNotEmpty() || date == null) {
            _state.update { it.copy(issues = issues) }
            return
        }
        busy {
            val edit = StaffEdit(name.text.toString(), position.text.toString(), _state.value.contract, date)
            when (val result = staff.repository.update(memberId, edit)) {
                is Result.Success -> {
                    show(result.data)
                    _state.update { it.copy(message = UiText.Resource(Res.string.detail_saved)) }
                }
                is Result.Failure -> _state.update { it.copy(message = result.error.toUiText()) }
            }
        }
    }

    private fun setActive(active: Boolean) {
        if (!active && _state.value.isSelf) return
        busy {
            when (val result = staff.repository.setActive(memberId, active)) {
                is Result.Success -> {
                    show(result.data)
                    _state.update {
                        it.copy(
                            message = UiText.Resource(if (active) Res.string.detail_reactivated else Res.string.detail_deactivated),
                        )
                    }
                }
                is Result.Failure -> _state.update { it.copy(message = result.error.toUiText()) }
            }
        }
    }

    private fun reset() {
        _state.update { it.copy(confirmingReset = false) }
        busy {
            when (val result = staff.repository.resetPassword(memberId)) {
                is Result.Success -> _state.update { it.copy(credentials = result.data) }
                is Result.Failure ->
                    if (result.error == StaffError.NoAccount) {
                        grantEmail.clearText()
                        _state.update { it.copy(grantingAccess = true, issues = emptyMap()) }
                    } else {
                        _state.update { it.copy(message = result.error.toUiText()) }
                    }
            }
        }
    }

    private fun grant() {
        val issue = StaffValidation.email(grantEmail)
        if (issue != null) {
            _state.update { it.copy(issues = mapOf(StaffField.EMAIL to issue)) }
            return
        }
        _state.update { it.copy(grantingAccess = false, issues = emptyMap()) }
        busy {
            when (val result = staff.repository.grantAccess(memberId, grantEmail.text.toString(), _state.value.grantRole)) {
                is Result.Success -> _state.update { it.copy(credentials = result.data) }
                is Result.Failure -> _state.update { it.copy(message = result.error.toUiText()) }
            }
        }
    }
}
