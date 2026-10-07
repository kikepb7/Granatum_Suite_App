package com.granatum.core.data.auth.storage

import com.granatum.core.data.auth.dto.AuthInfoSerializableDTO
import com.granatum.core.data.mappers.toDomain
import com.granatum.core.data.mappers.toDto
import com.granatum.core.domain.auth.model.AuthInfoModel
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * The only implementation of [SessionStorage]. Serialising, mapping and error handling happen
 * here, once; [SecureStore] supplies the three platform calls underneath.
 *
 * The flow is held in memory rather than read from the store on every collection. The iOS
 * Keychain emits no change notifications, so there is nothing to build a reactive flow on the
 * way `dataStore.data` allowed. One process with one writer means the in-memory value cannot
 * drift from the store — an assumption that would stop holding if an extension or widget ever
 * wrote the session too.
 *
 * Nothing thrown here escapes. A store that is unavailable and a credential that cannot be
 * parsed both resolve to "no session", because from the rest of the app's point of view there
 * is nothing useful to tell apart: either way nobody is signed in and the app must still start.
 */
class SecureSessionStorage(
    private val secureStore: SecureStore,
    private val logger: AppLogger
) : SessionStorage {

    private val json = Json { ignoreUnknownKeys = true }
    private val cached = MutableStateFlow<AuthInfoModel?>(null)
    private val loadMutex = Mutex()
    private var loaded = false

    override fun observeAuthInfo(): Flow<AuthInfoModel?> = cached.asStateFlow()

    override suspend fun set(info: AuthInfoModel?) {
        // Mark as loaded first: a write makes the in-memory value authoritative, so a later
        // first-read must not overwrite it with whatever was on disk beforehand.
        loadMutex.withLock { loaded = true }

        if (info == null) {
            secureStore.delete(SECURE_SESSION_KEY)
            cached.value = null
            return
        }

        val serialised = runCatching { json.encodeToString(info.toDto()) }
            .getOrElse { throwable ->
                logger.error("Could not serialise the session", throwable)
                return
            }

        secureStore.write(SECURE_SESSION_KEY, serialised)
        cached.value = info
    }

    /**
     * Reads the store once and seeds the flow. Callers that need the session at startup should
     * invoke this before collecting; everything afterwards is served from memory.
     */
    suspend fun load() {
        loadMutex.withLock {
            if (loaded) return
            loaded = true
        }

        val stored = secureStore.read(SECURE_SESSION_KEY) ?: return

        cached.value = runCatching {
            json.decodeFromString<AuthInfoSerializableDTO>(stored).toDomain()
        }.getOrElse { throwable ->
            // Unparseable means unusable. Drop it rather than keep handing back something
            // broken on every launch.
            logger.warn("Stored session could not be read; discarding it: ${throwable.message}")
            secureStore.delete(SECURE_SESSION_KEY)
            null
        }
    }
}
