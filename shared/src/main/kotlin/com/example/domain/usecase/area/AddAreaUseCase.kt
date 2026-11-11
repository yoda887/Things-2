package com.example.domain.usecase.area

import com.example.data.model.Area
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для создания новой сферы (области) жизни.
 */
class AddAreaUseCase @Inject constructor(private val repository: ITaskRepository) {

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
