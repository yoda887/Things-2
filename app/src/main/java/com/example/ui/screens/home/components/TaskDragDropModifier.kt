package com.example.ui.screens.home.components

import androidx.compose.ui.Modifier
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop
import java.util.Calendar

private const val MS_PER_DAY = 24 * 3600 * 1000L
private const val TASK_SCROLL_TOP_ZONE_FRACTION = 0.22f
private const val TASK_SCROLL_BOTTOM_ZONE_FRACTION = 0.18f


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
    selectedTaskIds: Set<String> = emptySet(),
    onExitSelectionMode: () -> Unit = {},
): Modifier {
    val lazyListState = state.lazyListState
    val taskItem = taskWrapper.item

    return this.universalDragAndDrop(
        state = state,
        key = taskItem.id,
        topScrollZoneFraction = TASK_SCROLL_TOP_ZONE_FRACTION,
        bottomScrollZoneFraction = TASK_SCROLL_BOTTOM_ZONE_FRACTION,
        canDropOver = { targetKey ->
            val keyStr = targetKey as? String
            when {
                // Элементы списка без явного ключа — не цели
                keyStr == null -> false
                // События календаря
                keyStr.startsWith(TaskListKeys.CALENDAR_EVENT_PREFIX) -> false
                // Неинтерактивные строки шапки. Раньше они проходили фильтр и становились
                // ближайшей целью сверху, а обработчик их отклонял — задача упиралась в них
                // и не могла подняться выше
                keyStr in TaskListKeys.nonDroppable -> false
                // Задачи, а также заголовки: экрана, вечерней секции и дня
                else -> true
            }
        },
        isBlockContinuation = { targetKey ->
            // Продолжение блока — только события календаря под заголовком дня: сбрасывая задачу
            // на заголовок, её кладут ПОСЛЕ событий этого дня. Всё остальное (включая хвостовую
            // распорку списка) продолжением не является, иначе её высота попадёт в компенсацию
            // прыжка и в конце списка карточка выскочит из-под пальца вверх.
            (targetKey as? String)?.startsWith(TaskListKeys.CALENDAR_EVENT_PREFIX) == true
        },
        onMoveIfNecessary = { draggedKey, targetKey ->
            val draggedId = draggedKey as? String ?: return@universalDragAndDrop false
            val targetId = targetKey as? String ?: return@universalDragAndDrop false

            val fromIndex = localTasksList.indexOfFirst { it.item.id == draggedId }
            if (fromIndex == -1) return@universalDragAndDrop false

            // Определение направления движения на основании видимых элементов списка LazyColumn
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val draggedItemInfo = visibleItems.firstOrNull { it.key == draggedId }
            val targetItemInfo = visibleItems.firstOrNull { it.key == targetId }
            if (draggedItemInfo == null || targetItemInfo == null) return@universalDragAndDrop false
            val movingDown = targetItemInfo.index > draggedItemInfo.index

            when {
                // ПЕРЕТАСКИВАНИЕ НА ЗАГОЛОВОК "ВЕЧЕР"
                targetId == TaskListKeys.EVENING_HEADER -> {
                    val list = localTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)
                    val wasTonight = moved.item.isTonight

                    moved = moved.copyWithTonight(!wasTonight)

                    val insertAt = if (!wasTonight) {
                        list.indexOfFirst { it.item.isTonight }.takeIf { it != -1 } ?: list.size
                    } else {
                        list.indexOfFirst { it.item.isTonight }.takeIf { it != -1 } ?: list.size
                    }

                    list.add(insertAt, moved)
                    onLocalTasksListChange(list)
                    true
                }

                // ПЕРЕТАСКИВАНИЕ НА ЗАГОЛОВОК "MAIN" (Основной список дня)
                targetId == TaskListKeys.MAIN_HEADER -> {
                    val list = localTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)
                    if (!moved.item.isTonight) return@universalDragAndDrop false

                    moved = moved.copyWithTonight(false)
                    list.add(0, moved)
                    onLocalTasksListChange(list)
                    true
                }

                // ПЕРЕТАСКИВАНИЕ НА ЗАГОЛОВОК ПРЕДСТОЯЩЕГО ДНЯ (Upcoming screen)
                screen == ActiveScreen.UPCOMING && targetId.startsWith(TaskListKeys.DAY_HEADER_PREFIX) -> {
                    val timestampStr = targetId.substringAfter(TaskListKeys.DAY_HEADER_PREFIX)
                    val timestamp = timestampStr.toLongOrNull() ?: return@universalDragAndDrop false

                    val list = localTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)

                    val tomorrowStart = upcomingDays.firstOrNull()?.dateMillis ?: 0L
                    val oldDayStart = moved.item.startDate?.dayStart() ?: tomorrowStart

                    // Определение целевого дня в зависимости от направления перетаскивания (вверх/вниз)
                    val targetTimestamp = if (movingDown) {
                        timestamp
                    } else {
                        val currentDayIdx = upcomingDays.indexOfFirst { it.dateMillis.dayStart() == timestamp.dayStart() }
                        if (currentDayIdx > 0) {
                            upcomingDays[currentDayIdx - 1].dateMillis
                        } else {
                            timestamp - MS_PER_DAY
                        }
                    }

                    val clipped = maxOf(targetTimestamp, tomorrowStart)
                    val targetDayStart = clipped.dayStart()

                    if (oldDayStart == targetDayStart) return@universalDragAndDrop false

                    // Расположение задачи внутри блока целевого дня:
                    // При движении вниз — в начало списка задач целевого дня
                    // При движении вверх — в конец списка задач целевого дня
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
                    onLocalTasksListChange(list)
                    true
                }

                // ПЕРЕТАСКИВАНИЕ НА ЗАГОЛОВОК МЕСЯЦА (Upcoming screen)
                screen == ActiveScreen.UPCOMING && targetId.startsWith(TaskListKeys.MONTH_HEADER_PREFIX) -> {
                    val timestampStr = targetId.substringAfter(TaskListKeys.MONTH_HEADER_PREFIX)
                    val monthTimestamp = timestampStr.toLongOrNull() ?: return@universalDragAndDrop false

                    val list = localTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)

                    // Защита от no-op: если задача уже физически лежит в разделе этого заголовка,
                    // обмен принимать не нужно (аналог oldDayStart == targetDayStart в ветке дней).
                    // Сравнение по (год, месяц) само по себе неверно для первого раздела после
                    // 14-дневного горизонта — это не целый месяц, а его остаток, и monthTimestamp
                    // для него не 1-е число, а конец горизонта. Задача из тех же календарных суток,
                    // но ещё внутри 14 дней, принадлежит разделу «дни», а не этому заголовку —
                    // поэтому условие "уже в этом разделе" дополнительно требует oldStart >= horizonEnd.
                    // Только при движении вниз: вверх через заголовок своего раздела задача как раз
                    // уходит в предыдущий раздел. Раньше проверка срабатывала в обе стороны, и задачу
                    // нельзя было поднять из месяца выше его заголовка — слот оставался внизу,
                    // уезжал за экран, и автопрокрутка вставала.
                    val horizonEndMillis = upcomingDays.lastOrNull()?.let { it.dateMillis + MS_PER_DAY } ?: 0L
                    val oldStart = moved.item.startDate
                    if (movingDown && oldStart != null && oldStart >= horizonEndMillis) {
                        val oldCal = Calendar.getInstance().apply { timeInMillis = oldStart }
                        val targetCal = Calendar.getInstance().apply { timeInMillis = monthTimestamp }
                        val alreadyInThisSection = oldCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
                            && oldCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH)
                        if (alreadyInThisSection) return@universalDragAndDrop false
                    }

                    val targetDateMillis = if (movingDown) {
                        // При движении вниз — задача помещается в начало месяца или периода (после событий календаря)
                        Calendar.getInstance().apply {
                            timeInMillis = monthTimestamp
                            set(Calendar.HOUR_OF_DAY, 12)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    } else {
                        // При движении вверх через заголовок месяца — задача переходит в предшествующий раздел
                        val prevCal = Calendar.getInstance().apply {
                            timeInMillis = monthTimestamp
                            add(Calendar.DAY_OF_MONTH, -1)
                            set(Calendar.HOUR_OF_DAY, 12)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val tomorrowStart = upcomingDays.firstOrNull()?.dateMillis ?: 0L
                        maxOf(prevCal.timeInMillis, tomorrowStart)
                    }

                    val insertAt = list.indexOfFirst { it.item.startDate != null && it.item.startDate!! >= monthTimestamp }
                        .takeIf { it != -1 } ?: list.size

                    moved = moved.copyWithStartDate(targetDateMillis)
                    moved = moved.copy(item = moved.item.copy(sortOrder = if (movingDown) -1 else 99999))

                    list.add(insertAt, moved)
                    onLocalTasksListChange(list)
                    true
                }

                // КЛАССИЧЕСКИЙ ОБМЕН ДВУХ ЗАДАЧ (Swap)
                else -> {
                    val toIndex = localTasksList.indexOfFirst { it.item.id == targetId }
                    if (toIndex == -1 || toIndex == fromIndex) return@universalDragAndDrop false

                    val list = localTasksList.toMutableList()
                    var moved = list.removeAt(fromIndex)

                    val hoveredTask = localTasksList.firstOrNull { it.item.id == targetId }
                    if (hoveredTask != null) {
                        // Изменяем статус "Вечер" только на экране "Сегодня" (ActiveScreen.TODAY)
                        if (screen == ActiveScreen.TODAY && hoveredTask.item.isTonight != moved.item.isTonight)
                            moved = moved.copyWithTonight(hoveredTask.item.isTonight)
                        if (screen == ActiveScreen.UPCOMING && hoveredTask.item.startDate != moved.item.startDate)
                            moved = moved.copyWithStartDate(hoveredTask.item.startDate)
                    }

                    list.add(toIndex, moved)
                    onLocalTasksListChange(list)
                    true
                }
            }
        },
        onDragStarted = {
            // Если перетаскиваемая задача входит в число выбранных задач:
            if (selectedTaskIds.contains(taskItem.id) && selectedTaskIds.size > 1) {
                // Ведущая задача первая, остальные выбранные - следом
                val otherSelectedIds = selectedTaskIds.filter { it != taskItem.id }
                state.stackedDragKeys = listOf(taskItem.id) + otherSelectedIds
            } else {
                state.stackedDragKeys = emptyList()
            }
            // Режим выбора автоматически завершается при начале перетаскивания (включая одиночно выбранную задачу)
            if (selectedTaskIds.isNotEmpty()) {
                onExitSelectionMode()
            }
        },
        onDragEnd = {
            val isBatch = state.stackedDragKeys.size > 1
            val batchKeys = state.stackedDragKeys.mapNotNull { it as? String }

            val listWithBatchOrdered = if (isBatch && batchKeys.contains(taskItem.id)) {
                val otherBatchKeys = batchKeys.filter { it != taskItem.id }.toSet()
                val remainingList = localTasksList.filterNot { otherBatchKeys.contains(it.item.id) }.toMutableList()
                val leadingIdx = remainingList.indexOfFirst { it.item.id == taskItem.id }

                if (leadingIdx != -1) {
                    val leadingTask = remainingList[leadingIdx]
                    val targetTonight = leadingTask.item.isTonight
                    val targetStartDate = leadingTask.item.startDate

                    val orderedOtherBatch = batchKeys.drop(1).mapNotNull { key ->
                        localTasksList.firstOrNull { it.item.id == key }
                    }.map { wrapper ->
                        var updated = wrapper
                        if (updated.item.isTonight != targetTonight) {
                            updated = updated.copyWithTonight(targetTonight)
                        }
                        if (updated.item.startDate != targetStartDate) {
                            updated = updated.copyWithStartDate(targetStartDate)
                        }
                        updated
                    }
                    remainingList.addAll(leadingIdx + 1, orderedOtherBatch)
                    remainingList
                } else {
                    localTasksList
                }
            } else {
                localTasksList
            }

            // Перерасчет окончательных порядковых индексов (sortOrder) для сохранения изменений
            val updatedList = listWithBatchOrdered.mapIndexed { index, wrapper ->
                val original = filteredTasks.firstOrNull { it.item.id == wrapper.item.id }
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
                val original = filteredTasks.firstOrNull { it.item.id == wrapper.item.id }
                original == null
                    || original.item.sortOrder != wrapper.item.sortOrder
                    || original.item.isTonight != wrapper.item.isTonight
                    || original.item.startDate != wrapper.item.startDate
            }

            onLocalTasksListChange(updatedList)
            if (changedTasks.isNotEmpty()) onTasksReordered(changedTasks.map { it.item })
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
