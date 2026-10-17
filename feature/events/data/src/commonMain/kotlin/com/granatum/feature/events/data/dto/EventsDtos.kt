package com.granatum.feature.events.data.dto

import kotlinx.serialization.Serializable

// Field names match docs/openapi.json (backend dc2f907) letter for letter (constitution principle VI).

@Serializable
internal data class EventoDto(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    /** `2027-06-13`. */
    val fecha: String,
    /** `PROGRAMADO` or `FINALIZADO`. */
    val estado: String,
    val materiales: List<EventoMaterialDto> = emptyList(),
    val creadoPor: String,
    val finalizadoEn: String? = null,
    val fechaAlta: String,
    val fechaUltimaModificacion: String
)

@Serializable
internal data class EventoMaterialDto(
    val materialId: String,
    val categoria: String,
    val descripcion: String,
    val cantidad: Int,
    val localizacion: LocalizacionEventoDto? = null,
    val imagenUrl: String? = null
)

@Serializable
internal data class MaterialDisponibleDto(
    val id: String,
    val categoria: String,
    val descripcion: String,
    val cantidad: Int,
    val localizacion: LocalizacionEventoDto,
    val imagenUrl: String? = null
)

@Serializable
internal data class LocalizacionEventoDto(val almacen: String, val seccion: Int)

@Serializable
internal data class EventoRequestDto(val nombre: String, val descripcion: String?, val fecha: String)

@Serializable
internal data class CantidadMaterialEventoRequestDto(val cantidad: Int)

/** The error body; `disponible` only comes with `STOCK_INSUFICIENTE`. */
@Serializable
internal data class EventsErrorBodyDto(val code: String? = null, val message: String? = null, val disponible: Int? = null)
