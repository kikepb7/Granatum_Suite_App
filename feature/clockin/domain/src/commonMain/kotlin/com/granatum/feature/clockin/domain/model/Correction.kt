package com.granatum.feature.clockin.domain.model

import com.granatum.core.domain.util.Error
import kotlin.time.Instant

enum class CorrectionState { PENDIENTE, APROBADA, RECHAZADA }

data class ProposedBreak(val type: BreakType, val start: Instant, val end: Instant)

data class CorrectionValues(val entry: Instant, val exit: Instant, val breaks: List<ProposedBreak>)

data class CorrectionModel(
    val id: String,
    val shiftServerId: String,
    val state: CorrectionState,
    val reason: String,
    val proposed: CorrectionValues,
    val resolutionReason: String?,
    val createdAt: Instant,
    val resolvedAt: Instant?
)

data class CorrectionDraft(val reason: String, val values: CorrectionValues)

/** What the server would call VALORES_INCOHERENTES, caught before sending (FR-020). */
enum class CorrectionIssue {
    MISSING_REASON,
    EXIT_BEFORE_ENTRY,
    BREAK_ENDS_BEFORE_START,
    BREAK_OUTSIDE_SHIFT,
    BREAKS_OVERLAP
}

object CorrectionValidation {
    fun validate(draft: CorrectionDraft): Set<CorrectionIssue> = buildSet {
        val values = draft.values
        if (draft.reason.isBlank()) add(CorrectionIssue.MISSING_REASON)
        if (values.exit <= values.entry) add(CorrectionIssue.EXIT_BEFORE_ENTRY)
        values.breaks.forEach { b ->
            if (b.end <= b.start) add(CorrectionIssue.BREAK_ENDS_BEFORE_START)
            if (b.start < values.entry || b.end > values.exit) add(CorrectionIssue.BREAK_OUTSIDE_SHIFT)
        }
        values.breaks.sortedBy { it.start }.zipWithNext().forEach { (a, b) ->
            if (b.start < a.end) add(CorrectionIssue.BREAKS_OVERLAP)
        }
    }
}

sealed interface CorrectionError : Error {
    data class Invalid(val issues: Set<CorrectionIssue>) : CorrectionError
    data object ShiftNotFinished : CorrectionError
    data object Inactive : CorrectionError
    data object NoInternet : CorrectionError
    data object Unknown : CorrectionError
}
