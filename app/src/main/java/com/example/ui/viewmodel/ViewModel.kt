package com.example.ui.viewmodel

import com.example.data.local.HomeLayoutPrefs
import com.example.domain.edits.TaskEditorFields
import com.example.domain.edits.TaskEdits
import java.util.UUID
import com.example.domain.edits.TaskMoves
import com.example.domain.edits.SortOrders
import com.example.domain.edits.ProjectEdits
import com.example.domain.edits.ProjectEdit
import com.example.domain.edits.NewTasks
import com.example.domain.edits.NewTaskPlace
import com.example.domain.edits.NewProjects
import com.example.domain.edits.MoveTarget
import com.example.domain.edits.Headings
import com.example.domain.edits.DropSpot
import com.example.domain.lists.SearchList
import com.example.domain.lists.ProjectList
import com.example.domain.lists.TagList
import com.example.domain.lists.AreaList
import com.example.domain.lists.LogbookList
import com.example.domain.lists.SomedayList
import com.example.domain.lists.AnytimeList
import com.example.domain.lists.UpcomingList
import com.example.domain.lists.TodayList
import com.example.domain.lists.InboxList
import androidx.lifecycle.ViewModel
import com.example.domain.tag.TagTitles
import com.example.domain.usecase.heading.HeadingUseCases
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
import com.example.data.model.toStartVal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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
    private val headingUseCases: HeadingUseCases,
    private val homeLayoutPrefs: HomeLayoutPrefs
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

    /**
     * Задачи, созданные на месте промежутка «+» (createTaskAt), — в списке сразу, не дожидаясь базы: новая строка
     * раскрывается, как только кнопка приземлилась, без паузы на запись. Уходят, когда база вернула задачу.
     */
    private val optimisticTasks = MutableStateFlow<Map<String, ItemWithChecklist>>(emptyMap())
    /** Какая новая задача держит временные копии (сама она и переставленные соседи) — по её id */
    private val optimisticGroups = HashMap<String, Set<String>>()

    val tasks: StateFlow<List<ItemWithChecklist>> = combine(queryUseCases.observeAllTasks(), optimisticTasks) { db, optimistic ->
        if (optimistic.isEmpty()) return@combine db
        val dbIds = db.mapTo(HashSet()) { it.item.id }
        // База вернула новую задачу (вместе с соседями — они пишутся одним вызовом) — временные копии не нужны
        var live = optimistic
        val landed = optimisticGroups.keys.filter { it in dbIds }
        if (landed.isNotEmpty()) {
            val drop = landed.flatMapTo(HashSet()) { optimisticGroups.remove(it).orEmpty() }
            live = optimistic - drop
            optimisticTasks.update { it - drop }
        }
        if (live.isEmpty()) db else db.map { live[it.item.id] ?: it } + live.values.filter { it.item.id !in dbIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Шаблоны повторяющихся проектов: в [projects] их нет, а экран шаблона открывают из «Повторяющихся» */
    val projectTemplates: StateFlow<List<Item>> = tasks
        .map { list -> list.map { it.item }.filter { com.example.domain.lists.RepeatingList.includesProject(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Правила повтора шаблонов повторяющихся задач — по id шаблона */
    val recurrenceRules: StateFlow<Map<String, com.example.data.model.RecurrenceRule>> = queryUseCases.observeRecurrenceRules()
        .map { rules -> rules.associateBy { it.templateId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    /** Задачи, ушедшие из «Входящих», — видны там под плашкой, пока не нажмут «OK» ([com.example.domain.lists.InboxMoves]) */
    private val _inboxMovedOut = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            var previousInbox: Set<String>? = null
            tasks.collect { list ->
                // Первый список — отсчёт: до него ничего не переезжало
                previousInbox?.let { previous ->
                    val moved = com.example.domain.lists.InboxMoves.update(_inboxMovedOut.value, previous, list)
                    if (moved != _inboxMovedOut.value) _inboxMovedOut.value = moved
                }
                if (list.isNotEmpty() || previousInbox != null) previousInbox = com.example.domain.lists.InboxMoves.inboxIds(list)
            }
        }
    }

    /** «OK» на плашке «Входящих»: переехавшие задачи больше там не показываются */
    fun dismissInboxMoved() {
        _inboxMovedOut.value = emptySet()
    }

    /** Что уже видели в «Сегодня» — помнится между запусками ([com.example.domain.lists.TodayNews]) */
    private var todayKnown: Set<String>? = homeLayoutPrefs.todaySeenIds()
    private val _todayNewIds = MutableStateFlow<Set<String>>(emptySet())
    /** Новое в «Сегодня»: жёлтая точка у задачи или проекта и плашка с «OK» */
    val todayNewIds: StateFlow<Set<String>> = _todayNewIds.asStateFlow()

    init {
        viewModelScope.launch {
            var previousToday: Set<String>? = null
            tasks.collect { list ->
                // Первый пустой список — база ещё не прочитана
                if (list.isEmpty() && previousToday == null) return@collect
                val bounds = com.example.data.model.DayBounds.now()
                val today = com.example.domain.lists.TodayNews.todayIds(list, bounds)
                // Самый первый раз всё, что уже в «Сегодня», — виденное
                val known = com.example.domain.lists.TodayNews.update(
                    todayKnown ?: today, previousToday, list, bounds, System.currentTimeMillis()
                )
                previousToday = today
                if (known != todayKnown) {
                    todayKnown = known
                    homeLayoutPrefs.setTodaySeenIds(known)
                }
                _todayNewIds.value = com.example.domain.lists.TodayNews.newIds(known, today)
            }
        }
    }

    private val _todayGroupedByPlace = MutableStateFlow(homeLayoutPrefs.todayGroupedByPlace())
    /** Дневная часть «Сегодня» — по проектам и областям (настройка) */
    val todayGroupedByPlace: StateFlow<Boolean> = _todayGroupedByPlace.asStateFlow()

    fun setTodayGroupedByPlace(grouped: Boolean) {
        homeLayoutPrefs.setTodayGroupedByPlace(grouped)
        _todayGroupedByPlace.value = grouped
    }

    private val _datesWithMonthName = MutableStateFlow(homeLayoutPrefs.datesWithMonthName())
    /** Даты в карточках — месяц словом или цифрами (настройка) */
    val datesWithMonthName: StateFlow<Boolean> = _datesWithMonthName.asStateFlow()

    fun setDatesWithMonthName(monthName: Boolean) {
        homeLayoutPrefs.setDatesWithMonthName(monthName)
        _datesWithMonthName.value = monthName
    }

    /** «OK» на плашке «Сегодня»: всё, что там сейчас, — виденное, точки и плашка пропадают */
    fun dismissTodayNew() {
        val known = (todayKnown ?: emptySet()) + _todayNewIds.value
        todayKnown = known
        homeLayoutPrefs.setTodaySeenIds(known)
        _todayNewIds.value = emptySet()
    }

    private val _highlightedTaskId = MutableStateFlow<String?>(null)
    val highlightedTaskId: StateFlow<String?> = _highlightedTaskId.asStateFlow()

    /** Свёрнутые области главного экрана — запоминаются между запусками. */
    private val _collapsedAreaIds = MutableStateFlow(homeLayoutPrefs.collapsedAreaIds())
    val collapsedAreaIds: StateFlow<Set<String>> = _collapsedAreaIds.asStateFlow()

    fun setAreaExpanded(areaId: String, expanded: Boolean) {
        homeLayoutPrefs.setAreaCollapsed(areaId, collapsed = !expanded)
        _collapsedAreaIds.value = homeLayoutPrefs.collapsedAreaIds()
    }

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
            headingList: List<Item> = emptyList(),
            repeatRules: Map<String, com.example.data.model.RecurrenceRule> = emptyMap(),
            inboxMovedOut: Set<String> = emptySet()
        ): ThingsCategoryListState {
            // Границы дня считаем один раз на весь список, а не в геттерах каждой задачи
            val bounds = DayBounds.now()
            val somedayProjectIds = AnytimeList.somedayProjectIds(projectList)

            // Все экраны перечислены явно (без else): новый экран компилятор заставит указать и здесь
            val screenTasks = taskList.filter { wrapper ->
                val task = wrapper.item
                if (task.id == expandedTaskId) {
                    true
                } else {
                    when (screen) {
                        ActiveScreen.INBOX -> InboxList.includes(task)
                        ActiveScreen.TODAY -> TodayList.includes(task, bounds)
                        ActiveScreen.UPCOMING -> UpcomingList.includes(task, bounds)
                        ActiveScreen.ANYTIME -> AnytimeList.includes(task, bounds, somedayProjectIds)
                        ActiveScreen.SOMEDAY -> SomedayList.includes(task, bounds, somedayProjectIds)
                        ActiveScreen.LOGBOOK -> LogbookList.includes(task)
                        ActiveScreen.PROJECT_DETAIL -> ProjectList.includes(task, project?.id)
                        ActiveScreen.AREA_DETAIL -> AreaList.includes(task, area?.id)
                        ActiveScreen.TAG_DETAIL -> tag != null && TagList.isActiveTask(task, tag.title)
                        ActiveScreen.SEARCH -> SearchList.includes(wrapper, query)
                        // У главного экрана и особых списков проектов нет списка задач
                        ActiveScreen.HOME, ActiveScreen.ALL_PROJECTS, ActiveScreen.LOGGED_PROJECTS, ActiveScreen.LATER_PROJECTS -> false
                        ActiveScreen.REPEATING -> com.example.domain.lists.RepeatingList.includes(task) ||
                            com.example.domain.lists.RepeatingList.includesCopy(task)
                    }
                }
            }.sortedBy { it.item.sortOrder }

            // «Предстоящие» показывают и шаблоны повтора — на день их следующей копии; шаблоны проектов —
            // строками проектов
            val previews = if (screen == ActiveScreen.UPCOMING) {
                com.example.domain.lists.RepeatingList.upcomingPreviews(taskList, repeatRules, bounds.endOfToday)
            } else emptyList()
            val (projectPreviews, taskPreviews) = previews.partition { it.item.type == Item.TYPE_PROJECT_TEMPLATE }
            val listTasks = if (screen == ActiveScreen.UPCOMING) {
                // Раскрытый шаблон тоже остаётся на дне копии: его подставленная дата заменяет «без даты»
                val previewIds = taskPreviews.mapTo(HashSet()) { it.item.id }
                screenTasks.filter { it.item.id !in previewIds } + taskPreviews
            } else screenTasks

            // Заголовки экрана проекта
            val projectHeadings = if (screen == ActiveScreen.PROJECT_DETAIL) ProjectList.headings(headingList, project?.id) else emptyList()
            // Задачи — в порядке экрана: перетаскивание работает по тому же списку, что виден.
            // Все экраны перечислены явно (без else): новый экран компилятор заставит указать и здесь
            val orderedTasks = when (screen) {
                ActiveScreen.PROJECT_DETAIL -> ProjectList.order(listTasks, projectHeadings)
                ActiveScreen.ANYTIME -> AnytimeList.order(listTasks, projectList, areaList)
                ActiveScreen.SOMEDAY -> SomedayList.order(listTasks, projectList, areaList)
                ActiveScreen.AREA_DETAIL -> AreaList.order(listTasks, area?.id, bounds)
                ActiveScreen.LOGBOOK -> LogbookList.order(listTasks)
                // Порядок экрана — общий порядок задач (sortOrder)
                // Тег — в порядке групп по месту, как видно на экране
                ActiveScreen.TAG_DETAIL -> com.example.domain.lists.PlaceGroups.order(listTasks, projectList, areaList)
                // «Планы» — по порядку внутри дня (UpcomingOrder): перенесённое на день — первым
                ActiveScreen.UPCOMING -> com.example.domain.lists.UpcomingOrder.orderTasks(listTasks)
                ActiveScreen.INBOX, ActiveScreen.TODAY,
                ActiveScreen.SEARCH, ActiveScreen.HOME, ActiveScreen.ALL_PROJECTS, ActiveScreen.LOGGED_PROJECTS, ActiveScreen.LATER_PROJECTS -> listTasks
                ActiveScreen.REPEATING -> com.example.domain.lists.RepeatingList.order(listTasks, repeatRules)
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
                        completed = tasks.count { it.item.isClosed },
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
                projects = if (projectPreviews.isEmpty()) projectList else projectList + projectPreviews.map { it.item },
                highlightedTaskId = highlighted,
                allTasks = taskList,
                projectProgressMap = projectProgressMap,
                searchQuery = query,
                headings = projectHeadings,
                allHeadings = headingList.filter { !it.trashed },
                repeatRules = repeatRules,
                inboxGhosts = if (screen == ActiveScreen.INBOX) {
                    com.example.domain.lists.InboxMoves.ghosts(inboxMovedOut, taskList, displayTasks)
                } else emptyList()
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
            headingList = headings.value,
            repeatRules = recurrenceRules.value,
            inboxMovedOut = _inboxMovedOut.value
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
                headings,
                recurrenceRules,
                _inboxMovedOut
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
                headingList = headingList,
                repeatRules = @Suppress("UNCHECKED_CAST") (array[12] as Map<String, com.example.data.model.RecurrenceRule>),
                inboxMovedOut = @Suppress("UNCHECKED_CAST") (array[13] as Set<String>)
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
    fun discardHeading(heading: Item, tasks: List<Item>, groupAboveId: String?) =
        discardHeading(heading, Headings.assign(tasks, groupAboveId))

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
    /** Новый пустой подзаголовок в конце проекта. */
    fun addHeading(id: String, projectId: String, headings: List<Item>) = saveHeading(Headings.newAtEnd(projectId, headings, id))

    fun renameHeading(heading: Item, title: String) = saveHeading(Headings.rename(heading, title))

    /** Новый подзаголовок на месте [index] среди [headings], под ним — [movedTasks]. */
    fun insertHeadingAt(id: String, projectId: String, headings: List<Item>, index: Int, movedTasks: List<Item>) =
        insertHeading(Headings.insertAt(headings, Headings.newHeading(projectId, id), index), Headings.assign(movedTasks, id))

    fun insertHeading(headings: List<Item>, movedTasks: List<Item>) {
        viewModelScope.launch { headingUseCases.insert(headings, movedTasks) }
    }

    /** Задача, созданная в шаблоне повторяющегося проекта, сразу становится его частью ([RepeatProjects.normalize]) */
    private fun Item.inRepeatingProject(): Item {
        val template = projectTemplates.value.firstOrNull { it.id == projectId } ?: return this
        return com.example.domain.repeat.RepeatProjects.normalize(listOf(template, this)).firstOrNull { it.id == id } ?: this
    }

    /** Новая задача по «+» на экране: поля по месту ([NewTasks]); задача сразу раскрывается для ввода. */
    fun createTask(place: NewTaskPlace, id: String = UUID.randomUUID().toString()) {
        val resolved = if (place is NewTaskPlace.Upcoming && place.startDate == null) {
            NewTaskPlace.Upcoming(NewTasks.defaultUpcomingDate(tasks.value.filter { it.item.isUpcoming }.mapNotNull { it.item.startDate }))
        } else {
            place
        }
        // Первой в своём списке (NewTasks.topSortOrder)
        val top = NewTasks.topSortOrder(tasks.value.map { it.item })
        updateTask(NewTasks.create(resolved, id).copy(sortOrder = top).inRepeatingProject())
        setInlineExpandedTaskId(id)
    }

    /**
     * Новая задача на месте сброса кнопки «+»: поля по месту и точке сброса, место в списке —
     * перед задачей [insertIndex] списка экрана [listTasks]; соседи при нужде перенумеровываются.
     */
    fun createTaskAt(id: String, place: NewTaskPlace, spot: DropSpot, insertIndex: Int, listTasks: List<Item>) {
        val insertion = SortOrders.insert(listTasks, insertIndex, NewTasks.atDrop(NewTasks.create(place, id), spot).inRepeatingProject())
        // В список — сразу, запись в базу догонит (см. optimisticTasks)
        val shown = (listOf(insertion.created) + insertion.reordered).associate { it.id to ItemWithChecklist(it) }
        optimisticGroups[id] = shown.keys
        optimisticTasks.update { it + shown }
        viewModelScope.launch {
            // Одной записью: база вернёт задачу сразу с новым порядком соседей
            taskUseCases.updateTask(listOf(insertion.created) + insertion.reordered)
        }
        setInlineExpandedTaskId(id)
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
        dueDate: Long? = null,
        reminderTime: Int? = null,
        tags: List<String> = emptyList(),
        projectId: String? = null,
        checklist: List<ChecklistItem> = emptyList(),
        priority: Int = 0
    ) {
        // Первой в своём списке (NewTasks.topSortOrder): окно быстрого добавления — «+» на главном экране, поиск
        val top = NewTasks.topSortOrder(tasks.value.map { it.item })
        viewModelScope.launch {
            taskUseCases.addTask(
                sortOrder = top,
                title = title,
                notes = notes,
                section = section,
                isTonight = isTonight,
                startDate = startDate,
                dueDate = dueDate,
                reminderTime = reminderTime,
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
        priority: Int,
        reminderTime: Int?,
        untitledTitle: String
    ) {
        viewModelScope.launch {
            // Место задачи (проект, область, подзаголовок) редактор сам не меняет — это делает «Перемістити» сразу
            // в базе, пока редактор открыт. Правки редактора кладутся на свежую задачу из базы, а не на снимок,
            // с которым он открылся: иначе новая задача, перенесённая до первого сохранения, возвращалась на старое место
            val current = latestItem(task)
            val fields = TaskEditorFields(title, notes, section, isTonight, startDate, dueDate, tags, current.projectId, priority, reminderTime)
            taskUseCases.updateTask(TaskEdits.apply(current, fields, untitledTitle), checklist)
        }
    }

    /** Свежая версия задачи из базы (поток задач), иначе — та, что передали */
    private fun latestItem(task: Item): Item = tasks.value.firstOrNull { it.item.id == task.id }?.item ?: task

    /** Отметка задачи прямо из открытого редактора — вместе с её ещё не сохранёнными правками. */
    fun toggleEditedTask(wrapper: ItemWithChecklist, fields: TaskEditorFields, checklist: List<ChecklistItem>, untitledTitle: String) {
        // Как при сохранении: место — из свежей задачи (см. updateTask)
        val current = latestItem(wrapper.item)
        toggleTaskCompletion(wrapper.copy(item = TaskEdits.apply(current, fields.copy(projectId = current.projectId), untitledTitle), checklist = checklist))
    }

    fun updateTasks(items: List<Item>) {
        viewModelScope.launch {
            taskUseCases.updateTask(items)
        }
    }

    fun toggleTaskCompletion(wrapper: ItemWithChecklist) {
        // Повторяющуюся задачу (шаблон) отмечают заранее: выполняется копия ближайшего повтора
        if (wrapper.item.isRepeatTemplate) return completeRepeatEarly(wrapper.item.id)
        viewModelScope.launch {
            taskUseCases.toggleTaskCompletion(wrapper)
        }
    }

    /** Отменить задачу или вернуть отменённую ([TaskStatuses]). */
    fun toggleTaskCancel(taskId: String) {
        viewModelScope.launch { taskUseCases.toggleTaskCancel(taskId) }
    }

    // Действия с шаблоном повтора ([com.example.domain.usecase.task.RepeatTemplateUseCase])
    fun pauseRepeat(templateId: String) { viewModelScope.launch { taskUseCases.repeatTemplate.pause(templateId) } }
    fun resumeRepeat(templateId: String) { viewModelScope.launch { taskUseCases.repeatTemplate.resume(templateId) } }
    fun stopRepeat(templateId: String) { viewModelScope.launch { taskUseCases.repeatTemplate.stop(templateId) } }
    fun repeatNextCopy(templateId: String) { viewModelScope.launch { taskUseCases.repeatTemplate.createNextCopy(templateId) } }
    fun completeRepeatEarly(templateId: String) { viewModelScope.launch { taskUseCases.repeatTemplate.completeEarly(templateId) } }
    /** Перенос ближайшего повтора на день появления [appearOn] (начало дня, мс) — исключением или новым правилом */
    fun moveRepeat(templateId: String, appearOn: Long, updateRule: Boolean) {
        val date = java.time.Instant.ofEpochMilli(appearOn).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        viewModelScope.launch { taskUseCases.repeatTemplate.move(templateId, date, updateRule) }
    }
    fun updateRepeatRule(templateId: String, rule: com.example.domain.repeat.RepeatRule, reminderTime: Int?) {
        viewModelScope.launch { taskUseCases.repeatTemplate.updateRule(templateId, rule, reminderTime) }
    }

    /** Сделать задачу повторяющейся ([com.example.domain.usecase.task.MakeTaskRepeatingUseCase]). */
    fun makeRepeating(taskId: String, rule: com.example.domain.repeat.RepeatRule, reminderTime: Int?) {
        viewModelScope.launch { taskUseCases.makeRepeating(taskId, rule, reminderTime) }
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
        // Повторяющиеся задачи (шаблоны) отмечаются заранее — выполняются копии их ближайших повторов
        val (templates, regular) = taskWrappers.partition { it.item.isRepeatTemplate }
        if (isCompleted) templates.forEach { completeRepeatEarly(it.item.id) }
        viewModelScope.launch {
            val updated = regular.map { wrapper ->
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
     * ([DayBounds.isSomeday] — по `start == Item.START_SOMEDAY` и т. д.), поэтому без него задача с новой датой
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
    fun batchMoveTasks(
        taskWrappers: List<ItemWithChecklist>,
        projectId: String?,
        areaId: String?,
        moveToInbox: Boolean,
        headingId: String? = null
    ) {
        val target = if (moveToInbox) MoveTarget.Inbox else MoveTarget.Place(projectId, areaId, headingId)
        viewModelScope.launch {
            taskUseCases.updateTask(taskWrappers.map { TaskMoves.moved(it.item, target) })
        }
    }

    /**
     * «Новый проект „…“» в окне «Переместить»: создаёт проект с названием из поиска и переносит в него задачи.
     * Проект сохраняется раньше задач — в одной корутине, по порядку.
     */
    fun moveTasksToNewProject(taskWrappers: List<ItemWithChecklist>, title: String) {
        if (title.isBlank() || taskWrappers.isEmpty()) return
        val project = NewProjects.create(title = title)
        val target = MoveTarget.Place(project.id, null)
        viewModelScope.launch {
            projectUseCases.updateProject(project)
            taskUseCases.updateTask(taskWrappers.map { TaskMoves.moved(it.item, target) })
        }
    }

    /** Перемещение одной задачи («Переместить» в редакторе) — по тому же правилу, что пакетное. */
    fun moveTask(wrapper: ItemWithChecklist, target: MoveTarget) {
        viewModelScope.launch {
            taskUseCases.updateTask(TaskMoves.moved(wrapper.item, target), wrapper.checklist)
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

    /** Новый проект с пустым названием (его тут же правят на главном экране). */
    fun createProject(id: String, areaId: String? = null) = updateProject(NewProjects.create(areaId, id))

    /** Новый проект на месте [index] среди [projects] (в порядке главного экрана); соседи при нужде перенумеровываются. */
    fun createProjectAt(id: String, areaId: String?, index: Int, projects: List<Item>) {
        val insertion = SortOrders.insert(projects, index, NewProjects.create(areaId, id))
        updateProject(insertion.created)
        if (insertion.reordered.isNotEmpty()) updateTasks(insertion.reordered)
    }

    /** Правка проекта ([ProjectEdits]); без изменений ничего не сохраняется. */
    fun editProject(project: Item, edit: ProjectEdit) {
        val edited = ProjectEdits.apply(project, edit)
        if (edited !== project) updateProject(edited)
    }

    fun deleteProject(project: Item) {
        viewModelScope.launch {
            projectUseCases.deleteProject(project)
        }
    }

    /** Завершает проект вместе с его открытыми задачами. */
    fun completeProject(project: Item) {
        viewModelScope.launch {
            projectUseCases.completeProject(project, tasks.value.filter { it.item.projectId == project.id })
        }
    }

    /** Дублирует проект с заголовками и задачами. */
    fun duplicateProject(project: Item) {
        viewModelScope.launch {
            projectUseCases.duplicateProject(
                project,
                headings.value.filter { it.projectId == project.id && !it.trashed },
                tasks.value.filter { it.item.projectId == project.id && !it.item.trashed },
                orderedProjects = projects.value
            )
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
                // Пустая строка — текст «неизвестная ошибка» подставит экран на языке системы
                _syncError.value = error.localizedMessage.orEmpty()
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
