package com.example.ui.screens.home.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.reorderableItem
import com.example.ui.components.dragdrop.detectItemSpacing
import kotlinx.coroutines.launch
import java.util.Calendar

private const val MS_PER_DAY = 24 * 3600 * 1000L
private const val MOVE_THRESHOLD = 0.5f

/**
 * Обертка над универсальным reorderableItem для привязки бизнес-логики перетаскивания задач.
 * Обрабатывает разделение задач по заголовкам (Сегодня, Вечер, Предстоящие дни) и выравнивание индексов.
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

    // Ссылка на lazyListState получается напрямую из GenericDragDropState
    val lazyListState = state.lazyListState

    val currentTaskWrapper by rememberUpdatedState(taskWrapper)
    val currentUpcomingDays by rememberUpdatedState(upcomingDays)
    val currentLocalTasksList by rememberUpdatedState(localTasksList)
    val currentFilteredTasks by rememberUpdatedState(filteredTasks)
    val currentOnLocalTasksListChange by rememberUpdatedState(onLocalTasksListChange)
    val currentOnTasksReordered by rememberUpdatedState(onTasksReordered)
    val currentScreen by rememberUpdatedState(screen)

    val taskItem = currentTaskWrapper.item
    val coroutineScope = rememberCoroutineScope()

    // ── Swap logic ────────────────────────────────────────────────────────────

    /**
     * Обрабатывает перетаскивание задачи на заголовок "Evening".
     */
    fun handleEveningHeaderDrop(
        fromIndex: Int,
        hoveredItem: androidx.compose.foundation.lazy.LazyListItemInfo,
        draggedItemInfo: androidx.compose.foundation.lazy.LazyListItemInfo,
    ) {
        val list = currentLocalTasksList.toMutableList()
        var moved = list.removeAt(fromIndex)
        val wasTonight = moved.item.isTonight

        moved = moved.copyWithTonight(!wasTonight)

        val insertAt = if (!wasTonight) {
            // Перемещение в вечерний список: ставится перед первым вечерним элементом или в конец.
            list.indexOfFirst { it.item.isTonight }.takeIf { it != -1 } ?: list.size
        } else {
            // Перемещение из вечернего списка: в ту же точку.
            list.indexOfFirst { it.item.isTonight }.takeIf { it != -1 } ?: list.size
        }

        list.add(insertAt, moved)
        currentOnLocalTasksListChange(list)

        val distance: Float = if (!wasTonight) {
            ((hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset - draggedItemInfo.size).toFloat()
        } else {
            if (hoveredItem.offset > draggedItemInfo.offset) hoveredItem.size.toFloat()
            else -hoveredItem.size.toFloat()
        }
        coroutineScope.launch {
            state.adjustOffset(-distance)
        }
    }

    /**
     * Обрабатывает перетаскивание задачи на заголовок "Main".
     */
    fun handleMainHeaderDrop(
        fromIndex: Int,
        hoveredItem: androidx.compose.foundation.lazy.LazyListItemInfo,
        draggedItemInfo: androidx.compose.foundation.lazy.LazyListItemInfo,
    ) {
        val list = currentLocalTasksList.toMutableList()
        var moved = list.removeAt(fromIndex)
        if (!moved.item.isTonight) return  // Уже в основном; ничего делать не нужно.

        moved = moved.copyWithTonight(false)
        list.add(0, moved)
        currentOnLocalTasksListChange(list)

        val distance: Float = ((hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset).toFloat()
        coroutineScope.launch {
            state.adjustOffset(-distance)
        }
    }

    /**
     * Обрабатывает перетаскивание задачи на заголовок дня в режиме "Upcoming".
     */
    fun handleUpcomingHeaderDrop(
        fromIndex: Int,
        hoveredKey: String,
        hoveredItem: androidx.compose.foundation.lazy.LazyListItemInfo,
        draggedItemInfo: androidx.compose.foundation.lazy.LazyListItemInfo,
        dragCenterY: Float,
    ) {
        val isEv = hoveredKey.startsWith("ev_")
        val timestampStr =
            if (!isEv) hoveredKey.substringAfter("hdr_")
            else hoveredKey.substringAfterLast("_")
        val timestamp = timestampStr.toLongOrNull() ?: return

        val list = currentLocalTasksList.toMutableList()
        var moved = list.removeAt(fromIndex)

        val tomorrowStart = currentUpcomingDays.firstOrNull()?.dateMillis ?: 0L
        val oldDayStart = moved.item.startDate?.dayStart() ?: tomorrowStart

        val headerTop = hoveredItem.offset.toFloat()
        val headerBottom = (hoveredItem.offset + hoveredItem.size).toFloat()

        val targetTimestamp: Long = when {
            isEv -> timestamp
            oldDayStart == timestamp -> if (dragCenterY < headerTop) timestamp - MS_PER_DAY else timestamp
            oldDayStart < timestamp -> if (dragCenterY > headerTop) timestamp else oldDayStart
            else -> when {                          // oldDayStart > timestamp
                dragCenterY < headerTop -> timestamp - MS_PER_DAY
                dragCenterY <= headerBottom -> timestamp
                else -> oldDayStart
            }
        }

        val clipped = maxOf(targetTimestamp, tomorrowStart)
        val targetDayStart = clipped.dayStart()

        if (oldDayStart == targetDayStart) return  // Нет смены даты; ничего менять не нужно.

        // Направление движения пальца: истинно, если тащим вниз, ложно, если тащим вверх.
        val movingDown = hoveredItem.offset > draggedItemInfo.offset

        // Умный расчет позиции вставки (insertAt) в зависимости от направления перетаскивания:
        // - При движении вниз задача добавляется в самое начало целевого дня. Это соответствует физическому
        //   интуитивному поведению, когда задача «затыкает» день сверху при перетягивании на заголовок.
        // - При движении вверх задача помещается в самый конец целевого дня. Таким образом, мы исключаем
        //   хаотичные скачки («прыжки» элементов) и аккуратно выстраиваем логику следования элементов.
        val insertAt = if (movingDown) {
            list.indexOfFirst { it.item.startDate != null && it.item.startDate!! >= targetDayStart }
                .takeIf { it != -1 } ?: list.size
        } else {
            val nextDayStart = targetDayStart + MS_PER_DAY
            val nextDayFirst = list.indexOfFirst { it.item.startDate != null && it.item.startDate!! >= nextDayStart }
            if (nextDayFirst != -1) nextDayFirst else list.size
        }

        // Стратегия копирования точного времени соседа (referenceTask) для предотвращения «прыжков» во времени.
        // Берем соседа в соответствии со стороной приземления, чтобы перенять его часы/минуты.
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

        // Присвоение временного экстремального значения sortOrder:
        // - При движении вниз выдаем значение -1, гарантируя, что задача превентивно встанет наверх.
        // - При движении вверх выдаем значение 99999, позиционируя ее в конец целевого блока дня.
        // Это запретит Jetpack Compose перебрасывать задачу в начало списка при равном значении startDate
        // и согласует логическую позицию с физическим положением пальца на экране.
        moved = moved.copyWithStartDate(finalStartDate)
        moved = moved.copy(item = moved.item.copy(sortOrder = if (movingDown) -1 else 99999))

        list.add(insertAt, moved)
        currentOnLocalTasksListChange(list)

        // Точный расчет физического смещения (компенсация высоты пропущенных элементов)
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val spacing = detectItemSpacing(visibleItems)
        var distanceShifted = 0f

        for (item in visibleItems) {
            if (item.key == taskItem.id) continue
            val key = item.key as? String ?: continue

            val isBypassed = when {
                key.startsWith("hdr_") -> {
                    val ts = key.substringAfter("hdr_").toLongOrNull() ?: 0L
                    if (movingDown) ts <= targetDayStart else ts > targetDayStart
                }
                key.startsWith("ev_") -> {
                    val ts = key.substringAfterLast("_").toLongOrNull() ?: 0L
                    if (movingDown) ts <= targetDayStart else ts > targetDayStart
                }
                else -> { // Это другая задача
                    val idx = list.indexOfFirst { it.item.id == key }
                    if (movingDown) (idx != -1 && idx < insertAt) else (idx != -1 && idx > insertAt)
                }
            }

            if (isBypassed) {
                if (movingDown && item.index > draggedItemInfo.index) {
                    distanceShifted += item.size + spacing
                } else if (!movingDown && item.index < draggedItemInfo.index) {
                    distanceShifted -= (item.size + spacing)
                }
            }
        }

        coroutineScope.launch {
            state.adjustOffset(-distanceShifted)
        }
    }

    /**
     * Обрабатывает изменение порядка задач (swap) при наведении на другую задачу.
     */
    fun handleTaskSwap(
        fromIndex: Int,
        hoveredItem: androidx.compose.foundation.lazy.LazyListItemInfo,
        draggedItemInfo: androidx.compose.foundation.lazy.LazyListItemInfo,
        spacing: Float,
    ) {
        val toIndex = currentLocalTasksList.indexOfFirst { it.item.id == hoveredItem.key }
        if (toIndex == -1) return

        val list = currentLocalTasksList.toMutableList()
        var moved = list.removeAt(fromIndex)

        val hoveredTask = currentLocalTasksList.firstOrNull { it.item.id == hoveredItem.key }
        if (hoveredTask != null) {
            if (hoveredTask.item.isTonight != moved.item.isTonight)
                moved = moved.copyWithTonight(hoveredTask.item.isTonight)
            if (currentScreen == ActiveScreen.UPCOMING && hoveredTask.item.startDate != moved.item.startDate)
                moved = moved.copyWithStartDate(hoveredTask.item.startDate)
        }

        list.add(toIndex, moved)
        currentOnLocalTasksListChange(list)

        val distance = if (hoveredItem.offset > draggedItemInfo.offset) {
            hoveredItem.size.toFloat() + spacing
        } else {
            -(hoveredItem.size.toFloat() + spacing)
        }
        coroutineScope.launch {
            state.adjustOffset(-distance)
        }
    }

    /**
     * Проверяет положение перетаскиваемого элемента и инициирует перемещение (swap/drop)
     * при наложении на другие элементы списка.
     */
    fun checkSwap() {
        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        val draggedItemInfo = visibleItems.firstOrNull { it.key == taskItem.id } ?: return
        val dragTop = draggedItemInfo.offset + state.dragAccumulatedOffset.value
        val dragBottom = dragTop + draggedItemInfo.size
        val dragCenterY = dragTop + draggedItemInfo.size / 2f

        val hoveredItem = visibleItems
            .filter { item ->
                val key = item.key as? String ?: return@filter false
                if (key == taskItem.id) return@filter false

                val overlapTop = maxOf(dragTop, item.offset.toFloat())
                val overlapBottom = minOf(dragBottom, (item.offset + item.size).toFloat())
                val overlapAmount = overlapBottom - overlapTop

                overlapAmount > (item.size * MOVE_THRESHOLD)
            }
            .maxByOrNull { candidate ->
                val overlapTop = maxOf(dragTop, candidate.offset.toFloat())
                val overlapBottom = minOf(dragBottom, (candidate.offset + candidate.size).toFloat())
                overlapBottom - overlapTop
            } ?: return

        val hoveredKey = hoveredItem.key as? String ?: return
        val fromIndex = currentLocalTasksList.indexOfFirst { it.item.id == taskItem.id }
        if (fromIndex == -1) return

        when {
            hoveredKey == "evening_header" ->
                handleEveningHeaderDrop(fromIndex, hoveredItem, draggedItemInfo)

            hoveredKey == "main_header" ->
                handleMainHeaderDrop(fromIndex, hoveredItem, draggedItemInfo)

            currentScreen == ActiveScreen.UPCOMING &&
                (hoveredKey.startsWith("hdr_") || hoveredKey.startsWith("ev_")) ->
                handleUpcomingHeaderDrop(fromIndex, hoveredKey, hoveredItem, draggedItemInfo, dragCenterY)

            else -> {
                val spacing = detectItemSpacing(visibleItems)
                handleTaskSwap(fromIndex, hoveredItem, draggedItemInfo, spacing)
            }
        }
    }

    this.reorderableItem(
        state = state,
        key = taskItem.id,
        onDragStart = {
            // Резерв для обработки внешних событий начала перетаскивания
        },
        onDrag = { _ ->
            checkSwap()
        },
        onDragged = {
            checkSwap()
        },
        onDragEnd = { runWithAnimation ->
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

            // Запуск возвращающей анимации
            runWithAnimation()
        },
        onDragCancel = { runWithAnimation ->
            // Запуск возвращающей анимации
            runWithAnimation()
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
