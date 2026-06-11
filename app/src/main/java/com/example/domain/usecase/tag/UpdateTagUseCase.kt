package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для обновления существующего тега.
 */
class UpdateTagUseCase(private val repository: ITaskRepository) {

    /**
     * Записывает обновленный тег в репозиторий.
     */
    suspend operator fun invoke(tag: Tag) {
        repository.insertTag(tag)
    }
}
