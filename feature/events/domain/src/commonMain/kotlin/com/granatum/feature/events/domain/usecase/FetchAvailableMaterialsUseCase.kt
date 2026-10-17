package com.granatum.feature.events.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.feature.events.domain.model.AvailableMaterial
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.repository.EventsRepository

class FetchAvailableMaterialsUseCase(private val repository: EventsRepository) {
    suspend fun fetch(): Result<List<AvailableMaterial>, EventsError> = repository.fetchAvailableMaterials()
}
