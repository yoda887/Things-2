package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.tag.TagTitles
import com.example.domain.usecase.heading.HeadingUseCases
import com.example.ui.screens.home.components.ProjectHeadings
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
    private val queryUseCases: QueryUseCases,
    private val headingUseCases: HeadingUseCases
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
        viewModelScope.launch {
            // Дубли тегов и потерянные связи задач с тегами, оставшиеся от прежних версий
            tagUseCases.repairTags()
        }
    }

    val tasks: StateFlow<List<ItemWithChecklist>> = queryUseCases.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Заголовки всех проектов, включая архивные; экран проекта берёт свои активные */
    val headings: StateFlow<List<Item>> = headingUseCases.observe()
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
            
            val matchesTag = tag == null || wrapper.item.tags.any { TagTitles.key(it) == TagTitles.key(tag) }
            
            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<Set<String>> = tasks
        .combine(searchQuery) { taskList, _ ->
            taskList.flatMap { wrapper ->
                wrapper.item.tags
            }.toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allSavedTags: StateFlow<List<String>> = queryUseCases.observeAllTags()
        .map { tags -> tags.map { it.title } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedTagObjects: StateFlow<List<Tag>> = queryUseCases.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        /**
         * Чистое вычисление состояния экрана категории по снимку входных данных.
         *
         * Вынесено в companion и открыто как `internal`, чтобы его можно было покрыть
         * модульными тестами без создания ViewModel, Hilt-зависимостей и корутин.
         */
        internal fun computeCategoryListState(
            screen: ActiveScreen,
            project: Item?,
            area: Area?,
            tag: Tag? = null,
            expandedTaskId: String?,
            selectedTag: String?,
            taskList: List<ItemWithChecklist>,
            projectList: List<Item>,
            areaList: List<Area>,
            calEvents: List<Item>,
            savedTags: List<String>,
            savedTagObjs: List<Tag>,
            allTagsSet: Set<String>,
            highlighted: String?,
            query: String = "",
            headingList: List<Item> = emptyList()
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
                        ActiveScreen.TAG_DETAIL -> task.type == 0 && tag != null && task.tags.contains(tag.title) && !task.isCompleted
                        // Поиск: задачи (открытые, выполненные и отменённые) по названию, заметкам и чек-листу
                        ActiveScreen.SEARCH -> {
                            val q = query.trim()
                            q.isNotEmpty() && task.type == 0 && !task.trashed && (
                                task.title.contains(q, ignoreCase = true) ||
                                    task.notes.contains(q, ignoreCase = true) ||
                                    wrapper.checklist.any { it.title.contains(q, ignoreCase = true) }
                                )
                        }
                        else -> false
                    }
                }
            }.sortedBy { it.item.sortOrder }

            // Экран проекта: активные заголовки проекта, задачи — в порядке экрана, по заголовкам
            val projectHeadings = if (screen == ActiveScreen.PROJECT_DETAIL && project != null) {
                headingList.filter { it.projectId == project.id && it.status == 0 && !it.trashed }
                    .sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
            val orderedTasks = if (projectHeadings.isEmpty()) listTasks else {
                ProjectHeadings.orderByHeading(listTasks, projectHeadings.map { it.id }) { it.item }
            }

            val displayTasks = if (selectedTag == null) {
                orderedTasks
            } else {
                orderedTasks.filter { it.item.tags.contains(selectedTag) || it.item.id == expandedTaskId }
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
                tag = tag,
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
                projectProgressMap = projectProgressMap,
                searchQuery = query,
                headings = projectHeadings
            )
        }
    }

    /**
     * Синхронный снимок состояния экрана по текущим значениям потоков.
     * Используется как начальное значение для [getCategoryListStateFlow], чтобы первый кадр
     * отрисовался без мигания пустым списком, пока считается первая эмиссия.
     */
    fun getCategoryListStateSnapshot(
        screen: ActiveScreen,
        project: Item?,
        area: Area?,
        tag: Tag? = null
    ): ThingsCategoryListState {
        return computeCategoryListState(
            screen = screen,
            project = project,
            area = area,
            tag = tag,
            expandedTaskId = inlineExpandedTaskId.value,
            selectedTag = selectedTagFilter.value,
            taskList = tasks.value,
            projectList = projects.value,
            areaList = areas.value,
            calEvents = calendarEvents.value,
            savedTags = allSavedTags.value,
            savedTagObjs = allSavedTagObjects.value,
            allTagsSet = allTags.value,
            highlighted = highlightedTaskId.value,
            query = searchQuery.value,
            headingList = headings.value
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
        area: Area?,
        tag: Tag? = null
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
                highlightedTaskId,
                searchQuery,
                headings
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
            val query = array[10] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val headingList = array[11] as List<Item>

            computeCategoryListState(
                screen = screen,
                project = project,
                area = area,
                tag = tag,
                expandedTaskId = expandedTaskId,
                selectedTag = selectedTag,
                taskList = taskList,
                projectList = projectList,
                areaList = areaList,
                calEvents = calEvents,
                savedTags = savedTags,
                savedTagObjs = savedTagObjs,
                allTagsSet = allTagsSet,
                highlighted = highlighted,
                query = query,
                headingList = headingList
            )
        }.flowOn(Dispatchers.Default)
    }

    // --- Заголовки проектов ---

    /** Сохраняет заголовок: новый (пустой — его название вводят сразу) или переименованный */
    fun saveHeading(heading: Item) {
        viewModelScope.launch { headingUseCases.save(heading) }
    }

    /** Удаляет заголовок вместе с его задачами */
    fun deleteHeading(heading: Item) {
        viewModelScope.launch { headingUseCases.delete(heading) }
    }

    /** Пустой новый заголовок: его задачи возвращаются в группу выше, сам заголовок удаляется */
    fun discardHeading(heading: Item, tasksBack: List<Item>) {
        viewModelScope.launch { headingUseCases.discard(heading, tasksBack) }
    }

    /** Архивирует заголовок: он и его задачи уходят в Logbook, невыполненные отмечаются выполненными */
    fun archiveHeading(heading: Item) {
        viewModelScope.launch { headingUseCases.archive(heading) }
    }

    fun reorderHeadings(headings: List<Item>) {
        viewModelScope.launch { headingUseCases.reorder(headings) }
    }

    /** Новый заголовок посреди проекта (сброс кнопки «+»): заголовки по порядку и задачи, ушедшие под него */
    fun insertHeading(headings: List<Item>, movedTasks: List<Item>) {
        viewModelScope.launch { headingUseCases.insert(headings, movedTasks) }
    }

    /**
     * Новая задача на месте сброса кнопки «+»: сама задача (со сверкой тегов) и соседи с новыми sortOrder.
     */
    fun createTaskAt(task: Item, reorderedOthers: List<Item>) {
        viewModelScope.launch {
            taskUseCases.updateTask(task)
            if (reorderedOthers.isNotEmpty()) taskUseCases.updateTask(reorderedOthers)
        }
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
                cachedTags = TagTitles.join(tags),
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
     * Пакетно назначает дату старта выбранным задачам.
     *
     * Меняется не только дата, но и сама секция: списки отбирают задачи по [Item.start]
     * ([DayBounds.isSomeday] — по `start == 3` и т. д.), поэтому без него задача с новой датой
     * осталась бы в своём прежнем списке.
     */
    fun batchScheduleTasks(
        taskWrappers: List<ItemWithChecklist>,
        startDate: Long?,
        isTonight: Boolean,
        section: TaskSection
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = taskWrappers.map { wrapper ->
                // Задача в проекте не может оказаться во «Входящих» — как и в одиночном редакторе
                val startVal = if (wrapper.item.projectId != null && section == TaskSection.INBOX) {
                    TaskSection.ANYTIME.toStartVal()
                } else {
                    section.toStartVal()
                }
                wrapper.item.copy(
                    start = startVal,
                    startDate = startDate,
                    isTonight = isTonight,
                    modificationDate = now
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
                val mergedTags = TagTitles.clean(wrapper.item.tags + newTags)
                wrapper.item.copy(cachedTags = TagTitles.join(mergedTags), modificationDate = System.currentTimeMillis())
            }
            taskUseCases.updateTask.withTags(updated)
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
