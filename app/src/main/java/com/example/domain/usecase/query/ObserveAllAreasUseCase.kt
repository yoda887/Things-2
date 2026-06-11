package com.example.domain.usecase.query

import com.example.data.model.Area
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Сценарий чтения (Use Case) для наблюдения за всеми сферами (областями) жизни в реальном времени.
 */
class ObserveAllAreasUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Возвращает реактивный поток всех сфер.
     */
    operator fun invoke(): Flow<List<Area>> {
        return repository.observeAllAreas()
    }
}
