package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для удаления тега из системы.
 */
class DeleteTagUseCase(private val repository: ITaskRepository) {

    /**
     * Удаляет тег каскадно (с его дочерними элементами) и очищает кэш связанных задач.
     */
    suspend operator fun invoke(tag: Tag) {
        repository.deleteTag(tag)
    }
}
