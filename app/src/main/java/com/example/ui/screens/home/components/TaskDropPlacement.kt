package com.example.ui.screens.home.components

import com.example.data.model.DayBounds
import com.example.data.model.ItemWithChecklist
import com.example.domain.edits.DropList
import com.example.domain.edits.DropTarget
import com.example.domain.edits.TaskDrops
import com.example.ui.viewmodel.ActiveScreen

/**
 * Перенос задачи перетаскиванием: UI переводит ключ строки списка и экран в понятия домена, а куда
 * встанет задача — решает [TaskDrops]. Закреплено тестами TaskDropPlacementTest.
 *
 * @param targetId ключ цели: идентификатор задачи либо заголовок из [TaskListKeys]
 * @param upcomingDayStarts начала суток дней-заголовков экрана «Предстоящие», по порядку
 * @param bounds границы сегодняшнего дня для разделов экрана области
 * @param projectHeadingIds подзаголовки экрана проекта по порядку
 * @param todayGroupedDay дневная часть «Сегодня» сгруппирована по проектам и областям
 * @return новый порядок списка либо null, если перенос принимать не нужно
 */
fun planTaskDrop(
    list: List<ItemWithChecklist>,
    draggedId: String,
    targetId: String,
    screen: ActiveScreen,
    movingDown: Boolean,
    upcomingDayStarts: List<Long>,
    bounds: DayBounds = DayBounds.now(),
    projectHeadingIds: List<String> = emptyList(),
    todayGroupedDay: Boolean = false,
): List<ItemWithChecklist>? {
    val target = dropTargetOf(targetId) ?: return null
    return TaskDrops.plan(list, draggedId, target, dropListOf(screen, upcomingDayStarts, bounds, projectHeadingIds, todayGroupedDay), movingDown)
}

/** Цель переноса по ключу строки списка; null — ключ заголовка без даты. */
fun dropTargetOf(key: String): DropTarget? = when {
    key == TaskListKeys.MAIN_HEADER -> DropTarget.MainHeader
    key == TaskListKeys.EVENING_HEADER -> DropTarget.EveningHeader
    key.startsWith(TaskListKeys.DAY_HEADER_PREFIX) ->
        key.removePrefix(TaskListKeys.DAY_HEADER_PREFIX).toLongOrNull()?.let { DropTarget.DayHeader(it) }
    key.startsWith(TaskListKeys.MONTH_HEADER_PREFIX) ->
        key.removePrefix(TaskListKeys.MONTH_HEADER_PREFIX).toLongOrNull()?.let { DropTarget.MonthHeader(it) }
    key.startsWith(TaskListKeys.HEADING_PREFIX) -> DropTarget.Heading(key.removePrefix(TaskListKeys.HEADING_PREFIX))
    else -> DropTarget.Task(key)
}

/** Правила переноса экрана [screen]. */
fun dropListOf(
    screen: ActiveScreen,
    upcomingDayStarts: List<Long>,
    bounds: DayBounds,
    projectHeadingIds: List<String>,
    todayGroupedDay: Boolean = false,
): DropList = when (screen) {
    ActiveScreen.TODAY -> DropList.Today(todayGroupedDay)
    ActiveScreen.UPCOMING -> DropList.Upcoming(upcomingDayStarts)
    ActiveScreen.PROJECT_DETAIL -> DropList.Project(projectHeadingIds)
    ActiveScreen.AREA_DETAIL -> DropList.Area(bounds)
    ActiveScreen.ANYTIME, ActiveScreen.SOMEDAY -> DropList.ByPlace
    ActiveScreen.LOGBOOK -> DropList.Logbook
    ActiveScreen.HOME, ActiveScreen.INBOX, ActiveScreen.SEARCH, ActiveScreen.TAG_DETAIL -> DropList.Plain
    // В особых списках проектов задач нет — переставлять нечего
    ActiveScreen.ALL_PROJECTS, ActiveScreen.LOGGED_PROJECTS, ActiveScreen.LATER_PROJECTS -> DropList.Logbook
    // Шаблоны повтора стоят по ближайшему повтору — переставлять нечего
    ActiveScreen.REPEATING -> DropList.Logbook
}
