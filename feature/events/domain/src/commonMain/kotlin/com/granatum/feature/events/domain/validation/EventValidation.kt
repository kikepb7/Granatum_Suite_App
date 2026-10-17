package com.granatum.feature.events.domain.validation

import com.granatum.feature.events.domain.model.EventDraft
import kotlinx.datetime.LocalDate

enum class EventIssue { NAME_REQUIRED, NAME_TOO_LONG, DESCRIPTION_TOO_LONG, DATE_IN_PAST }

/** The server's limits (backend 011), checked before sending. */
object EventValidation {
    const val NAME_MAX = 100
    const val DESCRIPTION_MAX = 500

    fun validate(draft: EventDraft, today: LocalDate): Set<EventIssue> = buildSet {
        val name = draft.name.trim()
        if (name.isEmpty()) add(EventIssue.NAME_REQUIRED)
        if (name.length > NAME_MAX) add(EventIssue.NAME_TOO_LONG)
        if ((draft.description?.trim()?.length ?: 0) > DESCRIPTION_MAX) add(EventIssue.DESCRIPTION_TOO_LONG)
        if (draft.date < today) add(EventIssue.DATE_IN_PAST)
    }

    /** The name the form proposes: «Evento N», one more than the events there are. */
    fun suggestedName(existing: Int): String = "Evento ${existing + 1}"
}
