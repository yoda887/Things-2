package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.example.data.model.*
import com.example.ui.screens.home.ActiveScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

import androidx.compose.ui.graphics.Color

/**
 * Вспомогательный класс для представления задач и календарных событий, сгруппированных по дням.
 */
data class UpcomingDay(
    val dateMillis: Long,
    val dayOfMonth: String,
    val dayOfWeekLabel: String,
    val calendarEvents: List<ItemWithChecklist>,
    val tasks: List<ItemWithChecklist>
)

/**
 * Вспомогательный класс для отображения заголовка дня в списке Upcoming.
 */
/**
 * Вспомогательный класс для отображения заголовка дня в списке Upcoming.
 */
data class UpcomingHeaderItem(
    val dateMillis: Long,
    val dayOfMonth: String,
    val dayOfWeekLabel: String
)

/**
 * Вспомогательный класс для отображения заголовка месяца в списке Upcoming.
 */
data class UpcomingMonthHeaderItem(
    val monthMillis: Long,
    val monthLabel: String
)

/**
 * Вспомогательный класс для представления месяца и входящих в него задач и событий.
 */
data class UpcomingMonth(
    val monthMillis: Long,
    val monthLabel: String,
    val calendarEvents: List<UpcomingEventItem>,
    val tasks: List<ItemWithChecklist>
)

/**
 * Полное расписание экрана «Предстоящие»: первые 14 дней и последующие месяцы.
 */
data class UpcomingSchedule(
    val days: List<UpcomingDay>,
    val months: List<UpcomingMonth>,
    val monthTaskBadges: Map<String, String> = emptyMap()
)

/**
 * Ключи элементов LazyColumn на экране списка задач.
 *
 * Единый источник правды: по этим же ключам модификаторы перетаскивания решают, куда задачу
 * класть можно, а куда нельзя. Раньше строки дублировались в двух файлах — переименование
 * ключа в панели молча ломало фильтр целей.
 *
 * Движок перетаскивания про эти ключи ничего не знает: для него ключ — просто `Any`.
 */
object TaskListKeys {
    /** Заголовок экрана */
    const val MAIN_HEADER = "main_header"

    /** Свойства проекта под заголовком: теги, «Когда», дедлайн, заметки */
    const val PROJECT_DETAILS = "project_details"

    /** Заголовок вечерней секции экрана «Сегодня» */
    const val EVENING_HEADER = "evening_header"

    /** Заголовок секции проектов на экране сферы */
    const val PROJECTS_HEADING = "projects_heading"

    /** Заголовок секции задач на экране сферы */
    const val TASKS_HEADING = "tasks_heading"

    /** Заголовок секции «Планы» на экране сферы */
    const val AREA_UPCOMING_HEADING = "area_upcoming_heading"

    /** Заголовок секции «Когда-нибудь» на экране сферы */
    const val AREA_SOMEDAY_HEADING = "area_someday_heading"

    /** Кнопка скрытия/показа более поздних объектов на экране сферы */
    const val AREA_LATER_TOGGLE = "area_later_toggle"

    /** Отступ между списком проектов и списком задач на экране сферы */
    const val AREA_PROJECTS_SPACER = "area_projects_spacer"

    /** Префикс заголовка дня на экране «Предстоящие» */
    const val DAY_HEADER_PREFIX = "hdr_"

    /** Префикс заголовка месяца на экране «Предстоящие» */
    const val MONTH_HEADER_PREFIX = "mhdr_"

    /** Префикс события календаря */
    const val CALENDAR_EVENT_PREFIX = "ev_"

    /** Виджет событий календаря в шапке экрана «Сегодня» */
    const val CALENDAR_WIDGET = "calendar_events"

    /** Строка фильтра по тегам */
    const val TAG_FILTER = "tag_filter"

    /** Заглушка пустого списка */
    const val EMPTY_STATE = "empty_state"

    /** Префикс заголовка внутри проекта */
    const val HEADING_PREFIX = "hd_"

    /**
     * Строки, целями перетаскивания быть не могут: обработчик перемещения их всё равно
     * отклонит, а как ближайшая цель сверху они запирают задачу и не дают подняться выше.
     *
     * Заголовки, у которых есть осмысленная обработка сброса ([MAIN_HEADER], [EVENING_HEADER],
     * [DAY_HEADER_PREFIX], [MONTH_HEADER_PREFIX]), сюда не входят.
     */
    val nonDroppable = setOf(CALENDAR_WIDGET, TAG_FILTER, EMPTY_STATE, PROJECTS_HEADING, TASKS_HEADING, AREA_UPCOMING_HEADING, AREA_SOMEDAY_HEADING, AREA_LATER_TOGGLE, AREA_PROJECTS_SPACER)
}

