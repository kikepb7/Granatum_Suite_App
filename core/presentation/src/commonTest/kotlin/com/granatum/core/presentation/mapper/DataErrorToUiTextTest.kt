package com.granatum.core.presentation.mapper

import com.granatum.core.domain.util.DataError
import com.granatum.core.presentation.util.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DataErrorToUiTextTest {

    private fun key(error: DataError) = assertIs<UiText.Resource>(error.toUiText()).id.key

    @Test
    fun every_remote_error_has_its_own_message() {
        val keys = DataError.Remote.entries.map { key(it) }
        assertEquals(keys.size, keys.toSet().size, "keys: $keys")
    }

    @Test
    fun well_known_errors_map_to_their_message() {
        assertEquals("error_no_internet", key(DataError.Remote.NO_INTERNET))
        assertEquals("error_server", key(DataError.Remote.SERVER_ERROR))
        assertEquals("error_disk_full", key(DataError.Local.DISK_FULL))
        assertEquals("error_unable_to_send_message", key(DataError.ConnectionModel.NOT_CONNECTED))
    }

    @Test
    fun local_and_remote_not_found_read_the_same() {
        assertEquals(key(DataError.Remote.NOT_FOUND), key(DataError.Local.NOT_FOUND))
        assertEquals(key(DataError.Remote.UNKNOWN), key(DataError.Local.UNKNOWN))
        assertEquals(key(DataError.Remote.NO_INTERNET), key(DataError.ConnectionModel.MESSAGE_SEND_FAILED))
    }
}
