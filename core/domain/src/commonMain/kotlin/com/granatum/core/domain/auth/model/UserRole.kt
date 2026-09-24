package com.granatum.core.domain.auth.model

enum class UserRole {
    ADMIN,
    ENCARGADO,
    EMPLEADO;

    val canManageTeam: Boolean get() = this == ADMIN || this == ENCARGADO
}
