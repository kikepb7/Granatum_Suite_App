package com.granatum.feature.clockin.domain.model

import com.granatum.core.domain.util.Error

/**
 * Local state-machine violations, checked against the last known event
 * before writing a new one — this validation happens even offline, since
 * it only needs the local event log, never the network.
 */
sealed interface ClockActionError : Error {
    data object AlreadyClockedIn : ClockActionError
    data object NotClockedIn : ClockActionError
    data object AlreadyOnBreak : ClockActionError
    data object NotOnBreak : ClockActionError

    /** No session: a punch must belong to someone (spec 004, FR-028). */
    data object NoSession : ClockActionError
}

sealed interface RequestCorrectionError : Error {
    data object BlankReason : RequestCorrectionError
    data class Remote(val dataError: com.granatum.core.domain.util.DataError.Remote) : RequestCorrectionError
}
