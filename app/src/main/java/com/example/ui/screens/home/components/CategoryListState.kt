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

/** Горизонт планирования экрана «Предстоящие» в днях: неделя вперёд, начиная с завтра */
private const val UPCOMING_DAYS_HORIZON = 7

/** Сколько целых месяцев показывать после остатка месяца, в котором закончился горизонт */
private const val UPCOMING_MONTHS_HORIZON = 4

/**
 * Вычисляет полное расписание экрана «Предстоящие»:
 * 1. Неделя подряд начиная с завтра (включая пустые дни).
 * 2. Остаток месяца, в котором закончилась неделя, и следующие [UPCOMING_MONTHS_HORIZON]
 *    месяцев — только те, где есть дела. Дальше этого горизонта экран ничего не показывает.
 */
fun computeUpcomingSchedule(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>,
    tomorrowLabel: String
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
    // Плашка задачи в месячных разделах — число и месяц, как на экране проекта
    val dayBadgeFormat = SimpleDateFormat("d MMM", locale)
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
            0 -> tomorrowLabel
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

    // Недельный горизонт закончился в cursor.timeInMillis
    val horizonEndMillis = cursor.timeInMillis

    // Дальняя граница экрана: остаток месяца, в котором закончилась неделя, плюс следующие
    // UPCOMING_MONTHS_HORIZON месяцев. Всё, что позже, на этом экране не показывается
    val laterLimitMillis = Calendar.getInstance().apply {
        timeInMillis = horizonEndMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, UPCOMING_MONTHS_HORIZON + 1)
    }.timeInMillis

    // Задачи и события позже недели, но в пределах месячного горизонта
    val laterTasks = localTasksList.filter { wrapper ->
        val taskStart = wrapper.item.startDate
        taskStart != null && taskStart >= horizonEndMillis && taskStart < laterLimitMillis
    }

    val laterEvents = calendarEvents.filter { event ->
        val eventStart = event.eventStartMillis
        eventStart != null && eventStart >= horizonEndMillis && eventStart < laterLimitMillis
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
    calendarEvents: List<Item>,
    tomorrowLabel: String
): List<UpcomingDay> {
    return computeUpcomingSchedule(localTasksList, calendarEvents, tomorrowLabel).days
}

/**
 * Запоминает и вычисляет список запланированных дней (UpcomingDays) со сгруппированными задачами и событиями.
 */
