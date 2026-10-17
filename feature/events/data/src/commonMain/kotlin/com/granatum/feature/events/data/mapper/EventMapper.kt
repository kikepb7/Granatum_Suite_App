package com.granatum.feature.events.data.mapper

import com.granatum.feature.events.data.dto.EventDto
import com.granatum.feature.events.data.dto.EventMaterialDto
import com.granatum.feature.events.data.dto.EventRequestDto
import com.granatum.feature.events.data.dto.EventLocationDto
import com.granatum.feature.events.data.dto.AvailableMaterialDto
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
    private val materialsSerializer = ListSerializer(EventMaterialDto.serializer())

    fun toEntity(dto: EventDto) = EventEntity(
        id = dto.id,
        name = dto.name,
        description = dto.description,
        date = dto.date,
        status = dto.status,
        materialsJson = json.encodeToString(materialsSerializer, dto.materials),
        createdAtEpochMillis = Instant.parse(dto.createdAt).toEpochMilliseconds(),
        updatedAtEpochMillis = Instant.parse(dto.updatedAt).toEpochMilliseconds(),
        finishedAtEpochMillis = dto.finishedAt?.let { Instant.parse(it).toEpochMilliseconds() }
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

    fun toDomain(dto: AvailableMaterialDto) = AvailableMaterial(
        id = dto.id,
        category = dto.category,
        description = dto.description,
        quantity = dto.quantity,
        location = dto.location.toDomain(),
        imageUrl = dto.imageUrl
    )

    fun toRequest(draft: EventDraft) = EventRequestDto(
        name = draft.name.trim(),
        description = draft.description?.trim()?.ifEmpty { null },
        date = draft.date.toString()
    )

    private fun EventMaterialDto.toDomain() = EventMaterial(
        materialId = materialId,
        category = category,
        description = description,
        quantity = quantity,
        location = location?.toDomain(),
        imageUrl = imageUrl
    )

    private fun EventLocationDto.toDomain() = MaterialLocation(warehouse = warehouse, section = section)

    private companion object {
        const val FINISHED = "FINISHED"
    }
}
