package com.example.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.LocalTaskDataSource
import com.example.data.local.DeviceCalendarDataSource
import com.example.data.local.DatabaseSeeder
import com.example.data.remote.RemoteTaskDataSource
import com.example.data.remote.GoogleTasksService
import com.example.data.repository.TaskRepositoryImpl
import com.example.domain.repository.ITaskRepository
import com.example.domain.usecase.ThingsUseCases
import com.example.domain.usecase.task.*
import com.example.domain.usecase.checklist.*
import com.example.domain.usecase.tag.*
import com.example.domain.usecase.project.*
import com.example.domain.usecase.area.*
import com.example.domain.usecase.sync.*
import com.example.domain.usecase.query.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Контейнер ручного внедрения зависимостей (Manual Dependency Injection).
 * Обеспечивает единственную точку инициализации и долговечность графа зависимостей на уровне всего приложения.
 */
class AppContainer(private val context: Context) {

    // 1. Создаем глобальный Scope для фоновых задач уровня приложения
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 2. Инициализация Базы Данных (Room)
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "things_database"
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Запускается ТОЛЬКО один раз при физическом создании файла БД
                applicationScope.launch {
                    // Используем localDataSource (благодаря by lazy нет проблем с циклической зависимостью)
                    DatabaseSeeder.seedDatabase(localDataSource)
                }
            }
        })
        .build()
    }

    // 2. Инициализация Сетевого источника данных (Retrofit)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://tasks.googleapis.com/v1/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private val googleTasksService: GoogleTasksService by lazy {
        retrofit.create(GoogleTasksService::class.java)
    }

    // 3. Локальные и удаленные DataSources
    private val localDataSource by lazy { LocalTaskDataSource(database.taskDao()) }
    private val remoteDataSource by lazy { RemoteTaskDataSource(googleTasksService) }
    private val calendarDataSource by lazy { DeviceCalendarDataSource(context.applicationContext) }

    // 4. Репозиторий
    val taskRepository: ITaskRepository by lazy {
        TaskRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            calendarDataSource = calendarDataSource
        )
    }

    // 5. Инициализация сценариев использования (Use Cases Facade)
    val useCases: ThingsUseCases by lazy {
        ThingsUseCases(
            // Задачи
            addTask = AddTaskUseCase(taskRepository),
            updateTask = UpdateTaskUseCase(taskRepository),
            toggleTaskCompletion = ToggleTaskCompletionUseCase(taskRepository),
            deleteTask = DeleteTaskUseCase(taskRepository),
            duplicateTask = DuplicateTaskUseCase(taskRepository),

            // Чек-листы
            addChecklistItem = AddChecklistItemUseCase(taskRepository),
            deleteChecklistItem = DeleteChecklistItemUseCase(taskRepository),
            toggleChecklistItem = ToggleChecklistItemUseCase(taskRepository),
            updateChecklistItems = UpdateChecklistItemsUseCase(taskRepository),

            // Теги
            createGroup = CreateGroupUseCase(taskRepository),
            createTagInGroup = CreateTagInGroupUseCase(taskRepository),
            moveTagToGroup = MoveTagToGroupUseCase(taskRepository),
            deleteTag = DeleteTagUseCase(taskRepository),
            updateTag = UpdateTagUseCase(taskRepository),
            updateTagsOrder = UpdateTagsOrderUseCase(taskRepository),
            insertTag = InsertTagUseCase(taskRepository),

            // Проекты
            addProject = AddProjectUseCase(taskRepository),
            updateProject = UpdateProjectUseCase(taskRepository),
            deleteProject = DeleteProjectUseCase(taskRepository),

            // Области
            addArea = AddAreaUseCase(taskRepository),
            deleteArea = DeleteAreaUseCase(taskRepository),

            // Синхронизация
            syncGoogleTasks = SyncGoogleTasksUseCase(taskRepository),
            fetchLocalCalendarEvents = FetchLocalCalendarEventsUseCase(taskRepository),

            // Запросы
            observeAllTasks = ObserveAllTasksUseCase(taskRepository),
            observeAllProjects = ObserveAllProjectsUseCase(taskRepository),
            observeAllAreas = ObserveAllAreasUseCase(taskRepository),
            observeAllTags = ObserveAllTagsUseCase(taskRepository)
        )
    }
}
