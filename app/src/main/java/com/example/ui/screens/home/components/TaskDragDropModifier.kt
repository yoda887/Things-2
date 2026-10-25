package com.example.ui.screens.home.components

import com.example.domain.edits.SortOrders
import com.example.domain.edits.TaskDrops
import com.example.ui.viewmodel.ActiveScreen
import androidx.compose.ui.Modifier
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop

/** Полосы у краёв списка, в которых перетаскивание включает автопрокрутку, в долях высоты экрана */
private const val TASK_SCROLL_TOP_ZONE_FRACTION = 0.11f
private const val TASK_SCROLL_BOTTOM_ZONE_FRACTION = 0.09f


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
    projectHeadingIds: List<String> = emptyList(),
    localTasksList: List<ItemWithChecklist>,
    filteredTasks: List<ItemWithChecklist>,
    onLocalTasksListChange: (List<ItemWithChecklist>) -> Unit,
    onTasksReordered: (List<Item>) -> Unit,
    selectedTaskIds: Set<String> = emptySet(),
    onExitSelectionMode: () -> Unit = {},
    // Дневная часть «Сегодня» сгруппирована по проектам и областям (настройка)
    todayGroupedDay: Boolean = false,
    // «Планы»: перенос в днях — общий порядок задач и проектов дня (UpcomingDragController)
    upcomingDrag: UpcomingDragController? = null,
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
            // задача, решают правила домена (TaskDrops через planTaskDrop) — они закреплены тестами
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val draggedItemInfo = visibleItems.firstOrNull { it.key == draggedId }
                ?: return@universalDragAndDrop false
            val targetItemInfo = visibleItems.firstOrNull { it.key == targetId }
                ?: return@universalDragAndDrop false
            val movingDown = targetItemInfo.index > draggedItemInfo.index

            // «Планы»: в днях — между любыми задачами и проектами дня (одна задача; группа — по прежним правилам)
            if (upcomingDrag != null && !state.hasStackedItems && upcomingDrag.move(draggedId, targetId, movingDown)) {
                return@universalDragAndDrop true
            }

            val reordered = planTaskDrop(
                list = localTasksList,
                draggedId = draggedId,
                targetId = targetId,
                screen = screen,
                movingDown = movingDown,
                upcomingDayStarts = upcomingDays.map { it.dateMillis },
                projectHeadingIds = projectHeadingIds,
                todayGroupedDay = todayGroupedDay,
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
                    // Остальные задачи группы встают вслед за ведущей — по правилу домена
                    val orderedOtherBatch = batchKeys.drop(1).mapNotNull { key ->
                        localTasksList.firstOrNull { it.item.id == key }
                    }.map { wrapper ->
                        TaskDrops.follower(wrapper, leadingTask.item, inProject = screen == ActiveScreen.PROJECT_DETAIL)
                    }
                    remainingList.addAll(leadingIdx + 1, orderedOtherBatch)
                    remainingList
                } else {
                    localTasksList
                }
            } else {
                localTasksList
            }

            // «Планы»: переносы в днях — только порядок дня и день перенесённой строки, общий порядок задач не трогаем
            if (upcomingDrag != null && upcomingDrag.hasMoves && !isBatch) {
                val saved = upcomingDrag.end()
                // Ушла в раздел месяца (там свои правила) — её новый день тоже сохранить
                val dragged = localTasksList.firstOrNull { it.item.id == taskItem.id }?.item
                val original = filteredTasks.firstOrNull { it.item.id == taskItem.id }?.item
                val extra = if (dragged != null && saved.none { it.id == dragged.id } && dragged.startDate != original?.startDate) {
                    listOf(dragged.copy(modificationDate = System.currentTimeMillis()))
                } else emptyList()
                if (saved.isNotEmpty() || extra.isNotEmpty()) onTasksReordered(saved + extra)
                return@universalDragAndDrop
            }

            // Новые sortOrder и задачи, которые надо сохранить, — по правилу домена
            val reordered = SortOrders.afterReorder(listWithBatchOrdered, filteredTasks)
            if (screen == ActiveScreen.UPCOMING && reordered.changed.isNotEmpty()) {
                // «Планы»: задачи и проекты дня — общий порядок дня (UpcomingOrder), дни с переносом нумеруются заново
                val renumbered = com.example.domain.lists.UpcomingOrder.afterDrag(
                    reordered.updated, reordered.changed,
                    upcomingDays.map { com.example.domain.lists.UpcomingOrder.DayRows(it.dateMillis, it.projects) }
                ).associateBy { it.id }
                val updated = reordered.updated.map { w -> renumbered[w.item.id]?.let { w.copy(item = w.item.copy(dayOrder = it.dayOrder)) } ?: w }
                val changedIds = reordered.changed.mapTo(HashSet()) { it.id }
                val now = System.currentTimeMillis()
                val toSave = updated.filter { it.item.id in changedIds || it.item.id in renumbered }
                    .map { w -> if (w.item.id in changedIds) w.item else w.item.copy(modificationDate = now) } +
                    renumbered.values.filter { r -> updated.none { it.item.id == r.id } }.map { it.copy(modificationDate = now) }
                onLocalTasksListChange(updated)
                onTasksReordered(toSave)
                return@universalDragAndDrop
            }
            onLocalTasksListChange(reordered.updated)
            if (reordered.changed.isNotEmpty()) onTasksReordered(reordered.changed)
        }
    )
}
