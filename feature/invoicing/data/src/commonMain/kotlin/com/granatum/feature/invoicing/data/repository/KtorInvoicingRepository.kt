package com.granatum.feature.invoicing.data.repository

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.map
import com.granatum.feature.invoicing.data.dto.EmpresaRequestDto
import com.granatum.feature.invoicing.data.mappers.toApi
import com.granatum.feature.invoicing.data.mappers.toDomain
import com.granatum.feature.invoicing.data.mappers.toDto
import com.granatum.feature.invoicing.data.remote.InvoicingRemoteDataSource
import com.granatum.feature.invoicing.data.remote.InvoicingRemoteDataSource.ReportParams
import com.granatum.feature.invoicing.domain.model.Company
import com.granatum.feature.invoicing.domain.model.FiscalLimits
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
import com.granatum.feature.invoicing.domain.repository.InvoicingRepository

class KtorInvoicingRepository(private val remote: InvoicingRemoteDataSource) : InvoicingRepository {

    override suspend fun invoices(filter: InvoiceFilter, page: Int): Result<Page<InvoiceSummary>, InvoicingError> =
        remote.invoices(
            estado = filter.state?.toApi(),
            tipo = filter.type?.toApi(),
            desde = filter.from?.toString(),
            hasta = filter.to?.toString(),
            parte = filter.text.trim().ifEmpty { null },
            pagina = page,
            tamano = FiscalLimits.PAGE_SIZE
        ).map { dto -> Page(dto.elementos.map { it.toDomain() }, dto.pagina, dto.tamano, dto.total) }

    override suspend fun invoice(id: String): Result<Invoice, InvoicingError> = remote.invoice(id).map { it.toDomain() }

    override suspend fun upload(documents: List<UploadDocument>): Result<List<UploadResult>, InvoicingError> =
        remote.upload(documents).map { list -> list.map { it.toDomain() }.sortedBy { it.position } }

    override suspend fun save(id: String, draft: InvoiceDraft): Result<Invoice, InvoicingError> = remote.save(id, draft.toDto()).map { it.toDomain() }
    override suspend fun confirm(id: String, version: Int): Result<Invoice, InvoicingError> = remote.confirm(id, version).map { it.toDomain() }
    override suspend fun discard(id: String, version: Int): Result<Invoice, InvoicingError> = remote.discard(id, version).map { it.toDomain() }
    override suspend fun recognize(id: String): EmptyResult<InvoicingError> = remote.recognize(id)
    override suspend fun original(id: String): Result<InvoicingFile, InvoicingError> = remote.original(id)
    override suspend fun history(id: String): Result<InvoiceHistory, InvoicingError> = remote.history(id).map { it.toDomain() }

    override suspend fun quarters(year: Int): Result<List<Quarter>, InvoicingError> =
        remote.quarters(year).map { list -> list.map { it.toDomain(year) }.sortedBy { it.quarter } }

    override suspend fun closeQuarter(year: Int, quarter: Int): Result<Quarter, InvoicingError> = remote.closeQuarter(year, quarter).map { it.toDomain(year) }
    override suspend fun reopenQuarter(year: Int, quarter: Int, reason: String): Result<Quarter, InvoicingError> =
        remote.reopenQuarter(year, quarter, reason.trim()).map { it.toDomain(year) }

    override suspend fun report(period: ReportPeriod): Result<Report, InvoicingError> = remote.report(period.toParams()).map { it.toDomain(period) }

    override suspend fun reportFile(period: ReportPeriod, format: ReportFormat): Result<InvoicingFile, InvoicingError> =
        remote.reportFile(period.toParams(), if (format == ReportFormat.CSV) "csv" else "pdf")

    override suspend fun company(): Result<Company?, InvoicingError> = when (val result = remote.company()) {
        is Result.Success -> Result.Success(result.data.toDomain())
        // Not configured yet is a state of the company, not a failure of the request.
        is Result.Failure -> if (result.error == InvoicingError.CompanyNotConfigured || result.error == InvoicingError.NotFound) {
            Result.Success(null)
        } else {
            result
        }
    }

    override suspend fun saveCompany(legalName: String, taxId: String): Result<Company, InvoicingError> =
        remote.saveCompany(EmpresaRequestDto(razonSocial = legalName.trim(), nif = taxId.trim())).map { it.toDomain() }

    private fun ReportPeriod.toParams() = when (this) {
        is ReportPeriod.Monthly -> ReportParams("MENSUAL", year, month, null)
        is ReportPeriod.Quarterly -> ReportParams("TRIMESTRAL", year, null, quarter)
        is ReportPeriod.Yearly -> ReportParams("ANUAL", year, null, null)
    }
}
