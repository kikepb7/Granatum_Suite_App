package com.granatum.core.data.environment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.environment.AppEnvironment
import com.granatum.core.domain.environment.EnvironmentError
import com.granatum.core.domain.environment.EnvironmentRepository
import com.granatum.core.domain.environment.EnvironmentScopedData
import com.granatum.core.domain.logger.AppLogger
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Remembers the chosen environment and switches safely (specs/010-entornos, research D4).
 * [load] must run before the first request; Koin does it when the app starts.
 */
class EnvironmentRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: () -> AuthRepository,
    private val scopedData: () -> List<EnvironmentScopedData>,
    private val demo: DemoGateway?,
    private val logger: AppLogger,
    override val available: List<AppEnvironment> = ActiveEnvironment.available(demoAvailable = demo != null)
) : EnvironmentRepository {

    private val _current = MutableStateFlow(ActiveEnvironment.default(available))
    override val current: StateFlow<AppEnvironment> = _current.asStateFlow()

    private val mutex = Mutex()

    private val _startingDemo = MutableStateFlow(false)

    /** True while the app starts in Demo and signs in: the gate keeps the splash meanwhile. */
    val startingDemo: StateFlow<Boolean> = _startingDemo.asStateFlow()

    suspend fun load() {
        val stored = runCatching { dataStore.data.first()[KEY] }.getOrNull()
            ?.let { name -> AppEnvironment.entries.firstOrNull { it.name == name } }
        // A choice that this build no longer offers (its URL was removed) falls back to the default.
        activate(stored?.takeIf { it in available } ?: ActiveEnvironment.default(available))
        _startingDemo.value = _current.value == AppEnvironment.DEMO
    }

    /**
     * At start, once the stored session is known: Demo opens signed in as the administrator (its
     * backend forgets sessions between runs, so there may be none).
     */
    suspend fun finishStartup(hasSession: Boolean) {
        try {
            if (_current.value == AppEnvironment.DEMO && !hasSession) signInDemoAdministrator()
        } finally {
            _startingDemo.value = false
        }
    }

    override suspend fun switchTo(environment: AppEnvironment): EmptyResult<EnvironmentError> = mutex.withLock {
        if (environment !in available) return Result.Failure(EnvironmentError.NotAvailable)
        if (environment == _current.value) return Result.Success(Unit)
        val scoped = scopedData()
        val unsynced = scoped.sumOf { it.unsyncedItems() }
        if (unsynced > 0) return Result.Failure(EnvironmentError.UnsyncedWork(unsynced))

        // Revoked against the server the session belongs to, before leaving it.
        authRepository().logout()
        scoped.forEach { it.clear() }
        dataStore.edit { it[KEY] = environment.name }
        activate(environment)
        logger.info("Environment switched to $environment")
        if (environment == AppEnvironment.DEMO) signInDemoAdministrator()
        return Result.Success(Unit)
    }

    /** Demo opens already signed in as the administrator (specs/010-entornos, US3). */
    suspend fun signInDemoAdministrator() {
        val account = demo?.defaultAccount ?: return
        authRepository().login(email = account.email, password = account.password)
    }

    override fun demoAccounts(): List<DemoAccount> =
        if (_current.value == AppEnvironment.DEMO) demo?.accounts.orEmpty() else emptyList()

    private fun activate(environment: AppEnvironment) {
        ActiveEnvironment.current = environment
        _current.value = environment
    }

    private companion object {
        val KEY = stringPreferencesKey("granatum.environment")
    }
}
