package com.granatum.feature.clockin.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Version 3 -> 4: the English contract (docs/english-naming.md). Nothing is deleted, so punches
 * still waiting to be sent survive the update and go out with the new values:
 *
 * 1. `clock_event.breakType` COMIDA/DESCANSO/OTRO become MEAL/REST/OTHER, the values the server
 *    now expects in `type`.
 * 2. `clock_event.rejectionCode` Spanish codes (server and local) become their English ones, so
 *    the screen still explains old rejections.
 * 3. `server_shift` columns are renamed to English, `status` values translated, and the cached
 *    breaks JSON rewritten to the contract's keys and types ([LegacyValueTranslation]).
 */
object Migration3To4 : Migration(3, 4) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(LegacyValueTranslation.updateValuesSql("clock_event", "breakType", LegacyValueTranslation.breakTypes))
        connection.execSQL(LegacyValueTranslation.updateValuesSql("clock_event", "rejectionCode", LegacyValueTranslation.rejectionCodes))

        LegacyValueTranslation.serverShiftColumns.forEach { (old, new) ->
            connection.execSQL("ALTER TABLE `server_shift` RENAME COLUMN `$old` TO `$new`")
        }
        connection.execSQL(LegacyValueTranslation.updateValuesSql("server_shift", "status", LegacyValueTranslation.shiftStatuses))

        val breaks = buildList {
            connection.prepare("SELECT id, breaksJson FROM server_shift").use { st ->
                while (st.step()) add(st.getText(0) to st.getText(1))
            }
        }
        breaks.forEach { (id, json) ->
            val translated = LegacyValueTranslation.translateBreaksJson(json)
            if (translated != json) {
                connection.prepare("UPDATE server_shift SET breaksJson = ? WHERE id = ?").use { st ->
                    st.bindText(1, translated)
                    st.bindText(2, id)
                    st.step()
                }
            }
        }
    }
}
