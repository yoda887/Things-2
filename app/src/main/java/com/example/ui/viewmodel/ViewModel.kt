package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.ItemTag
import com.example.data.model.ChecklistItem
import com.example.data.model.TaskSection
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Calendar

class ThingsViewModel(private val repository: TaskRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                val currentTasks = repository.allTasks.first()
                if (currentTasks.isEmpty()) {
                    // Seed Areas
                    val workArea = Area(id = "work_area", title = "Рабочие дела", sortOrder = 1)
                    val personalArea = Area(id = "personal_area", title = "Личная жизнь", sortOrder = 2)
                    repository.insertArea(workArea)
                    repository.insertArea(personalArea)

                    // Seed Projects (Item type = 1)
                    val workProj = Item(
                        id = "work_proj",
                        type = 1,
                        title = "Onboard James",
                        notes = "Onboarding new hire",
                        areaId = "work_area",
                        creationDate = System.currentTimeMillis() - 50000
                    )
                    val partyProj = Item(
                        id = "party_proj",
                        type = 1,
                        title = "Throw Party for Eve",
                        notes = "Planning Eve's birthday party",
                        areaId = "personal_area",
                        creationDate = System.currentTimeMillis() - 40000
                    )
                    
                    repository.insertProject(workProj)
                    repository.insertProject(partyProj)
                    
                    val tom = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    val tomStart = Calendar.getInstance().apply {
                        timeInMillis = tom.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val thur = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
                    val thurStart = Calendar.getInstance().apply {
                        timeInMillis = thur.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val prepareQuestions = Item(
                        id = "seed_prep_questions",
                        type = 0,
                        title = "Prepare interview questions",
                        notes = "Review Candidate resume and portfolio",
                        start = 1, // today's smart lists
                        status = 0,
                        dueDate = tomStart,
                        projectId = "work_proj"
                    )
                    
                    val reserveDinner = Item(
                        id = "seed_reserve_dinner",
                        type = 0,
                        title = "Make reservation for dinner",
                        notes = "Italian place by the corner",
                        start = 1,
                        status = 0,
                        dueDate = tomStart,
                        projectId = "party_proj"
                    )

                    val movieTickets = Item(
                        id = "seed_movie_tickets",
                        type = 0,
                        title = "Buy movie tickets for Friday",
                        notes = "IMAX 3D preferred",
                        start = 1,
                        status = 0,
                        dueDate = thurStart,
                        priority = 1
                    )

                    val signedContract = Item(
                        id = "seed_signed_contract",
                        type = 0,
                        title = "Get copy of signed contract",
                        notes = "Check compliance system",
                        start = 1,
                        status = 0,
                        dueDate = thurStart,
                        projectId = "work_proj"
                    )

                    repository.insertTasks(listOf(prepareQuestions, reserveDinner, movieTickets, signedContract))
                    syncLocalCalendar()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val calendarEvents = MutableStateFlow<List<Item>>(emptyList())

    val tasks: StateFlow<List<ItemWithChecklist>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<Item>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val areas: StateFlow<List<Area>> = repository.allAreas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)

    val isSyncing = MutableStateFlow(false)
    val syncError = MutableStateFlow<String?>(null)
    val googleAccessToken = MutableStateFlow("")

    val filteredTasks: StateFlow<List<ItemWithChecklist>> = combine(
        tasks,
        searchQuery,
        selectedTagFilter
    ) { itemList, query, tag ->
        itemList.filter { wrapper ->
            val matchesQuery = query.isEmpty() ||
                    wrapper.item.title.contains(query, ignoreCase = true) ||
                    wrapper.item.notes.contains(query, ignoreCase = true)
            
            val matchesTag = tag == null || wrapper.item.cachedTags.contains(tag, ignoreCase = true)
            
            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<Set<String>> = tasks
        .combine(searchQuery) { taskList, _ ->
            taskList.flatMap { wrapper ->
                if (wrapper.item.cachedTags.isBlank()) emptyList() else wrapper.item.cachedTags.split(", ").map { it.trim() }
            }.filter { it.isNotBlank() }.toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allSavedTags: StateFlow<List<String>> = repository.getAllTagsFlow()
        .map { tags -> tags.map { it.title } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedTagObjects: StateFlow<List<Tag>> = repository.getAllTagsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertTag(tagTitle: String, parentId: String? = null) {
        viewModelScope.launch {
            val titleTrimmed = tagTitle.trim()
            if (titleTrimmed.isNotBlank()) {
                repository.insertTag(Tag(title = titleTrimmed, parentId = parentId))
            }
        }
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            repository.createGroup(groupName)
        }
    }

    fun createTagInGroup(tagName: String, parentId: String?) {
        viewModelScope.launch {
            repository.createTagInGroup(tagName, parentId)
        }
    }

    fun moveTagToGroup(tagId: String, newGroupId: String?) {
        viewModelScope.launch {
            repository.moveTagToGroup(tagId, newGroupId)
        }
    }

    private fun sectionToStartValue(section: TaskSection): Int {
        return when (section) {
            TaskSection.INBOX -> 0
            TaskSection.TODAY -> 1
            TaskSection.ANYTIME -> 2
            TaskSection.SOMEDAY -> 3
            TaskSection.UPCOMING -> 2 // standard fallback
        }
    }

    fun addTask(
        title: String,
        notes: String = "",
        section: TaskSection = TaskSection.INBOX,
        isTonight: Boolean = false,
        startDate: Long? = null,
        tags: List<String> = emptyList(),
        projectId: String? = null,
        checklist: List<ChecklistItem> = emptyList(),
        priority: Int = 0
    ) {
        viewModelScope.launch {
            val itemId = UUID.randomUUID().toString()
            val cleanTags = tags.map { it.trim() }.filter { it.isNotEmpty() }
            val computedStartDate = startDate ?: if (section == TaskSection.TODAY) System.currentTimeMillis() else null
            val item = Item(
                id = itemId,
                type = 0,
                title = title.ifBlank { "Untitled To-Do" },
                notes = notes,
                start = sectionToStartValue(section),
                isTonight = isTonight,
                startDate = computedStartDate,
                dueDate = null, // Newly created tasks always start with null dueDate
                projectId = projectId,
                priority = priority
            )
            repository.insertTask(item)

            if (cleanTags.isNotEmpty()) {
                val tagList = cleanTags.map { Tag(title = it) }
                for (t in tagList) {
                    repository.insertTag(t)
                }
                repository.updateItemTags(itemId, tagList)
            }
            if (checklist.isNotEmpty()) {
                val listEntities = checklist.map { it.copy(itemId = itemId) }
                repository.updateChecklistItems(itemId, listEntities)
            }
        }
    }

    fun updateChecklistItems(itemId: String, list: List<ChecklistItem>) {
        viewModelScope.launch {
            repository.updateChecklistItems(itemId, list)
        }
    }

    fun updateTask(item: Item) {
        viewModelScope.launch {
            repository.insertTask(item)
        }
    }

    fun updateTask(item: Item, checklist: List<ChecklistItem>) {
        viewModelScope.launch {
            repository.insertTask(item, checklist)
        }
    }

    fun updateTasks(items: List<Item>) {
        viewModelScope.launch {
            repository.insertTasks(items)
        }
    }

    fun toggleTaskCompletion(wrapper: ItemWithChecklist) {
        viewModelScope.launch {
            val item = wrapper.item
            val isCompleting = !item.isCompleted
            val updated = item.copy(
                status = if (isCompleting) 3 else 0,
                stopDate = if (isCompleting) System.currentTimeMillis() else null
            )
            repository.insertTask(updated, wrapper.checklist)
        }
    }

    fun toggleChecklistItem(wrapper: ItemWithChecklist, itemId: String) {
        viewModelScope.launch {
            val updatedChecklist = wrapper.checklist.map {
                if (it.id == itemId) it.copy(isCompleted = !it.isCompleted) else it
            }
            repository.updateChecklistItems(wrapper.item.id, updatedChecklist)
        }
    }

    fun addChecklistItemToTask(wrapper: ItemWithChecklist, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val updatedChecklist = wrapper.checklist + ChecklistItem(itemId = wrapper.item.id, title = title)
            repository.updateChecklistItems(wrapper.item.id, updatedChecklist)
        }
    }

    fun deleteChecklistItemFromTask(wrapper: ItemWithChecklist, itemId: String) {
        viewModelScope.launch {
            val updatedChecklist = wrapper.checklist.filter { it.id != itemId }
            repository.updateChecklistItems(wrapper.item.id, updatedChecklist)
        }
    }

    fun deleteTask(wrapper: ItemWithChecklist) {
        viewModelScope.launch {
            repository.deleteTask(wrapper.item)
        }
    }

    // Projects Actions
    fun addProject(name: String, notes: String = "", areaId: String? = null) {
        viewModelScope.launch {
            val project = Item(
                type = 1,
                title = name.ifBlank { "New Project" },
                notes = notes,
                areaId = areaId
            )
            repository.insertProject(project)
        }
    }

    fun updateProject(project: Item) {
        viewModelScope.launch {
            repository.insertProject(project)
        }
    }

    fun deleteProject(project: Item) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    // Areas Actions
    fun addArea(title: String) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                val area = Area(title = title)
                repository.insertArea(area)
            }
        }
    }

    fun deleteArea(area: Area) {
        viewModelScope.launch {
            repository.deleteArea(area)
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
