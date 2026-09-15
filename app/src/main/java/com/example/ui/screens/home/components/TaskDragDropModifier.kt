package com.example.ui.screens.home.components

import androidx.compose.ui.Modifier
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop

/** Полосы у краёв списка, в которых перетаскивание включает автопрокрутку, в долях высоты экрана */
private const val TASK_SCROLL_TOP_ZONE_FRACTION = 0.14f
private const val TASK_SCROLL_BOTTOM_ZONE_FRACTION = 0.12f


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

            // Направление жеста знает только геометрия списка LazyColumn; куда именно встанет
            // задача, решает чистая функция planTaskDrop — её правила закреплены тестами
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val draggedItemInfo = visibleItems.firstOrNull { it.key == draggedId }
                ?: return@universalDragAndDrop false
            val targetItemInfo = visibleItems.firstOrNull { it.key == targetId }
                ?: return@universalDragAndDrop false
            val movingDown = targetItemInfo.index > draggedItemInfo.index

            val reordered = planTaskDrop(
                list = localTasksList,
                draggedId = draggedId,
                targetId = targetId,
                screen = screen,
                movingDown = movingDown,
                upcomingDayStarts = upcomingDays.map { it.dateMillis },
            ) ?: return@universalDragAndDrop false

            onLocalTasksListChange(reordered)
            true
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
