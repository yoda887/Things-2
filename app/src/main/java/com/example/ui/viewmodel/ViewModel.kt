package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.ChecklistItem
import com.example.data.model.TaskSection
import com.example.domain.usecase.ThingsUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel для управления состоянием UI приложения Things.
 * Основан на принципах Clean Architecture и делегирует все бизнес-сценарии в [ThingsUseCases].
 */
@HiltViewModel
class ThingsViewModel @Inject constructor(
    private val useCases: ThingsUseCases
) : ViewModel() {

    // --- 1. ПЕРЕНОСИМ СОСТОЯНИЯ СИНХРОНИЗАЦИИ ИЗ SYNCHELPER СЮДА ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    val googleAccessToken = MutableStateFlow("")

    private val _calendarEvents = MutableStateFlow<List<Item>>(emptyList())
    val calendarEvents: StateFlow<List<Item>> = _calendarEvents.asStateFlow()

    // --- 2. ИНИЦИАЛИЗАЦИЯ (Без DatabaseSeeder) ---
    init {
        viewModelScope.launch {
            // Просто загружаем календарь при старте
            syncLocalCalendar()
        }
    }

    val tasks: StateFlow<List<ItemWithChecklist>> = useCases.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<Item>> = useCases.observeAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val areas: StateFlow<List<Area>> = useCases.observeAllAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)

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

    val allSavedTags: StateFlow<List<String>> = useCases.observeAllTags()
        .map { tags -> tags.map { it.title } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedTagObjects: StateFlow<List<Tag>> = useCases.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertTag(tagTitle: String, parentId: String? = null) {
        viewModelScope.launch {
            useCases.insertTag(tagTitle, parentId)
        }
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            useCases.createGroup(groupName)
        }
    }

    fun createTagInGroup(tagName: String, parentId: String?) {
        viewModelScope.launch {
            useCases.createTagInGroup(tagName, parentId)
        }
    }

    fun moveTagToGroup(tagId: String, newGroupId: String?) {
        viewModelScope.launch {
            useCases.moveTagToGroup(tagId, newGroupId)
        }
    }

    /**
     * Удаляет тег каскадно.
     * @param tag тег для удаления
     */
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            useCases.deleteTag(tag)
        }
    }

    /**
     * Обновляет тег в системе.
     * @param tag тег для обновления
     */
    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            useCases.updateTag(tag)
        }
    }

    /**
     * Обновляет сортировку тегов.
     * @param tags неупорядоченный список тегов
     */
    fun updateTagsOrder(tags: List<Tag>) {
        viewModelScope.launch {
            useCases.updateTagsOrder(tags)
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
            useCases.addTask(
                title = title,
                notes = notes,
                section = section,
                isTonight = isTonight,
                startDate = startDate,
                tags = tags,
                projectId = projectId,
                checklist = checklist,
                priority = priority
            )
        }
    }

    fun updateChecklistItems(itemId: String, list: List<ChecklistItem>) {
        viewModelScope.launch {
            useCases.updateChecklistItems(itemId, list)
        }
    }

    /**
     * Обновляет выбранную задачу.
     */
    fun updateTask(item: Item) {
        viewModelScope.launch {
            useCases.updateTask(item)
        }
    }

    /**
     * Обновляет выбранную задачу вместе с подпунктами чек-листа.
     */
    fun updateTask(item: Item, checklist: List<ChecklistItem>) {
        viewModelScope.launch {
            useCases.updateTask(item, checklist)
        }
    }

    fun updateTasks(items: List<Item>) {
        viewModelScope.launch {
            useCases.updateTask(items)
        }
    }

    fun toggleTaskCompletion(wrapper: ItemWithChecklist) {
        viewModelScope.launch {
            useCases.toggleTaskCompletion(wrapper)
        }
    }

    fun toggleChecklistItem(wrapper: ItemWithChecklist, itemId: String) {
        viewModelScope.launch {
            useCases.toggleChecklistItem(wrapper, itemId)
        }
    }

    fun addChecklistItemToTask(wrapper: ItemWithChecklist, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            useCases.addChecklistItem(wrapper, title)
        }
    }

    fun deleteChecklistItemFromTask(wrapper: ItemWithChecklist, itemId: String) {
        viewModelScope.launch {
            useCases.deleteChecklistItem(wrapper, itemId)
        }
    }

    fun deleteTask(wrapper: ItemWithChecklist) {
        viewModelScope.launch {
            useCases.deleteTask(wrapper.item)
        }
    }

    /**
     * Создает копию задачи с новым уникальным идентификатором.
     */
    fun duplicateTask(wrapper: ItemWithChecklist) {
        viewModelScope.launch {
            useCases.duplicateTask(wrapper)
        }
    }

    // Сценарии работы с проектами
    fun addProject(name: String, notes: String = "", areaId: String? = null) {
        viewModelScope.launch {
            useCases.addProject(name, notes, areaId)
        }
    }

    fun updateProject(project: Item) {
        viewModelScope.launch {
            useCases.updateProject(project)
        }
    }

    fun deleteProject(project: Item) {
        viewModelScope.launch {
            useCases.deleteProject(project)
        }
    }

    // Сценарии работы со сферами
    fun addArea(title: String) {
        viewModelScope.launch {
            useCases.addArea(title)
        }
    }

    fun deleteArea(area: Area) {
        viewModelScope.launch {
            useCases.deleteArea(area)
        }
    }

    // --- 3. НОВАЯ ЛОГИКА СИНХРОНИЗАЦИИ ---
    fun setAccessToken(token: String) {
        googleAccessToken.value = token
    }

    fun syncLocalCalendar() {
        viewModelScope.launch {
            val result = useCases.fetchLocalCalendarEvents()
            result.onSuccess { events ->
                _calendarEvents.value = events
            }.onFailure {
                // Возможная обработка ошибки доступа к календарю
            }
        }
    }

    fun syncWithGoogle() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            
            // Вызываем юзкейс, передавая токен напрямую
            val result = useCases.syncGoogleTasks(googleAccessToken.value)
            
            result.onFailure { error ->
                _syncError.value = error.localizedMessage ?: "Unknown sync error"
            }
            
            _isSyncing.value = false
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectTag(tag: String?) {
        selectedTagFilter.value = tag
    }
}
