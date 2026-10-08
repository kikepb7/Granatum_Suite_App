package com.granatum.feature.staff.presentation.onboarding

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.staff.domain.ContractType
import com.granatum.feature.staff.domain.Onboarding
import com.granatum.feature.staff.domain.StaffError
import com.granatum.feature.staff.domain.StaffLimits
import com.granatum.feature.staff.domain.StaffUseCases
import com.granatum.feature.staff.domain.TemporaryCredentials
import com.granatum.feature.staff.presentation.common.FieldIssue
import com.granatum.feature.staff.presentation.common.StaffField
import com.granatum.feature.staff.presentation.common.StaffValidation
import com.granatum.feature.staff.presentation.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class OnboardingState(
    val contract: ContractType = ContractType.FULL_TIME,
    val role: UserRole = UserRole.EMPLEADO,
    val startDate: LocalDate,
    val issues: Map<StaffField, FieldIssue> = emptyMap(),
    val isSaving: Boolean = false,
    val error: UiText? = null,
    /** Shown once, then dropped from memory when the sheet closes (research D4). */
    val credentials: TemporaryCredentials? = null,
    val onboardedName: String = "",
)

class OnboardingViewModel(
    private val staff: StaffUseCases,
    today: LocalDate,
) : ViewModel() {
    val name = TextFieldState()
    val document = TextFieldState()
    val position = TextFieldState()
    val email = TextFieldState()
    private val _state = MutableStateFlow(OnboardingState(startDate = today))
    val state = _state.asStateFlow()

    fun setContract(contract: ContractType) = _state.update { it.copy(contract = contract) }

    fun setRole(role: UserRole) = _state.update { it.copy(role = role) }

    fun setStartDate(date: LocalDate) = _state.update { it.copy(startDate = date) }

    /** The credentials were handed over: forget them. */
    fun credentialsDismissed() = _state.update { it.copy(credentials = null) }

    fun submit() {
        if (_state.value.isSaving) return
        val issues =
            buildMap {
                StaffValidation.text(name, StaffLimits.NAME)?.let { put(StaffField.NAME, it) }
                StaffValidation.text(document, StaffLimits.DOCUMENT)?.let { put(StaffField.DOCUMENT, it) }
                StaffValidation.text(position, StaffLimits.POSITION)?.let { put(StaffField.POSITION, it) }
                StaffValidation.email(email)?.let { put(StaffField.EMAIL, it) }
            }
        if (issues.isNotEmpty()) {
            _state.update { it.copy(issues = issues, error = null) }
            return
        }
        val current = _state.value
        val onboarding =
            Onboarding(
                name = name.text.toString().trim(),
                identityDocument = document.text.toString().trim(),
                position = position.text.toString().trim(),
                contract = current.contract,
                startDate = current.startDate,
                email = email.text.toString().trim(),
                role = current.role,
            )
        _state.update { it.copy(isSaving = true, issues = emptyMap(), error = null) }
        viewModelScope.launch {
            when (val result = staff.repository.onboard(onboarding)) {
                is Result.Success -> _state.update { it.copy(isSaving = false, credentials = result.data, onboardedName = onboarding.name) }
                is Result.Failure ->
                    _state.update {
                        when (result.error) {
                            StaffError.InvalidDocument ->
                                it.copy(
                                    isSaving = false,
                                    issues = mapOf(StaffField.DOCUMENT to FieldIssue.INVALID),
                                )
                            else -> it.copy(isSaving = false, error = result.error.toUiText())
                        }
                    }
            }
        }
    }
}
