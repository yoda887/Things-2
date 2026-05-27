package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChecklistItem
import com.example.data.model.Project
import com.example.data.model.Task
import com.example.data.model.TaskSection
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Calendar

class ThingsViewModel(private val repository: TaskRepository) : ViewModel() {

    // [СЕМЕНА]: Автоматическое заполнение демонстрационных задач и проектов при первом запуске, если база данных пуста.
    init {
        viewModelScope.launch {
            try {
                val currentTasks = repository.allTasks.first()
                if (currentTasks.isEmpty()) {
                    val workProj = Project(id = "work_proj", name = "Work", notes = "Work related tasks")
                    val partyProj = Project(id = "party_proj", name = "Throw Party for Eve", notes = "Planning Eve's birthday party")
                    val onboardProj = Project(id = "onboard_proj", name = "Onboard James", notes = "Onboarding new hire")
                    
                    repository.insertProject(workProj)
                    repository.insertProject(partyProj)
                    repository.insertProject(onboardProj)
                    
                    val tom = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    val tomStart = Calendar.getInstance().apply {
                        timeInMillis = tom.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                    }.timeInMillis

                    val thur = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
                    val thurStart = Calendar.getInstance().apply {
                        timeInMillis = thur.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                    }.timeInMillis

                    val prepareQuestions = Task(
                        id = "seed_prep_questions",
                        title = "Prepare interview questions",
                        notes = "Review Candidate resume and portfolio",
                        section = TaskSection.UPCOMING,
                        isTonight = false,
                        dueDate = tomStart,
                        tags = listOf("Work"),
                        projectId = "work_proj"
                    )
                    
                    val reserveDinner = Task(
                        id = "seed_reserve_dinner",
                        title = "Make reservation for dinner",
                        notes = "Italian place by the corner",
                        section = TaskSection.UPCOMING,
                        isTonight = false,
                        dueDate = tomStart,
                        tags = listOf("Throw Party for Eve"),
                        projectId = "party_proj"
                    )

                    val movieTickets = Task(
                        id = "seed_movie_tickets",
                        title = "Buy movie tickets for Friday",
                        notes = "IMAX 3D preferred",
                        section = TaskSection.UPCOMING,
                        isTonight = false,
                        dueDate = thurStart,
                        tags = listOf("Personal"),
                        priority = 0
                    )

                    val signedContract = Task(
                        id = "seed_signed_contract",
                        title = "Get copy of signed contract",
                        notes = "Check compliance system",
                        section = TaskSection.UPCOMING,
                        isTonight = false,
                        dueDate = thurStart,
                        tags = listOf("Onboard James"),
                        projectId = "onboard_proj"
                    )

                    repository.insertTasks(listOf(prepareQuestions, reserveDinner, movieTickets, signedContract))
                    
                    // Также синхронизируем демонстрационный календарь
                    syncLocalCalendar()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val calendarEvents = MutableStateFlow<List<Task>>(emptyList())

    // Main streams from database
    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI filtering states
    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)

    // Syncing states
    val isSyncing = MutableStateFlow(false)
    val syncError = MutableStateFlow<String?>(null)
    val googleAccessToken = MutableStateFlow("")

    // Filtered tasks mapping
    val filteredTasks: StateFlow<List<Task>> = combine(
        tasks,
        searchQuery,
        selectedTagFilter
    ) { taskList, query, tag ->
        taskList.filter { task ->
            val matchesQuery = query.isEmpty() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.notes.contains(query, ignoreCase = true)
            
            val matchesTag = tag == null || task.tags.contains(tag)
            
            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated tags across all active tasks for filtering
    val allTags: StateFlow<Set<String>> = tasks
        .combine(searchQuery) { taskList, _ ->
            taskList.flatMap { it.tags }.filter { it.isNotBlank() }.toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // UI actions
    fun addTask(
        title: String,
        notes: String = "",
        section: TaskSection = TaskSection.INBOX,
        isTonight: Boolean = false,
        dueDate: Long? = null,
        tags: List<String> = emptyList(),
        projectId: String? = null,
        checklist: List<ChecklistItem> = emptyList()
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title.ifBlank { "Untitled To-Do" },
                notes = notes,
                section = section,
                isTonight = isTonight,
                dueDate = dueDate,
                tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
                projectId = projectId,
                checklist = checklist
            )
            repository.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun updateTasks(tasks: List<Task>) {
        viewModelScope.launch {
            repository.insertTasks(tasks)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val isCompleting = !task.isCompleted
            val updated = task.copy(
                isCompleted = isCompleting,
                completedDate = if (isCompleting) System.currentTimeMillis() else null
            )
            repository.insertTask(updated)
        }
    }

    fun toggleChecklistItem(task: Task, itemId: String) {
        viewModelScope.launch {
            val updatedChecklist = task.checklist.map { item ->
                if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
            }
            repository.insertTask(task.copy(checklist = updatedChecklist))
        }
    }

    fun addChecklistItemToTask(task: Task, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val updatedChecklist = task.checklist + ChecklistItem(title = title)
            repository.insertTask(task.copy(checklist = updatedChecklist))
        }
    }

    fun deleteChecklistItemFromTask(task: Task, itemId: String) {
        viewModelScope.launch {
            val updatedChecklist = task.checklist.filter { it.id != itemId }
            repository.insertTask(task.copy(checklist = updatedChecklist))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Project Actions
    fun addProject(name: String, notes: String = "") {
        viewModelScope.launch {
            val project = Project(
                name = name.ifBlank { "New Project" },
                notes = notes
            )
            repository.insertProject(project)
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            repository.insertProject(project)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            // Unlink all tasks under this project
            val relatedTasks = tasks.value.filter { it.projectId == project.id }
            relatedTasks.forEach { task ->
                repository.insertTask(task.copy(projectId = null))
            }
            repository.deleteProject(project)
        }
    }

    // Google Tasks Sync Interface Implementation
    fun setAccessToken(token: String) {
        googleAccessToken.value = token
    }

    fun syncLocalCalendar() {
        viewModelScope.launch {
            val calendarResult = repository.fetchLocalCalendarEvents()
            if (calendarResult.isSuccess) {
                calendarEvents.value = calendarResult.getOrNull() ?: emptyList()
            }
        }
    }

    fun syncWithGoogle() {
        val token = googleAccessToken.value
        if (token.isBlank()) {
            syncError.value = "Access token is empty. Please enter a Google Tasks OAuth access token."
            return
        }

        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            
            // Sync tasks
            val result = repository.syncWithGoogleTasks(token)
            
            isSyncing.value = false
            if (result.isSuccess) {
                syncError.value = null
            } else {
                syncError.value = result.exceptionOrNull()?.message ?: "Sync failed"
            }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectTag(tag: String?) {
        selectedTagFilter.value = tag
    }

    // Factory Class for construction
    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ThingsViewModel::class.java)) {
                return ThingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
