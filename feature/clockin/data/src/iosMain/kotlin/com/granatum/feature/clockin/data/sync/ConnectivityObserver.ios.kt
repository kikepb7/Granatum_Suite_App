@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.granatum.feature.clockin.data.sync

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * MVP note: this only reports connectivity while the app process is alive
 * and observing — it does not wake the app from the background. True
 * background delivery on iOS needs a BGProcessingTask registered in
 * Info.plist/Xcode, which is a deliberate follow-up (see ARCHITECTURE.md);
 * Android already gets real background delivery via `ClockSyncWorker`.
 */
actual class ConnectivityObserver {

    actual fun observe(): Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(monitor) { path ->
            trySend(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }.distinctUntilChanged()
}
