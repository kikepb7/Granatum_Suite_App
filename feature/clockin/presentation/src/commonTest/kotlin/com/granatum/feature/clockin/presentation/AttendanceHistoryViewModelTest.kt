package com.granatum.feature.clockin.presentation

import com.granatum.feature.clockin.domain.usecase.ObserveMonthUseCase
import com.granatum.feature.clockin.domain.usecase.RefreshMonthUseCase
import com.granatum.feature.clockin.presentation.history.AttendanceHistoryViewModel
import com.granatum.feature.clockin.presentation.history.HistoryAction
import com.granatum.feature.clockin.presentation.history.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceHistoryViewModelTest {
    private val repo = FakeClockInRepository()
    private val january = object : Clock { override fun now() = Instant.parse("2026-01-15T10:00:00Z") }

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun months_go_back_across_the_year_but_never_into_the_future() = runTest {
        val vm = AttendanceHistoryViewModel(ObserveMonthUseCase(repo), RefreshMonthUseCase(repo), january)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(YearMonth(2026, 1), vm.state.value.month)
        assertFalse(vm.state.value.canGoNext)

        vm.onAction(HistoryAction.OnNextMonth)
        assertEquals(YearMonth(2026, 1), vm.state.value.month)

        vm.onAction(HistoryAction.OnPreviousMonth)
        assertEquals(YearMonth(2025, 12), vm.state.value.month)
        assertTrue(vm.state.value.canGoNext)
        assertEquals(listOf(2026 to 1, 2025 to 12), repo.refreshed)
    }
}
