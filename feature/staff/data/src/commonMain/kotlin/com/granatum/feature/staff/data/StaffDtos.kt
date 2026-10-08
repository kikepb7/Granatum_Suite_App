package com.granatum.feature.staff.data

import kotlinx.serialization.Serializable

// Field for field with docs/openapi.json (backend 2ec33d0). Personal data: no toString leaks.

@Serializable
data class EmpleadoDto(
    val id: String,
    val nombre: String,
    val documentoIdentidad: String,
    val puesto: String,
    val tipoContrato: String,
    val fechaAlta: String,
    val activo: Boolean,
) {
    override fun toString(): String = "EmpleadoDto(id=$id)"
}

/** The identity document cannot change: it is not sent (the server refuses a non-null one). */
@Serializable
data class ActualizarEmpleadoRequestDto(
    val nombre: String,
    val puesto: String,
    val tipoContrato: String,
    val fechaAlta: String,
) {
    override fun toString(): String = "ActualizarEmpleadoRequestDto(tipoContrato=$tipoContrato)"
}

@Serializable
data class CambiarActivoRequestDto(
    val activo: Boolean,
)

@Serializable
data class AltaPersonaRequestDto(
    val nombre: String,
    val documentoIdentidad: String,
    val puesto: String,
    val tipoContrato: String,
    val fechaAlta: String,
    val email: String,
    val rol: String,
) {
    override fun toString(): String = "AltaPersonaRequestDto(tipoContrato=$tipoContrato, rol=$rol)"
}

@Serializable
data class AltaPersonaResponseDto(
    val empleadoId: String,
    val cuentaId: String,
    val email: String,
    val rol: String,
    val fichaCreada: Boolean,
    val passwordTemporal: String,
) {
    override fun toString(): String = "AltaPersonaResponseDto(empleadoId=$empleadoId, passwordTemporal=***)"
}

@Serializable
data class AltaCuentaRequestDto(
    val empleadoId: String,
    val email: String,
    val rol: String,
) {
    override fun toString(): String = "AltaCuentaRequestDto(empleadoId=$empleadoId, rol=$rol)"
}

@Serializable
data class AltaCuentaResponseDto(
    val cuentaId: String,
    val empleadoId: String,
    val email: String,
    val rol: String,
    val passwordTemporal: String,
) {
    override fun toString(): String = "AltaCuentaResponseDto(empleadoId=$empleadoId, passwordTemporal=***)"
}
