package com.example.domain.usecase.area

import com.example.data.model.Area
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для обновления сферы (области) жизни.
 */
class UpdateAreaUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Обновляет сферу деятельности в БД.
     */
    suspend operator fun invoke(area: Area) {
        // [ИЗМЕНЕНИЕ]: Вызов репозитория для сохранения обновленной области
        repository.insertArea(area)
    }

    /**
     * Пакетно обновляет порядок сфер (областей) деятельности в БД.
     */
    suspend operator fun invoke(areas: List<Area>) {
        areas.forEachIndexed { index, area ->
            repository.insertArea(area.copy(sortOrder = index))
        }
    }
}
