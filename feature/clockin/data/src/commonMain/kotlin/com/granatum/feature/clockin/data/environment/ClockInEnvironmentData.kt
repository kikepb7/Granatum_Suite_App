package com.granatum.feature.clockin.data.environment

import com.granatum.core.domain.environment.EnvironmentScopedData
import com.granatum.feature.clockin.database.dao.ClockInMaintenanceDao

/**
 * Punches belong to the server they were recorded against: switching environment waits for the
 * pending ones and then forgets everything (specs/010-entornos, SC-003).
 */
class ClockInEnvironmentData(private val dao: ClockInMaintenanceDao) : EnvironmentScopedData {
    override suspend fun unsyncedItems(): Int = dao.countUnsynced()
    override suspend fun clear() = dao.clearAll()
}
