package com.example.ui.screens.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

// ─── Constants ───────────────────────────────────────────────────────────────

private const val SCROLL_FRAME_MS = 16L
private const val SCROLL_ZONE_FRACTION = 0.15f
private const val SCROLL_MAX_PX = 20f
/** Scroll speed ramps up over this window, then is capped — mirrors AOSP's 2 000 ms limit. */
private const val DRAG_SCROLL_ACCELERATION_LIMIT_MS = 2_000f
private const val MS_PER_DAY = 24 * 3600 * 1000L
private const val MOVE_THRESHOLD = 0.5f

// ─── State ────────────────────────────────────────────────────────────────────

/**
 * Encapsulates mutable drag-and-drop state for a task list using Animatable.
 */
class TaskDragDropState {
    var draggedTaskId by mutableStateOf<String?>(null)
    /** Флаг указывает, продолжает ли пользователь удерживать палец на экране во время перетаскивания. */
    var isInteracting by mutableStateOf(false)
    /** Накопленный оффсет смещения с использованием Animatable для плавного возврата. */
    val dragAccumulatedOffset = Animatable(0f)
    /** Timestamp when edge-scroll started; Long.MIN_VALUE means "not scrolling". */
    var dragScrollStartMs by mutableStateOf(Long.MIN_VALUE)
}

@Composable
fun rememberTaskDragDropState(): TaskDragDropState = remember { TaskDragDropState() }

// ─── Modifier ─────────────────────────────────────────────────────────────────

