package com.example.ui.screens.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Доля высоты экрана для зон автоскролла сверху и снизу в списке проектов (10%) */
private const val PROJECTS_SCROLL_ZONE_FRACTION = 0.10f

/**
 * Обертка над универсальным [universalDragAndDrop] для привязки бизнес-логики
 * перетаскивания (Drag-and-Drop) проектов на главном экране (HomePanel).
 *
 * Поддерживает:
 * - Переупорядочивание проектов внутри области или внутри списка "Без области"
 * - Перенос проектов между областями и в список "Без области"
 * - Вход в свёрнутую область на первое место и её раскрытие при отпускании пальца
 * - Перенос проекта в пустую область
 * - Пересчёт sortOrder и сохранение изменённых проектов в базу данных
 *
 * @param state Общее состояние перетаскивания списка [GenericDragDropState]
 * @param project Проект, привязанный к текущей строке списка
 * @param originalProjects Исходный список проектов из StateFlow/ViewModel
 * @param localProjectsList Текущий локальный упорядоченный список проектов в памяти
 * @param areas Список всех доступных сфер/областей
 * @param expandedStates Карта состояний раскрытия областей (id области -> развернута/свернута)
 * @param onLocalProjectsListChange Callback при изменении локального списка в процессе перетаскивания
 * @param onProjectsReordered Callback для сохранения измененных проектов в ViewModel/БД
 * @param onExpandArea Callback для программного раскрытия свернутой области
 */
fun Modifier.projectDragAndDrop(
    state: GenericDragDropState,
    project: Item,
    originalProjects: List<Item>,
    localProjectsList: List<Item>,
    areas: List<Area>,
    expandedStates: Map<String, Boolean>,
    onLocalProjectsListChange: (List<Item>) -> Unit,
    onProjectsReordered: (List<Item>) -> Unit,
    onExpandArea: (String) -> Unit
): Modifier = composed {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val lazyListState = state.lazyListState

    val currentProject by rememberUpdatedState(project)
    val currentOriginalProjects by rememberUpdatedState(originalProjects)
    val currentLocalProjectsList by rememberUpdatedState(localProjectsList)
    val currentAreas by rememberUpdatedState(areas)
    val currentExpandedStates by rememberUpdatedState(expandedStates)
    val currentOnLocalProjectsListChange by rememberUpdatedState(onLocalProjectsListChange)
    val currentOnProjectsReordered by rememberUpdatedState(onProjectsReordered)
    val currentOnExpandArea by rememberUpdatedState(onExpandArea)

    this.universalDragAndDrop(
        state = state,
        key = "proj_${project.id}",
        topScrollZoneFraction = PROJECTS_SCROLL_ZONE_FRACTION,
        bottomScrollZoneFraction = PROJECTS_SCROLL_ZONE_FRACTION,
        canDropOver = { targetKey ->
            val keyStr = targetKey as? String ?: return@universalDragAndDrop false
            keyStr == "root_divider" || keyStr.startsWith("area_") || keyStr.startsWith("proj_")
        },
        onMoveIfNecessary = { draggedKey, targetKey ->
            val draggedKeyStr = draggedKey as? String ?: return@universalDragAndDrop false
            val targetKeyStr = targetKey as? String ?: return@universalDragAndDrop false

            if (!draggedKeyStr.startsWith("proj_")) return@universalDragAndDrop false
            val draggedId = draggedKeyStr.removePrefix("proj_")

            val fromIndex = currentLocalProjectsList.indexOfFirst { it.id == draggedId }
            if (fromIndex == -1) return@universalDragAndDrop false

            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val draggedItemInfo = visibleItems.firstOrNull { it.key == draggedKey }
            val targetItemInfo = visibleItems.firstOrNull { it.key == targetKey }
            if (draggedItemInfo == null || targetItemInfo == null) return@universalDragAndDrop false
            val movingDown = targetItemInfo.index > draggedItemInfo.index

            when {
                // ── 1. Наведение на верхний разделитель списка ("Без области") ──
                targetKeyStr == "root_divider" -> {
                    val currentProjectAreaId = currentLocalProjectsList[fromIndex].areaId
                    if (currentProjectAreaId == null) return@universalDragAndDrop false

                    val list = currentLocalProjectsList.toMutableList()
                    val moved = list.removeAt(fromIndex).copy(areaId = null)
                    list.add(0, moved)
                    currentOnLocalProjectsListChange(list)
                    true
                }

                // ── 2. Наведение на заголовок области (Area Header) ──
                targetKeyStr.startsWith("area_") -> {
                    val targetAreaId = targetKeyStr.removePrefix("area_")
                    val targetAreaIndex = currentAreas.indexOfFirst { it.id == targetAreaId }
                    if (targetAreaIndex == -1) return@universalDragAndDrop false

                    val currentProjectAreaId = currentLocalProjectsList[fromIndex].areaId

                    // 2.1. Движение ВВЕРХ через заголовок собственной области:
                    // Проект покидает текущую область и переходит в секцию выше (работает даже если область свёрнута)
                    if (currentProjectAreaId == targetAreaId) {
                        if (movingDown) {
                            return@universalDragAndDrop false
                        }

                        // Предыдущая секция: область выше или список "Без области" (null)
                        val destAreaId = if (targetAreaIndex > 0) currentAreas[targetAreaIndex - 1].id else null
                        val isDestCollapsed = destAreaId != null && currentExpandedStates[destAreaId] == false

                        val list = currentLocalProjectsList.toMutableList()
                        val moved = list.removeAt(fromIndex).copy(areaId = destAreaId)
                        // При заходе снизу вверх: если область свёрнута — встаёт в начало списка (atStart = true),
                        // если развёрнута — в конец списка (atStart = false)
                        val insertAt = findInsertionIndexForArea(list, currentAreas, destAreaId, atStart = isDestCollapsed)
                        list.add(insertAt, moved)
                        currentOnLocalProjectsListChange(list)
                        return@universalDragAndDrop true
                    }

                    // 2.2. Наведение на ДРУГУЮ область (вход в чужую область):
                    // Если целевая область свёрнута — проект всегда становится на первое место (atStart = true),
                    // чтобы при отпускании пальца и раскрытии области проект оказался самым первым в списке.
                    // Если развёрнута — при заходе сверху (movingDown = true) в начало списка (atStart = true),
                    // а при заходе снизу (movingDown = false) в конец списка (atStart = false).
                    val isTargetCollapsed = currentExpandedStates[targetAreaId] == false
                    val list = currentLocalProjectsList.toMutableList()
                    val moved = list.removeAt(fromIndex).copy(areaId = targetAreaId)
                    val insertAt = findInsertionIndexForArea(
                        list,
                        currentAreas,
                        targetAreaId,
                        atStart = if (isTargetCollapsed) true else movingDown
                    )
                    list.add(insertAt, moved)
                    currentOnLocalProjectsListChange(list)
                    true
                }

                // ── 3. Наведение на другой проект (Project Swap) ──
                targetKeyStr.startsWith("proj_") -> {
                    val targetId = targetKeyStr.removePrefix("proj_")
                    val toIndex = currentLocalProjectsList.indexOfFirst { it.id == targetId }
                    if (toIndex == -1 || toIndex == fromIndex) return@universalDragAndDrop false

                    val targetProject = currentLocalProjectsList[toIndex]
                    val list = currentLocalProjectsList.toMutableList()
                    val moved = list.removeAt(fromIndex).copy(areaId = targetProject.areaId)
                    list.add(toIndex, moved)

                    currentOnLocalProjectsListChange(list)
                    true
                }

                else -> false
            }
        },
        onDragStarted = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onMoveCommitted = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        },
        onDragEnd = {
            val currentAreaId = currentLocalProjectsList.firstOrNull { it.id == currentProject.id }?.areaId
            val finalProjectsList = if (currentAreaId != null && currentExpandedStates[currentAreaId] == false) {
                // Если проект отпущен в свёрнутой области — гарантированно перемещаем его на самую первую позицию (индекс 0) этой области
                val fromIndex = currentLocalProjectsList.indexOfFirst { it.id == currentProject.id }
                val adjustedList = if (fromIndex != -1) {
                    val list = currentLocalProjectsList.toMutableList()
                    val moved = list.removeAt(fromIndex)
                    val firstIndex = findInsertionIndexForArea(list, currentAreas, currentAreaId, atStart = true)
                    list.add(firstIndex, moved)
                    list
                } else {
                    currentLocalProjectsList
                }
                currentOnExpandArea(currentAreaId)
                adjustedList
            } else {
                currentLocalProjectsList
            }

            // Перерасчёт порядковых индексов sortOrder для сохранения в БД
            val updatedList = finalProjectsList.mapIndexed { index, proj ->
                val original = currentOriginalProjects.firstOrNull { it.id == proj.id }
                val changed = original == null || original.sortOrder != index || original.areaId != proj.areaId
                if (changed) {
                    proj.copy(
                        sortOrder = index,
                        modificationDate = System.currentTimeMillis()
                    )
                } else {
                    proj.copy(sortOrder = index)
                }
            }

            val changedProjects = updatedList.filter { proj ->
                val original = currentOriginalProjects.firstOrNull { it.id == proj.id }
                original == null || original.sortOrder != proj.sortOrder || original.areaId != proj.areaId
            }

            currentOnLocalProjectsListChange(updatedList)
            if (changedProjects.isNotEmpty()) {
                currentOnProjectsReordered(changedProjects)
            }
        }
    )
}

