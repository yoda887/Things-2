package com.example.domain.usecase.query

import com.example.data.model.RecurrenceRule
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Правила повтора всех шаблонов повторяющихся задач. */
class ObserveRecurrenceRulesUseCase @Inject constructor(private val repository: ITaskRepository) {
    operator fun invoke(): Flow<List<RecurrenceRule>> = repository.observeRecurrenceRules()
}
