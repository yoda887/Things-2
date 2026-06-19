package com.example.ui.screens.home.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop
import kotlinx.coroutines.launch
import java.util.Calendar
import android.util.Log

private const val MS_PER_DAY = 24 * 3600 * 1000L

/**
 * Обертка над универсальным [universalDragAndDrop] для привязки бизнес-логики перетаскивания задач.
 * Инкапсулирует правила обмена элементами, перемещения между заголовками (Сегодня, Вечер, Предстоящие дни)
 * и синхронизации локального списка и базы данных.
 *
 * @param state Общее состояние перетаскивания списка [GenericDragDropState]
 * @param taskWrapper Экземпляр задачи с чек-листом для привязки к элементу списка
 * @param screen Текущий активный экран отображения задач
 * @param upcomingDays Список дней-заголовков в режиме "Предстоящие"
 * @param localTasksList Текущий локальный упорядоченный список задач в памяти
 * @param filteredTasks Отфильтрованный список задач, отображаемый на экране
 * @param onLocalTasksListChange Callback-функция, вызываемая при изменении локального списка в процессе перетаскивания
 * @param onTasksReordered Callback-функция для сохранения измененных задач в базу данных/ViewModel
 */
fun Modifier.taskDragAndDrop(
    state: GenericDragDropState,
    taskWrapper: ItemWithChecklist,
    screen: ActiveScreen,
    upcomingDays: List<UpcomingDay>,
    localTasksList: List<ItemWithChecklist>,
    filteredTasks: List<ItemWithChecklist>,
    onLocalTasksListChange: (List<ItemWithChecklist>) -> Unit,
    onTasksReordered: (List<Item>) -> Unit,
): Modifier = composed {

    val lazyListState = state.lazyListState

    val currentTaskWrapper by rememberUpdatedState(taskWrapper)
    val currentUpcomingDays by rememberUpdatedState(upcomingDays)
    val currentLocalTasksList by rememberUpdatedState(localTasksList)
    val currentFilteredTasks by rememberUpdatedState(filteredTasks)
    val currentOnLocalTasksListChange by rememberUpdatedState(onLocalTasksListChange)
    val currentOnTasksReordered by rememberUpdatedState(onTasksReordered)
    val currentScreen by rememberUpdatedState(screen)

    val taskItem = currentTaskWrapper.item

    this.universalDragAndDrop(
        state = state,
        key = taskItem.id,
        onMoveIfNecessary = { draggedKey, targetKey ->
            val draggedId = draggedKey as? String ?: return@universalDragAndDrop false
            val targetId = targetKey as? String ?: return@universalDragAndDrop false

            val fromIndex = currentLocalTasksList.indexOfFirst { it.item.id == draggedId }
            if (fromIndex == -1) return@universalDragAndDrop false

            // Определение направления движения на основании видимых элементов списка LazyColumn
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val draggedItemInfo = visibleItems.firstOrNull { it.key == draggedId }
            val targetItemInfo = visibleItems.firstOrNull { it.key == targetId }
            if (draggedItemInfo == null || targetItemInfo == null) return@universalDragAndDrop false
            val movingDown = targetItemInfo.index > draggedItemInfo.index

            when {
                // ПЕРЕТАСКИВАНИЕ НА ЗАГОЛОВОК "ВЕЧЕР"
                targetId == "evening_header" -> {
                    val list = currentLocalTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)
                    val wasTonight = moved.item.isTonight

                    moved = moved.copyWithTonight(!wasTonight)

                    val insertAt = if (!wasTonight) {
                        list.indexOfFirst { it.item.isTonight }.takeIf { it != -1 } ?: list.size
                    } else {
                        list.indexOfFirst { it.item.isTonight }.takeIf { it != -1 } ?: list.size
                    }

                    list.add(insertAt, moved)
                    currentOnLocalTasksListChange(list)
                    true
                }

                // ПЕРЕТАСКИВАНИЕ НА ЗАГОЛОВОК "MAIN" (Основной список дня)
                targetId == "main_header" -> {
                    val list = currentLocalTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)
                    if (!moved.item.isTonight) return@universalDragAndDrop false

                    moved = moved.copyWithTonight(false)
                    list.add(0, moved)
                    currentOnLocalTasksListChange(list)
                    true
                }

                // ПЕРЕТАСКИВАНИЕ НА ЗАГОЛОВОК ПРЕДСТОЯЩЕГО ДНЯ (Upcoming screen)
                currentScreen == ActiveScreen.UPCOMING && 
                        (targetId.startsWith("hdr_") || targetId.startsWith("ev_")) -> {

                    val isEv = targetId.startsWith("ev_")
                    val timestampStr = if (!isEv) targetId.substringAfter("hdr_") else targetId.substringAfterLast("_")
                    val timestamp = timestampStr.toLongOrNull() ?: return@universalDragAndDrop false

                    val list = currentLocalTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)

                    val tomorrowStart = currentUpcomingDays.firstOrNull()?.dateMillis ?: 0L
                    val oldDayStart = moved.item.startDate?.dayStart() ?: tomorrowStart

                    // Определение целевого дня в зависимости от направления перетаскивания (вверх/вниз)
                    val targetTimestamp = if (isEv) {
                        timestamp
                    } else {
                        if (movingDown) timestamp else timestamp - MS_PER_DAY
                    }

                    val clipped = maxOf(targetTimestamp, tomorrowStart)
                    val targetDayStart = clipped.dayStart()

                    if (oldDayStart == targetDayStart) return@universalDragAndDrop false

                    // Расположение задачи внутри блока целевого дня
                    val insertAt = if (movingDown) {
                        list.indexOfFirst { it.item.startDate != null && it.item.startDate!! >= targetDayStart }
                            .takeIf { it != -1 } ?: list.size
                    } else {
                        val nextDayStart = Calendar.getInstance().apply {
                            timeInMillis = targetDayStart
                            add(Calendar.DAY_OF_YEAR, 1)
                        }.timeInMillis
                        val nextDayFirst = list.indexOfFirst { it.item.startDate != null && it.item.startDate!! >= nextDayStart }
                        if (nextDayFirst != -1) nextDayFirst else list.size
                    }

                    // Копируем точное время соседа, чтобы время не прыгало хаотично
                    val referenceTask = if (movingDown) list.getOrNull(insertAt) else list.getOrNull(insertAt - 1)
                    val finalStartDate = if (referenceTask != null && referenceTask.item.startDate?.dayStart() == targetDayStart) {
                        referenceTask.item.startDate
                    } else {
                        Calendar.getInstance().apply {
                            timeInMillis = clipped
                            set(Calendar.HOUR_OF_DAY, 12)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }

                    // Устанавливаем временный sortOrder, чтобы список не дергался при равном startDate
                    moved = moved.copyWithStartDate(finalStartDate)
                    moved = moved.copy(item = moved.item.copy(sortOrder = if (movingDown) -1 else 99999))

                    list.add(insertAt, moved)
                    currentOnLocalTasksListChange(list)
                    true
                }

                    // КЛАССИЧЕСКИЙ ОБМЕН ДВУХ ЗАДАЧ (Swap)
                else -> {
                    val toIndex = currentLocalTasksList.indexOfFirst { it.item.id == targetId }
                    if (toIndex == -1) return@universalDragAndDrop false

                    val list = currentLocalTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)

                    val hoveredTask = currentLocalTasksList.firstOrNull { it.item.id == targetId }
                    if (hoveredTask != null) {
                        // Изменяем статус "Вечер" только на экране "Сегодня" (ActiveScreen.TODAY)
                        if (currentScreen == ActiveScreen.TODAY && hoveredTask.item.isTonight != moved.item.isTonight)
                            moved = moved.copyWithTonight(hoveredTask.item.isTonight)
                        if (currentScreen == ActiveScreen.UPCOMING && hoveredTask.item.startDate != moved.item.startDate)
                            moved = moved.copyWithStartDate(hoveredTask.item.startDate)
                    }

                    list.add(toIndex, moved)
                    currentOnLocalTasksListChange(list)
                    true
                }
            }
        },
        onDragEnd = {
            // Перерасчет окончательных порядковых индексов (sortOrder) для сохранения изменений
            val updatedList = currentLocalTasksList.mapIndexed { index, wrapper ->
                val original = currentFilteredTasks.firstOrNull { it.item.id == wrapper.item.id }
                val changed = original == null
                    || original.item.sortOrder != index
                    || original.item.isTonight != wrapper.item.isTonight
                    || original.item.startDate != wrapper.item.startDate
                if (changed) {
                    ItemWithChecklist(
                        item = wrapper.item.copy(
                            sortOrder = index,
                            modificationDate = System.currentTimeMillis(),
                        ),
                        checklist = wrapper.checklist,
                    )
                } else {
                    wrapper.copy(item = wrapper.item.copy(sortOrder = index))
                }
            }

            val changedTasks = updatedList.filter { wrapper ->
                val original = currentFilteredTasks.firstOrNull { it.item.id == wrapper.item.id }
                original == null
                    || original.item.sortOrder != wrapper.item.sortOrder
                    || original.item.isTonight != wrapper.item.isTonight
                    || original.item.startDate != wrapper.item.startDate
            }

            currentOnLocalTasksListChange(updatedList)
            if (changedTasks.isNotEmpty()) currentOnTasksReordered(changedTasks.map { it.item })
        }
    )
}

// ─── Private helpers ──────────────────────────────────────────────────────────

private fun Long.dayStart(): Long = Calendar.getInstance().run {
    timeInMillis = this@dayStart
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    timeInMillis
}

private fun ItemWithChecklist.copyWithTonight(isTonight: Boolean) = ItemWithChecklist(
    item = item.copy(isTonight = isTonight),
    checklist = checklist,
)

private fun ItemWithChecklist.copyWithStartDate(startDate: Long?) = ItemWithChecklist(
    item = item.copy(startDate = startDate),
    checklist = checklist,
)
