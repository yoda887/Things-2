package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для вставки нового тега по названию.
 */
class InsertTagUseCase(private val repository: ITaskRepository) {

    /**
     * Обрезает пробелы и добавляет тег, если имя не пустое.
     */
    suspend operator fun invoke(tagTitle: String, parentId: String? = null) {
        val titleTrimmed = tagTitle.trim()
        if (titleTrimmed.isNotBlank()) {
            repository.insertTag(Tag(title = titleTrimmed, parentId = parentId))
        }
    }
}
