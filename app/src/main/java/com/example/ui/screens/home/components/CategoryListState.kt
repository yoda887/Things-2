package com.example.ui.screens.home.components

import com.example.domain.lists.PlaceGroups
import com.example.data.model.ChecklistItem
import com.example.domain.edits.TaskEditorFields
import com.example.domain.edits.ProjectEdit
import com.example.domain.edits.NewTaskPlace
import com.example.domain.edits.DropSpot
import com.example.ui.viewmodel.ActiveScreen
import com.example.domain.lists.TagList
import com.example.domain.lists.AreaList
import com.example.domain.lists.LogbookList
import com.example.domain.lists.SomedayList
import com.example.domain.lists.AnytimeList
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

    /** Префикс заголовка раздела «Журнала» (сегодня, вчера, месяц) */
    const val LOGBOOK_HEADER_PREFIX = "lbhdr_"

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
    headings: List<Item> = emptyList(),
    // Место задачи в открытом редакторе — «В любое время» и «Когда-нибудь» держат её там до закрытия
    pinnedPlaces: Map<String, PlaceGroups.Place> = emptyMap()
): List<Any> {
    val headerLogbook = stringResource(R.string.category_logbook)
    val todayLabel = stringResource(R.string.category_today)
    val yesterdayLabel = stringResource(R.string.yesterday)
    return remember(screen, standardToday, eveningToday, headerLogbook, todayLabel, yesterdayLabel, draggedItemKey, upcomingDays, upcomingMonths, projects, area, displayTasks, isLaterItemsHidden, tag, allTasks, areas, savedTags, searchQuery, headings, pinnedPlaces) {
        // Все экраны перечислены явно (без else): новый экран компилятор заставит указать и здесь
        when (screen) {
            ActiveScreen.TODAY -> TodayScreen.rows(standardToday, eveningToday, isDragging = draggedItemKey != null)
            ActiveScreen.UPCOMING -> UpcomingScreen.rows(upcomingDays, upcomingMonths)
            ActiveScreen.AREA_DETAIL -> AreaScreen.rows(AreaList.content(area?.id, displayTasks, projects), isLaterItemsHidden)
            ActiveScreen.LOGBOOK -> LogbookScreen.rows(LogbookList.sections(displayTasks, projects), todayLabel, yesterdayLabel)
            ActiveScreen.TAG_DETAIL -> TagScreen.rows(TagList.content(tag?.title, displayTasks, allTasks, projects))
            ActiveScreen.PROJECT_DETAIL -> ProjectScreen.rows(displayTasks, headings, draggedItemKey)
            ActiveScreen.ANYTIME -> PlaceGroupRows.rows(AnytimeList.groups(displayTasks, projects, areas, pinnedPlaces))
            ActiveScreen.SOMEDAY -> PlaceGroupRows.rows(SomedayList.groups(displayTasks, projects, areas, pinnedPlaces))
            ActiveScreen.SEARCH -> SearchScreen.rows(displayTasks, projects, areas, savedTags, searchQuery, headerLogbook)
            // «Входящие» — задачи как есть; у главного экрана списка нет
            ActiveScreen.INBOX, ActiveScreen.HOME -> displayTasks
        }
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
 * События пользовательского интерфейса для CategoryListPanel.
 */
sealed interface ThingsCategoryListEvent {
    data class SelectTag(val tag: String?) : ThingsCategoryListEvent
    data class ToggleTask(val task: ItemWithChecklist) : ThingsCategoryListEvent
    /** Отметка из открытого редактора — вместе с ещё не сохранёнными правками полей */
    data class ToggleEditedTask(val task: ItemWithChecklist, val fields: TaskEditorFields, val checklist: List<ChecklistItem>) : ThingsCategoryListEvent
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
    /** Правка проекта: заметки, «Когда», дедлайн, теги, область. */
    data class EditProject(val project: Item, val edit: ProjectEdit) : ThingsCategoryListEvent
    data class CompleteProject(val project: Item) : ThingsCategoryListEvent
    data class DuplicateProject(val project: Item) : ThingsCategoryListEvent
    data class DeleteArea(val area: Area) : ThingsCategoryListEvent

    // Заголовки проекта
    data class SaveHeading(val heading: Item) : ThingsCategoryListEvent
    /** Новый пустой подзаголовок в конце проекта */
    data class AddHeading(val headingId: String, val projectId: String, val headings: List<Item>) : ThingsCategoryListEvent
    data class RenameHeading(val heading: Item, val title: String) : ThingsCategoryListEvent
    data class DeleteHeading(val heading: Item) : ThingsCategoryListEvent
    /** Новый заголовок без названия: удалить его, вернув [tasksBack] в группу выше */
    /** Пустой новый подзаголовок: удалить, а его задачи [tasks] вернуть в группу [groupAboveId] */
    data class DiscardHeading(val heading: Item, val tasks: List<Item>, val groupAboveId: String?) : ThingsCategoryListEvent
    data class ArchiveHeading(val heading: Item) : ThingsCategoryListEvent
    data class ReorderHeadings(val headings: List<Item>) : ThingsCategoryListEvent

    // Добавление перетаскиванием кнопки «+»
    /** «+», брошенный в список: где ([place], [spot]) и перед какой задачей списка экрана ([insertIndex]) */
    data class CreateTaskAt(
        val taskId: String,
        val place: NewTaskPlace,
        val spot: DropSpot,
        val insertIndex: Int,
        val listTasks: List<Item>
    ) : ThingsCategoryListEvent
    /** «+», брошенный у левого края проекта: новый подзаголовок на месте [index], под ним — [movedTasks] */
    data class InsertHeadingAt(
        val headingId: String,
        val projectId: String,
        val headings: List<Item>,
        val index: Int,
        val movedTasks: List<Item>
    ) : ThingsCategoryListEvent

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

/**
 * Место новой задачи на экране [screen] — для «+» и «+», брошенного в список. Какие поля получит задача
 * в этом месте, решает домен ([com.example.domain.edits.NewTasks]).
 */
fun newTaskPlace(screen: ActiveScreen, projectId: String?, areaId: String?, tagTitle: String?): NewTaskPlace = when (screen) {
    ActiveScreen.TODAY -> NewTaskPlace.Today
    ActiveScreen.UPCOMING -> NewTaskPlace.Upcoming(null)
    ActiveScreen.ANYTIME -> NewTaskPlace.Anytime
    ActiveScreen.SOMEDAY -> NewTaskPlace.Someday
    ActiveScreen.PROJECT_DETAIL -> projectId?.let { NewTaskPlace.Project(it) } ?: NewTaskPlace.Inbox
    ActiveScreen.AREA_DETAIL -> areaId?.let { NewTaskPlace.Area(it) } ?: NewTaskPlace.Anytime
    ActiveScreen.TAG_DETAIL -> NewTaskPlace.Tag(tagTitle.orEmpty())
    ActiveScreen.INBOX, ActiveScreen.LOGBOOK, ActiveScreen.SEARCH, ActiveScreen.HOME -> NewTaskPlace.Inbox
}
