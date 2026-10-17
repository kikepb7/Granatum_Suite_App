package com.granatum.feature.events.presentation

import app.cash.turbine.test
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.domain.model.EventStatus
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.usecase.ObserveEventsUseCase
import com.granatum.feature.events.domain.usecase.RefreshEventsUseCase
import com.granatum.feature.events.presentation.common.EventDateFormatter
import com.granatum.feature.events.presentation.common.EventUiMapper
import com.granatum.feature.events.presentation.list.EventListScreenUiState
import com.granatum.feature.events.presentation.list.EventListUiAction
import com.granatum.feature.events.presentation.list.EventListUiEvent
import com.granatum.feature.events.presentation.list.EventListViewModel
import com.granatum.feature.events.presentation.list.EventTab
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.error_forbidden
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventListViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repository = FakeEventsRepository(
        listOf(
            event("late", date = LocalDate(2027, 7, 1), materials = listOf(eventMaterial("m1", 5))),
            event("soon", date = LocalDate(2027, 6, 14), materials = listOf(eventMaterial("m2", 2))),
            event("old", date = LocalDate(2027, 5, 1), status = EventStatus.FINISHED)
        )
    )

    private fun sut() = EventListViewModel(
        ObserveEventsUseCase(repository),
        RefreshEventsUseCase(repository),
        EventUiMapper(EventDateFormatter()),
        todayProvider
    )

    @Test
    fun `When the list opens then it refreshes and shows the upcoming events soonest first`() = runTest {
        val sut = sut()

        sut.uiState.test {
            val actual = assertIs<EventListScreenUiState.Loaded>(expectMostRecentItem()).data

            assertEquals(expected = listOf("soon", "late"), actual = actual.upcoming.map { it.id })
            assertEquals(expected = listOf("old"), actual = actual.finished.map { it.id })
            assertEquals(expected = 7, actual = actual.reservedUnits)
            assertEquals(expected = "soon", actual = actual.nextEvent?.id)
            assertEquals(expected = listOf("late"), actual = actual.shown.map { it.id }, message = "the next one is shown large, not twice")
            assertEquals(expected = 1, actual = repository.refreshes)
        }
    }

    @Test
    fun `When the finished tab is chosen then the finished events are shown`() = runTest {
        val sut = sut()

        sut.uiState.test {
            expectMostRecentItem()
            sut.onAction(EventListUiAction.SelectTab(EventTab.FINISHED))
            val actual = assertIs<EventListScreenUiState.Loaded>(awaitItem()).data

            assertEquals(expected = EventTab.FINISHED, actual = actual.selectedTab)
            assertEquals(expected = listOf("old"), actual = actual.shown.map { it.id })
        }
    }

    @Test
    fun `When the refresh has no coverage then the list is marked stale without a message`() = runTest {
        repository.failure = EventsError.NoInternet
        val sut = sut()

        sut.uiState.test {
            val actual = assertIs<EventListScreenUiState.Loaded>(expectMostRecentItem()).data

            assertTrue(actual.isStale)
        }
    }

    @Test
    fun `When the refresh fails for another reason then a message is emitted`() = runTest {
        val sut = sut()
        val expected = EventListUiEvent.ShowError(UiText.Resource(Res.string.error_forbidden))

        sut.uiEvents.test {
            repository.failure = EventsError.Forbidden
            sut.onAction(EventListUiAction.Refresh)

            assertEquals(expected = expected, actual = awaitItem())
        }
    }
}
