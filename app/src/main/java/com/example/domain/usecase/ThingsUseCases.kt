package com.example.domain.usecase

import com.example.domain.usecase.task.*
import com.example.domain.usecase.checklist.*
import com.example.domain.usecase.tag.*
import com.example.domain.usecase.project.*
import com.example.domain.usecase.area.*
import com.example.domain.usecase.sync.*
import com.example.domain.usecase.query.*

/**
 * Единый контейнер (фасад) всех сценариев использования (Use Cases) приложения.
 * Упрощает внедрение зависимостей во ViewModel и сохраняет архитектурную чистоту.
 */
class ThingsUseCases(
    // Задачи (Tasks)
    val addTask: AddTaskUseCase,
    val updateTask: UpdateTaskUseCase,
    val toggleTaskCompletion: ToggleTaskCompletionUseCase,
    val deleteTask: DeleteTaskUseCase,
    val duplicateTask: DuplicateTaskUseCase,

    // Чек-листы (Checklists)
    val addChecklistItem: AddChecklistItemUseCase,
    val deleteChecklistItem: DeleteChecklistItemUseCase,
    val toggleChecklistItem: ToggleChecklistItemUseCase,
    val updateChecklistItems: UpdateChecklistItemsUseCase,

    // Теги (Tags)
    val createGroup: CreateGroupUseCase,
    val createTagInGroup: CreateTagInGroupUseCase,
    val moveTagToGroup: MoveTagToGroupUseCase,
    val deleteTag: DeleteTagUseCase,
    val updateTag: UpdateTagUseCase,
    val updateTagsOrder: UpdateTagsOrderUseCase,
    val insertTag: InsertTagUseCase,

    // Проекты (Projects)
    val addProject: AddProjectUseCase,
    val updateProject: UpdateProjectUseCase,
    val deleteProject: DeleteProjectUseCase,

    // Области (Areas)
    val addArea: AddAreaUseCase,
    val deleteArea: DeleteAreaUseCase,

    // Синхронизация (Sync)
    val syncGoogleTasks: SyncGoogleTasksUseCase,
    val fetchLocalCalendarEvents: FetchLocalCalendarEventsUseCase,

    // Запросы (Queries)
    val observeAllTasks: ObserveAllTasksUseCase,
    val observeAllProjects: ObserveAllProjectsUseCase,
    val observeAllAreas: ObserveAllAreasUseCase,
    val observeAllTags: ObserveAllTagsUseCase
)
