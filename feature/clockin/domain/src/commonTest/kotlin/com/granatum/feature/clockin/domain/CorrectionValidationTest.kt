package com.granatum.feature.clockin.domain

import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.CorrectionDraft
import com.granatum.feature.clockin.domain.model.CorrectionIssue
import com.granatum.feature.clockin.domain.model.CorrectionValidation
import com.granatum.feature.clockin.domain.model.CorrectionValues
import com.granatum.feature.clockin.domain.model.ProposedBreak
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CorrectionValidationTest {
    private fun t(hhmm: String) = Instant.parse("2026-10-08T$hhmm:00Z")
    private fun draft(entry: String = "08:00", exit: String = "16:00", reason: String = "Olvidé fichar", vararg breaks: Pair<String, String>) =
        CorrectionDraft(reason, CorrectionValues(t(entry), t(exit), breaks.map { ProposedBreak(BreakType.COMIDA, t(it.first), t(it.second)) }))

    @Test
    fun a_coherent_day_is_valid() {
        assertTrue(CorrectionValidation.validate(draft(breaks = arrayOf("12:00" to "12:30", "14:00" to "14:10"))).isEmpty())
    }

    @Test
    fun each_rule_on_its_own() {
        assertEquals(setOf(CorrectionIssue.MISSING_REASON), CorrectionValidation.validate(draft(reason = " ")))
        assertEquals(setOf(CorrectionIssue.EXIT_BEFORE_ENTRY), CorrectionValidation.validate(draft(entry = "16:00", exit = "08:00")))
        assertEquals(setOf(CorrectionIssue.BREAK_ENDS_BEFORE_START), CorrectionValidation.validate(draft(breaks = arrayOf("12:30" to "12:00"))))
        assertEquals(setOf(CorrectionIssue.BREAK_OUTSIDE_SHIFT), CorrectionValidation.validate(draft(breaks = arrayOf("07:00" to "07:30"))))
        assertEquals(setOf(CorrectionIssue.BREAKS_OVERLAP), CorrectionValidation.validate(draft(breaks = arrayOf("12:00" to "12:30", "12:15" to "12:45"))))
    }
}
