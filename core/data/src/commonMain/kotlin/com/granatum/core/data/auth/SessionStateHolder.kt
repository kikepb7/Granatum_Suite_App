package com.granatum.core.data.auth

import com.granatum.core.data.auth.storage.SecureSessionStorage
import com.granatum.core.domain.auth.model.SessionState
import com.granatum.core.domain.auth.model.sessionStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * What navigation shows, derived from the stored session alone.
 *
 * Nothing here touches the network, on purpose: with a session stored and no coverage the app
 * still opens straight into it, even if the access token expired hours ago (FR-022, FR-023).
 * The server decides when a session is over, and says so through [SessionEvents].
 */
class SessionStateHolder(
    storage: SecureSessionStorage,
    events: SessionEvents,
    scope: CoroutineScope
) {
    val state: StateFlow<SessionState> = combine(
        storage.observeSession(),
        storage.isReady,
        events.lastSignOutReason
    ) { session, ready, reason ->
        sessionStateOf(session = session, loaded = ready, reason = reason)
    }.stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)
}
