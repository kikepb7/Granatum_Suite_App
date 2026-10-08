package com.granatum.core.data.auth.storage

import com.granatum.core.data.testing.FakeSecureStore
import com.granatum.core.data.testing.RecordingLogger
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureSessionStorageTest {

    private val session = Session(
        accessToken = "a", refreshToken = "r", employeeId = "e-1",
        role = UserRole.REPRESENTANTE, mustChangePassword = false, email = "ana@granatum.es"
    )

    @Test
    fun a_written_session_survives_a_fresh_instance() = runTest {
        val store = FakeSecureStore()
        SecureSessionStorage(store, RecordingLogger()).set(session)

        val reopened = SecureSessionStorage(store, RecordingLogger())
        reopened.load()
        assertEquals(session, reopened.observeSession().first())
    }

    @Test
    fun an_unreadable_session_is_discarded_and_the_app_starts_signed_out() = runTest {
        val store = FakeSecureStore(mapOf(SECURE_SESSION_KEY to """{"user":{"id":"1"}}"""))
        val storage = SecureSessionStorage(store, RecordingLogger())
        storage.load()
        assertNull(storage.observeSession().first())
        assertFalse(SECURE_SESSION_KEY in store.values)
        assertTrue(storage.isReady.value)
    }

    @Test
    fun the_previous_session_key_is_deleted_on_start() = runTest {
        val store = FakeSecureStore(mapOf(PREVIOUS_SECURE_SESSION_KEY to "anything"))
        SecureSessionStorage(store, RecordingLogger()).load()
        assertFalse(PREVIOUS_SECURE_SESSION_KEY in store.values)
    }

    @Test
    fun not_ready_until_read() = runTest {
        val storage = SecureSessionStorage(FakeSecureStore(), RecordingLogger())
        assertFalse(storage.isReady.value)
        storage.load()
        assertTrue(storage.isReady.value)
    }
}
