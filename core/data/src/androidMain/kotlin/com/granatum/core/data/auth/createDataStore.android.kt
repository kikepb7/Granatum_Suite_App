package com.granatum.core.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.granatum.core.data.auth.storage.DATA_STORE_FILE_NAME
import com.granatum.core.data.auth.storage.createDataStore

fun createDataStore(context: Context): DataStore<Preferences> {
    return createDataStore {
        context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
    }
}