package com.granatum.core.domain.auth

import com.granatum.core.domain.auth.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoleTest {

    private fun permissions(role: UserRole) =
        Triple(role.canClockIn, role.canManageInventory, role.canSeeTeam)

    @Test
    fun permissions_match_what_the_backend_allows() {
        assertEquals(Triple(true, true, true), permissions(UserRole.ADMIN))
        assertEquals(Triple(true, true, true), permissions(UserRole.ENCARGADO))
        assertEquals(Triple(true, false, false), permissions(UserRole.EMPLEADO))
        assertEquals(Triple(false, false, false), permissions(UserRole.REPRESENTANTE))
        assertEquals(Triple(false, false, false), permissions(UserRole.DESCONOCIDO))
    }

    @Test
    fun the_backend_name_maps_to_its_role_and_anything_else_to_unknown() {
        assertEquals(UserRole.REPRESENTANTE, UserRole.fromBackend("REPRESENTANTE"))
        assertEquals(UserRole.DESCONOCIDO, UserRole.fromBackend("AUDITOR"))
        assertEquals(UserRole.DESCONOCIDO, UserRole.fromBackend(null))
    }
}
