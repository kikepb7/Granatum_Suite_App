package com.granatum.feature.staff.presentation.common

import androidx.compose.foundation.text.input.TextFieldState
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.staff.domain.ContractType
import com.granatum.feature.staff.domain.StaffError
import com.granatum.feature.staff.domain.StaffLimits
import granatumsuite.feature.staff.presentation.generated.resources.*
import granatumsuite.feature.staff.presentation.generated.resources.Res
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

fun ContractType.label() =
    when (this) {
        ContractType.FULL_TIME -> Res.string.contract_full_time
        ContractType.PART_TIME -> Res.string.contract_part_time
        ContractType.HOURLY -> Res.string.contract_hourly
    }

fun UserRole.label() =
    when (this) {
        UserRole.ADMIN -> Res.string.role_admin
        UserRole.ENCARGADO -> Res.string.role_manager
        UserRole.REPRESENTANTE -> Res.string.role_representative
        else -> Res.string.role_employee
    }

fun StaffError.toUiText(): UiText =
    UiText.Resource(
        when (this) {
            StaffError.NoInternet -> Res.string.error_no_internet
            StaffError.NotFound -> Res.string.error_not_found
            StaffError.InvalidDocument -> Res.string.error_invalid_document
            StaffError.DuplicateDocument -> Res.string.error_duplicate_document
            StaffError.EmailTaken -> Res.string.error_email_taken
            StaffError.AccountExists -> Res.string.error_account_exists
            StaffError.Incoherent -> Res.string.error_incoherent
            StaffError.Invalid -> Res.string.error_invalid
            StaffError.Forbidden -> Res.string.error_forbidden
            StaffError.RateLimited -> Res.string.error_rate_limited
            StaffError.NoAccount, StaffError.Unknown -> Res.string.error_unknown
        },
    )

fun LocalDate.label(): String = "${day.toString().padStart(2, '0')}/${month.number.toString().padStart(2, '0')}/$year"

enum class StaffField { NAME, DOCUMENT, POSITION, EMAIL, START_DATE }

enum class FieldIssue { REQUIRED, TOO_LONG, INVALID }

private val EMAIL_SHAPE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Local checks with the server's limits; the server has the last word (DNI letter, duplicates). */
object StaffValidation {
    fun text(
        state: TextFieldState,
        max: Int,
    ): FieldIssue? {
        val value = state.text.toString().trim()
        return when {
            value.isEmpty() -> FieldIssue.REQUIRED
            value.length > max -> FieldIssue.TOO_LONG
            else -> null
        }
    }

    fun email(state: TextFieldState): FieldIssue? =
        text(state, StaffLimits.EMAIL)
            ?: FieldIssue.INVALID.takeUnless { EMAIL_SHAPE.matches(state.text.toString().trim()) }
}
