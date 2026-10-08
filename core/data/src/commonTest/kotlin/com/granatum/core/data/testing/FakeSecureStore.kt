package com.granatum.core.data.testing

import com.granatum.core.data.auth.storage.SecureStore

class FakeSecureStore(initial: Map<String, String> = emptyMap()) : SecureStore {
    val values = initial.toMutableMap()
    override suspend fun read(key: String): String? = values[key]
    override suspend fun write(key: String, value: String) { values[key] = value }
    override suspend fun delete(key: String) { values.remove(key) }
}
