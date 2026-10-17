package com.granatum.core.domain.auth

import com.granatum.core.domain.auth.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoleTest {

    private fun permissions(role: UserRole) =
        listOf(role.canClockIn, role.canManageInventory, role.canSeeTeam, role.canManageInvoicing, role.canManageStaff)

    @Test
    fun permissions_match_what_the_backend_allows() {
        assertEquals(listOf(true, true, true, true, true), permissions(UserRole.ADMIN))
        assertEquals(listOf(true, true, true, false, false), permissions(UserRole.MANAGER))
        assertEquals(listOf(true, false, false, false, false), permissions(UserRole.EMPLOYEE))
        assertEquals(listOf(false, false, false, false, false), permissions(UserRole.REPRESENTATIVE))
        assertEquals(listOf(false, false, false, false, false), permissions(UserRole.UNKNOWN))
    }

    @Test
    fun `When events are checked then everyone sees them and all but REPRESENTATIVE manage them`() {
        val expected = mapOf(
            UserRole.ADMIN to (true to true),
            UserRole.MANAGER to (true to true),
            UserRole.EMPLOYEE to (true to true),
            UserRole.REPRESENTATIVE to (true to false),
            UserRole.UNKNOWN to (false to false)
        )

        val actual = UserRole.entries.associateWith { it.canSeeEvents to it.canManageEvents }

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun the_backend_name_maps_to_its_role_and_anything_else_to_unknown() {
        assertEquals(UserRole.REPRESENTATIVE, UserRole.fromBackend("REPRESENTATIVE"))
        assertEquals(UserRole.UNKNOWN, UserRole.fromBackend("AUDITOR"))
        assertEquals(UserRole.UNKNOWN, UserRole.fromBackend(null))
        assertEquals(UserRole.UNKNOWN, UserRole.fromBackend("UNKNOWN"))
    }

    @Test
    fun `When a role name from before the English contract arrives then it is unknown`() {
        listOf("ENCARGADO", "EMPLEADO", "REPRESENTANTE", "DESCONOCIDO").forEach { old ->
            assertEquals(UserRole.UNKNOWN, UserRole.fromBackend(old), old)
        }
    }
}
