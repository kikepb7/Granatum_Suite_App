package com.granatum.feature.inventory.data.dto

import kotlinx.serialization.Serializable

// Field names match docs/openapi.json letter for letter (constitution principle VI).

@Serializable
data class CategoriaDto(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CategoriaRequestDto(val nombre: String, val descripcion: String? = null)

/** Centimetres. */
@Serializable
data class DimensionesDto(val alto: Double, val ancho: Double)

/** `almacen` is `A`, `B` or `C`; `seccion` 1 to 5. */
@Serializable
data class LocalizacionDto(val almacen: String, val seccion: Int)

@Serializable
data class MaterialDto(
    val id: String,
    val categoria: CategoriaDto,
    val descripcion: String,
    val dimensiones: DimensionesDto,
    val cantidad: Int,
    val localizacion: LocalizacionDto,
    /** Temporary signed URL; null without image or when the server could not sign it. */
    val imagenUrl: String? = null,
    val fechaAlta: String,
    val fechaUltimaModificacion: String
)

@Serializable
data class CreateMaterialRequestDto(
    val categoriaId: String,
    val descripcion: String,
    val dimensiones: DimensionesDto,
    val cantidad: Int,
    val localizacion: LocalizacionDto
)

/** No `cantidad`: after creation it only changes through `PATCH …/cantidad`, with a reason. */
@Serializable
data class UpdateMaterialRequestDto(
    val categoriaId: String,
    val descripcion: String,
    val dimensiones: DimensionesDto,
    val localizacion: LocalizacionDto
)

/** `motivo` is required (not blank). */
@Serializable
data class UpdateCantidadRequestDto(val cantidad: Int, val motivo: String)

@Serializable
data class HistorialMaterialDto(
    val id: String,
    val materialId: String,
    val tipoCambio: String,
    val motivo: String,
    val fecha: String,
    val usuarioId: String,
    val valorAnterior: String? = null,
    val valorNuevo: String? = null
)
