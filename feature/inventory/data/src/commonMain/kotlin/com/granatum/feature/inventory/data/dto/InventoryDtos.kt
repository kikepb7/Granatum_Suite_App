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

@Serializable
data class TamanoDto(val alto: Double, val ancho: Double, val unidadMedida: String, val diametro: Double? = null)

@Serializable
data class MaterialDto(
    val id: String,
    val nombre: String,
    val categoria: CategoriaDto,
    val cantidadDisponible: Int,
    val cantidadTotal: Int,
    val tamano: TamanoDto,
    val color: String,
    val materialFisico: String,
    val estado: String,
    val ubicacion: String,
    val precioUnitario: Double,
    val proveedor: String,
    val fotos: List<String> = emptyList(),
    val fechaAlta: String,
    val fechaUltimaModificacion: String
)

@Serializable
data class CreateMaterialRequestDto(
    val nombre: String,
    val categoriaId: String,
    val cantidadDisponible: Int,
    val cantidadTotal: Int,
    val tamano: TamanoDto,
    val color: String,
    val materialFisico: String,
    val estado: String,
    val ubicacion: String,
    val precioUnitario: Double,
    val proveedor: String,
    val fotos: List<String>
)

/** No `cantidadDisponible`: after creation it only changes through an adjustment with a reason. */
@Serializable
data class UpdateMaterialRequestDto(
    val nombre: String,
    val categoriaId: String,
    val cantidadTotal: Int,
    val tamano: TamanoDto,
    val color: String,
    val materialFisico: String,
    val estado: String,
    val ubicacion: String,
    val precioUnitario: Double,
    val proveedor: String,
    val fotos: List<String>
)

@Serializable
data class UpdateCantidadRequestDto(val cantidadDisponible: Int, val motivo: String)

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
