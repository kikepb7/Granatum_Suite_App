package com.granatum.feature.events.data.environment

import com.granatum.core.domain.environment.EnvironmentScopedData
import com.granatum.feature.events.database.dao.EventsDao

/** The events cache belongs to one server; writes are online-only, so nothing is ever pending. */
internal class EventsEnvironmentData(private val dao: EventsDao) : EnvironmentScopedData {
    override suspend fun unsyncedItems(): Int = 0
    override suspend fun clear() = dao.clear()
}
