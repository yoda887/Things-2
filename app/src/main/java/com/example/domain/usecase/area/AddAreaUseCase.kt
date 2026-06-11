package com.example.domain.usecase.area

import com.example.data.model.Area
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для создания новой сферы (области) жизни.
 */
class AddAreaUseCase(private val repository: ITaskRepository) {

    /**
     * Создает и вставляет сферу деятельности.
     */
    suspend operator fun invoke(title: String) {
        if (title.isNotBlank()) {
            val area = Area(title = title)
            repository.insertArea(area)
        }
    }
}
