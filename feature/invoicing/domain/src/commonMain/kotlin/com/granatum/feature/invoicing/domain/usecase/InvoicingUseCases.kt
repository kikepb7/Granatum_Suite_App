package com.granatum.feature.invoicing.domain.usecase

import com.granatum.feature.invoicing.domain.repository.InvoicingRepository

/**
 * The invoicing screens reach the repository through this single entry point: its operations are
 * one-to-one with the server's routes, as in inventory. Rules that grow later (the owner will add
 * features) get their own use case here.
 */
class InvoicingUseCases(val repository: InvoicingRepository)
