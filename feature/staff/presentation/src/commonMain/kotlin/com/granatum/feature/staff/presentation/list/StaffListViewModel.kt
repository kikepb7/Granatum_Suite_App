package com.granatum.feature.staff.presentation.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.staff.domain.StaffMember
import com.granatum.feature.staff.domain.StaffUseCases
import com.granatum.feature.staff.domain.search
import com.granatum.feature.staff.presentation.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffListState(
    val members: List<StaffMember> = emptyList(),
    /** null: everyone; true: active only (the default); false: those on leave. */
    val active: Boolean? = true,
    val isLoading: Boolean = true,
    val error: UiText? = null,
) {
    fun visible(query: String) = members.search(query, active)
}

/** The list is fetched whenever the screen shows, so edits made in a record are seen on return. */
class StaffListViewModel(
    private val staff: StaffUseCases,
) : ViewModel() {
    val query = TextFieldState()
    private val _state = MutableStateFlow(StaffListState())
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.members.isEmpty(), error = null) }
            when (val result = staff.repository.members()) {
                is Result.Success -> _state.update { it.copy(members = result.data, isLoading = false) }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    fun filter(active: Boolean?) = _state.update { it.copy(active = active) }
}
