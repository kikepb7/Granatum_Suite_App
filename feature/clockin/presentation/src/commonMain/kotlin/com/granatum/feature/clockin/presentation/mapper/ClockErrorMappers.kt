package com.granatum.feature.clockin.presentation.mapper

import com.granatum.core.presentation.mapper.toUiText
import com.granatum.core.presentation.util.UiText
import com.granatum.core.presentation.util.UiText.DynamicString
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.RequestCorrectionError

fun ClockActionError.toUiText(): UiText = when (this) {
    ClockActionError.AlreadyClockedIn -> DynamicString(value = "Ya has fichado la entrada")
    ClockActionError.NotClockedIn -> DynamicString(value = "Todavía no has fichado la entrada")
    ClockActionError.AlreadyOnBreak -> DynamicString(value = "Ya estás en pausa")
    ClockActionError.NotOnBreak -> DynamicString(value = "No estás en pausa")
    ClockActionError.NoSession -> DynamicString(value = "Inicia sesión para fichar")
}

fun RequestCorrectionError.toUiText(): UiText = when (this) {
    RequestCorrectionError.BlankReason -> DynamicString(value = "Indica el motivo de la corrección")
    is RequestCorrectionError.Remote -> dataError.toUiText()
}
