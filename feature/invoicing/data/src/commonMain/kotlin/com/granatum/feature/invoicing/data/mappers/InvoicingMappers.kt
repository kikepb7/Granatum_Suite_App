package com.granatum.feature.invoicing.data.mappers

import com.granatum.feature.invoicing.data.dto.AvisoDto
import com.granatum.feature.invoicing.data.dto.CambioHistorialDto
import com.granatum.feature.invoicing.data.dto.EmpresaDto
import com.granatum.feature.invoicing.data.dto.EstadoTrimestreDto
import com.granatum.feature.invoicing.data.dto.FacturaDto
import com.granatum.feature.invoicing.data.dto.FacturaRequestDto
import com.granatum.feature.invoicing.data.dto.GrupoReporteDto
import com.granatum.feature.invoicing.data.dto.HistorialDto
import com.granatum.feature.invoicing.data.dto.LineaIvaDto
import com.granatum.feature.invoicing.data.dto.LineaIvaRequestDto
import com.granatum.feature.invoicing.data.dto.ParteDto
import com.granatum.feature.invoicing.data.dto.ReporteDto
import com.granatum.feature.invoicing.data.dto.ResultadoSubidaDto
import com.granatum.feature.invoicing.data.dto.ResumenFacturaDto
import com.granatum.feature.invoicing.domain.model.ChangeAction
import com.granatum.feature.invoicing.domain.model.Company
import com.granatum.feature.invoicing.domain.model.Invoice
import com.granatum.feature.invoicing.domain.model.InvoiceChange
import com.granatum.feature.invoicing.domain.model.InvoiceDraft
import com.granatum.feature.invoicing.domain.model.InvoiceHistory
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceSummary
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.InvoiceWarning
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.domain.model.Party
import com.granatum.feature.invoicing.domain.model.Quarter
import com.granatum.feature.invoicing.domain.model.QuarterAction
import com.granatum.feature.invoicing.domain.model.QuarterEvent
import com.granatum.feature.invoicing.domain.model.RecognitionAttempt
import com.granatum.feature.invoicing.domain.model.RecognitionSummary
import com.granatum.feature.invoicing.domain.model.Report
import com.granatum.feature.invoicing.domain.model.ReportGroup
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import com.granatum.feature.invoicing.domain.model.UploadResult
import com.granatum.feature.invoicing.domain.model.VatLine
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

fun String.toInvoiceState(): InvoiceState = when (this) {
    "PENDIENTE_RECONOCER" -> InvoiceState.PENDING_RECOGNITION
    "BORRADOR" -> InvoiceState.DRAFT
    "CONFIRMADA" -> InvoiceState.CONFIRMED
    else -> InvoiceState.DISCARDED
}

fun InvoiceState.toApi(): String = when (this) {
    InvoiceState.PENDING_RECOGNITION -> "PENDIENTE_RECONOCER"
    InvoiceState.DRAFT -> "BORRADOR"
    InvoiceState.CONFIRMED -> "CONFIRMADA"
    InvoiceState.DISCARDED -> "DESCARTADA"
}

fun String?.toInvoiceType(): InvoiceType? = when (this) {
    "EMITIDA" -> InvoiceType.ISSUED
    "RECIBIDA" -> InvoiceType.RECEIVED
    else -> null
}

fun InvoiceType.toApi(): String = if (this == InvoiceType.ISSUED) "EMITIDA" else "RECIBIDA"

fun String?.toNoQuotaCause(): NoQuotaCause? = when (this) {
    "EXENTA" -> NoQuotaCause.EXEMPT
    "INVERSION_SUJETO_PASIVO" -> NoQuotaCause.REVERSE_CHARGE
    "INTRACOMUNITARIA" -> NoQuotaCause.INTRA_EU
    else -> null
}

fun NoQuotaCause.toApi(): String = when (this) {
    NoQuotaCause.EXEMPT -> "EXENTA"
    NoQuotaCause.REVERSE_CHARGE -> "INVERSION_SUJETO_PASIVO"
    NoQuotaCause.INTRA_EU -> "INTRACOMUNITARIA"
}

private fun money(text: String?): Money = Money.parse(text) ?: Money.ZERO

private fun ParteDto.toDomain() = Party(name = nombre, taxId = nif)

/** An empty party is sent as null: the server treats both the same and null keeps it simple. */
private fun Party?.toDto(): ParteDto? = this?.takeUnless { it.isEmpty }?.let {
    ParteDto(nombre = it.name?.trim()?.ifEmpty { null }, nif = it.taxId?.trim()?.ifEmpty { null })
}

fun FacturaDto.toDomain() = Invoice(
    id = id,
    state = estado.toInvoiceState(),
    type = tipo.toInvoiceType(),
    version = version,
    issuer = emisor?.toDomain(),
    recipient = destinatario?.toDomain(),
    number = numero,
    issueDate = fechaEmision?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    concept = concepto,
    currency = moneda,
    corrective = rectificativa,
    lines = lineas.map { it.toDomain() },
    withholding = money(retenciones),
    total = Money.parse(total),
    quarterClosed = trimestreCerrado,
    recognition = reconocimiento?.let { RecognitionSummary(it.resultado, it.camposDudosos) },
    warnings = avisos.map { it.toDomain() }
)

