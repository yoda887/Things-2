package com.example.ui.screens.home.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import java.util.Calendar

/**
 * Класс состояния, инкапсулирующий переменные для жеста перетаскивания (Drag & Drop) задач.
 */
class TaskDragDropState {
    var draggedTaskId by mutableStateOf<String?>(null)
    var dragAccumulatedOffset by mutableStateOf(0f)
}

/**
 * Создает и запоминает экземпляр [TaskDragDropState].
 */
@Composable
fun rememberTaskDragDropState(): TaskDragDropState {
    return remember { TaskDragDropState() }
}

/**
 * Модификатор для обработки жеста перетаскивания задач и обновления их порядка сортировки.
 */
fun Modifier.taskDragAndDrop(
    state: TaskDragDropState,
    taskWrapper: ItemWithChecklist,
    lazyListState: LazyListState,
    screen: ActiveScreen,
    upcomingDays: List<UpcomingDay>,
    localTasksList: List<ItemWithChecklist>,
    filteredTasks: List<ItemWithChecklist>,
    onLocalTasksListChange: (List<ItemWithChecklist>) -> Unit,
    onTasksReordered: (List<Item>) -> Unit
): Modifier = this.composed {
    val currentTaskWrapper by rememberUpdatedState(taskWrapper)
    val currentLazyListState by rememberUpdatedState(lazyListState)
    val currentUpcomingDays by rememberUpdatedState(upcomingDays)
    val currentLocalTasksList by rememberUpdatedState(localTasksList)
    val currentFilteredTasks by rememberUpdatedState(filteredTasks)
    val currentOnLocalTasksListChange by rememberUpdatedState(onLocalTasksListChange)
    val currentOnTasksReordered by rememberUpdatedState(onTasksReordered)
    val currentScreen by rememberUpdatedState(screen)

    val taskItem = taskWrapper.item
    this.pointerInput(taskItem.id) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.draggedTaskId = taskItem.id
                state.dragAccumulatedOffset = 0f
            },
            onDrag = { change, dragAmount ->
                change.consume()
                state.dragAccumulatedOffset += dragAmount.y

                val visibleItems = currentLazyListState.layoutInfo.visibleItemsInfo
                val detectedSpacing = run {
                    var spacing = 0f
                    try {
                        spacing = currentLazyListState.layoutInfo.mainAxisItemSpacing.toFloat()
                    } catch (e: Exception) {
                        // ignore
                    }
                    if (spacing <= 0f && visibleItems.size >= 2) {
                        for (i in 0 until visibleItems.lastIndex) {
                            val current = visibleItems[i]
                            val next = visibleItems[i + 1]
                            if (next.index == current.index + 1) {
                                val gap = next.offset - (current.offset + current.size)
                                if (gap >= 0) {
                                    spacing = gap.toFloat()
                                    break
                                }
                            }
                        }
                    }
                    spacing
                }
                val draggedItemInfo = visibleItems.firstOrNull { it.key == taskItem.id }
                if (draggedItemInfo != null) {
                    val dragCenterY = draggedItemInfo.offset + draggedItemInfo.size / 2f + state.dragAccumulatedOffset
                    val hoveredItem = visibleItems.firstOrNull { item ->
                        val itemKey = item.key as? String
                        itemKey != null && itemKey != taskItem.id &&
                        dragCenterY > item.offset &&
                        dragCenterY < item.offset + item.size
                    }
                    if (hoveredItem != null) {
                        val fromIndex = currentLocalTasksList.indexOfFirst { it.item.id == taskItem.id }
                        val cleanKey = (hoveredItem.key as? String) ?: ""
                        val isHdr = cleanKey.startsWith("hdr_")
                        val isEv = cleanKey.startsWith("ev_")

                        if (hoveredItem.key == "evening_header") {
                            if (fromIndex != -1) {
                                val newList = currentLocalTasksList.toMutableList()
                                var movedItem = newList.removeAt(fromIndex)
                                if (!movedItem.item.isTonight) {
                                    movedItem = ItemWithChecklist(
                                        item = movedItem.item.copy(isTonight = true),
                                        checklist = movedItem.checklist
                                    )
                                    val firstEveningIndex = newList.indexOfFirst { it.item.isTonight }
                                    val toIndex = if (firstEveningIndex != -1) firstEveningIndex else newList.size
                                    newList.add(toIndex, movedItem)
                                    currentOnLocalTasksListChange(newList)
                                    
                                    val distance = (hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset - draggedItemInfo.size
                                    state.dragAccumulatedOffset -= distance
                                } else {
                                    movedItem = ItemWithChecklist(
                                        item = movedItem.item.copy(isTonight = false),
                                        checklist = movedItem.checklist
                                    )
                                    val firstEveningIndex = newList.indexOfFirst { it.item.isTonight }
                                    val toIndex = if (firstEveningIndex != -1) firstEveningIndex else newList.size
                                    newList.add(toIndex, movedItem)
                                    currentOnLocalTasksListChange(newList)
                                    
                                    val distance = if (hoveredItem.offset > draggedItemInfo.offset) {
                                        hoveredItem.size.toFloat()
                                    } else {
                                        -hoveredItem.size.toFloat()
                                    }
                                    state.dragAccumulatedOffset -= distance
                                }
                            }
                        } else if (hoveredItem.key == "main_header") {
                            if (fromIndex != -1) {
                                val newList = currentLocalTasksList.toMutableList()
                                var movedItem = newList.removeAt(fromIndex)
                                if (movedItem.item.isTonight) {
                                    movedItem = ItemWithChecklist(
                                        item = movedItem.item.copy(isTonight = false),
                                        checklist = movedItem.checklist
                                    )
                                    val toIndex = 0
                                    newList.add(toIndex, movedItem)
                                    currentOnLocalTasksListChange(newList)
                                    
                                    val distance = (hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset
                                    state.dragAccumulatedOffset -= distance
                                }
                            }
                        } else if (currentScreen == ActiveScreen.UPCOMING && (isHdr || isEv)) {
                            val timestampStr = if (isHdr) cleanKey.substringAfter("hdr_") else cleanKey.substringAfterLast("_")
                            val timestamp = timestampStr.toLongOrNull()
                            if (timestamp != null && fromIndex != -1) {
                                val newList = currentLocalTasksList.toMutableList()
                                var movedItem = newList.removeAt(fromIndex)
                                
                                val tomorrowStart = currentUpcomingDays.firstOrNull()?.dateMillis ?: 0L
                                
                                val isCurrentlyInHoveredDay = if (movedItem.item.startDate == null) {
                                    timestamp == tomorrowStart
                                } else {
                                    movedItem.item.startDate!! in timestamp..(timestamp + 24 * 3600 * 1000 - 1)
                                }
                                
                                var targetTimestamp = if (isCurrentlyInHoveredDay && draggedItemInfo.offset > hoveredItem.offset) {
                                    timestamp - 24 * 3600 * 1000L
                                } else {
                                    timestamp
                                }
                                
                                if (targetTimestamp < tomorrowStart) {
                                    targetTimestamp = tomorrowStart
                                }
                                
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = targetTimestamp
                                    set(Calendar.HOUR_OF_DAY, 12)
                                    set(Calendar.MINUTE, 0)
                                }
                                val newStartDate = cal.timeInMillis
                                
                                val oldStartDateDayStart = if (movedItem.item.startDate == null) {
                                    tomorrowStart
                                } else {
                                    Calendar.getInstance().apply {
                                        timeInMillis = movedItem.item.startDate!!
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                }
                                
                                val targetDayStart = Calendar.getInstance().apply {
                                    timeInMillis = targetTimestamp
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                
                                if (oldStartDateDayStart != targetDayStart) {
                                    movedItem = ItemWithChecklist(
                                        item = movedItem.item.copy(startDate = newStartDate),
                                        checklist = movedItem.checklist
                                    )
                                    
                                    var toIndex = newList.indexOfFirst { it.item.startDate != null && it.item.startDate!! >= targetDayStart }
                                    if (toIndex == -1) {
                                        toIndex = newList.size
                                    }
                                    newList.add(toIndex, movedItem)
                                    currentOnLocalTasksListChange(newList)
                                    
                                    val distance = if (hoveredItem.offset > draggedItemInfo.offset) {
                                        hoveredItem.size.toFloat()
                                    } else {
                                        -hoveredItem.size.toFloat()
                                    }
                                    state.dragAccumulatedOffset -= distance
                                }
                            }
                        } else {
                            val toIndex = currentLocalTasksList.indexOfFirst { it.item.id == hoveredItem.key }
                            if (fromIndex != -1 && toIndex != -1) {
                                val newList = currentLocalTasksList.toMutableList()
                                var movedItem = newList.removeAt(fromIndex)
                                
                                val hoveredItemTask = currentLocalTasksList.firstOrNull { it.item.id == hoveredItem.key }
                                if (hoveredItemTask != null) {
                                    if (hoveredItemTask.item.isTonight != movedItem.item.isTonight) {
                                        movedItem = ItemWithChecklist(
                                            item = movedItem.item.copy(isTonight = hoveredItemTask.item.isTonight),
                                            checklist = movedItem.checklist
                                        )
                                    }
                                    if (currentScreen == ActiveScreen.UPCOMING && movedItem.item.startDate != hoveredItemTask.item.startDate) {
                                        movedItem = ItemWithChecklist(
                                            item = movedItem.item.copy(startDate = hoveredItemTask.item.startDate),
                                            checklist = movedItem.checklist
                                        )
                                    }
                                }
                                
                                newList.add(toIndex, movedItem)
                                currentOnLocalTasksListChange(newList)
                                
                                val distance = if (hoveredItem.offset > draggedItemInfo.offset) {
                                    hoveredItem.size.toFloat() + detectedSpacing
                                } else {
                                    -(hoveredItem.size.toFloat() + detectedSpacing)
                                }
                                state.dragAccumulatedOffset -= distance
                            }
                        }
                    }
                }
            },
            onDragEnd = {
                val updatedList = currentLocalTasksList.mapIndexed { index, t ->
                    val original = currentFilteredTasks.firstOrNull { it.item.id == t.item.id }
                    if (original != null && (original.item.sortOrder != index || original.item.isTonight != t.item.isTonight || original.item.startDate != t.item.startDate)) {
                        ItemWithChecklist(
                            item = t.item.copy(sortOrder = index, modificationDate = System.currentTimeMillis()),
                            checklist = t.checklist
                        )
                    } else {
                        ItemWithChecklist(
                            item = t.item.copy(sortOrder = index),
                            checklist = t.checklist
                        )
                    }
                }
                val changedTasks = updatedList.filter { t ->
                    val original = currentFilteredTasks.firstOrNull { it.item.id == t.item.id }
                    original == null || original.item.sortOrder != t.item.sortOrder || original.item.isTonight != t.item.isTonight || original.item.startDate != t.item.startDate
                }
                currentOnLocalTasksListChange(updatedList)
                if (changedTasks.isNotEmpty()) {
                    currentOnTasksReordered(changedTasks.map { it.item })
                }
                state.draggedTaskId = null
                state.dragAccumulatedOffset = 0f
            },
            onDragCancel = {
                state.draggedTaskId = null
                state.dragAccumulatedOffset = 0f
            }
        )
    }
}
