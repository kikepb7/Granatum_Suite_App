package com.granatum.feature.clockin.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * 15 minutes is WorkManager's minimum periodic interval — it is a backstop
 * for when the app isn't running, not the primary sync path (that's
 * [ClockEventSyncManager]'s connectivity callback, which reacts within
 * seconds while the app is alive).
 */
fun scheduleClockSync(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = PeriodicWorkRequestBuilder<ClockSyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "clock_event_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}
