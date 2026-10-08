package com.granatum.feature.staff.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.util.Result
import com.granatum.feature.staff.domain.ContractType
import com.granatum.feature.staff.domain.Onboarding
import com.granatum.feature.staff.domain.StaffEdit
import com.granatum.feature.staff.domain.StaffError
import com.granatum.feature.staff.domain.StaffMember
import com.granatum.feature.staff.domain.StaffRepository
import com.granatum.feature.staff.domain.StaffUseCases
import com.granatum.feature.staff.domain.TemporaryCredentials
import com.granatum.feature.staff.presentation.common.FieldIssue
import com.granatum.feature.staff.presentation.common.StaffField
import com.granatum.feature.staff.presentation.detail.StaffDetailAction
import com.granatum.feature.staff.presentation.detail.StaffDetailViewModel
import com.granatum.feature.staff.presentation.list.StaffListViewModel
import com.granatum.feature.staff.presentation.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun member(
    id: String,
    name: String,
    active: Boolean = true,
) = StaffMember(id, name, "12345678Z", "Florista", ContractType.FULL_TIME, LocalDate(2026, 1, 1), active)

private class FakeStaffRepository : StaffRepository {
    val members =
        mutableMapOf(
            "e1" to member("e1", "Ana"),
            "e2" to member("e2", "Luis", active = false),
            "me" to member("me", "Propietaria"),
        )
    var failNext: StaffError? = null
    var hasAccount = true
    val onboarded = mutableListOf<Onboarding>()
    val updates = mutableListOf<Pair<String, StaffEdit>>()
    val granted = mutableListOf<Triple<String, String, UserRole>>()

    private fun <T> answer(value: () -> T): Result<T, StaffError> =
        failNext?.let {
            failNext = null
            Result.Failure(it)
        } ?: Result.Success(value())

    override suspend fun members() = answer { members.values.toList() }

    override suspend fun member(id: String) = answer { members.getValue(id) }

    override suspend fun update(
        id: String,
        edit: StaffEdit,
    ) = answer {
        updates += id to edit
        members
            .getValue(
                id,
            ).copy(name = edit.name.trim(), position = edit.position.trim(), contract = edit.contract, startDate = edit.startDate)
            .also {
                members[id] =
                    it
            }
    }

    override suspend fun setActive(
        id: String,
        active: Boolean,
    ) = answer {
        members.getValue(id).copy(active = active).also {
            members[id] =
                it
        }
    }

    override suspend fun onboard(onboarding: Onboarding) =
        answer {
            onboarded += onboarding
            TemporaryCredentials("n1", onboarding.email, onboarding.role, "Tmp-123", recordCreated = true)
        }

    override suspend fun grantAccess(
        employeeId: String,
        email: String,
        role: UserRole,
    ) = answer {
        granted += Triple(employeeId, email, role)
        TemporaryCredentials(employeeId, email, role, "Tmp-456")
    }

    override suspend fun resetPassword(employeeId: String): Result<TemporaryCredentials, StaffError> =
        if (!hasAccount) {
            Result.Failure(StaffError.NoAccount)
        } else {
            answer {
                TemporaryCredentials(employeeId, "a@b.es", UserRole.EMPLEADO, "Tmp-789")
            }
        }
}

private class FakeSessionStorage : SessionStorage {
    private val flow = MutableStateFlow<Session?>(Session("a", "r", "me", UserRole.ADMIN, false, "owner@granatum.es"))

    override fun observeSession(): Flow<Session?> = flow