/** Вид заголовка секции в результатах поиска. */
enum class SearchSectionKind { PROJECT, AREA, LOGBOOK }

/**
 * Заголовок секции списка: задачи конкретного проекта или области (поиск, «В любое время», «Когда-нибудь»), Logbook поиска.
 * Найденные проекты, области и теги идут строками без заголовка.
 */
data class SearchSectionHeaderItem(
    val key: String,
    val title: String,
    val kind: SearchSectionKind,
    val project: Item? = null,
    val area: Area? = null
)

/** Найденная по названию область. */
data class SearchAreaItem(val area: Area)

/** Найденный по названию тег. */
data class SearchTagItem(val tag: Tag)

/**
 * Создает плоский список элементов для отображения в LazyColumn на основе текущего экрана и данных.
 */
@Composable
fun rememberFlattenedList(
    screen: ActiveScreen,
    standardToday: List<ItemWithChecklist>,
    eveningToday: List<ItemWithChecklist>,
    draggedItemKey: Any?,
    upcomingDays: List<UpcomingDay>,
    upcomingMonths: List<UpcomingMonth> = emptyList(),
    projects: List<Item>,
    area: Area?,
    displayTasks: List<ItemWithChecklist>,
    isLaterItemsHidden: Boolean = false,
    tag: Tag? = null,
    allTasks: List<ItemWithChecklist> = emptyList(),
    areas: List<Area> = emptyList(),
    savedTags: List<Tag> = emptyList(),
    searchQuery: String = "",
    headings: List<Item> = emptyList()
): List<Any> {
    val headerLogbook = stringResource(R.string.category_logbook)
    return remember(screen, standardToday, eveningToday, headerLogbook, draggedItemKey, upcomingDays, upcomingMonths, projects, area, displayTasks, isLaterItemsHidden, tag, allTasks, areas, savedTags, searchQuery, headings) {
        buildList<Any> {
            if (screen == ActiveScreen.TODAY) {
                addAll(TodayScreen.rows(standardToday, eveningToday, isDragging = draggedItemKey != null))
            } else if (screen == ActiveScreen.UPCOMING) {
                addAll(UpcomingScreen.rows(upcomingDays, upcomingMonths))
            } else if (screen == ActiveScreen.AREA_DETAIL) {
                addAll(AreaScreen.rows(AreaScreen.content(area?.id, displayTasks, projects), isLaterItemsHidden))
            } else if (screen == ActiveScreen.LOGBOOK) {
                addAll(LogbookScreen.rows(displayTasks, projects))
            } else if (screen == ActiveScreen.TAG_DETAIL) {
                addAll(TagScreen.rows(TagScreen.content(tag?.title, displayTasks, allTasks, projects)))
            } else if (screen == ActiveScreen.PROJECT_DETAIL && headings.isNotEmpty()) {
                // Сначала задачи без заголовка, затем каждый заголовок со своими задачами.
                // Задачи заголовка, который несут под пальцем, убраны — как проекты свёрнутой области
                val headingIds = headings.map { it.id }
                val draggedHeadingId = (draggedItemKey as? String)
                    ?.takeIf { it.startsWith(TaskListKeys.HEADING_PREFIX) }
                    ?.removePrefix(TaskListKeys.HEADING_PREFIX)
                addAll(ProjectHeadings.tasksOf(displayTasks, null, headingIds))
                headings.forEach { heading ->
                    add(ProjectHeadingItem(heading))
                    if (heading.id != draggedHeadingId) {
                        addAll(ProjectHeadings.tasksOf(displayTasks, heading.id, headingIds))
                    }
                }
            } else if (screen == ActiveScreen.ANYTIME) {
                // Область — заголовком, только если у неё есть свои задачи; её проекты видны и без него
                addPlaceGroups(PlaceGroups.group(displayTasks, projects, areas, areaHeaderOnlyWithOwnItems = true))
            } else if (screen == ActiveScreen.SOMEDAY) {
                // «Когда-нибудь»: по областям, внутри области — отложенные проекты строками и задачи
                addPlaceGroups(PlaceGroups.group(displayTasks, projects, areas, PlaceGroups.somedayProjects(projects)))
            } else if (screen == ActiveScreen.SEARCH) {
                addSearchResults(displayTasks, projects, areas, savedTags, searchQuery, headerLogbook)
            } else {
                addAll(displayTasks)
            }
        }
    }
}

