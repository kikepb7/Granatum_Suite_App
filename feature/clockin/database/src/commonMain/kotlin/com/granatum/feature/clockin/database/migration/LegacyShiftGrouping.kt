package com.granatum.feature.clockin.database.migration

/** A punch as version 2 stored it, with an owner. */
data class LegacyEvent(val id: String, val employeeId: String, val type: String, val occurredAtEpochMillis: Long)

data class GroupedShift(val localId: String, val employeeId: String, val startedAtEpochMillis: Long)

data class Grouping(
    val shifts: List<GroupedShift>,
    /** Event id to the shift it belongs to. */
    val assignments: Map<String, String>,
    /** Events before a person's first entry: they belong to no shift and are rejected. */
    val orphans: List<String>
)

/**
 * Version 2 kept loose punches; version 3 groups them into shifts (specs/005-fichaje-real,
 * research D8). Per person, in the order they happened: every CLOCK_IN opens a shift and the
 * punches after it belong to it until the next CLOCK_IN. A pure function so it can be tested
 * without a database.
 */
fun groupLegacyEvents(events: List<LegacyEvent>, newId: () -> String): Grouping {
    val shifts = mutableListOf<GroupedShift>()
    val assignments = mutableMapOf<String, String>()
    val orphans = mutableListOf<String>()

    events.groupBy { it.employeeId }.forEach { (employeeId, ofPerson) ->
        var current: GroupedShift? = null
        ofPerson.sortedWith(compareBy({ it.occurredAtEpochMillis }, { it.id })).forEach { event ->
            if (event.type == "CLOCK_IN") {
                current = GroupedShift(newId(), employeeId, event.occurredAtEpochMillis).also { shifts += it }
            }
            val shift = current
            if (shift == null) orphans += event.id else assignments[event.id] = shift.localId
        }
    }
    return Grouping(shifts, assignments, orphans)
}
