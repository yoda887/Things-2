package com.example.domain.usecase.tag

import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для ремонта данных тегов: слияние дублей справочника
 * и восстановление связей задач с тегами.
 */
class RepairTagsUseCase @Inject constructor(private val repository: ITaskRepository) {

    suspend operator fun invoke() {
        repository.repairTags()
    }
}
