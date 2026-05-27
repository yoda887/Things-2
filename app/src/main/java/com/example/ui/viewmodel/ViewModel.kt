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
import kotlinx.coroutines.launch
import java.util.UUID

class ThingsViewModel(private val repository: TaskRepository) : ViewModel() {

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
