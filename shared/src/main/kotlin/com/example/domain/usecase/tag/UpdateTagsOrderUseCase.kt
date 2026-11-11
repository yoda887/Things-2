package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для изменения порядка сортировки тегов.
 */
class UpdateTagsOrderUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Пересчитывает индексы сортировки и сохраняет теги.
     */
    suspend operator fun invoke(tags: List<Tag>) {
        val updatedTags = tags.mapIndexed { index, tag -> tag.copy(sortOrder = index) }
        repository.updateTagsOrder(updatedTags)
    }
}
