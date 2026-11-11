package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для удаления тега из системы.
 */
class DeleteTagUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Удаляет тег каскадно (с его дочерними элементами) и очищает кэш связанных задач.
     */
    suspend operator fun invoke(tag: Tag) {
        repository.deleteTag(tag)
    }
}
