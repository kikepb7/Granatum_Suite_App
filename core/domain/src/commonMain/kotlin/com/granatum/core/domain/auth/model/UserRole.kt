package com.granatum.core.domain.auth.model

/**
 * The four roles the backend assigns, plus [DESCONOCIDO] for one this version of the app does
 * not know yet.
 *
 * Each permission is spelled out per role instead of grouping roles together, so adding a role
 * forces a decision on every permission rather than inheriting one by accident. They mirror
 * the backend's `SecurityConfig`: inventory is ADMIN and ENCARGADO, writing to the time register
 * is everyone but REPRESENTANTE, who only reads it; invoicing and staff are ADMIN alone.
 */
enum class UserRole(
    val canClockIn: Boolean,
    val canManageInventory: Boolean,
    val canSeeTeam: Boolean,
    val canManageInvoicing: Boolean,
    val canManageStaff: Boolean
) {
    ADMIN(canClockIn = true, canManageInventory = true, canSeeTeam = true, canManageInvoicing = true, canManageStaff = true),
    ENCARGADO(canClockIn = true, canManageInventory = true, canSeeTeam = true, canManageInvoicing = false, canManageStaff = false),
    EMPLEADO(canClockIn = true, canManageInventory = false, canSeeTeam = false, canManageInvoicing = false, canManageStaff = false),
    REPRESENTANTE(canClockIn = false, canManageInventory = false, canSeeTeam = false, canManageInvoicing = false, canManageStaff = false),

    /** Least privilege: a role the app cannot interpret gets nothing it cannot take back. */
    DESCONOCIDO(canClockIn = false, canManageInventory = false, canSeeTeam = false, canManageInvoicing = false, canManageStaff = false);

    companion object {
        fun fromBackend(name: String?): UserRole =
            entries.firstOrNull { it != DESCONOCIDO && it.name == name } ?: DESCONOCIDO
    }
}
