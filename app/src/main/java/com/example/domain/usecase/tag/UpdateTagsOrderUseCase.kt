package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для изменения порядка сортировки тегов.
 */
class UpdateTagsOrderUseCase(private val repository: ITaskRepository) {

    /**
     * Пересчитывает индексы сортировки и сохраняет теги.
     */
    suspend operator fun invoke(tags: List<Tag>) {
        val updatedTags = tags.mapIndexed { index, tag -> tag.copy(sortOrder = index) }
        repository.updateTagsOrder(updatedTags)
    }
}
