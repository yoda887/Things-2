package com.example.ui.screens.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
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
data class UpcomingHeaderItem(
    val dateMillis: Long,
    val dayOfMonth: String,
    val dayOfWeekLabel: String
)

/**
 * Объект-держатель для ссылки на UpcomingEventItem из Календаря
 */
object UpcomingHeaderItemHelper {
    class UpcomingEventItem(val event: Item, val dateMillis: Long)
}

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

    /** Заголовок вечерней секции экрана «Сегодня» */
    const val EVENING_HEADER = "evening_header"

    /** Заголовок секции проектов на экране сферы */
    const val PROJECTS_HEADING = "projects_heading"

    /** Заголовок секции задач на экране сферы */
    const val TASKS_HEADING = "tasks_heading"

    /** Префикс заголовка дня на экране «Предстоящие» */
    const val DAY_HEADER_PREFIX = "hdr_"

    /** Префикс события календаря */
    const val CALENDAR_EVENT_PREFIX = "ev_"

    /** Виджет событий календаря в шапке экрана «Сегодня» */
    const val CALENDAR_WIDGET = "calendar_events"

    /** Строка фильтра по тегам */
    const val TAG_FILTER = "tag_filter"

    /** Заглушка пустого списка */
    const val EMPTY_STATE = "empty_state"

    /**
     * Строки, целями перетаскивания быть не могут: обработчик перемещения их всё равно
     * отклонит, а как ближайшая цель сверху они запирают задачу и не дают подняться выше.
     *
     * Заголовки, у которых есть осмысленная обработка сброса ([MAIN_HEADER], [EVENING_HEADER],
     * [DAY_HEADER_PREFIX]), сюда не входят.
     */
    val nonDroppable = setOf(CALENDAR_WIDGET, TAG_FILTER, EMPTY_STATE, PROJECTS_HEADING, TASKS_HEADING)
}

/** Горизонт планирования экрана «Предстоящие» в днях */
private const val UPCOMING_DAYS_HORIZON = 14

/**
 * Вычисляет список запланированных дней (UpcomingDays) со сгруппированными задачами и событиями.
 */
fun computeUpcomingDays(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>
): List<UpcomingDay> {
    val daysList = mutableListOf<UpcomingDay>()

    // Один переиспользуемый курсор вместо трёх Calendar.getInstance() на каждый день цикла.
    // Стартует с начала завтрашнего дня и сдвигается по суткам.
    val cursor = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Форматтеры вынесены из цикла: их создание заметно дороже самого форматирования
    val weekdayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
    val monthFormat = SimpleDateFormat("MMMM", Locale.ENGLISH)

    for (offset in 0 until UPCOMING_DAYS_HORIZON) {
        val dayStart = cursor.timeInMillis
        val dayOfMonthLabel = cursor.get(Calendar.DAY_OF_MONTH).toString()

        // Сдвигаем курсор на сутки — его новое значение служит верхней границей текущего дня
        cursor.add(Calendar.DAY_OF_YEAR, 1)
        val nextDayStart = cursor.timeInMillis

        // Находим все календарные события на этот день
        val dayEvents = calendarEvents.filter { event ->
            val eventStart = event.eventStartMillis
            eventStart != null && eventStart >= dayStart && eventStart < nextDayStart
        }.map { ItemWithChecklist(item = it, checklist = emptyList()) }

        // Находим все задачи на этот день
        val dayTasks = localTasksList.filter { wrapper ->
            val taskStart = wrapper.item.startDate
            taskStart != null && taskStart >= dayStart && taskStart < nextDayStart
        }

        if (dayEvents.isNotEmpty() || dayTasks.isNotEmpty()) {
            val dayOfWeekLabel = when {
                offset == 0 -> "Tomorrow"
                offset < 6 -> weekdayFormat.format(Date(dayStart))
                else -> monthFormat.format(Date(dayStart))
            }

            daysList.add(
                UpcomingDay(
                    dateMillis = dayStart,
                    dayOfMonth = dayOfMonthLabel,
                    dayOfWeekLabel = dayOfWeekLabel,
                    calendarEvents = dayEvents,
                    tasks = dayTasks
                )
            )
        }
    }
    return daysList
}

/**
 * Запоминает и вычисляет список запланированных дней (UpcomingDays) со сгруппированными задачами и событиями.
 */
@Composable
fun rememberUpcomingDays(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>
): List<UpcomingDay> {
    return remember(localTasksList, calendarEvents) {
        computeUpcomingDays(localTasksList, calendarEvents)
    }
}

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
    projects: List<Item>,
    area: Area?,
    displayTasks: List<ItemWithChecklist>
): List<Any> {
    return remember(screen, standardToday, eveningToday, draggedItemKey, upcomingDays, projects, area, displayTasks) {
        buildList<Any> {
            if (screen == ActiveScreen.TODAY) {
                addAll(standardToday)
                if (eveningToday.isNotEmpty() || draggedItemKey != null) {
                    add(TaskListKeys.EVENING_HEADER)
                    addAll(eveningToday)
                }
            } else if (screen == ActiveScreen.UPCOMING) {
                upcomingDays.forEach { day ->
                    add(UpcomingHeaderItem(day.dateMillis, day.dayOfMonth, day.dayOfWeekLabel))
                    day.calendarEvents.forEach { event ->
                        add(UpcomingEventItem(event.item, day.dateMillis))
                    }
                    day.tasks.forEach { task ->
                        add(task)
                    }
                }
            } else if (screen == ActiveScreen.AREA_DETAIL) {
                val areaProjects = projects.filter { it.areaId == area?.id }
                if (areaProjects.isNotEmpty()) {
                    add(TaskListKeys.PROJECTS_HEADING)
                    addAll(areaProjects)
                }
                val areaDirectTasks = displayTasks.filter { it.item.areaId == area?.id && (it.item.projectId == null || it.item.projectId == "") }
                if (areaDirectTasks.isNotEmpty()) {
                    add(TaskListKeys.TASKS_HEADING)
                    addAll(areaDirectTasks)
                }
            } else {
                addAll(displayTasks)
            }
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
    val selectedTaskIds: Set<String> = emptySet()
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
    object ClickSearch : ThingsCategoryListEvent
    object ClickBack : ThingsCategoryListEvent
    
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
    data class DeleteArea(val area: Area) : ThingsCategoryListEvent

    // Свайп-события (для вызова мультиселекции и When/календаря)
    data class SwipeTaskLeft(val task: ItemWithChecklist) : ThingsCategoryListEvent
    data class SwipeTaskRight(val task: ItemWithChecklist) : ThingsCategoryListEvent

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
    data class BatchScheduleTasks(val startDate: Long?, val isTonight: Boolean) : ThingsCategoryListEvent
    data class BatchMoveTasks(val projectId: String?, val areaId: String?, val moveToInbox: Boolean) : ThingsCategoryListEvent
    data class BatchSetTags(val tags: List<String>) : ThingsCategoryListEvent
    data class BatchSetDeadline(val deadline: Long?) : ThingsCategoryListEvent
}

