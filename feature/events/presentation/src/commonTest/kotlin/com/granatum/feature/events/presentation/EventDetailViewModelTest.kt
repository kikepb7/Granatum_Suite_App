package com.granatum.feature.events.presentation

import app.cash.turbine.test
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.domain.model.EventStatus
import com.granatum.feature.events.domain.usecase.DeleteEventUseCase
import com.granatum.feature.events.domain.usecase.FetchAvailableMaterialsUseCase
import com.granatum.feature.events.domain.usecase.FinishEventUseCase
import com.granatum.feature.events.domain.usecase.ObserveEventUseCase
import com.granatum.feature.events.domain.usecase.RefreshEventsUseCase
import com.granatum.feature.events.domain.usecase.SetEventMaterialUseCase
import com.granatum.feature.events.presentation.common.EventDateFormatter
import com.granatum.feature.events.presentation.common.EventUiMapper
import com.granatum.feature.events.presentation.detail.EventDetailDialog
import com.granatum.feature.events.presentation.detail.EventDetailScreenUiState
import com.granatum.feature.events.presentation.detail.EventDetailUiAction
import com.granatum.feature.events.presentation.detail.EventDetailUiEvent
import com.granatum.feature.events.presentation.detail.EventDetailUiState
import com.granatum.feature.events.presentation.detail.EventDetailViewModel
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.error_insufficient_stock
import granatumsuite.feature.events.presentation.generated.resources.event_finished_message
import granatumsuite.feature.events.presentation.generated.resources.material_added
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

    /** Shared by Main and runTest, so the quantity debounce runs on virtual time. */
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repository = FakeEventsRepository(listOf(event("e1", materials = listOf(eventMaterial("m1", 5)))))

    private fun sut(canManage: Boolean = true, eventId: String = "e1") = EventDetailViewModel(
        eventId = eventId,
        canManage = canManage,
        observeEventUseCase = ObserveEventUseCase(repository),
        refreshEventsUseCase = RefreshEventsUseCase(repository),
        setEventMaterialUseCase = SetEventMaterialUseCase(repository),
        finishEventUseCase = FinishEventUseCase(repository),
        deleteEventUseCase = DeleteEventUseCase(repository),
        fetchAvailableMaterialsUseCase = FetchAvailableMaterialsUseCase(repository),
        mapper = EventUiMapper(EventDateFormatter()),
        dates = EventDateFormatter(),
        todayProvider = todayProvider
    )

    private suspend fun EventDetailViewModel.loaded(): EventDetailUiState =
        assertIs<EventDetailScreenUiState.Loaded>(uiState.first { it is EventDetailScreenUiState.Loaded }).data

    private fun TestScope.keepCollecting(sut: EventDetailViewModel) = backgroundScope.launchCollect(sut)

    @Test
    fun `When material is chosen in the picker then it is added on top of what the event takes`() = runTest(dispatcher) {
        repository.stock["m1"] = 8
        repository.stock["m2"] = 4
        val sut = sut()
        keepCollecting(sut)

        sut.uiEvents.test {
            sut.onAction(EventDetailUiAction.OpenPicker)
            sut.onAction(EventDetailUiAction.SelectPickerMaterial("m1"))
            sut.onAction(EventDetailUiAction.ChangePickerQuantity(3))
            sut.onAction(EventDetailUiAction.ConfirmPicker)

            assertEquals(expected = EventDetailUiEvent.ShowMessage(UiText.Resource(Res.string.material_added)), actual = awaitItem())
        }
        val actual = sut.loaded()
        assertEquals(expected = listOf("m1" to 8), actual = actual.materials.map { it.materialId to it.quantity })
        assertEquals(expected = 5, actual = repository.stock["m1"])
        assertNull(actual.picker)
    }

    @Test
    fun `When the picker quantity goes past the free units then it stops at the maximum`() = runTest(dispatcher) {
        repository.stock["m2"] = 4
        val sut = sut()
        keepCollecting(sut)

        sut.onAction(EventDetailUiAction.OpenPicker)
        sut.onAction(EventDetailUiAction.SelectPickerMaterial("m2"))
        sut.onAction(EventDetailUiAction.ChangePickerQuantity(9))
        val actual = sut.loaded().picker?.selection

        assertEquals(expected = 4, actual = actual?.quantity)
        assertEquals(expected = 4, actual = actual?.max)
    }

    @Test
    fun `When someone took the units meanwhile then the person is told how many are left`() = runTest(dispatcher) {
        repository.stock["m2"] = 4
        val sut = sut()
        keepCollecting(sut)
        val expected = EventDetailUiEvent.ShowMessage(UiText.Resource(Res.string.error_insufficient_stock, arrayOf(1)))

        sut.uiEvents.test {
            sut.onAction(EventDetailUiAction.OpenPicker)
            sut.onAction(EventDetailUiAction.SelectPickerMaterial("m2"))
            sut.onAction(EventDetailUiAction.ChangePickerQuantity(3))
            repository.stock["m2"] = 1
            sut.onAction(EventDetailUiAction.ConfirmPicker)

            assertEquals(expected = expected, actual = awaitItem())
        }
        val picker = sut.loaded().picker
        assertEquals(expected = 1, actual = picker?.items?.single { it.id == "m2" }?.freeUnits, message = "the list shows what is left")
        assertEquals(expected = listOf("m1"), actual = sut.loaded().materials.map { it.materialId })
    }

    @Test
    fun `When the quantity is tapped several times then only the final figure is sent`() = runTest(dispatcher) {
        repository.stock["m1"] = 10
        val sut = sut()
        keepCollecting(sut)

        sut.onAction(EventDetailUiAction.ChangeQuantity("m1", 6))
        sut.onAction(EventDetailUiAction.ChangeQuantity("m1", 7))
        sut.onAction(EventDetailUiAction.ChangeQuantity("m1", 8))
        val whilePending = sut.loaded().materials.single()
        advanceTimeBy(1_000)

        assertEquals(expected = 8 to true, actual = whilePending.quantity to whilePending.isSaving)
        assertEquals(expected = listOf(Triple("e1", "m1", 8)), actual = repository.setCalls)
        assertFalse(sut.loaded().materials.single().isSaving)
    }

    @Test
    fun `When a material is removed then it goes at once and its units return`() = runTest(dispatcher) {
        repository.stock["m1"] = 0
        val sut = sut()
        keepCollecting(sut)

        sut.onAction(EventDetailUiAction.RemoveMaterial("m1"))

        assertTrue(sut.loaded().materials.isEmpty())
        assertEquals(expected = 5, actual = repository.stock["m1"])
    }

    @Test
    fun `When finishing is confirmed then the event finishes and becomes read only`() = runTest(dispatcher) {
        val sut = sut()
        keepCollecting(sut)

        sut.onAction(EventDetailUiAction.RequestFinish)
        assertEquals(expected = EventDetailDialog.FINISH, actual = sut.loaded().dialog)
        sut.uiEvents.test {
            sut.onAction(EventDetailUiAction.ConfirmDialog)

            assertEquals(expected = EventDetailUiEvent.ShowMessage(UiText.Resource(Res.string.event_finished_message)), actual = awaitItem())
        }
        val actual = sut.loaded()
        assertTrue(actual.isFinished)
        assertFalse(actual.canEdit)
        assertEquals(expected = EventStatus.FINISHED, actual = repository.events.value.single().status)
    }

    @Test
    fun `When the role only reads then nothing can be edited and the picker does not open`() = runTest(dispatcher) {
        val sut = sut(canManage = false)
        keepCollecting(sut)

        sut.onAction(EventDetailUiAction.OpenPicker)
        val actual = sut.loaded()

        assertFalse(actual.canEdit)
        assertNull(actual.picker)
    }

    @Test
    fun `When the event no longer exists then the screen says so`() = runTest(dispatcher) {
        val sut = sut(eventId = "gone")

        sut.uiState.test {
            assertEquals(expected = EventDetailScreenUiState.NotFound, actual = expectMostRecentItem())
        }
    }

    @Test
    fun `When deleting is confirmed then the screen is told to leave`() = runTest(dispatcher) {
        val sut = sut()
        keepCollecting(sut)

        sut.uiEvents.test {
            sut.onAction(EventDetailUiAction.RequestDelete)
            sut.onAction(EventDetailUiAction.ConfirmDialog)

            assertEquals(expected = EventDetailUiEvent.Deleted, actual = awaitItem())
        }
        assertTrue(repository.events.value.isEmpty())
    }
}

private fun CoroutineScope.launchCollect(sut: EventDetailViewModel) = launch { sut.uiState.collect {} }
