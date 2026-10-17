package com.granatum.core.domain.auth.model

/**
 * The four roles the backend assigns, plus [UNKNOWN] for one this version of the app does
 * not know yet.
 *
 * Each permission is spelled out per role instead of grouping roles together, so adding a role
 * forces a decision on every permission rather than inheriting one by accident. They mirror
 * the backend's `SecurityConfig`: inventory is ADMIN and MANAGER, writing to the time register
 * is everyone but REPRESENTATIVE, who only reads it; invoicing and staff are ADMIN alone. Events
 * (backend 011) are seen by all four and managed by everyone but REPRESENTATIVE.
 */
enum class UserRole(
    val canClockIn: Boolean,
    val canManageInventory: Boolean,
    val canSeeTeam: Boolean,
    val canManageInvoicing: Boolean,
    val canManageStaff: Boolean,
    val canSeeEvents: Boolean,
    val canManageEvents: Boolean
) {
    ADMIN(canClockIn = true, canManageInventory = true, canSeeTeam = true, canManageInvoicing = true, canManageStaff = true, canSeeEvents = true, canManageEvents = true),
    MANAGER(canClockIn = true, canManageInventory = true, canSeeTeam = true, canManageInvoicing = false, canManageStaff = false, canSeeEvents = true, canManageEvents = true),
    EMPLOYEE(canClockIn = true, canManageInventory = false, canSeeTeam = false, canManageInvoicing = false, canManageStaff = false, canSeeEvents = true, canManageEvents = true),
    REPRESENTATIVE(canClockIn = false, canManageInventory = false, canSeeTeam = false, canManageInvoicing = false, canManageStaff = false, canSeeEvents = true, canManageEvents = false),

    /** Least privilege: a role the app cannot interpret gets nothing it cannot take back. */
    UNKNOWN(canClockIn = false, canManageInventory = false, canSeeTeam = false, canManageInvoicing = false, canManageStaff = false, canSeeEvents = false, canManageEvents = false);

    companion object {
        fun fromBackend(name: String?): UserRole =
            entries.firstOrNull { it != UNKNOWN && it.name == name } ?: UNKNOWN
    }
}
