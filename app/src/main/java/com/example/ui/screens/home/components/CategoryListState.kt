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

    /** Заголовок вечерней секции экрана «Сегодня» */
    const val EVENING_HEADER = "evening_header"

    /** Заголовок секции проектов на экране сферы */
    const val PROJECTS_HEADING = "projects_heading"

    /** Заголовок секции задач на экране сферы */
    const val TASKS_HEADING = "tasks_heading"

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

    /**
     * Строки, целями перетаскивания быть не могут: обработчик перемещения их всё равно
     * отклонит, а как ближайшая цель сверху они запирают задачу и не дают подняться выше.
     *
     * Заголовки, у которых есть осмысленная обработка сброса ([MAIN_HEADER], [EVENING_HEADER],
     * [DAY_HEADER_PREFIX], [MONTH_HEADER_PREFIX]), сюда не входят.
     */
    val nonDroppable = setOf(CALENDAR_WIDGET, TAG_FILTER, EMPTY_STATE, PROJECTS_HEADING, TASKS_HEADING)
}

/** Горизонт планирования экрана «Предстоящие» в днях */
private const val UPCOMING_DAYS_HORIZON = 14

/**
 * Вычисляет полное расписание экрана «Предстоящие»:
 * 1. Первые 14 дней подряд (включая пустые).
 * 2. Месяцы позже 14 дней (только месяцы с делами, в стиле Things 3).
 */
fun computeUpcomingSchedule(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>
): UpcomingSchedule {
    val daysList = mutableListOf<UpcomingDay>()

    val cursor = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val locale = Locale.getDefault()
    val weekdayFormat = SimpleDateFormat("EEEE", locale)
    val monthFormat = SimpleDateFormat("MMMM", locale)
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", locale)
    val dayBadgeFormat = SimpleDateFormat("dd/MM", locale)
    val dayNumFormat = SimpleDateFormat("d", locale)

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    // Первые 14 дней отображаются ВСЕГДА, даже если они пустые
    for (offset in 0 until UPCOMING_DAYS_HORIZON) {
        val dayStart = cursor.timeInMillis
        val dayOfMonthLabel = cursor.get(Calendar.DAY_OF_MONTH).toString()

        cursor.add(Calendar.DAY_OF_YEAR, 1)
        val nextDayStart = cursor.timeInMillis

        val dayEvents = calendarEvents.filter { event ->
            val eventStart = event.eventStartMillis
            eventStart != null && eventStart >= dayStart && eventStart < nextDayStart
        }.map { ItemWithChecklist(item = it, checklist = emptyList()) }

        val dayTasks = localTasksList.filter { wrapper ->
            val taskStart = wrapper.item.startDate
            taskStart != null && taskStart >= dayStart && taskStart < nextDayStart
        }

        val dayOfWeekLabel = when (offset) {
            0 -> "Tomorrow"
            else -> weekdayFormat.format(Date(dayStart))
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

    // Горизонт 14 дней закончился в cursor.timeInMillis
    val horizonEndMillis = cursor.timeInMillis

    // Задачи и события ПОЗЖЕ 14 дней
    val laterTasks = localTasksList.filter { wrapper ->
        val taskStart = wrapper.item.startDate
        taskStart != null && taskStart >= horizonEndMillis
    }

    val laterEvents = calendarEvents.filter { event ->
        val eventStart = event.eventStartMillis
        eventStart != null && eventStart >= horizonEndMillis
    }

    val monthTaskBadges = mutableMapOf<String, String>()
    laterTasks.forEach { wrapper ->
        val taskStart = wrapper.item.startDate ?: 0L
        monthTaskBadges[wrapper.item.id] = dayBadgeFormat.format(Date(taskStart))
    }

    // Собираем все уникальные месяцы, где есть хотя бы одна задача или событие
    val monthCal = Calendar.getInstance()
    data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
        override fun compareTo(other: MonthKey): Int {
            return if (year != other.year) year.compareTo(other.year) else month.compareTo(other.month)
        }
    }

    val monthsMap = sortedMapOf<MonthKey, Pair<MutableList<ItemWithChecklist>, MutableList<UpcomingEventItem>>>()

    laterTasks.forEach { task ->
        val start = task.item.startDate ?: 0L
        monthCal.timeInMillis = start
        val key = MonthKey(monthCal.get(Calendar.YEAR), monthCal.get(Calendar.MONTH))
        val pair = monthsMap.getOrPut(key) { Pair(mutableListOf(), mutableListOf()) }
        pair.first.add(task)
    }

    laterEvents.forEach { event ->
        val start = event.eventStartMillis ?: 0L
        monthCal.timeInMillis = start
        val key = MonthKey(monthCal.get(Calendar.YEAR), monthCal.get(Calendar.MONTH))
        val pair = monthsMap.getOrPut(key) { Pair(mutableListOf(), mutableListOf()) }
        val dayNum = dayNumFormat.format(Date(start))
        pair.second.add(UpcomingEventItem(event = event, dateMillis = start, dayOfMonthLabel = dayNum))
    }

    val horizonCal = Calendar.getInstance().apply { timeInMillis = horizonEndMillis }

    val monthsList = mutableListOf<UpcomingMonth>()
    monthsMap.forEach { (key, pair) ->
        monthCal.set(Calendar.YEAR, key.year)
        monthCal.set(Calendar.MONTH, key.month)
        monthCal.set(Calendar.DAY_OF_MONTH, 1)
        monthCal.set(Calendar.HOUR_OF_DAY, 0)
        monthCal.set(Calendar.MINUTE, 0)
        monthCal.set(Calendar.SECOND, 0)
        monthCal.set(Calendar.MILLISECOND, 0)
        val monthStart = monthCal.timeInMillis

        // Проверяем, является ли секция остатком месяца после 14-дневного горизонта
        val isHorizonMonth = key.year == horizonCal.get(Calendar.YEAR) && key.month == horizonCal.get(Calendar.MONTH)
        val startDay = if (isHorizonMonth) horizonCal.get(Calendar.DAY_OF_MONTH) else 1

        val (label, sectionStartMillis) = if (isHorizonMonth && startDay > 1) {
            val endDayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, key.year)
                set(Calendar.MONTH, key.month)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            val pattern = if (key.year == currentYear) "d MMMM" else "d MMMM yyyy"
            val endFormatted = SimpleDateFormat(pattern, locale).format(endDayCal.time)
            val endDay = endDayCal.get(Calendar.DAY_OF_MONTH)
            val periodLabel = if (startDay == endDay) {
                endFormatted
            } else {
                "$startDay – $endFormatted"
            }
            Pair(periodLabel, horizonEndMillis)
        } else {
            val rawLabel = if (key.year == currentYear) {
                monthFormat.format(Date(monthStart))
            } else {
                monthYearFormat.format(Date(monthStart))
            }
            val capitalizedLabel = rawLabel.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            }
            Pair(capitalizedLabel, monthStart)
        }

        pair.first.sortBy { it.item.startDate ?: 0L }
        pair.second.sortBy { it.dateMillis }

        monthsList.add(
            UpcomingMonth(
                monthMillis = sectionStartMillis,
                monthLabel = label,
                calendarEvents = pair.second,
                tasks = pair.first
            )
        )
    }

    return UpcomingSchedule(
        days = daysList,
        months = monthsList,
        monthTaskBadges = monthTaskBadges
    )
}

