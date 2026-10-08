package com.granatum.core.data.auth.storage

/**
 * The platform's protected store, reduced to the three operations this app needs.
 *
 * Everything above it — serialising the session, mapping it to domain, exposing it as a flow,
 * swallowing failures — lives once in [SecureSessionStorage]. Only these three calls differ
 * between Android and iOS, so only these three are implemented per platform.
 *
 * This is an interface rather than `expect`/`actual` because the two implementations need
 * different constructor dependencies: Android injects the already-wired DataStore, iOS needs
 * nothing. An `expect class` would have to force one shared constructor signature on both.
 * Each platform's Koin module binds its own implementation.
 *
 * No operation throws. [read] returning null covers both "nothing stored" and "could not be
 * read", because from the caller's side there is nothing useful to distinguish: either way
 * there is no session.
 */
interface SecureStore {

    /** The stored value, or null when absent or unreadable. */
    suspend fun read(key: String): String?

    /** Replaces whatever was there. Atomic. */
    suspend fun write(key: String, value: String)

    /** Idempotent: deleting what is not there is not an error. */
    suspend fun delete(key: String)
}

/**
 * Key for the session in the secure store.
 *
 * It MUST stay different from [LEGACY_PLAINTEXT_SESSION_KEY]. They are read and deleted by
 * different components, and if they ever collided the legacy cleaner would wipe the session
 * that was just written — a failure that would look like "the session does not persist"
 * rather than like a key clash.
 */
const val SECURE_SESSION_KEY = "granatum.session.secure.v2"

/**
 * Where sessions of the previous shape lived. They never came from a real sign-in — the app had
 * no login screen, only a development seeder — so there is nothing to migrate: the key is
 * deleted on start and the person signs in.
 */
const val PREVIOUS_SECURE_SESSION_KEY = "granatum.session.secure.v1"

/**
 * The key the previous version wrote the session to, in plain text, in DataStore.
 * [LegacySessionCleaner] deletes it; nothing reads it.
 */
const val LEGACY_PLAINTEXT_SESSION_KEY = "KEY_AUTH_INFO"
