package com.granatum.feature.events.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A read cache of the server's events (specs/012-eventos, research D2). The materials travel as
 * JSON in one column: the cache is replaced whole on every refresh and never queried by material,
 * so a second table would only add joins. Nothing here is waiting to be sent.
 */
@Entity(tableName = "event")
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    /** ISO date, `2027-06-13`. */
    val date: String,
    val status: String,
    val materialsJson: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long?
)
