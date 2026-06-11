package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для обновления существующего тега.
 */
class UpdateTagUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Записывает обновленный тег в репозиторий.
     */
    suspend operator fun invoke(tag: Tag) {
        repository.insertTag(tag)
    }
}
