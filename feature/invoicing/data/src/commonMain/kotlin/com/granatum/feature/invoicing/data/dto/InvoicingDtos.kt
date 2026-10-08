package com.granatum.feature.invoicing.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// Field for field with docs/openapi.json (backend d1857ad), schemas of /api/facturacion.
// Amounts are decimal strings except ResumenFactura.total, a JSON number (research D2, D12).

@Serializable
data class ParteDto(
    val nombre: String? = null,
    val nif: String? = null,
)

@Serializable
data class LineaIvaDto(
    val tipoIva: String,
    val base: String,
    val cuota: String,
    val recargo: String = "0.00",
    val causaSinCuota: String? = null,
)

/** No defaults: the contract requires every field and kotlinx would leave defaults out. */
@Serializable
data class LineaIvaRequestDto(
    val tipoIva: String,
    val base: String,
    val cuota: String,
    val recargo: String,
    val causaSinCuota: String?,
)

@Serializable
data class AvisoDto(
    val campo: String,
    val codigo: String,
    val bloquea: Boolean,
    val mensaje: String,
)

@Serializable
data class ReconocimientoResumenDto(
    val resultado: String,
    val camposDudosos: List<String> = emptyList(),
)

@Serializable
data class FacturaDto(
    val id: String,
    val estado: String,
    val tipo: String? = null,
    val version: Int,
    val emisor: ParteDto? = null,
    val destinatario: ParteDto? = null,
    val numero: String? = null,
    val fechaEmision: String? = null,
    val concepto: String? = null,
    val moneda: String = "EUR",
    val rectificativa: Boolean = false,
    val lineas: List<LineaIvaDto> = emptyList(),
    val retenciones: String = "0.00",
    val total: String? = null,
    val trimestreCerrado: Boolean = false,
    val reconocimiento: ReconocimientoResumenDto? = null,
    val avisos: List<AvisoDto> = emptyList(),
)

@Serializable
data class FacturaRequestDto(
    val tipo: String?,
    val emisor: ParteDto?,
    val destinatario: ParteDto?,
    val numero: String?,
    val fechaEmision: String?,
    val concepto: String?,
    val moneda: String,
    val rectificativa: Boolean,
    val lineas: List<LineaIvaRequestDto>,
    val retenciones: String,
    val total: String?,
    val version: Int,
)

@Serializable
data class VersionRequestDto(
    val version: Int,
)

@Serializable
data class ResumenFacturaDto(
    val id: String,
    val estado: String,
    val tipo: String? = null,
    val emisorNombre: String? = null,
    val emisorNif: String? = null,
    val destinatarioNombre: String? = null,
    val destinatarioNif: String? = null,
    val numero: String? = null,
    val fechaEmision: String? = null,
    /** A JSON number on the wire: kept as an element and read as text, never as Double. */
    val total: JsonElement? = null,
    val numeroAvisos: Int = 0,
)

@Serializable
data class PaginaResumenFacturaDto(
    val elementos: List<ResumenFacturaDto>,
    val pagina: Int,
    val tamano: Int,
    val total: Long,
)

@Serializable
data class ResultadoSubidaDto(
    val fichero: Int,
    val resultado: String,
    val facturaId: String? = null,
)

@Serializable
data class ReconocimientoHistorialDto(
    val resultado: String,
    val modelo: String,
    val creadoEn: String,
    val error: String? = null,
)

@Serializable
data class CambioHistorialDto(
    val accion: String,
    val autorId: String,
    val ocurridoEn: String,
    val valoresAnteriores: JsonObject? = null,
)

@Serializable
data class HistorialDto(
    val reconocimientos: List<ReconocimientoHistorialDto> = emptyList(),
    val cambios: List<CambioHistorialDto> = emptyList(),
)

@Serializable
data class EventoTrimestreDto(
    val accion: String,
    val autorId: String,
    val ocurridoEn: String,
    val motivo: String? = null,
)

@Serializable
data class EstadoTrimestreDto(
    val trimestre: Int,
    val cerrado: Boolean,
    val eventos: List<EventoTrimestreDto> = emptyList(),
)

@Serializable
data class ReaperturaRequestDto(
    val motivo: String,
)

@Serializable
data class EmpresaDto(
    val razonSocial: String,
    val nif: String,
    val reconocimientoActivo: Boolean = false,
)

@Serializable
data class EmpresaRequestDto(
    val razonSocial: String,
    val nif: String,
)

// GET /reportes?formato=json: the contract only says "object"; shape from the backend's
// ReporteDtos.kt at d1857ad.

@Serializable
data class PeriodoReporteDto(
    val tipo: String,
    val anio: Int,
    val mes: Int? = null,
    val trimestre: Int? = null,
    val desde: String,
    val hasta: String,
)

@Serializable
data class GrupoReporteDto(
    val facturas: Int = 0,
    val base: String = "0.00",
    val ivaPorTipo: Map<String, String> = emptyMap(),
    val recargo: String = "0.00",
    val retenciones: String = "0.00",
    val total: String = "0.00",
    val sinCuota: Map<String, String> = emptyMap(),
)

@Serializable
data class TrimestreCerradoDto(
    val anio: Int,
    val trimestre: Int,
    val desde: String? = null,
)

@Serializable
data class ReporteDto(
    val periodo: PeriodoReporteDto,
    val emitidas: GrupoReporteDto,
    val recibidas: GrupoReporteDto,
    val pendientes: Int = 0,
    val trimestresCerrados: List<TrimestreCerradoDto> = emptyList(),
    val calculadoEn: String,
)