/**
 * Attaches long-press drag-and-drop behaviour to a task item.
 *
 * Dragging a task over a header reassigns its [Item.isTonight] / [Item.startDate];
 * dragging over another task swaps their positions. Auto-scroll activates when
 * the dragged item approaches the top or bottom of the viewport.
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
    onTasksReordered: (List<Item>) -> Unit,
): Modifier = composed {

    // Pin each lambda param to its latest value so closures never go stale.
    val currentTaskWrapper by rememberUpdatedState(taskWrapper)
    val currentLazyListState by rememberUpdatedState(lazyListState)
    val currentUpcomingDays by rememberUpdatedState(upcomingDays)
    val currentLocalTasksList by rememberUpdatedState(localTasksList)
    val currentFilteredTasks by rememberUpdatedState(filteredTasks)
    val currentOnLocalTasksListChange by rememberUpdatedState(onLocalTasksListChange)
    val currentOnTasksReordered by rememberUpdatedState(onTasksReordered)
    val currentScreen by rememberUpdatedState(screen)

    val haptic = LocalHapticFeedback.current
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
            // Moving into evening: place before first evening item (or at end).
            list.indexOfFirst { it.item.isTonight }.takeIf { it != -1 } ?: list.size
        } else {
            // Moving out of evening: same insertion point.
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
            state.dragAccumulatedOffset.snapTo(state.dragAccumulatedOffset.value - distance)
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
        if (!moved.item.isTonight) return  // Already in main; nothing to do.

        moved = moved.copyWithTonight(false)
        list.add(0, moved)
        currentOnLocalTasksListChange(list)

        val distance: Float = ((hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset).toFloat()
        coroutineScope.launch {
            state.dragAccumulatedOffset.snapTo(state.dragAccumulatedOffset.value - distance)
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

        if (oldDayStart == targetDayStart) return  // No date change; nothing to reorder.

        moved = moved.copyWithStartDate(
            Calendar.getInstance().apply {
                timeInMillis = clipped
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )

        val insertAt = list.indexOfFirst { it.item.startDate != null && it.item.startDate!! >= targetDayStart }
            .takeIf { it != -1 } ?: list.size

        list.add(insertAt, moved)
        currentOnLocalTasksListChange(list)

        val diff = (hoveredItem.offset - draggedItemInfo.offset).toFloat()
        coroutineScope.launch {
            state.dragAccumulatedOffset.snapTo(state.dragAccumulatedOffset.value - diff)
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
            state.dragAccumulatedOffset.snapTo(state.dragAccumulatedOffset.value - distance)
        }
    }

    /**
     * Проверяет положение перетаскиваемого элемента и инициирует перемещение (swap/drop)
     * при наложении на другие элементы списка.
     */
    fun checkSwap() {
        val layoutInfo = currentLazyListState.layoutInfo
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

    // ── Auto-scroll ───────────────────────────────────────────────────────────

    // Key on the actual dragged ID: effect restarts only when dragging starts/stops,
    // not on every recomposition (avoids the stale-bool pitfall of keying on `isDragged`).
    // Перезапуск эффекта происходит при изменении id перетаскиваемой задачи.
    val isDraggedId = state.draggedTaskId
    LaunchedEffect(isDraggedId) {
        // Цикл прокрутки активен только пока пользователь физически взаимодействует с экраном
        while (isActive && isDraggedId == taskItem.id && state.isInteracting) {
            val draggedItemInfo = currentLazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == taskItem.id }

            if (draggedItemInfo != null) {
                val dragCenterY =
                    draggedItemInfo.offset + draggedItemInfo.size / 2f + state.dragAccumulatedOffset.value
                val viewportHeight = currentLazyListState.layoutInfo.viewportSize.height.toFloat()
                val margin = viewportHeight * SCROLL_ZONE_FRACTION

                val scrollAmount = when {
                    dragCenterY < margin -> {
                        val ratio = (1f - dragCenterY / margin).coerceIn(0f, 1f)
                        val interpolatedRatio = outOfBoundsScrollCapInterpolator(ratio)
                        -interpolatedRatio * SCROLL_MAX_PX
                    }
                    dragCenterY > viewportHeight - margin -> {
                        val ratio = ((dragCenterY - (viewportHeight - margin)) / margin).coerceIn(0f, 1f)
                        val interpolatedRatio = outOfBoundsScrollCapInterpolator(ratio)
                        interpolatedRatio * SCROLL_MAX_PX
                    }
                    else -> 0f
                }

                if (scrollAmount != 0f) {
                    val now = System.currentTimeMillis()
                    if (state.dragScrollStartMs == Long.MIN_VALUE) {
                        state.dragScrollStartMs = now
                    }
                    // Quintic ease-in matches AOSP's sDragScrollInterpolator:
                    // slow at first, then accelerates hard — feels natural on long lists.
                    val elapsed = (now - state.dragScrollStartMs)
                        .coerceAtMost(DRAG_SCROLL_ACCELERATION_LIMIT_MS.toLong())
                    val timeRatio = elapsed / DRAG_SCROLL_ACCELERATION_LIMIT_MS
                    val interpolated = timeRatio * timeRatio * timeRatio * timeRatio * timeRatio
                    val finalScroll = scrollAmount * interpolated.coerceAtLeast(0.05f)
                    // Накапливаем только ту часть прокрутки, которая была реально выполнена списком (consumed).
                    // Это предотвращает "улетание" элемента за границы экрана, когда список упирается в край.
                    val consumed = currentLazyListState.scrollBy(finalScroll)
                    state.dragAccumulatedOffset.snapTo(state.dragAccumulatedOffset.value + consumed)
                    checkSwap()
                } else {
                    state.dragScrollStartMs = Long.MIN_VALUE
                }
            }
            delay(SCROLL_FRAME_MS)
        }
    }

    // ── Pointer input ─────────────────────────────────────────────────────────

    // Настраиваем обработку жестов долгого нажатия и перетаскивания с защитой от "зависаний"
    pointerInput(taskItem.id) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.draggedTaskId = taskItem.id
                // Устанавливаем флаг физического взаимодействия пользователя с экраном
                state.isInteracting = true
                coroutineScope.launch {
                    state.dragAccumulatedOffset.snapTo(0f)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                coroutineScope.launch {
                    state.dragAccumulatedOffset.snapTo(state.dragAccumulatedOffset.value + dragAmount.y)
                    checkSwap()
                }
            },
            onDragEnd = {
                // Пользователь отпустил палец — останавливаем авто-скролл немедленно
                state.isInteracting = false

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

                coroutineScope.launch {
                    try {
                        // Анимируем плавный возврат элемента на место перед окончательным сбросом draggedTaskId
                        state.dragAccumulatedOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 200)
                        )
                    } finally {
                        // Гарантируем сброс состояния драга независимо от завершения или прерывания анимации
                        state.draggedTaskId = null
                        state.dragScrollStartMs = Long.MIN_VALUE
                    }
                }
            },
            onDragCancel = {
                // Прекращаем любое взаимодействие при отмене жеста
                state.isInteracting = false
                coroutineScope.launch {
                    try {
                        state.dragAccumulatedOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 200)
                        )
                    } finally {
                        state.draggedTaskId = null
                        state.dragScrollStartMs = Long.MIN_VALUE
                    }
                }
            },
        )
    }
}

// ─── Private helpers ──────────────────────────────────────────────────────────

/**
 * Замедляющийся пятистепенной (quintic) интерполятор из AOSP для плавной автопрокрутки у границ экрана.
 */
private fun outOfBoundsScrollCapInterpolator(t: Float): Float {
    val time = t - 1f
    return time * time * time * time * time + 1f
}

/** Returns the inter-item spacing detected from visible layout info, or 0. */
private fun detectItemSpacing(
    visibleItems: List<androidx.compose.foundation.lazy.LazyListItemInfo>,
): Float {
    for (i in 0 until visibleItems.lastIndex) {
        val cur = visibleItems[i]
        val next = visibleItems[i + 1]
        if (next.index == cur.index + 1) {
            val gap = next.offset - (cur.offset + cur.size)
            if (gap >= 0) return gap.toFloat()
        }
    }
    return 0f
}

/** Midnight of the day this timestamp belongs to. */
private fun Long.dayStart(): Long = Calendar.getInstance().run {
    timeInMillis = this@dayStart
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    timeInMillis
}

// Convenience copy helpers keep call-sites concise.

private fun ItemWithChecklist.copyWithTonight(isTonight: Boolean) = ItemWithChecklist(
    item = item.copy(isTonight = isTonight),
    checklist = checklist,
)

private fun ItemWithChecklist.copyWithStartDate(startDate: Long?) = ItemWithChecklist(
    item = item.copy(startDate = startDate),
    checklist = checklist,
)