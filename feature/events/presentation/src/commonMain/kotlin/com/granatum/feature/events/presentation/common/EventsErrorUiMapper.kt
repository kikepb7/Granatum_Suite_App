package com.granatum.feature.events.presentation.common

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.domain.model.EventsError
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.error_date_in_past
import granatumsuite.feature.events.presentation.generated.resources.error_event_finished
import granatumsuite.feature.events.presentation.generated.resources.error_event_not_found
import granatumsuite.feature.events.presentation.generated.resources.error_forbidden
import granatumsuite.feature.events.presentation.generated.resources.error_insufficient_stock
import granatumsuite.feature.events.presentation.generated.resources.error_invalid
import granatumsuite.feature.events.presentation.generated.resources.error_material_not_found
import granatumsuite.feature.events.presentation.generated.resources.error_no_internet
import granatumsuite.feature.events.presentation.generated.resources.error_unknown

/** What each error tells the person; the insufficient stock one says how many units are left. */
internal fun EventsError.toUiText(): UiText = when (this) {
    EventsError.NoInternet -> UiText.Resource(Res.string.error_no_internet)
    EventsError.EventNotFound -> UiText.Resource(Res.string.error_event_not_found)
    EventsError.MaterialNotFound -> UiText.Resource(Res.string.error_material_not_found)
    is EventsError.InsufficientStock -> UiText.Resource(Res.string.error_insufficient_stock, arrayOf(available))
    EventsError.EventFinished -> UiText.Resource(Res.string.error_event_finished)
    EventsError.DateInPast -> UiText.Resource(Res.string.error_date_in_past)
    EventsError.Invalid -> UiText.Resource(Res.string.error_invalid)
    EventsError.Forbidden -> UiText.Resource(Res.string.error_forbidden)
    EventsError.Unknown -> UiText.Resource(Res.string.error_unknown)
}