/** Группы «по месту»: заголовок проекта или области и задачи под ним; задачи без места — без заголовка. */
private fun MutableList<Any>.addPlaceGroups(groups: List<PlaceGroups.Group>) {
    groups.forEach { group ->
        when {
            group.project != null -> add(
                SearchSectionHeaderItem("place_hdr_project_${group.project.id}", group.project.title, SearchSectionKind.PROJECT, project = group.project)
            )
            group.area != null -> add(
                SearchSectionHeaderItem("place_hdr_area_${group.area.id}", group.area.title, SearchSectionKind.AREA, area = group.area)
            )
        }
        addAll(group.projectRows)
        addAll(group.tasks)
    }
}

/**
 * Результаты поиска: открытые задачи без проекта и области, найденные проекты, области и теги (строками, без заголовка),
 * открытые задачи по проектам и областям — в порядке главного экрана, затем выполненное (Logbook).
 */
private fun MutableList<Any>.addSearchResults(
    matchedTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    savedTags: List<Tag>,
    searchQuery: String,
    headerLogbook: String
) {
    val q = searchQuery.trim()
    if (q.isEmpty()) return

    fun Item.isDone() = isCompleted || status == Item.STATUS_CANCELLED
    val projectsById = projects.filter { it.type == Item.TYPE_PROJECT }.associateBy { it.id }
    val areasById = areas.associateBy { it.id }
    // Задача в удалённом проекте или области — в корзине вместе с ними, в поиске её нет
    val active = matchedTasks.filter { task ->
        !task.item.isDone() &&
            task.item.projectId?.let { projectsById[it]?.trashed } != true &&
            task.item.areaId?.let { areasById[it]?.trashed } != true
    }
    // Проект и область задачи, если они есть и не удалены; иначе задача — среди задач без проекта
    fun ItemWithChecklist.projectOrNull() = item.projectId?.let { projectsById[it] }
    fun ItemWithChecklist.areaOrNull() = item.areaId?.let { areasById[it] }

    // 1. Открытые задачи без проекта и области — сразу под строкой поиска, без заголовка.
    // Сюда же — задачи, чьих проекта или области больше нет: иначе они пропали бы из поиска
    addAll(active.filter { it.projectOrNull() == null && it.areaOrNull() == null })

    // 2. Найденные открытые проекты, области и теги — строками, без заголовка секции
    val matchedProjects = projects.filter {
        it.type == Item.TYPE_PROJECT && !it.trashed && !it.isDone() &&
            (it.title.contains(q, ignoreCase = true) || it.notes.contains(q, ignoreCase = true))
    }
    addAll(matchedProjects)
    val matchedAreas = areas.filter { !it.trashed && it.title.contains(q, ignoreCase = true) }
    addAll(matchedAreas.map { SearchAreaItem(it) })
    val matchedTags = savedTags.filter { it.title.isNotBlank() && it.title.contains(q, ignoreCase = true) }
    addAll(matchedTags.map { SearchTagItem(it) })

    // 3. Открытые задачи по проектам и областям — в порядке главного экрана: сначала проекты
    // без области, затем каждая область — её задачи без проекта, потом её проекты
    val tasksByProject = active.filter { it.projectOrNull() != null }.groupBy { it.item.projectId!! }
    val tasksByArea = active.filter { it.projectOrNull() == null && it.areaOrNull() != null }
        .groupBy { it.item.areaId!! }
    fun addProjectGroup(project: Item) {
        val tasks = tasksByProject[project.id] ?: return
        add(SearchSectionHeaderItem("search_hdr_project_${project.id}", project.title, SearchSectionKind.PROJECT, project = project))
        addAll(tasks)
    }
    val groupProjects = projects.filter { it.id in tasksByProject }
    // Проект, чьей области нет, на главном экране стоит среди проектов без области
    groupProjects.filter { it.areaId.isNullOrEmpty() || it.areaId !in areasById }.forEach(::addProjectGroup)
    areas.filter { it.id in tasksByArea || groupProjects.any { p -> p.areaId == it.id } }.forEach { area ->
        tasksByArea[area.id]?.let { tasks ->
            add(SearchSectionHeaderItem("search_hdr_area_${area.id}", area.title, SearchSectionKind.AREA, area = area))
            addAll(tasks)
        }
        groupProjects.filter { it.areaId == area.id }.forEach(::addProjectGroup)
    }

    // 4. Выполненные и отменённые задачи и проекты — Logbook, свежие сверху
    val doneTasks = matchedTasks.filter { it.item.isDone() }
    val doneProjects = projects.filter {
        it.type == Item.TYPE_PROJECT && !it.trashed && it.isDone() &&
            (it.title.contains(q, ignoreCase = true) || it.notes.contains(q, ignoreCase = true))
    }
    val logbook = (doneTasks.map { it to it.item } + doneProjects.map { it to it })
        .sortedByDescending { (_, item) -> item.stopDate ?: item.modificationDate }
        .map { it.first }
    if (logbook.isNotEmpty()) {
        add(SearchSectionHeaderItem("search_hdr_logbook", headerLogbook, SearchSectionKind.LOGBOOK))
        addAll(logbook)
    }
}

