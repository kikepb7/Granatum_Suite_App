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
        assertEquals(listOf(true, true, true, false, false), permissions(UserRole.ENCARGADO))
        assertEquals(listOf(true, false, false, false, false), permissions(UserRole.EMPLEADO))
        assertEquals(listOf(false, false, false, false, false), permissions(UserRole.REPRESENTANTE))
        assertEquals(listOf(false, false, false, false, false), permissions(UserRole.DESCONOCIDO))
    }

    @Test
    fun `When events are checked then everyone sees them and all but REPRESENTANTE manage them`() {
        val expected = mapOf(
            UserRole.ADMIN to (true to true),
            UserRole.ENCARGADO to (true to true),
            UserRole.EMPLEADO to (true to true),
            UserRole.REPRESENTANTE to (true to false),
            UserRole.DESCONOCIDO to (false to false)
        )

        val actual = UserRole.entries.associateWith { it.canSeeEvents to it.canManageEvents }

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun the_backend_name_maps_to_its_role_and_anything_else_to_unknown() {
        assertEquals(UserRole.REPRESENTANTE, UserRole.fromBackend("REPRESENTANTE"))
        assertEquals(UserRole.DESCONOCIDO, UserRole.fromBackend("AUDITOR"))
        assertEquals(UserRole.DESCONOCIDO, UserRole.fromBackend(null))
    }
}
