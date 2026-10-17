package com.granatum.feature.clockin.presentation

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.ClockRejection
import com.granatum.feature.clockin.domain.model.CorrectionError
import com.granatum.feature.clockin.domain.model.CorrectionIssue
import com.granatum.feature.clockin.domain.model.CorrectionState
import com.granatum.feature.clockin.presentation.mapper.label
import com.granatum.feature.clockin.presentation.mapper.toUiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Every distinct problem must reach the person with its own message, never a shared one. */
class ClockErrorMappersTest {

    private fun key(text: UiText): String = assertIs<UiText.Resource>(text).id.key

    private fun assertAllDistinct(keys: List<String>) = assertEquals(keys.size, keys.toSet().size, "keys: $keys")

    @Test
    fun every_clock_action_error_has_its_own_message() {
        val errors = listOf(
            ClockActionError.AlreadyClockedIn, ClockActionError.NotClockedIn, ClockActionError.AlreadyOnBreak,
            ClockActionError.NotOnBreak, ClockActionError.OnBreak, ClockActionError.NoSession
        )
        assertAllDistinct(errors.map { key(it.toUiText()) })
        assertEquals("error_already_clocked_in", key(ClockActionError.AlreadyClockedIn.toUiText()))
    }

    @Test
    fun every_server_rejection_has_its_own_message() {
        assertAllDistinct(ClockRejection.entries.map { key(it.toUiText()) })
        assertEquals("rejection_too_old", key(ClockRejection.TooOld.toUiText()))
    }

    @Test
    fun every_correction_issue_has_its_own_message() {
        assertAllDistinct(CorrectionIssue.entries.map { key(it.toUiText()) })
    }

    @Test
    fun an_invalid_correction_shows_its_first_issue_or_the_generic_text() {
        val issue = CorrectionIssue.MISSING_REASON
        assertEquals(key(issue.toUiText()), key(CorrectionError.Invalid(setOf(issue)).toUiText()))
        assertEquals("correction_error_invalid", key(CorrectionError.Invalid(emptySet()).toUiText()))
    }

    @Test
    fun the_other_correction_errors_have_their_own_message() {
        val keys = listOf(
            CorrectionError.ShiftNotFinished, CorrectionError.Inactive,
            CorrectionError.NoInternet, CorrectionError.Unknown
        ).map { key(it.toUiText()) }
        assertAllDistinct(keys + key(CorrectionError.Invalid(emptySet()).toUiText()))
    }

    @Test
    fun break_types_and_correction_states_have_distinct_labels() {
        assertAllDistinct(BreakType.entries.map { it.label().key })
        assertAllDistinct(CorrectionState.entries.map { it.label().key })
        assertEquals("break_meal", BreakType.MEAL.label().key)
        assertEquals("correction_state_pending", CorrectionState.PENDING.label().key)
    }
}
