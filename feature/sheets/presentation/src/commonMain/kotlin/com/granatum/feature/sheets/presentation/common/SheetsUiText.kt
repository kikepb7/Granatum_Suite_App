package com.granatum.feature.sheets.presentation.common

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.sheets.domain.model.SheetAccessProblem
import com.granatum.feature.sheets.domain.model.SheetFailureReason
import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.model.SheetTask
import com.granatum.feature.sheets.domain.model.SheetTaskKind
import com.granatum.feature.sheets.domain.model.SheetsError
import granatumsuite.feature.sheets.presentation.generated.resources.Res
import granatumsuite.feature.sheets.presentation.generated.resources.error_forbidden
import granatumsuite.feature.sheets.presentation.generated.resources.error_invalid_link
import granatumsuite.feature.sheets.presentation.generated.resources.error_invalid_period
import granatumsuite.feature.sheets.presentation.generated.resources.error_no_internet
import granatumsuite.feature.sheets.presentation.generated.resources.error_not_configured
import granatumsuite.feature.sheets.presentation.generated.resources.error_same_spreadsheet
import granatumsuite.feature.sheets.presentation.generated.resources.error_task_not_failed
import granatumsuite.feature.sheets.presentation.generated.resources.error_task_not_found
import granatumsuite.feature.sheets.presentation.generated.resources.error_unavailable
import granatumsuite.feature.sheets.presentation.generated.resources.error_unknown
import granatumsuite.feature.sheets.presentation.generated.resources.error_unreachable
import granatumsuite.feature.sheets.presentation.generated.resources.field_not_found
import granatumsuite.feature.sheets.presentation.generated.resources.field_not_shared
import granatumsuite.feature.sheets.presentation.generated.resources.field_read_only
import granatumsuite.feature.sheets.presentation.generated.resources.reason_no_handler
import granatumsuite.feature.sheets.presentation.generated.resources.reason_not_found_invoices
import granatumsuite.feature.sheets.presentation.generated.resources.reason_not_found_shifts
import granatumsuite.feature.sheets.presentation.generated.resources.reason_not_shared_invoices
import granatumsuite.feature.sheets.presentation.generated.resources.reason_not_shared_shifts
import granatumsuite.feature.sheets.presentation.generated.resources.reason_other
import granatumsuite.feature.sheets.presentation.generated.resources.reason_read_only_invoices
import granatumsuite.feature.sheets.presentation.generated.resources.reason_read_only_shifts
import granatumsuite.feature.sheets.presentation.generated.resources.reason_transient
import granatumsuite.feature.sheets.presentation.generated.resources.reason_unavailable
import granatumsuite.feature.sheets.presentation.generated.resources.task_invoice
import granatumsuite.feature.sheets.presentation.generated.resources.task_invoice_month
import granatumsuite.feature.sheets.presentation.generated.resources.task_shift_month
import granatumsuite.feature.sheets.presentation.generated.resources.task_unknown
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** What each error tells the `ADMIN` (FR-007): never the server's code. */
internal fun SheetsError.toUiText(): UiText =
    UiText.Resource(
        when (this) {
            is SheetsError.NotAccessible -> reason.fieldText()
            SheetsError.InvalidLink -> Res.string.error_invalid_link
            SheetsError.SameSpreadsheet -> Res.string.error_same_spreadsheet
            SheetsError.NotConfigured -> Res.string.error_not_configured
            SheetsError.Unavailable -> Res.string.error_unavailable
            SheetsError.Unreachable -> Res.string.error_unreachable
            SheetsError.InvalidPeriod -> Res.string.error_invalid_period
            SheetsError.TaskNotFound -> Res.string.error_task_not_found
            SheetsError.TaskNotFailed -> Res.string.error_task_not_failed
            SheetsError.NoInternet -> Res.string.error_no_internet
            SheetsError.Forbidden -> Res.string.error_forbidden
            SheetsError.Unknown -> Res.string.error_unknown
        },
    )

/** Under the link field: what is wrong and what to do (share it with the account as Editor). */
internal fun SheetAccessProblem.fieldText() =
    when (this) {
        SheetAccessProblem.NOT_SHARED -> Res.string.field_not_shared
        SheetAccessProblem.READ_ONLY -> Res.string.field_read_only
        SheetAccessProblem.NOT_FOUND -> Res.string.field_not_found
    }

/** «Hoja de recibos sin permiso de edición», «Google no responde»… */
internal fun SheetTask.reasonText(): UiText {
    val invoices = isInvoices
    return UiText.Resource(
        when (failureReason) {
            SheetFailureReason.NOT_SHARED -> if (invoices) Res.string.reason_not_shared_invoices else Res.string.reason_not_shared_shifts
            SheetFailureReason.READ_ONLY -> if (invoices) Res.string.reason_read_only_invoices else Res.string.reason_read_only_shifts
            SheetFailureReason.NOT_FOUND -> if (invoices) Res.string.reason_not_found_invoices else Res.string.reason_not_found_shifts
            SheetFailureReason.TRANSIENT -> Res.string.reason_transient
            SheetFailureReason.NO_HANDLER -> Res.string.reason_no_handler
            SheetFailureReason.UNAVAILABLE -> Res.string.reason_unavailable
            SheetFailureReason.OTHER, null -> Res.string.reason_other
        },
    )
}

/** «Fichajes de octubre de 2026», «Recibos de septiembre de 2026». */
internal fun SheetTask.titleText(dates: SheetsDateFormatter): UiText {
    val month = month
    return when {
        kind == SheetTaskKind.SHIFT_MONTH && month != null -> UiText.Resource(Res.string.task_shift_month, arrayOf(dates.month(month)))
        kind == SheetTaskKind.INVOICE_MONTH && month != null -> UiText.Resource(Res.string.task_invoice_month, arrayOf(dates.month(month)))
        kind == SheetTaskKind.INVOICE -> UiText.Resource(Res.string.task_invoice)
        else -> UiText.Resource(Res.string.task_unknown)
    }
}

/**
 * Months and moments as a Spanish reader writes them (`octubre de 2026`, `18/10/2026 12:05`). The
 * app is in Spanish only, so the names are written here, as in the events feature.
 */
internal class SheetsDateFormatter(
    private val zone: TimeZone = TimeZone.of("Europe/Madrid"),
) {
    fun month(month: SheetMonth): String = "${MONTHS[month.month - 1]} de ${month.year}"

    fun moment(instant: Instant): String {
        val local = instant.toLocalDateTime(zone)
        return "${two(local.day)}/${two(local.month.number)}/${local.year} ${two(local.hour)}:${two(local.minute)}"
    }

    private fun two(value: Int) = value.toString().padStart(2, '0')

    private companion object {
        val MONTHS =
            listOf(
                "enero",
                "febrero",
                "marzo",
                "abril",
                "mayo",
                "junio",
                "julio",
                "agosto",
                "septiembre",
                "octubre",
                "noviembre",
                "diciembre",
            )
    }
}
