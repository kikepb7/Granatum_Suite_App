package com.granatum.core.data.auth.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.granatum.core.data.auth.dto.AuthInfoSerializableDTO
import com.granatum.core.data.mappers.toDomain
import com.granatum.core.data.mappers.toDto
import com.granatum.core.domain.auth.model.AuthInfoModel
import com.granatum.core.domain.auth.repository.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.let

class DataStoreSessionStorage(
    private val dataStore: DataStore<Preferences>
) : SessionStorage {

    private val authInfoKey = stringPreferencesKey("KEY_AUTH_INFO")
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun observeAuthInfo(): Flow<AuthInfoModel?> {
        return dataStore.data.map { preferences ->
            val serializedJson = preferences[authInfoKey]
            serializedJson?.let {
                json.decodeFromString<AuthInfoSerializableDTO>(string = it).toDomain()
            }
        }
    }

    override suspend fun set(info: AuthInfoModel?) {
        if (info == null) {
            dataStore.edit {
                it.remove(key = authInfoKey)
            }
            return
        }

        val serialized = json.encodeToString(value = info.toDto())
        dataStore.edit { prefs ->
            prefs[authInfoKey] = serialized
        }
    }
}