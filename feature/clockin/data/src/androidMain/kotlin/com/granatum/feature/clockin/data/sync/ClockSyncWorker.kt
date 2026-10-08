package com.granatum.feature.clockin.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Backstop for when the app process isn't alive to run
 * [ShiftSyncEngine]'s connectivity-triggered sync — e.g. the OS killed
 * the app while a punch was still queued. WorkManager guarantees this runs
 * (subject to its own battery/Doze constraints) even then, which is why
 * Android gets real offline resilience where the iOS MVP only re-syncs in
 * the foreground (see `ConnectivityObserver.ios.kt`).
 */
class ClockSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val syncManager: ShiftSyncEngine by inject()

    override suspend fun doWork(): Result {
        // Temporary failures ask WorkManager to back off and retry; everything else is done.
        return if (syncManager.syncNow()) Result.success() else Result.retry()
    }
}
