package com.granatum.feature.clockin.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.CorrectionError
import com.granatum.feature.clockin.domain.model.CorrectionIssue
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.CorrectionState
import com.granatum.feature.clockin.domain.model.CorrectionValues
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.ShiftState
import com.granatum.feature.clockin.domain.model.SyncState
import com.granatum.feature.clockin.domain.model.TimelineItem
import com.granatum.feature.clockin.domain.usecase.ObserveShiftUseCase
import com.granatum.feature.clockin.domain.usecase.RequestCorrectionUseCase
import com.granatum.feature.clockin.presentation.correction.CorrectionAction
import com.granatum.feature.clockin.presentation.correction.CorrectionEvent
import com.granatum.feature.clockin.presentation.correction.CorrectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CorrectionViewModelTest {
    private val repo = FakeClockInRepository()
    private fun t(hhmm: String) = Instant.parse("2026-10-08T$hhmm:00Z")

    @BeforeTest fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo.shifts.value = listOf(
            ShiftModel(
                key = "srv", serverId = "srv", start = t("08:00"), end = t("16:00"),
                items = listOf(
                    TimelineItem(ClockEventType.CLOCK_IN, t("08:00"), SyncState.SYNCED),
                    TimelineItem(ClockEventType.BREAK_START, t("12:00"), SyncState.SYNCED, BreakType.COMIDA),
                    TimelineItem(ClockEventType.BREAK_END, t("12:30"), SyncState.SYNCED, BreakType.COMIDA),
                    TimelineItem(ClockEventType.CLOCK_OUT, t("16:00"), SyncState.SYNCED)
                ),
                state = ShiftState.CLOSED, workedMinutes = 450, isWorkedMinutesProvisional = false
            )
        )
    }
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm() = CorrectionViewModel("srv", ObserveShiftUseCase(repo), RequestCorrectionUseCase(repo), TimeZone.UTC)

    @Test
    fun it_starts_from_the_shift_as_it_stands() = runTest {
        val vm = vm()
        assertEquals("08:00", vm.entry.text.toString())
        assertEquals("16:00", vm.exit.text.toString())
        val b = vm.state.value.breaks.single()
        assertEquals(BreakType.COMIDA to ("12:00" to "12:30"), b.type to (b.start.text.toString() to b.end.text.toString()))
    }

    @Test
    fun a_badly_typed_time_is_caught_before_validation() = runTest {
        val vm = vm()
        vm.exit.setTextAndPlaceCursorAtEnd("4pm")
        vm.reason.setTextAndPlaceCursorAtEnd("motivo")
        vm.onAction(CorrectionAction.OnSubmit)
        assertTrue(vm.state.value.badTime)
        assertTrue(repo.drafts.isEmpty())
    }

    @Test
    fun incoherent_values_and_a_missing_reason_are_flagged() = runTest {
        val vm = vm()
        vm.exit.setTextAndPlaceCursorAtEnd("07:00")
        vm.onAction(CorrectionAction.OnSubmit)
        assertEquals(setOf(CorrectionIssue.MISSING_REASON, CorrectionIssue.EXIT_BEFORE_ENTRY, CorrectionIssue.BREAK_OUTSIDE_SHIFT), vm.state.value.issues)
        assertTrue(repo.drafts.isEmpty())
    }

    @Test
    fun a_valid_request_is_sent_with_the_shifts_day() = runTest {
        repo.correctionResult = Result.Success(
            CorrectionModel("c", "srv", CorrectionState.PENDIENTE, "m", CorrectionValues(t("08:00"), t("17:00"), emptyList()), null, t("18:00"), null)
        )
        val vm = vm()
        vm.exit.setTextAndPlaceCursorAtEnd("17:00")
        vm.reason.setTextAndPlaceCursorAtEnd("Olvidé fichar la salida")
        vm.onAction(CorrectionAction.OnSubmit)
        assertEquals(CorrectionEvent.Sent, vm.events.first())
        assertEquals(t("17:00"), repo.drafts.single().values.exit)
    }

    @Test
    fun without_coverage_it_says_so() = runTest {
        repo.correctionResult = Result.Failure(CorrectionError.NoInternet)
        val vm = vm()
        vm.reason.setTextAndPlaceCursorAtEnd("motivo")
        vm.onAction(CorrectionAction.OnSubmit)
        assertNotNull(vm.state.value.error)
    }
}
