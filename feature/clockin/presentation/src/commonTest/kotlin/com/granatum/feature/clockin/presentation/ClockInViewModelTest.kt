package com.granatum.feature.clockin.presentation

import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.TodayState
import com.granatum.feature.clockin.domain.usecase.ClockInUseCase
import com.granatum.feature.clockin.domain.usecase.ClockOutUseCase
import com.granatum.feature.clockin.domain.usecase.EndBreakUseCase
import com.granatum.feature.clockin.domain.usecase.ObserveTodayUseCase
import com.granatum.feature.clockin.domain.usecase.RefreshMonthUseCase
import com.granatum.feature.clockin.domain.usecase.StartBreakUseCase
import com.granatum.feature.clockin.presentation.clockin.ClockInAction
import com.granatum.feature.clockin.presentation.clockin.ClockInEvent
import com.granatum.feature.clockin.presentation.clockin.ClockInViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClockInViewModelTest {
    private val repo = FakeClockInRepository()

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ClockInViewModel(
        ObserveTodayUseCase(repo), RefreshMonthUseCase(repo), ClockInUseCase(repo),
        ClockOutUseCase(repo), StartBreakUseCase(repo), EndBreakUseCase(repo)
    )

    private fun status(s: ShiftStatus) { repo.today.value = TodayState(status = s) }

    @Test
    fun opening_the_screen_refreshes_the_current_month() = runTest {
        vm()
        assertEquals(1, repo.refreshed.size)
    }

    @Test
    fun starting_a_break_asks_for_its_type_first() = runTest {
        status(ShiftStatus.CLOCKED_IN)
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.onAction(ClockInAction.OnBreakToggleClick)
        assertTrue(vm.state.value.isChoosingBreak)
        assertTrue(repo.calls.isEmpty())

        vm.onAction(ClockInAction.OnBreakTypeChosen(BreakType.COMIDA))
        assertEquals(listOf("startBreak:COMIDA"), repo.calls)
        assertFalse(vm.state.value.isChoosingBreak)
    }

    @Test
    fun no_exit_while_on_a_break() = runTest {
        status(ShiftStatus.ON_BREAK)
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertFalse(vm.state.value.canClockOut)
        vm.onAction(ClockInAction.OnPrimaryButtonClick)
        assertTrue(repo.calls.isEmpty())
        vm.onAction(ClockInAction.OnBreakToggleClick)
        assertEquals(listOf("endBreak"), repo.calls)
    }

    @Test
    fun a_second_tap_while_processing_does_nothing() = runTest {
        repo.gate = CompletableDeferred()
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.onAction(ClockInAction.OnPrimaryButtonClick)
        vm.onAction(ClockInAction.OnPrimaryButtonClick)
        repo.gate!!.complete(Unit)
        assertEquals(listOf("clockIn"), repo.calls)
    }

    @Test
    fun a_refused_action_is_reported() = runTest {
        repo.actionResult = Result.Failure(ClockActionError.AlreadyClockedIn)
        val vm = vm()
        vm.onAction(ClockInAction.OnPrimaryButtonClick)
        assertIs<ClockInEvent.Error>(vm.events.first())
    }
}