@Composable
fun rememberUpcomingDays(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>
): List<UpcomingDay> {
    val tomorrowLabel = stringResource(R.string.tomorrow)
    return remember(localTasksList, calendarEvents, tomorrowLabel) {
        computeUpcomingDays(localTasksList, calendarEvents, tomorrowLabel)
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
    val tomorrowLabel = stringResource(R.string.tomorrow)
    return remember(localTasksList, calendarEvents, tomorrowLabel) {
        computeUpcomingSchedule(localTasksList, calendarEvents, tomorrowLabel)
    }
}

/** Вид заголовка секции в результатах поиска. */
enum class SearchSectionKind { TAGS, PROJECT, AREA, LOGBOOK }

/**
 * Заголовок секции результатов поиска: найденные теги, задачи конкретного проекта или области, Logbook.
 * Найденные проекты и области идут строками без заголовка.
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
    val headerTags = stringResource(R.string.tag_dialog_title)
    val headerLogbook = stringResource(R.string.category_logbook)
    return remember(screen, standardToday, eveningToday, headerTags, headerLogbook, draggedItemKey, upcomingDays, upcomingMonths, projects, area, displayTasks, isLaterItemsHidden, tag, allTasks, areas, savedTags, searchQuery, headings) {
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
                    addAll(areaProjects)
                }
                val areaDirectTasks = displayTasks.filter { it.item.areaId == area?.id && (it.item.projectId == null || it.item.projectId == "") }
                val bounds = DayBounds.now()
                val currentTasks = areaDirectTasks.filter { !bounds.isUpcoming(it.item) && it.item.start != Item.START_SOMEDAY }
                val upcomingTasks = areaDirectTasks.filter { bounds.isUpcoming(it.item) && it.item.start != Item.START_SOMEDAY }
                val somedayTasks = areaDirectTasks.filter { it.item.start == Item.START_SOMEDAY }

                if (areaProjects.isNotEmpty() && (currentTasks.isNotEmpty() || upcomingTasks.isNotEmpty() || somedayTasks.isNotEmpty())) {
                    add(TaskListKeys.AREA_PROJECTS_SPACER)
                }

                addAll(currentTasks)

                val hasLaterItems = upcomingTasks.isNotEmpty() || somedayTasks.isNotEmpty()
                if (hasLaterItems) {
                    if (!isLaterItemsHidden) {
                        if (upcomingTasks.isNotEmpty()) {
                            add(TaskListKeys.AREA_UPCOMING_HEADING)
                            addAll(upcomingTasks)
                        }
                        if (somedayTasks.isNotEmpty()) {
                            add(TaskListKeys.AREA_SOMEDAY_HEADING)
                            addAll(somedayTasks)
                        }
                    }
                    add(TaskListKeys.AREA_LATER_TOGGLE)
                }
            } else if (screen == ActiveScreen.TAG_DETAIL) {
                val tagTitle = tag?.title ?: ""
                val tagProjects = projects.filter { !it.trashed && !it.isCompleted && it.status != Item.STATUS_CANCELLED && it.tags.contains(tagTitle) }
                if (tagProjects.isNotEmpty()) {
                    addAll(tagProjects)
                }
                val activeTagTasks = displayTasks.filter { !it.item.trashed && !it.item.isCompleted && it.item.status != Item.STATUS_CANCELLED }
                if (tagProjects.isNotEmpty() && activeTagTasks.isNotEmpty()) {
                    add(TaskListKeys.AREA_PROJECTS_SPACER)
                }
                addAll(activeTagTasks)

                val logbookTasks = allTasks.filter { !it.item.trashed && (it.item.isCompleted || it.item.status == Item.STATUS_CANCELLED) && it.item.tags.contains(tagTitle) }
                    .sortedByDescending { it.item.stopDate ?: it.item.modificationDate }
                if (logbookTasks.isNotEmpty()) {
                    addAll(logbookTasks)
                }
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
            } else if (screen == ActiveScreen.SEARCH) {
                addSearchResults(displayTasks, projects, areas, savedTags, searchQuery, headerTags, headerLogbook)
            } else {
                addAll(displayTasks)
            }
        }
    }
}

/**
 * Результаты поиска: открытые задачи без проекта и области, найденные проекты, области и теги,
 * открытые задачи по проектам и областям — в порядке главного экрана, затем выполненное (Logbook).
 */
private fun MutableList<Any>.addSearchResults(
    matchedTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    savedTags: List<Tag>,
    searchQuery: String,
    headerTags: String,
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

    // 2. Найденные открытые проекты и области — строками без заголовка секции, затем теги
    val matchedProjects = projects.filter {
        it.type == Item.TYPE_PROJECT && !it.trashed && !it.isDone() &&
            (it.title.contains(q, ignoreCase = true) || it.notes.contains(q, ignoreCase = true))
    }
    addAll(matchedProjects)
    val matchedAreas = areas.filter { !it.trashed && it.title.contains(q, ignoreCase = true) }
    addAll(matchedAreas.map { SearchAreaItem(it) })
    val matchedTags = savedTags.filter { it.title.isNotBlank() && it.title.contains(q, ignoreCase = true) }
    if (matchedTags.isNotEmpty()) {
        add(SearchSectionHeaderItem("search_hdr_tags", headerTags, SearchSectionKind.TAGS))
        addAll(matchedTags.map { SearchTagItem(it) })
    }

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

