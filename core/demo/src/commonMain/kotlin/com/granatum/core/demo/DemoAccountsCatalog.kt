package com.granatum.core.demo

import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.auth.model.UserRole

/** The demo people. Test credentials of the in-memory backend only: they open nothing else. */
object DemoAccountsCatalog {
    val ADMIN = DemoAccount(UserRole.ADMIN, "Lucía Ferrer", "admin@demo.granatum.es", "Demo-Admin-2026!")
    val EMPLOYEE = DemoAccount(UserRole.EMPLOYEE, "Marcos Vidal", "empleado@demo.granatum.es", "Demo-Empleado-2026!")

    val all = listOf(ADMIN, EMPLOYEE)
}
