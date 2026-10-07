package com.granatum.core.data.auth.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.granatum.core.domain.logger.AppLogger

/**
 * Deletes the session the previous version left in plain text.
 *
 * It is never read, only removed. A credential that sat in readable storage is treated as no
 * longer trustworthy, so there is nothing to migrate — and not reading it avoids pulling that
 * value back into memory on the way to deleting it.
 *
 * Runs unconditionally on every start: the delete is idempotent and cheap, which is less
 * fragile than tracking whether it has already happened.
 *
 * Safe to run alongside [SecureSessionStorage] because [LEGACY_PLAINTEXT_SESSION_KEY] and
 * [SECURE_SESSION_KEY] are different keys. If they ever converged this would quietly erase
 * every session as it was written.
 */
class LegacySessionCleaner(
    private val dataStore: DataStore<Preferences>,
    private val logger: AppLogger
) {

    suspend fun clean() {
        runCatching {
            dataStore.edit { it.remove(stringPreferencesKey(LEGACY_PLAINTEXT_SESSION_KEY)) }
        }.onFailure { throwable ->
            logger.warn("Could not clear the legacy plaintext session: ${throwable.message}")
        }
    }
}