/**
 * Предоставляет реализацию `NestedScrollConnection` для жеста "тяни-для-поиска" (Pull-To-Search).
 */
@Composable
fun rememberPullToSearchConnection(
    coroutineScope: CoroutineScope,
    pullOffset: Animatable<Float, *>,
    lazyListState: LazyListState,
    thresholdPx: Float,
    maxOffsetPx: Float,
    onSearchClick: () -> Unit
): NestedScrollConnection {
    return remember(coroutineScope, pullOffset, lazyListState, thresholdPx, maxOffsetPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val currentOffset = pullOffset.value
                return if (delta < 0 && currentOffset > 0f) {
                    val newOffset = (currentOffset + delta).coerceAtLeast(0f)
                    val consumed = newOffset - currentOffset
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    Offset(0f, consumed)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                return if (source == NestedScrollSource.UserInput && delta > 0 && isAtTop) {
                    val newOffset = (pullOffset.value + delta * 0.5f).coerceAtMost(maxOffsetPx)
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    Offset(0f, delta)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val currentOffset = pullOffset.value
                if (currentOffset > 0f) {
                    val triggered = currentOffset >= thresholdPx
                    if (triggered) {
                        onSearchClick()
                    }
                    pullOffset.animateTo(0f, spring())
                    return available
                }
                return super.onPreFling(available)
            }
        }
    }
}

/**
 * Данные о прогрессе выполнения проекта (количество выполненных и общее количество задач).
 */
data class ProjectProgress(
    val completed: Int = 0,
    val total: Int = 0
)

/**
 * Состояние экрана категорий приложения Things.
 */
data class ThingsCategoryListState(
    val screen: ActiveScreen = ActiveScreen.INBOX,
    val project: Item? = null,
    val area: Area? = null,
    val tag: Tag? = null,
    val inlineExpandedTaskId: String? = null,
    val selectedTagFilter: String? = null,
    val allTags: Set<String> = emptySet(),
    val displayTasks: List<ItemWithChecklist> = emptyList(),
    val calendarEvents: List<Item> = emptyList(),
    val allSavedTags: List<String> = emptyList(),
    val allSavedTagObjects: List<Tag> = emptyList(),
    val areas: List<Area> = emptyList(),
    val projects: List<Item> = emptyList(),
    val highlightedTaskId: String? = null,
    val textPrimaryColor: Color = Color.Unspecified,
    val textSecondaryColor: Color = Color.Unspecified,
    val dividerColor: Color = Color.Unspecified,
    val allTasks: List<ItemWithChecklist> = emptyList(),
    val projectProgressMap: Map<String, ProjectProgress> = emptyMap(),
    val isSelectionMode: Boolean = false,
    val selectedTaskIds: Set<String> = emptySet(),
    // Текст запроса экрана поиска
    val searchQuery: String = "",
    // Активные заголовки проекта по порядку (экран проекта)
    val headings: List<Item> = emptyList()
)

/**
 * События пользовательского интерфейса для CategoryListPanel.
 */
sealed interface ThingsCategoryListEvent {
    data class SelectTag(val tag: String?) : ThingsCategoryListEvent
    data class ToggleTask(val task: ItemWithChecklist) : ThingsCategoryListEvent
    data class ClickTask(val task: ItemWithChecklist) : ThingsCategoryListEvent
    data class ClickProject(val project: Item) : ThingsCategoryListEvent
    data class ClickArea(val area: Area) : ThingsCategoryListEvent
    data class ChangeInlineExpandedTaskId(val taskId: String?) : ThingsCategoryListEvent
    // sourceBounds — круг индикатора оттяжки в координатах корня, из него вырастает Quick Find
    data class ClickSearch(val sourceBounds: Rect? = null) : ThingsCategoryListEvent
    object ClickBack : ThingsCategoryListEvent
    // Экран поиска: изменение запроса и переход к найденному тегу
    data class ChangeSearchQuery(val query: String) : ThingsCategoryListEvent
    data class ClickTag(val tag: Tag) : ThingsCategoryListEvent
    
