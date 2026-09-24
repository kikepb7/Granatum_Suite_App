package com.granatum.feature.clockin.data.sync

import kotlinx.coroutines.flow.Flow

/**
 * Cheap, platform-native "am I online" signal (ConnectivityManager on
 * Android, NWPathMonitor on iOS) used only to trigger an opportunistic sync
 * as soon as the warehouse wifi comes back — it is a hint, not a guarantee,
 * so `ClockEventSyncManager` always re-checks by actually trying the call.
 */
expect class ConnectivityObserver {
    fun observe(): Flow<Boolean>
}
