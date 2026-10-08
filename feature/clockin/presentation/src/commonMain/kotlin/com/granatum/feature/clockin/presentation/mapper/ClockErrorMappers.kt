package com.granatum.feature.clockin.presentation.mapper

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.ClockRejection
import com.granatum.feature.clockin.domain.model.CorrectionError
import com.granatum.feature.clockin.domain.model.CorrectionIssue
import com.granatum.feature.clockin.domain.model.CorrectionState
import granatumsuite.feature.clockin.presentation.generated.resources.Res
import granatumsuite.feature.clockin.presentation.generated.resources.*

/** Text for the person lives here, in presentation, never in domain (constitution IV). */
fun ClockActionError.toUiText(): UiText = UiText.Resource(
    when (this) {
        ClockActionError.AlreadyClockedIn -> Res.string.error_already_clocked_in
        ClockActionError.NotClockedIn -> Res.string.error_not_clocked_in
        ClockActionError.AlreadyOnBreak -> Res.string.error_already_on_break
        ClockActionError.NotOnBreak -> Res.string.error_not_on_break
        ClockActionError.OnBreak -> Res.string.error_on_break
        ClockActionError.NoSession -> Res.string.error_no_session
    }
)

fun ClockRejection.toUiText(): UiText = UiText.Resource(
    when (this) {
        ClockRejection.TooOld -> Res.string.rejection_too_old
        ClockRejection.ClockSkew -> Res.string.rejection_clock_skew
        ClockRejection.Inactive -> Res.string.rejection_inactive
        ClockRejection.InvalidTransition -> Res.string.rejection_invalid_transition
        ClockRejection.ShiftNotRegistered -> Res.string.rejection_shift_not_registered
        ClockRejection.Unexpected -> Res.string.rejection_unexpected
    }
)

fun BreakType.label() = when (this) {
    BreakType.COMIDA -> Res.string.break_comida
    BreakType.DESCANSO -> Res.string.break_descanso
    BreakType.OTRO -> Res.string.break_otro
}

fun CorrectionIssue.toUiText(): UiText = UiText.Resource(
    when (this) {
        CorrectionIssue.MISSING_REASON -> Res.string.issue_missing_reason
        CorrectionIssue.EXIT_BEFORE_ENTRY -> Res.string.issue_exit_before_entry
        CorrectionIssue.BREAK_ENDS_BEFORE_START -> Res.string.issue_break_ends_before_start
        CorrectionIssue.BREAK_OUTSIDE_SHIFT -> Res.string.issue_break_outside_shift
        CorrectionIssue.BREAKS_OVERLAP -> Res.string.issue_breaks_overlap
    }
)

fun CorrectionError.toUiText(): UiText = when (this) {
    is CorrectionError.Invalid -> issues.firstOrNull()?.toUiText() ?: UiText.Resource(Res.string.correction_error_invalid)
    CorrectionError.ShiftNotFinished -> UiText.Resource(Res.string.correction_error_not_finished)
    CorrectionError.Inactive -> UiText.Resource(Res.string.correction_error_inactive)
    CorrectionError.NoInternet -> UiText.Resource(Res.string.correction_error_no_internet)
    CorrectionError.Unknown -> UiText.Resource(Res.string.correction_error_unknown)
}

fun CorrectionState.label() = when (this) {
    CorrectionState.PENDIENTE -> Res.string.correction_state_pendiente
    CorrectionState.APROBADA -> Res.string.correction_state_aprobada
    CorrectionState.RECHAZADA -> Res.string.correction_state_rechazada
}
