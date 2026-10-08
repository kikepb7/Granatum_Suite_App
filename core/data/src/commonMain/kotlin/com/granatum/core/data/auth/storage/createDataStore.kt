package com.granatum.core.data.auth.storage

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.granatum.core.domain.logger.AppLogger
import okio.Path.Companion.toPath

/**
 * A file that no longer parses is replaced with an empty one. Without the handler every read
 * *and every write* keeps failing, so a single corrupt file would lock the device out of
 * signing in for good. Losing its contents is acceptable: it only holds the session, which
 * the user can recover by signing in again.
 */
fun createDataStore(logger: AppLogger, producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { exception ->
            logger.warn("Preferences file was corrupt; replacing it with an empty one: ${exception.message}")
            emptyPreferences()
        }
    ) {
        producePath().toPath()
    }
}

internal const val DATA_STORE_FILE_NAME = "prefs.preferences_pb"
