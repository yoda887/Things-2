package com.example.domain.usecase

import com.example.domain.usecase.project.*
import javax.inject.Inject

/**
 * Фасад для сценариев использования, связанных с проектами.
 */
class ProjectUseCases @Inject constructor(
    val addProject: AddProjectUseCase,
    val updateProject: UpdateProjectUseCase,
    val deleteProject: DeleteProjectUseCase
)
