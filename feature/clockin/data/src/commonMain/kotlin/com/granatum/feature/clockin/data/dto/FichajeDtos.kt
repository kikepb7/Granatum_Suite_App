package com.granatum.feature.clockin.data.dto

import kotlinx.serialization.Serializable

// Field names match docs/openapi.json letter for letter (constitution principle VI). No
// `ubicacion`: optional in the contract and not sent by this app (spec 005, FR-024).

@Serializable
data class EntradaRequestDto(val clientEventId: String, val occurredAt: String)

@Serializable
data class InicioPausaRequestDto(val clientEventId: String, val occurredAt: String, val tipo: String)

@Serializable
data class FinPausaRequestDto(val clientEventId: String, val occurredAt: String)

@Serializable
data class SalidaRequestDto(val clientEventId: String, val occurredAt: String)

@Serializable
data class PausaDto(
    val id: String,
    val inicio: String,
    val tipo: String,
    val fin: String? = null
)

@Serializable
data class FichajeDto(
    val id: String,
    val empleadoId: String,
    val entrada: String,
    val estado: String,
    val fueIncompleto: Boolean,
    val pausas: List<PausaDto> = emptyList(),
    val salida: String? = null,
    val minutosTrabajados: Int? = null
)

@Serializable
data class DiaResumenDto(
    val fecha: String,
    val entrada: String,
    val corregido: Boolean,
    val reconstruido: Boolean,
    val minutosPausa: Int,
    val salida: String? = null,
    val minutosTrabajados: Int? = null
)

@Serializable
data class ResumenMensualDto(
    val anio: Int,
    val mes: Int,
    val empleadoId: String,
    val tipoContrato: String,
    val totalMinutosTrabajados: Int,
    val dias: List<DiaResumenDto> = emptyList()
)

@Serializable
data class ValoresPausaDto(val tipo: String, val inicio: String, val fin: String)

@Serializable
data class ValoresFichajeDto(val entrada: String, val salida: String, val pausas: List<ValoresPausaDto>)

@Serializable
data class CrearCorreccionRequestDto(val motivo: String, val valoresPropuestos: ValoresFichajeDto)

@Serializable
data class CorreccionDto(
    val id: String,
    val fichajeId: String,
    val solicitanteId: String,
    val estado: String,
    val motivo: String,
    val valoresPropuestos: ValoresFichajeDto,
    val creadaEn: String,
    val motivoResolucion: String? = null,
    val resueltaEn: String? = null,
    val resueltaPorId: String? = null
)
