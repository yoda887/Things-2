package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.ChecklistItem
import com.example.data.model.TaskSection
import com.example.domain.usecase.TaskUseCases
import com.example.domain.usecase.TagUseCases
import com.example.domain.usecase.ProjectUseCases
import com.example.domain.usecase.AreaUseCases
import com.example.domain.usecase.SyncUseCases
import com.example.domain.usecase.ChecklistUseCases
import com.example.domain.usecase.QueryUseCases
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.components.ProjectProgress
import com.example.ui.screens.home.components.ThingsCategoryListState
import com.example.data.model.toStartVal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel для управления состоянием UI приложения Things.
 */
@HiltViewModel
class ThingsViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val tagUseCases: TagUseCases,
    private val projectUseCases: ProjectUseCases,
    private val areaUseCases: AreaUseCases,
    private val syncUseCases: SyncUseCases,
    private val checklistUseCases: ChecklistUseCases,
    private val queryUseCases: QueryUseCases
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

    val tasks: StateFlow<List<ItemWithChecklist>> = queryUseCases.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<Item>> = queryUseCases.observeAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val areas: StateFlow<List<Area>> = queryUseCases.observeAllAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)

    private val _inlineExpandedTaskId = MutableStateFlow<String?>(null)
    val inlineExpandedTaskId: StateFlow<String?> = _inlineExpandedTaskId.asStateFlow()

    private val _highlightedTaskId = MutableStateFlow<String?>(null)
    val highlightedTaskId: StateFlow<String?> = _highlightedTaskId.asStateFlow()

    fun setInlineExpandedTaskId(taskId: String?) {
        _inlineExpandedTaskId.value = taskId
    }

    fun setHighlightedTaskId(taskId: String?) {
        _highlightedTaskId.value = taskId
    }

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

    val allSavedTags: StateFlow<List<String>> = queryUseCases.observeAllTags()
        .map { tags -> tags.map { it.title } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedTagObjects: StateFlow<List<Tag>> = queryUseCases.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun computeCategoryListState(
        screen: ActiveScreen,
        project: Item?,
        area: Area?,
        expandedTaskId: String?,
        selectedTag: String?,
        taskList: List<ItemWithChecklist>,
        projectList: List<Item>,
        areaList: List<Area>,
        calEvents: List<Item>,
        savedTags: List<String>,
        savedTagObjs: List<Tag>,
        allTagsSet: Set<String>,
        highlighted: String?
    ): ThingsCategoryListState {
        // Границы дня считаем один раз на весь список, а не в геттерах каждой задачи
        val bounds = DayBounds.now()

        val listTasks = taskList.filter { wrapper ->
            val task = wrapper.item
            if (task.id == expandedTaskId) {
                true
            } else {
                when (screen) {
                    ActiveScreen.INBOX -> task.isInbox && !task.isCompleted
                    ActiveScreen.TODAY -> bounds.isToday(task)
                    ActiveScreen.UPCOMING -> bounds.isUpcoming(task)
                    ActiveScreen.ANYTIME -> bounds.isAnytime(task)
                    ActiveScreen.SOMEDAY -> bounds.isSomeday(task)
                    ActiveScreen.LOGBOOK -> task.isCompleted
                    ActiveScreen.PROJECT_DETAIL -> task.projectId == project?.id && !task.isCompleted
                    ActiveScreen.AREA_DETAIL -> task.areaId == area?.id && task.type == 0 && !task.isCompleted
                    else -> false
                }
            }
        }.sortedBy { it.item.sortOrder }

        val displayTasks = if (selectedTag == null) {
            listTasks
        } else {
            listTasks.filter { it.item.tags.contains(selectedTag) || it.item.id == expandedTaskId }
        }

        val projectProgressMap = taskList
            .filter { !it.item.projectId.isNullOrEmpty() }
            .groupBy { it.item.projectId!! }
            .mapValues { (_, tasks) ->
                ProjectProgress(
                    completed = tasks.count { it.item.isCompleted },
                    total = tasks.size
                )
            }

        return ThingsCategoryListState(
            screen = screen,
            project = project,
            area = area,
            inlineExpandedTaskId = expandedTaskId,
            selectedTagFilter = selectedTag,
            allTags = allTagsSet,
            displayTasks = displayTasks,
            calendarEvents = calEvents,
            allSavedTags = savedTags,
            allSavedTagObjects = savedTagObjs,
            areas = areaList,
            projects = projectList,
            highlightedTaskId = highlighted,
            allTasks = taskList,
            projectProgressMap = projectProgressMap
        )
    }

    /**
     * Синхронный снимок состояния экрана по текущим значениям потоков.
     * Используется как начальное значение для [getCategoryListStateFlow], чтобы первый кадр
     * отрисовался без мигания пустым списком, пока считается первая эмиссия.
     */
    fun getCategoryListStateSnapshot(
        screen: ActiveScreen,
        project: Item?,
        area: Area?
    ): ThingsCategoryListState {
        return computeCategoryListState(
            screen = screen,
            project = project,
            area = area,
            expandedTaskId = inlineExpandedTaskId.value,
            selectedTag = selectedTagFilter.value,
            taskList = tasks.value,
            projectList = projects.value,
            areaList = areas.value,
            calEvents = calendarEvents.value,
            savedTags = allSavedTags.value,
            savedTagObjs = allSavedTagObjects.value,
            allTagsSet = allTags.value,
            highlighted = highlightedTaskId.value
        )
    }

    /**
     * Холодный поток состояния для конкретного экрана.
     *
     * Намеренно не превращается в StateFlow через stateIn(viewModelScope): такой поток жил бы
     * до смерти ViewModel и накапливался бы по одному на каждую посещённую пару экран/проект.
     * Подписка живёт ровно столько, сколько экран отображается, а начальное значение
     * даёт [getCategoryListStateSnapshot].
     *
     * Пересчёт вынесен на [Dispatchers.Default] — фильтрация и группировка всего списка задач
     * не должны выполняться на главном потоке.
     */
    fun getCategoryListStateFlow(
        screen: ActiveScreen,
        project: Item?,
        area: Area?
    ): Flow<ThingsCategoryListState> {
        return combine(
            listOf(
                inlineExpandedTaskId,
                selectedTagFilter,
                tasks,
                projects,
                areas,
                calendarEvents,
                allSavedTags,
                allSavedTagObjects,
                allTags,
                highlightedTaskId
            )
        ) { array ->
            val expandedTaskId = array[0] as? String
            val selectedTag = array[1] as? String
            @Suppress("UNCHECKED_CAST")
            val taskList = array[2] as List<ItemWithChecklist>
            @Suppress("UNCHECKED_CAST")
            val projectList = array[3] as List<Item>
            @Suppress("UNCHECKED_CAST")
            val areaList = array[4] as List<Area>
            @Suppress("UNCHECKED_CAST")
            val calEvents = array[5] as List<Item>
            @Suppress("UNCHECKED_CAST")
            val savedTags = array[6] as List<String>
            @Suppress("UNCHECKED_CAST")
            val savedTagObjs = array[7] as List<Tag>
            @Suppress("UNCHECKED_CAST")
            val allTagsSet = array[8] as Set<String>
            val highlighted = array[9] as? String

            computeCategoryListState(
                screen = screen,
                project = project,
                area = area,
                expandedTaskId = expandedTaskId,
                selectedTag = selectedTag,
                taskList = taskList,
                projectList = projectList,
                areaList = areaList,
                calEvents = calEvents,
                savedTags = savedTags,
                savedTagObjs = savedTagObjs,
                allTagsSet = allTagsSet,
                highlighted = highlighted
            )
        }.flowOn(Dispatchers.Default)
    }

    fun insertTag(tagTitle: String, parentId: String? = null) {
        viewModelScope.launch {
            tagUseCases.insertTag(tagTitle, parentId)
        }
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            tagUseCases.createGroup(groupName)
        }
    }

    fun createTagInGroup(tagName: String, parentId: String?) {
        viewModelScope.launch {
            tagUseCases.createTagInGroup(tagName, parentId)
        }
    }

    fun moveTagToGroup(tagId: String, newGroupId: String?) {
        viewModelScope.launch {
            tagUseCases.moveTagToGroup(tagId, newGroupId)
        }
    }

    /**
     * Удаляет тег каскадно.
     * @param tag тег для удаления
     */
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagUseCases.deleteTag(tag)
        }
    }

    /**
     * Обновляет тег в системе.
     * @param tag тег для обновления
     */
    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            tagUseCases.updateTag(tag)
        }
    }

    /**
     * Обновляет сортировку тегов.
     * @param tags неупорядоченный список тегов
     */
    fun updateTagsOrder(tags: List<Tag>) {
        viewModelScope.launch {
            tagUseCases.updateTagsOrder(tags)
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
            taskUseCases.addTask(
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
            checklistUseCases.updateChecklistItems(itemId, list)
        }
    }

    /**
     * Обновляет выбранную задачу.
     */
    fun updateTask(item: Item) {
        viewModelScope.launch {
            taskUseCases.updateTask(item)
        }
    }

    /**
     * Обновляет выбранную задачу вместе с подпунктами чек-листа.
     */
    fun updateTask(item: Item, checklist: List<ChecklistItem>) {
        viewModelScope.launch {
            taskUseCases.updateTask(item, checklist)
        }
    }

    /**
     * Сценарий сохранения полной структуры задачи из инлайн-редактора в UI-потоке.
     * Сюда вынесен маппинг из UI во избежание нарушения чистоты слоев.
     */
    fun updateTask(
        task: Item,
        checklist: List<ChecklistItem>,
        section: TaskSection,
        title: String,
        notes: String,
        isTonight: Boolean,
        startDate: Long?,
        dueDate: Long?,
        tags: List<String>,
        projectId: String?,
        priority: Int
    ) {
        viewModelScope.launch {
            val startVal = if (projectId != null && section == TaskSection.INBOX) {
                TaskSection.ANYTIME.toStartVal()
            } else {
                section.toStartVal()
            }
            val updatedTask = task.copy(
                title = title.ifBlank { "Untitled To-Do" },
                notes = notes,
                start = startVal,
                isTonight = isTonight,
                startDate = startDate,
                dueDate = dueDate,
                cachedTags = tags.joinToString(", "),
                projectId = projectId,
                priority = priority,
                modificationDate = System.currentTimeMillis()
            )
            taskUseCases.updateTask(updatedTask, checklist)
        }
    }

    fun updateTasks(items: List<Item>) {
        viewModelScope.launch {
            taskUseCases.updateTask(items)
        }
    }

    fun toggleTaskCompletion(wrapper: ItemWithChecklist) {
        viewModelScope.launch {
            taskUseCases.toggleTaskCompletion(wrapper)
        }
    }

    fun toggleChecklistItem(wrapper: ItemWithChecklist, itemId: String) {
        viewModelScope.launch {
            checklistUseCases.toggleChecklistItem(wrapper, itemId)
        }
    }

    fun addChecklistItemToTask(wrapper: ItemWithChecklist, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            checklistUseCases.addChecklistItem(wrapper, title)
        }
    }

    fun deleteChecklistItemFromTask(wrapper: ItemWithChecklist, itemId: String) {
        viewModelScope.launch {
            checklistUseCases.deleteChecklistItem(wrapper, itemId)
        }
    }

    fun deleteTask(wrapper: ItemWithChecklist) {
        viewModelScope.launch {
            taskUseCases.deleteTask(wrapper.item)
        }
    }

    /**
     * Создает копию задачи с новым уникальным идентификатором.
     */
    fun duplicateTask(wrapper: ItemWithChecklist) {
        viewModelScope.launch {
            taskUseCases.duplicateTask(wrapper)
        }
    }

    /**
     * Пакетно удаляет список задач из базы данных.
     */
    fun deleteTasks(tasks: List<Item>) {
        viewModelScope.launch {
            taskUseCases.deleteTask(tasks)
        }
    }

    /**
     * Пакетно переключает статус выполнения для списка задач.
     */
    fun batchSetCompleted(taskWrappers: List<ItemWithChecklist>, isCompleted: Boolean) {
        viewModelScope.launch {
            val updated = taskWrappers.map { wrapper ->
                wrapper.item.copy(
                    status = if (isCompleted) 3 else 0,
                    stopDate = if (isCompleted) System.currentTimeMillis() else null
                )
            }
            taskUseCases.updateTask(updated)
        }
    }

    /**
     * Пакетно планирует дату и режим "сегодня вечером" для списка задач.
     */
    fun batchScheduleTasks(taskWrappers: List<ItemWithChecklist>, startDate: Long?, isTonight: Boolean) {
        viewModelScope.launch {
            val updated = taskWrappers.map { wrapper ->
                wrapper.item.copy(
                    startDate = startDate,
                    isTonight = isTonight
                )
            }
            taskUseCases.updateTask(updated)
        }
    }

    /**
     * Пакетно устанавливает дедлайн для списка задач.
     */
    fun batchSetDeadline(taskWrappers: List<ItemWithChecklist>, dueDate: Long?) {
        viewModelScope.launch {
            val updated = taskWrappers.map { wrapper ->
                wrapper.item.copy(dueDate = dueDate)
            }
            taskUseCases.updateTask(updated)
        }
    }

    /**
     * Пакетно перемещает список задач в проект или сферу.
     */
    fun batchMoveTasks(taskWrappers: List<ItemWithChecklist>, projectId: String?, areaId: String?, moveToInbox: Boolean) {
        viewModelScope.launch {
            val updated = taskWrappers.map { wrapper ->
                if (moveToInbox) {
                    wrapper.item.copy(
                        start = 0,
                        projectId = null,
                        areaId = null,
                        startDate = null,
                        dueDate = null,
                        modificationDate = System.currentTimeMillis()
                    )
                } else {
                    val newStart = if (wrapper.item.start == 0 && projectId != null) 2 else wrapper.item.start
                    wrapper.item.copy(
                        start = newStart,
                        projectId = projectId,
                        areaId = areaId,
                        modificationDate = System.currentTimeMillis()
                    )
                }
            }
            taskUseCases.updateTask(updated)
        }
    }

    /**
     * Пакетно объединяет новые теги с уже имеющимися у задач тегами.
     */
    fun batchAddTags(taskWrappers: List<ItemWithChecklist>, newTags: List<String>) {
        viewModelScope.launch {
            val updated = taskWrappers.map { wrapper ->
                val currentTags = wrapper.item.tags
                val mergedTags = (currentTags + newTags).distinct()
                val cachedString = mergedTags.joinToString(", ")
                wrapper.item.copy(cachedTags = cachedString)
            }
            taskUseCases.updateTask(updated)
        }
    }

    /**
     * Пакетно дублирует список задач.
     */
    fun batchDuplicateTasks(taskWrappers: List<ItemWithChecklist>) {
        viewModelScope.launch {
            taskWrappers.forEach { wrapper ->
                taskUseCases.duplicateTask(wrapper)
            }
        }
    }

    // Сценарии работы с проектами
    fun addProject(name: String, notes: String = "", areaId: String? = null) {
        viewModelScope.launch {
            projectUseCases.addProject(name, notes, areaId)
        }
    }

    fun updateProject(project: Item) {
        viewModelScope.launch {
            projectUseCases.updateProject(project)
        }
    }

    fun deleteProject(project: Item) {
        viewModelScope.launch {
            projectUseCases.deleteProject(project)
        }
    }

    // Сценарии работы со сферами
    fun addArea(title: String) {
        viewModelScope.launch {
            areaUseCases.addArea(title)
        }
    }

    fun updateArea(area: Area) {
        // [ИЗМЕНЕНИЕ]: Добавлен метод обновления области (сферы) в БД для inline-редактирования
        viewModelScope.launch {
            areaUseCases.updateArea(area)
        }
    }

    /**
     * Пакетно обновляет порядок сфер (областей) в БД.
     */
    fun updateAreas(areas: List<Area>) {
        viewModelScope.launch {
            areaUseCases.updateArea(areas)
        }
    }

    fun deleteArea(area: Area) {
        viewModelScope.launch {
            areaUseCases.deleteArea(area)
        }
    }

    // --- 3. НОВАЯ ЛОГИКА СИНХРОНИЗАЦИИ ---
    fun setAccessToken(token: String) {
        googleAccessToken.value = token
    }

    fun syncLocalCalendar() {
        viewModelScope.launch {
            val result = syncUseCases.fetchLocalCalendarEvents()
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
            val result = syncUseCases.syncGoogleTasks(googleAccessToken.value)
            
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
