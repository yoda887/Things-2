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
 * Вычисляет список запланированных дней (UpcomingDays) со сгруппированными задачами и событиями.
 */
fun computeUpcomingDays(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>
): List<UpcomingDay> {
    val daysList = mutableListOf<UpcomingDay>()
    
    // Начало завтрашнего дня в миллисекундах
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val tomorrowStart = cal.timeInMillis
    
    for (offset in 0 until 14) {
        val c = Calendar.getInstance()
        c.timeInMillis = tomorrowStart
        c.add(Calendar.DAY_OF_YEAR, offset)
        val dayStart = c.timeInMillis
        
        val dayEnd = Calendar.getInstance().apply {
            timeInMillis = dayStart
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        // Находим все календарные события на этот день
        val dayEvents = calendarEvents.filter { event ->
            event.eventStartMillis != null && event.eventStartMillis in dayStart..dayEnd
        }.map { ItemWithChecklist(item = it, checklist = emptyList()) }
        
        // Находим все задачи на этот день
        val dayTasks = localTasksList.filter { wrapper ->
            wrapper.item.startDate != null && wrapper.item.startDate in dayStart..dayEnd
        }
        
        if (dayEvents.isNotEmpty() || dayTasks.isNotEmpty()) {
            val dayOfMonthLabel = Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_MONTH).toString()
            val dayOfWeekLabel = if (offset == 0) {
                "Tomorrow"
            } else if (offset < 6) {
                SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date(dayStart))
            } else {
                SimpleDateFormat("MMMM", Locale.ENGLISH).format(Date(dayStart))
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
                    add("evening_header")
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
                    add("projects_heading")
                    addAll(areaProjects)
                }
                val areaDirectTasks = displayTasks.filter { it.item.areaId == area?.id && (it.item.projectId == null || it.item.projectId == "") }
                if (areaDirectTasks.isNotEmpty()) {
                    add("tasks_heading")
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
    val standardToday: List<ItemWithChecklist> = emptyList(),
    val eveningToday: List<ItemWithChecklist> = emptyList(),
    val upcomingDays: List<UpcomingDay> = emptyList(),
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
    val projectProgressMap: Map<String, ProjectProgress> = emptyMap()
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

    // Свайп-события (заглушки для будущей реализации мультиселекции и When/календаря)
    data class SwipeTaskLeft(val task: ItemWithChecklist) : ThingsCategoryListEvent
    data class SwipeTaskRight(val task: ItemWithChecklist) : ThingsCategoryListEvent
}

