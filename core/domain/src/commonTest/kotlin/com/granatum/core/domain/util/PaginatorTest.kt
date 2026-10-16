package com.granatum.core.domain.util

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Runs a block that never really suspends, so the test needs no coroutine-test dependency. */
private fun runNow(block: suspend () -> Unit) {
    var failure: Throwable? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { it.exceptionOrNull()?.let { e -> failure = e } })
    failure?.let { throw it }
}

class PaginatorTest {

    private val requested = mutableListOf<Int>()
    private val loadStates = mutableListOf<Boolean>()
    private val successes = mutableListOf<Pair<List<String>, Int>>()
    private val errors = mutableListOf<Throwable?>()
    private var response: suspend (Int) -> Result<List<String>, DataError> = { key -> Result.Success(listOf("item$key")) }

    private fun paginator() = Paginator<Int, String>(
        initialKey = 1,
        onLoadUpdated = { loadStates += it },
        onRequest = { key -> requested += key; response(key) },
        getNextKey = { requested.last() + 1 },
        onError = { errors += it },
        onSuccess = { items, key -> successes += items to key }
    )

    @Test
    fun a_successful_page_advances_the_key_and_toggles_loading() = runNow {
        val p = paginator()
        p.loadNextItems()
        p.loadNextItems()

        assertEquals(listOf(1, 2), requested)
        assertEquals(listOf(listOf("item1") to 2, listOf("item2") to 3), successes)
        assertEquals(listOf(true, false, true, false), loadStates)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun a_failed_page_reports_a_data_error_and_retries_the_same_key_only_after_reset() = runNow {
        response = { Result.Failure(DataError.Remote.NO_INTERNET) }
        val p = paginator()

        p.loadNextItems()
        val error = errors.single()
        assertIs<DataErrorException>(error)
        assertEquals(DataError.Remote.NO_INTERNET, error.error)
        assertTrue(successes.isEmpty())
        assertEquals(false, loadStates.last())

        // The key did not advance and was already requested: asking again sends nothing.
        p.loadNextItems()
        assertEquals(listOf(1), requested)

        p.reset()
        p.loadNextItems()
        assertEquals(listOf(1, 1), requested)
    }

    @Test
    fun an_exception_from_the_request_is_reported_and_releases_the_guard() = runNow {
        val boom = IllegalStateException("boom")
        response = { throw boom }
        val p = paginator()

        p.loadNextItems()

        assertEquals(listOf<Throwable?>(boom), errors)
        assertEquals(listOf(true, false), loadStates)

        response = { Result.Success(listOf("ok")) }
        p.reset()
        p.loadNextItems()
        assertEquals(1, successes.size)
    }

    @Test
    fun a_request_in_flight_blocks_re_entrant_loads() = runNow {
        lateinit var p: Paginator<Int, String>
        response = { key ->
            // Re-entering while the first request is still running must be ignored.
            p.loadNextItems()
            Result.Success(listOf("item$key"))
        }
        p = paginator()

        p.loadNextItems()

        assertEquals(listOf(1), requested)
        assertEquals(1, successes.size)
    }

    @Test
    fun reset_goes_back_to_the_initial_key() = runNow {
        val p = paginator()
        p.loadNextItems()
        p.loadNextItems()
        p.reset()
        p.loadNextItems()

        assertEquals(listOf(1, 2, 1), requested)
    }
}