private fun LineaIvaDto.toDomain() = VatLine(
    rate = money(tipoIva),
    base = money(base),
    quota = money(cuota),
    surcharge = money(recargo),
    noQuotaCause = causaSinCuota.toNoQuotaCause()
)

private fun AvisoDto.toDomain() = InvoiceWarning(field = campo, code = codigo, blocking = bloquea, message = mensaje)

fun InvoiceDraft.toDto() = FacturaRequestDto(
    tipo = type?.toApi(),
    emisor = issuer.toDto(),
    destinatario = recipient.toDto(),
    numero = number?.trim()?.ifEmpty { null },
    fechaEmision = issueDate?.toString(),
    concepto = concept?.trim()?.ifEmpty { null },
    moneda = currency,
    rectificativa = corrective,
    lineas = lines.map {
        LineaIvaRequestDto(
            tipoIva = it.rate.toApi(),
            base = it.base.toApi(),
            cuota = it.quota.toApi(),
            recargo = it.surcharge.toApi(),
            causaSinCuota = it.noQuotaCause?.toApi()
        )
    },
    retenciones = withholding.toApi(),
    total = total?.toApi(),
    version = version
)

fun ResumenFacturaDto.toDomain() = InvoiceSummary(
    id = id,
    state = estado.toInvoiceState(),
    type = tipo.toInvoiceType(),
    issuer = Party(emisorNombre, emisorNif).takeUnless { it.isEmpty },
    recipient = Party(destinatarioNombre, destinatarioNif).takeUnless { it.isEmpty },
    number = numero,
    issueDate = fechaEmision?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    total = (total as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content?.let { Money.parseNumber(it) },
    warningCount = numeroAvisos
)

fun ResultadoSubidaDto.toDomain() = UploadResult(
    position = fichero,
    outcome = when (resultado) {
        "ACEPTADA" -> UploadOutcome.ACCEPTED
        "DUPLICADA" -> UploadOutcome.DUPLICATE
        "FORMATO_NO_ADMITIDO" -> UploadOutcome.UNSUPPORTED_FORMAT
        "DEMASIADO_GRANDE" -> UploadOutcome.TOO_LARGE
        "VACIO" -> UploadOutcome.EMPTY
        "PDF_NO_LEGIBLE" -> UploadOutcome.UNREADABLE_PDF
        else -> UploadOutcome.UNKNOWN
    },
    invoiceId = facturaId
)

private fun instant(text: String): Instant = runCatching { Instant.parse(text) }.getOrElse { Instant.fromEpochSeconds(0) }

fun HistorialDto.toDomain() = InvoiceHistory(
    recognitions = reconocimientos.map { RecognitionAttempt(it.resultado, it.modelo, instant(it.creadoEn), it.error) }
        .sortedByDescending { it.createdAt },
    changes = cambios.map { it.toDomain() }.sortedByDescending { it.at }
)

private fun CambioHistorialDto.toDomain() = InvoiceChange(
    action = when (accion) {
        "CORRECCION" -> ChangeAction.CORRECTION
        "DESCARTE" -> ChangeAction.DISCARD
        "RECLASIFICACION" -> ChangeAction.RECLASSIFICATION
        else -> ChangeAction.OTHER
    },
    authorId = autorId,
    at = instant(ocurridoEn),
    // A free-form map (research D12): nested values are shown as their JSON text.
    previousValues = valoresAnteriores.orEmpty().mapValues { (_, value) ->
        when (value) {
            is JsonNull -> ""
            is JsonPrimitive -> value.content
            else -> value.toString()
        }
    }
)

fun EstadoTrimestreDto.toDomain(year: Int) = Quarter(
    year = year,
    quarter = trimestre,
    closed = cerrado,
    events = eventos.map {
        QuarterEvent(
            action = when (it.accion) {
                "CIERRE" -> QuarterAction.CLOSE
                "REAPERTURA" -> QuarterAction.REOPEN
                else -> QuarterAction.OTHER
            },
            authorId = it.autorId,
            at = instant(it.ocurridoEn),
            reason = it.motivo
        )
    }.sortedByDescending { it.at }
)

fun EmpresaDto.toDomain() = Company(legalName = razonSocial, taxId = nif, recognitionEnabled = reconocimientoActivo)

private fun GrupoReporteDto.toDomain() = ReportGroup(
    count = facturas,
    base = money(base),
    vatByRate = ivaPorTipo.entries.sortedBy { money(it.key) }.associate { it.key to money(it.value) },
    surcharge = money(recargo),
    withholding = money(retenciones),
    total = money(total),
    noQuota = sinCuota.mapNotNull { (cause, base) -> cause.toNoQuotaCause()?.let { it to money(base) } }.toMap()
)

fun ReporteDto.toDomain(period: ReportPeriod) = Report(
    period = period,
    from = LocalDate.parse(periodo.desde),
    to = LocalDate.parse(periodo.hasta),
    issued = emitidas.toDomain(),
    received = recibidas.toDomain(),
    pending = pendientes,
    closedQuarters = trimestresCerrados.map { it.anio to it.trimestre },
    computedAt = instant(calculadoEn)
)
