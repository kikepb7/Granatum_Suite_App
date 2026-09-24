package com.granatum.core.data.database

import androidx.sqlite.SQLiteException
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result

suspend inline fun <T> safeDatabaseUpdate(update: suspend () -> T): Result<T, DataError.Local> {
    return try {
        Result.Success(data = update())
    } catch (_: SQLiteException) {
        Result.Failure(error = DataError.Local.DISK_FULL)
    }
}