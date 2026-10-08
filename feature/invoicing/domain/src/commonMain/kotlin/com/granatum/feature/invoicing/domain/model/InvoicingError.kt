package com.granatum.feature.invoicing.domain.model

import com.granatum.core.domain.util.Error

/** Errors of the invoicing module, from the backend's InvoicesExceptions (research D10). */
sealed interface InvoicingError : Error {
    data object NoInternet : InvoicingError

    data object NotFound : InvoicingError

    data object CompanyNotConfigured : InvoicingError

    data object InvalidTaxId : InvoicingError

    data object StaleVersion : InvoicingError

    data object StateNotAllowed : InvoicingError

    data object QuarterClosed : InvoicingError

    data object QuarterOpen : InvoicingError

    data class QuarterHasPending(
        val message: String,
    ) : InvoicingError

    data object Duplicate : InvoicingError

    /** The server's message lists `field: CODE` pairs; kept to show which fields block. */
    data class Incoherent(
        val message: String,
    ) : InvoicingError

    data object RecognitionUnavailable : InvoicingError

    data object InvalidPeriod : InvoicingError

    data object Invalid : InvoicingError

    data object TooLarge : InvoicingError

    data object Forbidden : InvoicingError

    data object RateLimited : InvoicingError

    data object Unknown : InvoicingError
}
