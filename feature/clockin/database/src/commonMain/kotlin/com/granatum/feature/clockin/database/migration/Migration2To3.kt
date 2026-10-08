package com.granatum.feature.clockin.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Version 2 -> 3 (specs/005-fichaje-real, research D8), in this order:
 *
 * 1. Rows without an owner are deleted: they predate sign-in and belong to nobody (FR-023).
 * 2. The new tables and columns are created, with Room's own SQL from schemas/3.json.
 * 3. Each person's punches are grouped into shifts ([groupLegacyEvents]); punches before a
 *    person's first entry belong to no shift and are rejected.
 * 4. FAILED no longer exists: it becomes PENDING and is sent again with the new format.
 * 5. Breaks recorded without a type get OTRO, because the server requires one.
 */
object Migration2To3 : Migration(2, 3) {

    @OptIn(ExperimentalUuidApi::class)
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DELETE FROM clock_event WHERE employeeId IS NULL")

        connection.execSQL("ALTER TABLE clock_event ADD COLUMN shiftLocalId TEXT")
        connection.execSQL("ALTER TABLE clock_event ADD COLUMN breakType TEXT")
        connection.execSQL("ALTER TABLE clock_event ADD COLUMN rejectionCode TEXT")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `shift` (`localId` TEXT NOT NULL, `employeeId` TEXT NOT NULL, " +
                "`serverId` TEXT, `startedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`localId`))"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `server_shift` (`id` TEXT NOT NULL, `employeeId` TEXT NOT NULL, " +
                "`entradaEpochMillis` INTEGER NOT NULL, `salidaEpochMillis` INTEGER, `estado` TEXT NOT NULL, " +
                "`minutosTrabajados` INTEGER, `fueIncompleto` INTEGER NOT NULL, `pausasJson` TEXT NOT NULL, " +
                "`corregido` INTEGER NOT NULL, `reconstruido` INTEGER NOT NULL, " +
                "`fetchedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )

        connection.execSQL("UPDATE clock_event SET syncState = 'PENDING' WHERE syncState = 'FAILED'")
        // Version 2 recorded breaks without a type, and the server requires one. Unknown is OTRO;
        // the person can still correct the shift if it matters.
        connection.execSQL("UPDATE clock_event SET breakType = 'OTRO' WHERE type = 'BREAK_START' AND breakType IS NULL")

        val legacy = buildList {
            connection.prepare("SELECT id, employeeId, type, clientTimestampEpochMillis FROM clock_event").use { st ->
                while (st.step()) add(LegacyEvent(st.getText(0), st.getText(1), st.getText(2), st.getLong(3)))
            }
        }
        val grouping = groupLegacyEvents(legacy) { Uuid.random().toString() }

        grouping.shifts.forEach { shift ->
            connection.prepare("INSERT INTO shift (localId, employeeId, serverId, startedAtEpochMillis) VALUES (?, ?, NULL, ?)").use { st ->
                st.bindText(1, shift.localId)
                st.bindText(2, shift.employeeId)
                st.bindLong(3, shift.startedAtEpochMillis)
                st.step()
            }
        }
        grouping.assignments.forEach { (eventId, shiftLocalId) ->
            connection.prepare("UPDATE clock_event SET shiftLocalId = ? WHERE id = ?").use { st ->
                st.bindText(1, shiftLocalId)
                st.bindText(2, eventId)
                st.step()
            }
        }
        grouping.orphans.forEach { eventId ->
            connection.prepare(
                "UPDATE clock_event SET syncState = 'REJECTED', rejectionCode = 'JORNADA_NO_REGISTRADA' " +
                    "WHERE id = ? AND syncState != 'SYNCED'"
            ).use { st ->
                st.bindText(1, eventId)
                st.step()
            }
        }
    }
}