/**
 * Вычисляет оптимальный индекс вставки проекта в локальный список проектов для заданной области.
 */
private fun findInsertionIndexForArea(
    list: List<Item>,
    areas: List<Area>,
    targetAreaId: String?,
    atStart: Boolean
): Int {
    if (targetAreaId == null) {
        val lastNoArea = list.indexOfLast { it.areaId == null }
        return if (atStart || lastNoArea == -1) {
            if (lastNoArea != -1 && !atStart) lastNoArea + 1 else 0
        } else {
            lastNoArea + 1
        }
    }

    val existingInArea = list.filter { it.areaId == targetAreaId }
    if (existingInArea.isNotEmpty()) {
        return if (atStart) {
            list.indexOfFirst { it.areaId == targetAreaId }
        } else {
            list.indexOfLast { it.areaId == targetAreaId } + 1
        }
    }

    // В целевой области пока нет проектов — находим границу по списку областей
    val targetAreaIndex = areas.indexOfFirst { it.id == targetAreaId }
    if (targetAreaIndex <= 0) {
        val lastNoArea = list.indexOfLast { it.areaId == null }
        return if (lastNoArea != -1) lastNoArea + 1 else 0
    }

    // Ищем последнюю область перед целевой, в которой есть проекты
    for (i in (targetAreaIndex - 1) downTo 0) {
        val prevAreaId = areas[i].id
        val lastInPrev = list.indexOfLast { it.areaId == prevAreaId }
        if (lastInPrev != -1) return lastInPrev + 1
    }

    val lastNoArea = list.indexOfLast { it.areaId == null }
    return if (lastNoArea != -1) lastNoArea + 1 else 0
}