    override suspend fun set(session: Session?) {
        flow.value = session
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelsTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeStaffRepository()

    @Test
    fun the_list_shows_active_people_by_default_and_can_switch() =
        runTest {
            val vm = StaffListViewModel(StaffUseCases(repo))
            vm.refresh()
            assertEquals(
                listOf("Ana", "Propietaria"),
                vm.state.value
                    .visible("")
                    .map { it.name },
            )
            vm.filter(false)
            assertEquals(
                listOf("Luis"),
                vm.state.value
                    .visible("")
                    .map { it.name },
            )
            vm.filter(null)
            assertEquals(
                listOf("Ana"),
                vm.state.value
                    .visible("an")
                    .map { it.name },
            )
        }

    @Test
    fun the_list_reports_a_failed_load() =
        runTest {
            repo.failNext = StaffError.NoInternet
            val vm = StaffListViewModel(StaffUseCases(repo))
            vm.refresh()
            assertNotNull(vm.state.value.error)
        }

    @Test
    fun onboarding_validates_then_shows_the_credentials_once() =
        runTest {
            val vm = OnboardingViewModel(StaffUseCases(repo), today = LocalDate(2026, 10, 9))
            vm.submit()
            assertEquals(setOf(StaffField.NAME, StaffField.DOCUMENT, StaffField.POSITION, StaffField.EMAIL), vm.state.value.issues.keys)

            vm.name.setTextAndPlaceCursorAtEnd("Marta Gil")
            vm.document.setTextAndPlaceCursorAtEnd("12345678Z")
            vm.position.setTextAndPlaceCursorAtEnd("Florista")
            vm.email.setTextAndPlaceCursorAtEnd("marta@")
            vm.submit()
            assertEquals(mapOf(StaffField.EMAIL to FieldIssue.INVALID), vm.state.value.issues)

            vm.email.setTextAndPlaceCursorAtEnd("marta@granatum.es")
            vm.setRole(UserRole.ENCARGADO)
            vm.setContract(ContractType.PART_TIME)
            vm.submit()
            val sent = repo.onboarded.single()
            assertEquals(UserRole.ENCARGADO, sent.role)
            assertEquals(ContractType.PART_TIME, sent.contract)
            assertEquals(LocalDate(2026, 10, 9), sent.startDate)
            assertEquals(
                "Tmp-123",
                vm.state.value.credentials
                    ?.password,
            )
            vm.credentialsDismissed()
            assertNull(vm.state.value.credentials, "forgotten once handed over")
        }

    @Test
    fun an_invalid_document_lands_on_its_field() =
        runTest {
            val vm = OnboardingViewModel(StaffUseCases(repo), today = LocalDate(2026, 10, 9))
            vm.name.setTextAndPlaceCursorAtEnd("Marta")
            vm.document.setTextAndPlaceCursorAtEnd("12345678A")
            vm.position.setTextAndPlaceCursorAtEnd("Florista")
            vm.email.setTextAndPlaceCursorAtEnd("marta@granatum.es")
            repo.failNext = StaffError.InvalidDocument
            vm.submit()
            assertEquals(FieldIssue.INVALID, vm.state.value.issues[StaffField.DOCUMENT])
            assertNull(vm.state.value.credentials)
        }

    @Test
    fun a_record_is_edited_without_its_document() =
        runTest {
            val vm = StaffDetailViewModel("e1", StaffUseCases(repo), FakeSessionStorage())
            assertEquals("Ana", vm.name.text.toString())
            vm.position.setTextAndPlaceCursorAtEnd("Encargada")
            vm.onAction(StaffDetailAction.OnContract(ContractType.HOURLY))
            vm.onAction(StaffDetailAction.OnSave)

            val (id, edit) = repo.updates.single()
            assertEquals("e1", id)
            assertEquals("Encargada", edit.position)
            assertEquals(ContractType.HOURLY, edit.contract)
            assertNotNull(vm.state.value.message)
        }

    @Test
    fun deactivating_asks_first_and_reactivating_does_not() =
        runTest {
            val vm = StaffDetailViewModel("e1", StaffUseCases(repo), FakeSessionStorage())
            vm.onAction(StaffDetailAction.OnToggleActive)
            assertTrue(vm.state.value.confirmingDeactivate)
            assertTrue(repo.members.getValue("e1").active)
            vm.onAction(StaffDetailAction.OnDeactivateConfirm)
            assertFalse(
                vm.state.value.member!!
                    .active,
            )

            vm.onAction(StaffDetailAction.OnToggleActive)
            assertTrue(
                vm.state.value.member!!
                    .active,
            )
        }

    @Test
    fun nobody_deactivates_their_own_record() =
        runTest {
            val vm = StaffDetailViewModel("me", StaffUseCases(repo), FakeSessionStorage())
            assertTrue(vm.state.value.isSelf)
            vm.onAction(StaffDetailAction.OnToggleActive)
            vm.onAction(StaffDetailAction.OnDeactivateConfirm)
            assertTrue(repo.members.getValue("me").active)
        }

    @Test
    fun resetting_shows_a_new_password() =
        runTest {
            val vm = StaffDetailViewModel("e1", StaffUseCases(repo), FakeSessionStorage())
            vm.onAction(StaffDetailAction.OnReset)
            assertTrue(vm.state.value.confirmingReset)
            vm.onAction(StaffDetailAction.OnResetConfirm)
            assertEquals(
                "Tmp-789",
                vm.state.value.credentials
                    ?.password,
            )
            vm.onAction(StaffDetailAction.OnCredentialsDismissed)
            assertNull(vm.state.value.credentials)
        }

    @Test
    fun a_record_without_account_is_offered_access() =
        runTest {
            repo.hasAccount = false
            val vm = StaffDetailViewModel("e1", StaffUseCases(repo), FakeSessionStorage())
            vm.onAction(StaffDetailAction.OnReset)
            vm.onAction(StaffDetailAction.OnResetConfirm)
            assertTrue(vm.state.value.grantingAccess)

            vm.onAction(StaffDetailAction.OnGrantConfirm)
            assertEquals(FieldIssue.REQUIRED, vm.state.value.issues[StaffField.EMAIL])
            vm.grantEmail.setTextAndPlaceCursorAtEnd("ana@granatum.es")
            vm.onAction(StaffDetailAction.OnGrantRole(UserRole.REPRESENTANTE))
            vm.onAction(StaffDetailAction.OnGrantConfirm)
            assertEquals(Triple("e1", "ana@granatum.es", UserRole.REPRESENTANTE), repo.granted.single())
            assertEquals(
                "Tmp-456",
                vm.state.value.credentials
                    ?.password,
            )
        }
}
