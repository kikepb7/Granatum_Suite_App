package com.granatum.feature.events.presentation

import app.cash.turbine.test
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.usecase.ObserveEventUseCase
import com.granatum.feature.events.domain.usecase.ObserveEventsUseCase
import com.granatum.feature.events.domain.usecase.SaveEventUseCase
import com.granatum.feature.events.presentation.common.EventDateFormatter
import com.granatum.feature.events.presentation.form.EventFormScreenUiState
import com.granatum.feature.events.presentation.form.EventFormUiAction
import com.granatum.feature.events.presentation.form.EventFormUiEvent
import com.granatum.feature.events.presentation.form.EventFormViewModel
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.date_required
import granatumsuite.feature.events.presentation.generated.resources.name_required
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
class EventFormViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repository = FakeEventsRepository(listOf(event("1"), event("2")))

    private fun sut(eventId: String? = null) = EventFormViewModel(
        eventId,
        ObserveEventUseCase(repository),
        ObserveEventsUseCase(repository),
        SaveEventUseCase(repository),
        todayProvider,
        EventDateFormatter()
    )

    @Test
    fun `When a new event is opened then the name proposes the next number`() = runTest {
        val sut = sut()
        val expected = "Evento 3"

        sut.uiState.test { cancelAndIgnoreRemainingEvents() }

        assertEquals(expected = expected, actual = sut.name.text.toString())
    }

    @Test
    fun `When the event is saved with a date then it is created and the screen is told`() = runTest {
        val sut = sut()
        val date = LocalDate(2027, 6, 20)
        val expected: List<Pair<String?, EventDraft>> = listOf(null to EventDraft("Evento 3", "Boda de Juan y María", date))

        sut.uiEvents.test {
            sut.description.setTextAndPlaceCursorAtEnd("Boda de Juan y María")
            sut.onAction(EventFormUiAction.DateSelected(date))
            sut.onAction(EventFormUiAction.Save)

            assertEquals(expected = EventFormUiEvent.Saved("new"), actual = awaitItem())
        }
        assertEquals(expected = expected, actual = repository.saved)
    }

    @Test
    fun `When there is no name and no date then both are pointed out and nothing is sent`() = runTest {
        val sut = sut()

        sut.uiState.test {
            expectMostRecentItem()
            sut.name.setTextAndPlaceCursorAtEnd("  ")
            sut.onAction(EventFormUiAction.Save)
            val actual = assertIs<EventFormScreenUiState.Loaded>(awaitItem()).data

            assertEquals(expected = UiText.Resource(Res.string.name_required), actual = actual.nameError)
            assertEquals(expected = UiText.Resource(Res.string.date_required), actual = actual.dateError)
        }
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `When an event is edited then its data is filled in and the date is shown`() = runTest {
        val sut = sut(eventId = "1")

        sut.uiState.test {
            val actual = assertIs<EventFormScreenUiState.Loaded>(expectMostRecentItem()).data

            assertTrue(actual.isEdit)
            assertEquals(expected = "dom, 13 jun 2027", actual = actual.dateLabel)
        }
        assertEquals(expected = "Evento 1", actual = sut.name.text.toString())
    }
}