/**
 * Вычисляет список запланированных дней (UpcomingDays) со сгруппированными задачами и событиями.
 */
fun computeUpcomingDays(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>
): List<UpcomingDay> {
    return computeUpcomingSchedule(localTasksList, calendarEvents).days
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
 * Запоминает и вычисляет полное расписание экрана «Предстоящие» (дни и месяцы).
 */
@Composable
fun rememberUpcomingSchedule(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>
): UpcomingSchedule {
    return remember(localTasksList, calendarEvents) {
        computeUpcomingSchedule(localTasksList, calendarEvents)
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
    upcomingMonths: List<UpcomingMonth> = emptyList(),
    projects: List<Item>,
    area: Area?,
    displayTasks: List<ItemWithChecklist>
): List<Any> {
    return remember(screen, standardToday, eveningToday, draggedItemKey, upcomingDays, upcomingMonths, projects, area, displayTasks) {
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
                    day.calendarEvents.forEachIndexed { index, event ->
                        // Отступ после последнего события дня — всегда, а не только когда за ним есть задачи.
                        // Наличие задач меняется прямо во время перетаскивания, и вместе с ним менялась бы
                        // высота этой строки: движок, рассчитывающий компенсацию прыжка при неизменных
                        // высотах, промахивался бы на величину отступа, и карточка дёргалась бы на переходе дня.
                        val isLast = index == day.calendarEvents.lastIndex
                        add(UpcomingEventItem(event.item, day.dateMillis, isLastBeforeTasks = isLast))
                    }
                    day.tasks.forEach { task ->
                        add(task)
                    }
                }
                upcomingMonths.forEach { month ->
                    add(UpcomingMonthHeaderItem(month.monthMillis, month.monthLabel))
                    // События календаря месяца выводятся первыми, не смешиваясь с задачами
                    month.calendarEvents.forEachIndexed { index, event ->
                        // Как и у дней: отступ не зависит от наличия задач, иначе высота строки
                        // менялась бы во время перетаскивания
                        val isLast = index == month.calendarEvents.lastIndex
                        add(event.copy(isLastBeforeTasks = isLast))
                    }
                    // Задачи месяца выводятся строго после событий календаря
                    month.tasks.forEach { task ->
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

