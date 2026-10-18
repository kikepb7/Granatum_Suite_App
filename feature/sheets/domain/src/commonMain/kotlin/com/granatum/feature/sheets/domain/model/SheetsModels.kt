package com.granatum.feature.sheets.domain.model

import kotlin.time.Instant

/** One of the two spreadsheets the copy writes to, as the server names it after saving. */
data class Spreadsheet(
    val id: String,
    val url: String,
)

/**
 * `GET /api/sheets/settings` (backend spec 012). Without Google credentials on the server
 * [available] is false and nothing else is known; without spreadsheets both are null.
 */
data class SheetsSettings(
    val available: Boolean,
    val serviceAccountEmail: String?,
    val shifts: Spreadsheet?,
    val invoices: Spreadsheet?,
    val configuredAt: Instant?,
    val configuredBy: String?,
) {
    val isConfigured: Boolean get() = shifts != null && invoices != null
}

/** Which of the two link fields an error is about (`"shifts"` / `"invoices"` in the API). */
enum class SpreadsheetField { SHIFTS, INVOICES }

/** Why the technical account cannot write to a spreadsheet (`reason` in the API). */
enum class SheetAccessProblem { NOT_FOUND, NOT_SHARED, READ_ONLY, API_DISABLED }

enum class SheetTaskKind { SHIFT_MONTH, INVOICE, INVOICE_MONTH, UNKNOWN }

enum class SheetTaskStatus { PENDING, RUNNING, DONE, FAILED, UNKNOWN }

/**
 * Why a task failed: the server's `errorReason` code (backend 16b76cb). Tasks from an older server
 * carry only the Spanish `lastError`, which [from] still recognises.
 */
enum class SheetFailureReason {
    NOT_SHARED,
    READ_ONLY,
    NOT_FOUND,

    /** Google refused the request itself. */
    BAD_REQUEST,

    /** The Google Sheets API is not enabled in the service account's project. */
    API_DISABLED,

    /** The writer gave up, e.g. the spreadsheet is no longer configured. */
    ABORTED,

    /** Google did not answer; the server keeps retrying. */
    TRANSIENT,

    /** The server has nothing that writes this kind of task. */
    NO_HANDLER,

    /** The server has no Google credentials. */
    UNAVAILABLE,
    OTHER,
    ;

    companion object {
        /** The server's `errorReason`; unknown codes become [OTHER]. */
        fun fromCode(code: String?): SheetFailureReason? =
            code?.takeIf { it.isNotBlank() }?.let { value ->
                when (value) {
                    "NO_HANDLER" -> NO_HANDLER
                    else -> entries.firstOrNull { it.name == value } ?: OTHER
                }
            }

        /** Matches the messages of the backend's `SheetsFailureReason` and `SheetTaskWorker`. */
        fun from(lastError: String?): SheetFailureReason? {
            val text = lastError?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
            return when {
                "solo como lectora" in text || "read_only" in text -> READ_ONLY
                "no está compartida" in text || "not_shared" in text -> NOT_SHARED
                "no existe" in text || "no la ve" in text || "not_found" in text -> NOT_FOUND
                "ningún escritor" in text -> NO_HANDLER
                "no está disponible" in text || "credenciales" in text -> UNAVAILABLE
                "no responde" in text || "se reintentará" in text || "timeout" in text -> TRANSIENT
                else -> OTHER
            }
        }
    }
}

/** `GET /api/sheets/tasks/{id}`: one write to a spreadsheet, queued on the server. */
data class SheetTask(
    val id: String,
    val kind: SheetTaskKind,
    /** `2026-10` for a month, the invoice id for one invoice. */
    val reference: String,
    val status: SheetTaskStatus,
    val attempts: Int,
    val rowCount: Int?,
    val lastNamesPending: Int?,
    val lastError: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val completedAt: Instant?,
    /** The server's `errorReason` code, when it sends one. */
    val errorReason: String? = null,
) {
    val isFinished: Boolean get() = status == SheetTaskStatus.DONE || status == SheetTaskStatus.FAILED

    val failureReason: SheetFailureReason? get() = SheetFailureReason.fromCode(errorReason) ?: SheetFailureReason.from(lastError)

    /** The month a month task is about, from its `AAAA-MM` reference. */
    val month: SheetMonth? get() = SheetMonth.parse(reference)

    /** Whether the task writes to the receipts spreadsheet rather than the shifts one. */
    val isInvoices: Boolean get() = kind == SheetTaskKind.INVOICE || kind == SheetTaskKind.INVOICE_MONTH
}

/** `GET /api/sheets/status`. */
data class SheetsStatus(
    val lastSuccessAt: Instant?,
    val pending: Long,
    val failed: Long,
    val recent: List<SheetTask>,
) {
    val failedTasks: List<SheetTask> get() = recent.filter { it.status == SheetTaskStatus.FAILED }
}

/** A calendar month, as the export routes take it (`/{year}/{month}`) and the tabs name it. */
data class SheetMonth(
    val year: Int,
    val month: Int,
) : Comparable<SheetMonth> {
    init {
        require(month in 1..12) { "month must be 1..12" }
    }

    fun previous(): SheetMonth = if (month == 1) SheetMonth(year - 1, 12) else SheetMonth(year, month - 1)

    override fun compareTo(other: SheetMonth): Int = compareValuesBy(this, other, { it.year }, { it.month })

    /** `2026-10`, the tab's name and the task's reference. */
    override fun toString(): String = "$year-${month.toString().padStart(2, '0')}"

    companion object {
        /** How far back the selector goes: the 4 years the time register must be kept. */
        const val RETENTION_MONTHS = 48

        fun parse(reference: String): SheetMonth? {
            val parts = reference.split('-')
            if (parts.size != 2) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
            return SheetMonth(year, month)
        }

        /** From [current] backwards, never a future month (the server refuses them with `INVALID_PERIOD`). */
        fun exportable(
            current: SheetMonth,
            count: Int = RETENTION_MONTHS,
        ): List<SheetMonth> = generateSequence(current) { it.previous() }.take(count).toList()
    }
}

/** What waiting for a task ends with: finished (done or failed), or still in the queue. */
sealed interface SheetTaskOutcome {
    data class Finished(
        val task: SheetTask,
    ) : SheetTaskOutcome

    /** Google has not answered in time: the server finishes it later and the status shows it. */
    data class StillQueued(
        val task: SheetTask,
    ) : SheetTaskOutcome
}
