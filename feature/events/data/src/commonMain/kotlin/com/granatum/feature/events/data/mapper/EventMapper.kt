package com.granatum.feature.events.data.mapper

import com.granatum.feature.events.data.dto.EventoDto
import com.granatum.feature.events.data.dto.EventoMaterialDto
import com.granatum.feature.events.data.dto.EventoRequestDto
import com.granatum.feature.events.data.dto.LocalizacionEventoDto
import com.granatum.feature.events.data.dto.MaterialDisponibleDto
import com.granatum.feature.events.database.entity.EventEntity
import com.granatum.feature.events.domain.model.AvailableMaterial
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.model.EventMaterial
import com.granatum.feature.events.domain.model.EventStatus
import com.granatum.feature.events.domain.model.MaterialLocation
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Between the server's events, their cached rows and the domain (specs/012-eventos, research D2). */
internal class EventMapper {

    private val json = Json { ignoreUnknownKeys = true }
    private val materialsSerializer = ListSerializer(EventoMaterialDto.serializer())

    fun toEntity(dto: EventoDto) = EventEntity(
        id = dto.id,
        name = dto.nombre,
        description = dto.descripcion,
        date = dto.fecha,
        status = dto.estado,
        materialsJson = json.encodeToString(materialsSerializer, dto.materiales),
        createdAtEpochMillis = Instant.parse(dto.fechaAlta).toEpochMilliseconds(),
        updatedAtEpochMillis = Instant.parse(dto.fechaUltimaModificacion).toEpochMilliseconds(),
        finishedAtEpochMillis = dto.finalizadoEn?.let { Instant.parse(it).toEpochMilliseconds() }
    )

    fun toDomain(entity: EventEntity) = Event(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        date = LocalDate.parse(entity.date),
        status = if (entity.status == FINISHED) EventStatus.FINISHED else EventStatus.SCHEDULED,
        materials = runCatching { json.decodeFromString(materialsSerializer, entity.materialsJson) }
            .getOrDefault(emptyList())
            .map { it.toDomain() }
            .sortedWith(compareBy({ it.category.lowercase() }, { it.description.lowercase() })),
        createdAt = Instant.fromEpochMilliseconds(entity.createdAtEpochMillis),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAtEpochMillis),
        finishedAt = entity.finishedAtEpochMillis?.let { Instant.fromEpochMilliseconds(it) }
    )

    fun toDomain(dto: MaterialDisponibleDto) = AvailableMaterial(
        id = dto.id,
        category = dto.categoria,
        description = dto.descripcion,
        quantity = dto.cantidad,
        location = dto.localizacion.toDomain(),
        imageUrl = dto.imagenUrl
    )

    fun toRequest(draft: EventDraft) = EventoRequestDto(
        nombre = draft.name.trim(),
        descripcion = draft.description?.trim()?.ifEmpty { null },
        fecha = draft.date.toString()
    )

    private fun EventoMaterialDto.toDomain() = EventMaterial(
        materialId = materialId,
        category = categoria,
        description = descripcion,
        quantity = cantidad,
        location = localizacion?.toDomain(),
        imageUrl = imagenUrl
    )

    private fun LocalizacionEventoDto.toDomain() = MaterialLocation(warehouse = almacen, section = seccion)

    private companion object {
        const val FINISHED = "FINALIZADO"
    }
}
