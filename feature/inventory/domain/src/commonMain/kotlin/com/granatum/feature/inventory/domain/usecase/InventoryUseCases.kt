package com.granatum.feature.inventory.domain.usecase

import com.granatum.feature.inventory.domain.repository.InventoryRepository

/**
 * The inventory screens reach the data through this single entry point: the operations are
 * one-to-one with the server's routes, and a use case per call would only
 * add files.
 */
class InventoryUseCases(val repository: InventoryRepository)
