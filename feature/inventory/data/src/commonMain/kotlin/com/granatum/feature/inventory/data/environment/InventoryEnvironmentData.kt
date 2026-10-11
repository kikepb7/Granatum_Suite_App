package com.granatum.feature.inventory.data.environment

import com.granatum.core.domain.environment.EnvironmentScopedData
import com.granatum.feature.inventory.database.dao.InventoryMaintenanceDao

/** The inventory cache belongs to one server; writes are online-only, so nothing is ever pending. */
class InventoryEnvironmentData(private val dao: InventoryMaintenanceDao) : EnvironmentScopedData {
    override suspend fun unsyncedItems(): Int = 0
    override suspend fun clear() = dao.clearAll()
}
