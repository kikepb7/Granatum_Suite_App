package com.granatum.feature.invoicing.presentation.common

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.invoicing.domain.model.ChangeAction
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import granatumsuite.feature.invoicing.presentation.generated.resources.*
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun InvoiceState.label() =
    when (this) {
        InvoiceState.PENDING_RECOGNITION -> Res.string.state_pending
        InvoiceState.DRAFT -> Res.string.state_draft
        InvoiceState.CONFIRMED -> Res.string.state_confirmed
        InvoiceState.DISCARDED -> Res.string.state_discarded
    }

fun InvoiceType?.label() =
    when (this) {
        InvoiceType.ISSUED -> Res.string.type_issued
        InvoiceType.RECEIVED -> Res.string.type_received
        null -> Res.string.type_unknown
    }

fun NoQuotaCause.label() =
    when (this) {
        NoQuotaCause.EXEMPT -> Res.string.cause_exempt
        NoQuotaCause.REVERSE_CHARGE -> Res.string.cause_reverse_charge
        NoQuotaCause.INTRA_EU -> Res.string.cause_intra_eu
    }

fun ChangeAction.label() =
    when (this) {
        ChangeAction.CORRECTION -> Res.string.change_correction
        ChangeAction.DISCARD -> Res.string.change_discard
        ChangeAction.RECLASSIFICATION -> Res.string.change_reclassification
        ChangeAction.OTHER -> Res.string.change_other
    }

fun UploadOutcome.label() =
    when (this) {
        UploadOutcome.ACCEPTED -> Res.string.upload_accepted
        UploadOutcome.DUPLICATE -> Res.string.upload_duplicate
        UploadOutcome.UNSUPPORTED_FORMAT -> Res.string.upload_unsupported
        UploadOutcome.TOO_LARGE -> Res.string.upload_too_large
        UploadOutcome.EMPTY -> Res.string.upload_empty
        UploadOutcome.UNREADABLE_PDF -> Res.string.upload_unreadable_pdf
        UploadOutcome.UNKNOWN -> Res.string.upload_unknown
    }

fun InvoicingError.toUiText(): UiText =
    when (this) {
        is InvoicingError.QuarterHasPending -> UiText.Resource(Res.string.error_quarter_pending, arrayOf(message))
        is InvoicingError.Incoherent -> UiText.Resource(Res.string.error_incoherent, arrayOf(message))
        else ->
            UiText.Resource(
                when (this) {
                    InvoicingError.NoInternet -> Res.string.error_no_internet
                    InvoicingError.NotFound -> Res.string.error_not_found
                    InvoicingError.CompanyNotConfigured -> Res.string.error_company_not_configured
                    InvoicingError.InvalidTaxId -> Res.string.error_invalid_tax_id
                    InvoicingError.StaleVersion -> Res.string.error_stale_version
                    InvoicingError.StateNotAllowed -> Res.string.error_state_not_allowed
                    InvoicingError.QuarterClosed -> Res.string.error_quarter_closed
                    InvoicingError.QuarterOpen -> Res.string.error_quarter_open
                    InvoicingError.Duplicate -> Res.string.error_duplicate
                    InvoicingError.RecognitionUnavailable -> Res.string.error_recognition_unavailable
                    InvoicingError.InvalidPeriod -> Res.string.error_invalid_period
                    InvoicingError.Invalid -> Res.string.error_invalid
                    InvoicingError.TooLarge -> Res.string.error_too_large
                    InvoicingError.Forbidden -> Res.string.error_forbidden
                    InvoicingError.RateLimited -> Res.string.error_rate_limited
                    else -> Res.string.error_unknown
                },
            )
    }

/** `01/10/2026`, as dates are written in Spain. */
fun LocalDate.label(): String = "${day.toString().padStart(2, '0')}/${month.number.toString().padStart(2, '0')}/$year"

/** `01/10/2026 09:30` in the device's zone. */
fun Instant.label(zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = toLocalDateTime(zone)
    return "${local.date.label()} ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
