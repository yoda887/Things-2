package com.example.di

import com.example.domain.repository.ITaskRepository
import com.example.domain.usecase.ThingsUseCases
import com.example.domain.usecase.task.*
import com.example.domain.usecase.checklist.*
import com.example.domain.usecase.tag.*
import com.example.domain.usecase.project.*
import com.example.domain.usecase.area.*
import com.example.domain.usecase.sync.*
import com.example.domain.usecase.query.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideThingsUseCases(repository: ITaskRepository): ThingsUseCases {
        return ThingsUseCases(
            addTask = AddTaskUseCase(repository),
            updateTask = UpdateTaskUseCase(repository),
            toggleTaskCompletion = ToggleTaskCompletionUseCase(repository),
            deleteTask = DeleteTaskUseCase(repository),
            duplicateTask = DuplicateTaskUseCase(repository),
            addChecklistItem = AddChecklistItemUseCase(repository),
            deleteChecklistItem = DeleteChecklistItemUseCase(repository),
            toggleChecklistItem = ToggleChecklistItemUseCase(repository),
            updateChecklistItems = UpdateChecklistItemsUseCase(repository),
            createGroup = CreateGroupUseCase(repository),
            createTagInGroup = CreateTagInGroupUseCase(repository),
            moveTagToGroup = MoveTagToGroupUseCase(repository),
            deleteTag = DeleteTagUseCase(repository),
            updateTag = UpdateTagUseCase(repository),
            updateTagsOrder = UpdateTagsOrderUseCase(repository),
            insertTag = InsertTagUseCase(repository),
            addProject = AddProjectUseCase(repository),
            updateProject = UpdateProjectUseCase(repository),
            deleteProject = DeleteProjectUseCase(repository),
            addArea = AddAreaUseCase(repository),
            deleteArea = DeleteAreaUseCase(repository),
            syncGoogleTasks = SyncGoogleTasksUseCase(repository),
            fetchLocalCalendarEvents = FetchLocalCalendarEventsUseCase(repository),
            observeAllTasks = ObserveAllTasksUseCase(repository),
            observeAllProjects = ObserveAllProjectsUseCase(repository),
            observeAllAreas = ObserveAllAreasUseCase(repository),
            observeAllTags = ObserveAllTagsUseCase(repository)
        )
    }
}
