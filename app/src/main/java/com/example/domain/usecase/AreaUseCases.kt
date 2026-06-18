package com.example.domain.usecase

import com.example.domain.usecase.area.*
import javax.inject.Inject

/**
 * Фасад для сценариев использования, связанных с областями ответственности.
 */
class AreaUseCases @Inject constructor(
    val addArea: AddAreaUseCase,
    val deleteArea: DeleteAreaUseCase,
    val updateArea: UpdateAreaUseCase
)
