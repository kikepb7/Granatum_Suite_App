package com.granatum.feature.invoicing.domain.repository

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.invoicing.domain.model.Company
import com.granatum.feature.invoicing.domain.model.Invoice
import com.granatum.feature.invoicing.domain.model.InvoiceDraft
import com.granatum.feature.invoicing.domain.model.InvoiceFilter
import com.granatum.feature.invoicing.domain.model.InvoiceHistory
import com.granatum.feature.invoicing.domain.model.InvoiceSummary
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.InvoicingFile
import com.granatum.feature.invoicing.domain.model.Page
import com.granatum.feature.invoicing.domain.model.Quarter
import com.granatum.feature.invoicing.domain.model.Report
import com.granatum.feature.invoicing.domain.model.ReportFormat
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.domain.model.UploadDocument
import com.granatum.feature.invoicing.domain.model.UploadResult

/**
 * Straight to the server, nothing kept on the device: tax data of third parties, validated only
 * there (specs/008-facturacion, research D1).
 */
interface InvoicingRepository {
    suspend fun invoices(filter: InvoiceFilter, page: Int): Result<Page<InvoiceSummary>, InvoicingError>
    suspend fun invoice(id: String): Result<Invoice, InvoicingError>
    suspend fun upload(documents: List<UploadDocument>): Result<List<UploadResult>, InvoicingError>
    suspend fun save(id: String, draft: InvoiceDraft): Result<Invoice, InvoicingError>
    suspend fun confirm(id: String, version: Int): Result<Invoice, InvoicingError>
    suspend fun discard(id: String, version: Int): Result<Invoice, InvoicingError>
    suspend fun recognize(id: String): EmptyResult<InvoicingError>
    suspend fun original(id: String): Result<InvoicingFile, InvoicingError>
    suspend fun history(id: String): Result<InvoiceHistory, InvoicingError>

    suspend fun quarters(year: Int): Result<List<Quarter>, InvoicingError>
    suspend fun closeQuarter(year: Int, quarter: Int): Result<Quarter, InvoicingError>
    suspend fun reopenQuarter(year: Int, quarter: Int, reason: String): Result<Quarter, InvoicingError>

    suspend fun report(period: ReportPeriod): Result<Report, InvoicingError>
    suspend fun reportFile(period: ReportPeriod, format: ReportFormat): Result<InvoicingFile, InvoicingError>

    /** Null when the company has not been configured yet. */
    suspend fun company(): Result<Company?, InvoicingError>
    suspend fun saveCompany(legalName: String, taxId: String): Result<Company, InvoicingError>
}
