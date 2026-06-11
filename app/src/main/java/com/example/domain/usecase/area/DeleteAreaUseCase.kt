package com.example.domain.usecase.area

import com.example.data.model.Area
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для удаления сферы (области) жизни.
 */
class DeleteAreaUseCase(private val repository: ITaskRepository) {

    /**
     * Удаляет переданную сферу из репозитория.
     */
    suspend operator fun invoke(area: Area) {
        repository.deleteArea(area)
    }
}
