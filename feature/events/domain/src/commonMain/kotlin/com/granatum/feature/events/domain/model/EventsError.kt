package com.granatum.feature.events.domain.model

import com.granatum.core.domain.util.Error

/** The backend's error codes for events (specs/011-events/contracts/README.md), as the app uses them. */
sealed interface EventsError : Error {
    /** Every change needs a connection: nothing is queued for later. */
    data object NoInternet : EventsError
    data object EventNotFound : EventsError
    data object MaterialNotFound : EventsError

    /** Fewer free units than asked for; [available] is how many there are. */
    data class InsufficientStock(val available: Int) : EventsError
    data object EventFinished : EventsError
    data object DateInPast : EventsError
    data object Invalid : EventsError
    data object Forbidden : EventsError
    data object Unknown : EventsError
}