    // События изменения/сохранения задачи из инлайн-редактора
    data class CreateTag(val title: String, val parentId: String?) : ThingsCategoryListEvent
    data class DeleteTag(val tag: Tag) : ThingsCategoryListEvent
    data class UpdateTag(val tag: Tag) : ThingsCategoryListEvent
    data class UpdateTagsOrder(val tags: List<Tag>) : ThingsCategoryListEvent
    
    // Сохранение задачи
    data class SaveTask(
        val taskWrapper: ItemWithChecklist,
        val title: String,
        val notes: String,
        val section: TaskSection,
        val isTonight: Boolean,
        val startDate: Long?,
        val dueDate: Long?,
        val tags: List<String>,
        val projectId: String?,
        val priority: Int,
        val checklist: List<ChecklistItem>
    ) : ThingsCategoryListEvent
    
    // Удаление, дублирование
    data class DeleteTask(val taskWrapper: ItemWithChecklist) : ThingsCategoryListEvent
    data class DuplicateTask(val taskWrapper: ItemWithChecklist) : ThingsCategoryListEvent
    data class MoveTask(val taskWrapper: ItemWithChecklist, val projectId: String?, val areaId: String?, val moveToInbox: Boolean) : ThingsCategoryListEvent
    data class ReorderTasks(val items: List<Item>) : ThingsCategoryListEvent
    // Удаление проекта и области
    data class DeleteProject(val project: Item) : ThingsCategoryListEvent
    /** Сохранить изменённый проект: заметки, «Когда», дедлайн, теги, область. */
    data class UpdateProject(val project: Item) : ThingsCategoryListEvent
    data class CompleteProject(val project: Item) : ThingsCategoryListEvent
    data class DuplicateProject(val project: Item) : ThingsCategoryListEvent
    data class DeleteArea(val area: Area) : ThingsCategoryListEvent

    // Заголовки проекта
    data class SaveHeading(val heading: Item) : ThingsCategoryListEvent
    data class DeleteHeading(val heading: Item) : ThingsCategoryListEvent
    /** Новый заголовок без названия: удалить его, вернув [tasksBack] в группу выше */
    data class DiscardHeading(val heading: Item, val tasksBack: List<Item>) : ThingsCategoryListEvent
    data class ArchiveHeading(val heading: Item) : ThingsCategoryListEvent
    data class ReorderHeadings(val headings: List<Item>) : ThingsCategoryListEvent

    // Добавление перетаскиванием кнопки «+»
    data class CreateTaskAt(val task: Item, val reorderedOthers: List<Item>) : ThingsCategoryListEvent
    data class InsertHeading(val headings: List<Item>, val movedTasks: List<Item>) : ThingsCategoryListEvent

    // Свайп-события (для вызова мультиселекции и When/календаря)
    data class SwipeTaskLeft(val task: ItemWithChecklist) : ThingsCategoryListEvent
    /** growFromOffset — смещение строки от центра экрана: из него вырастает диалог When */
    data class SwipeTaskRight(
        val task: ItemWithChecklist,
        val growFromOffset: androidx.compose.ui.geometry.Offset? = null
    ) : ThingsCategoryListEvent

    // Мультивыбор и пакетные операции
    data class EnterSelectionMode(val initialTaskId: String?) : ThingsCategoryListEvent
    data class ToggleTaskSelection(val taskId: String) : ThingsCategoryListEvent
    data class SetTaskSelected(val taskId: String, val selected: Boolean) : ThingsCategoryListEvent
    object SelectAllTasks : ThingsCategoryListEvent
    object DeselectAllTasks : ThingsCategoryListEvent
    object ExitSelectionMode : ThingsCategoryListEvent
    data class BatchCompleteTasks(val completed: Boolean) : ThingsCategoryListEvent
    object BatchDeleteTasks : ThingsCategoryListEvent
    object BatchDuplicateTasks : ThingsCategoryListEvent
    data class BatchScheduleTasks(val startDate: Long?, val isTonight: Boolean, val section: TaskSection) : ThingsCategoryListEvent
    data class BatchMoveTasks(val projectId: String?, val areaId: String?, val moveToInbox: Boolean) : ThingsCategoryListEvent
    data class BatchSetTags(val tags: List<String>) : ThingsCategoryListEvent
    data class BatchSetDeadline(val deadline: Long?) : ThingsCategoryListEvent
}

