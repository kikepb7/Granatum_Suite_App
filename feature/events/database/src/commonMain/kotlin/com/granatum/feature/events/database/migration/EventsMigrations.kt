package com.granatum.feature.events.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Migrations of the events cache, handed to the Room builder. */
object EventsMigrations {

    /**
     * Version 1 stored the pre-English contract: `status` as `PROGRAMADO`/`FINALIZADO` and the
     * materials JSON with Spanish keys. The table is only a read cache with nothing waiting to be
     * sent, so its rows are dropped and the next refresh fills it with the English contract.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DELETE FROM event")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
