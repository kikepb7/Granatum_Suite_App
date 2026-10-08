package com.granatum.core.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.granatum.core.data.auth.storage.DATA_STORE_FILE_NAME
import com.granatum.core.data.auth.storage.createDataStore
import com.granatum.core.domain.logger.AppLogger

fun createDataStore(context: Context, logger: AppLogger): DataStore<Preferences> {
    return createDataStore(logger) {
        context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
    }
}