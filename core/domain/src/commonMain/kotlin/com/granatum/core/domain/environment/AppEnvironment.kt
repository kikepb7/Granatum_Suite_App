package com.granatum.core.domain.environment

import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Error
import kotlinx.coroutines.flow.StateFlow

/**
 * The backend the app is talking to (specs/010-entornos). The store app (PRO build) only ever has
 * [PRO]; the internal app (PRE build) can switch between the ones configured for it.
 */
enum class AppEnvironment {
    /** The developer's machine. */
    LOCAL,

    /** Pre-production: the shared test server. */
    PRE,

    /** Production: real data. */
    PRO,

    /** No server: an in-memory imitation with sample data and ready-made accounts. PRE build only. */
    DEMO
}

sealed interface EnvironmentError : Error {
    /** Punches not yet sent would end up on the wrong server: switching waits for them. */
    data class UnsyncedWork(val count: Int) : EnvironmentError
    data object NotAvailable : EnvironmentError
}

interface EnvironmentRepository {
    val current: StateFlow<AppEnvironment>

    /** What this build can switch to, in display order. A single entry means no switching. */
    val available: List<AppEnvironment>

    val canSwitch: Boolean get() = available.size > 1

    /**
     * Signs out, clears every feature's local data and makes [environment] the active one,
     * remembered across launches. Refused while there is work not yet sent to the current server.
     */
    suspend fun switchTo(environment: AppEnvironment): EmptyResult<EnvironmentError>

    /** The ready-made accounts of the current environment: only Demo has any. */
    fun demoAccounts(): List<DemoAccount>
}

/**
 * Local data that belongs to one backend and must not survive a switch to another. Each feature
 * with a local database registers one.
 */
interface EnvironmentScopedData {
    /** Items recorded here and not yet accepted by the server. */
    suspend fun unsyncedItems(): Int

    suspend fun clear()
}
