package com.example.domain.usecase

import com.example.domain.usecase.query.*
import javax.inject.Inject

/**
 * Фасад для реактивного наблюдения за данными в реальном времени.
 */
class QueryUseCases @Inject constructor(
    val observeAllTasks: ObserveAllTasksUseCase,
    val observeAllProjects: ObserveAllProjectsUseCase,
    val observeAllAreas: ObserveAllAreasUseCase,
    val observeAllTags: ObserveAllTagsUseCase
)
