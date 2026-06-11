package com.example.domain.usecase

import com.example.domain.usecase.task.*
import javax.inject.Inject

/**
 * Фасад для сценариев использования, связанных с задачами.
 */
class TaskUseCases @Inject constructor(
    val addTask: AddTaskUseCase,
    val deleteTask: DeleteTaskUseCase,
    val duplicateTask: DuplicateTaskUseCase,
    val toggleTaskCompletion: ToggleTaskCompletionUseCase,
    val updateTask: UpdateTaskUseCase
)
