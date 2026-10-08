package com.granatum.feature.clockin.data.mappers

import com.granatum.feature.clockin.data.dto.CorreccionDto
import com.granatum.feature.clockin.data.dto.CorrectionRequestDto
import com.granatum.feature.clockin.data.dto.EmployeeAttendanceSummaryDto
import com.granatum.feature.clockin.data.dto.FichajeDto
import com.granatum.feature.clockin.data.dto.PausaDto
import com.granatum.feature.clockin.data.dto.ValoresFichajeDto
import com.granatum.feature.clockin.data.dto.ValoresPausaDto
import com.granatum.feature.clockin.database.entity.ServerShiftEntity
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.CorrectionRequestModel
import com.granatum.feature.clockin.domain.model.CorrectionState
import com.granatum.feature.clockin.domain.model.CorrectionStatus
import com.granatum.feature.clockin.domain.model.CorrectionValues
import com.granatum.feature.clockin.domain.model.EmployeeAttendanceSummaryModel
import com.granatum.feature.clockin.domain.model.ProposedBreak
import com.granatum.feature.clockin.domain.model.ShiftStatus
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal val clockInJson = Json { ignoreUnknownKeys = true }

fun FichajeDto.toEntity(fetchedAtEpochMillis: Long, corregido: Boolean = false, reconstruido: Boolean = false) = ServerShiftEntity(
    id = id,
    employeeId = empleadoId,
    entradaEpochMillis = Instant.parse(entrada).toEpochMilliseconds(),
    salidaEpochMillis = salida?.let { Instant.parse(it).toEpochMilliseconds() },
    estado = estado,
    minutosTrabajados = minutosTrabajados,
    fueIncompleto = fueIncompleto,
    pausasJson = clockInJson.encodeToString(pausas),
    corregido = corregido,
    reconstruido = reconstruido,
    fetchedAtEpochMillis = fetchedAtEpochMillis
)

fun ServerShiftEntity.breaks(): List<PausaDto> =
    runCatching { clockInJson.decodeFromString<List<PausaDto>>(pausasJson) }.getOrDefault(emptyList())

fun String.toBreakType(): BreakType = runCatching { BreakType.valueOf(this) }.getOrDefault(BreakType.OTRO)

fun CorreccionDto.toDomain(): CorrectionModel = CorrectionModel(
    id = id,
    shiftServerId = fichajeId,
    state = runCatching { CorrectionState.valueOf(estado) }.getOrDefault(CorrectionState.PENDIENTE),
    reason = motivo,
    proposed = valoresPropuestos.toDomain(),
    resolutionReason = motivoResolucion,
    createdAt = Instant.parse(creadaEn),
    resolvedAt = resueltaEn?.let(Instant::parse)
)

fun ValoresFichajeDto.toDomain() = CorrectionValues(
    entry = Instant.parse(entrada),
    exit = Instant.parse(salida),
    breaks = pausas.map { ProposedBreak(it.tipo.toBreakType(), Instant.parse(it.inicio), Instant.parse(it.fin)) }
)

fun CorrectionValues.toDto() = ValoresFichajeDto(
    entrada = entry.toString(),
    salida = exit.toString(),
    pausas = breaks.map { ValoresPausaDto(tipo = it.type.name, inicio = it.start.toString(), fin = it.end.toString()) }
)

// Team screen — unchanged until the team phase.

fun CorrectionRequestDto.toDomain(): CorrectionRequestModel = CorrectionRequestModel(
    id = id,
    clockEventId = clockEventId,
    employeeId = employeeId,
    employeeName = employeeName,
    reason = reason,
    requestedClientTimestamp = Instant.parse(requestedClientTimestamp),
    status = runCatching { CorrectionStatus.valueOf(status) }.getOrDefault(CorrectionStatus.PENDING)
)

fun EmployeeAttendanceSummaryDto.toDomain(): EmployeeAttendanceSummaryModel = EmployeeAttendanceSummaryModel(
    employeeId = employeeId,
    employeeName = employeeName,
    currentStatus = runCatching { ShiftStatus.valueOf(currentStatus) }.getOrDefault(ShiftStatus.CLOCKED_OUT),
    workedMinutesInPeriod = workedMinutesInPeriod,
    pendingCorrections = pendingCorrections
)
