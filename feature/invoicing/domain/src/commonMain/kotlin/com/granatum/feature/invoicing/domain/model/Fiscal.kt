package com.granatum.feature.invoicing.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

enum class QuarterAction { CLOSE, REOPEN, OTHER }

data class QuarterEvent(val action: QuarterAction, val authorId: String, val at: Instant, val reason: String?)

data class Quarter(val year: Int, val quarter: Int, val closed: Boolean, val events: List<QuarterEvent>)

sealed interface ReportPeriod {
    val year: Int

    data class Monthly(override val year: Int, val month: Int) : ReportPeriod
    data class Quarterly(override val year: Int, val quarter: Int) : ReportPeriod
    data class Yearly(override val year: Int) : ReportPeriod
}

enum class ReportFormat { CSV, PDF }

data class ReportGroup(
    val count: Int,
    val base: Money,
    /** Keyed by the rate as the server writes it (`"21.00"`), in rate order. */
    val vatByRate: Map<String, Money>,
    val surcharge: Money,
    val withholding: Money,
    val total: Money,
    val noQuota: Map<NoQuotaCause, Money>
)

data class Report(
    val period: ReportPeriod,
    val from: LocalDate,
    val to: LocalDate,
    val issued: ReportGroup,
    val received: ReportGroup,
    val pending: Int,
    val closedQuarters: List<Pair<Int, Int>>,
    val computedAt: Instant
)

data class Company(val legalName: String, val taxId: String, val recognitionEnabled: Boolean)

object FiscalLimits {
    const val NAME = 200
    const val TAX_ID = 20
    const val NUMBER = 60
    const val CONCEPT = 500
    const val COMPANY_NAME = 200
    const val REOPEN_REASON_MIN = 10
    const val REOPEN_REASON_MAX = 500
    val MAX_RATE = Money(100_00)
    const val PAGE_SIZE = 50
    const val MAX_FILE_BYTES = 10L * 1024 * 1024
    const val MAX_REQUEST_BYTES = 50L * 1024 * 1024
}
