package com.example.ui.screens.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import com.example.data.model.Area
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop

/**
 * Обертка над универсальным [universalDragAndDrop] для реализации перетаскивания (Drag-and-Drop)
 * сфер/областей (Area) на главном экране (HomePanel).
 *
 * Логика работы:
 * - Сферы перемещаются исключительно относительно других сфер (canDropOver: только ключи "area_").
 * - При начале перетаскивания (onDragStarted) развернутая область автоматически временно сворачивается,
 *   а после отпускания (onDragEnd) восстанавливается в исходное состояние.
 * - При успешном пересечении с другой сферой элементы переупорядочиваются в localAreas.
 * - При завершении жеста вызывается [onAreasReordered] для сохранения нового sortOrder в БД.
 */
fun Modifier.areaDragAndDrop(
    state: GenericDragDropState,
    area: Area,
    hasProjects: Boolean,
    originalAreas: List<Area>,
    localAreasList: List<Area>,
    expandedStates: SnapshotStateMap<String, Boolean>,
    onLocalAreasListChange: (List<Area>) -> Unit,
    onAreasReordered: (List<Area>) -> Unit
): Modifier = composed {
    val view = LocalView.current

    val currentArea by rememberUpdatedState(area)
    val currentHasProjects by rememberUpdatedState(hasProjects)
    val currentOriginalAreas by rememberUpdatedState(originalAreas)
    val currentLocalAreasList by rememberUpdatedState(localAreasList)
    val currentOnLocalAreasListChange by rememberUpdatedState(onLocalAreasListChange)
    val currentOnAreasReordered by rememberUpdatedState(onAreasReordered)

    // Запоминаем, была ли данная сфера развернута до начала перетаскивания
    var wasExpandedBeforeDrag by remember { mutableStateOf(false) }
    var isDraggingThisArea by remember { mutableStateOf(false) }

    LaunchedEffect(state.draggedItemKey) {
        val draggedKeyStr = state.draggedItemKey as? String
        if (draggedKeyStr == "area_${area.id}") {
            isDraggingThisArea = true
        } else if (isDraggingThisArea && draggedKeyStr == null) {
            // Анимация возврата завершилась, восстанавливаем состояние раскрытия
            isDraggingThisArea = false
            if (wasExpandedBeforeDrag) {
                expandedStates[area.id] = true
                wasExpandedBeforeDrag = false
            }
        }
    }

    this.universalDragAndDrop(
        state = state,
        key = "area_${area.id}",
        canDropOver = { targetKey ->
            (targetKey as? String)?.startsWith("area_") == true
        },
        onDragStarted = {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            if (currentHasProjects) {
                val isExpanded = expandedStates[currentArea.id] ?: true
                wasExpandedBeforeDrag = isExpanded
                if (isExpanded) {
                    expandedStates[currentArea.id] = false
                }
            }
        },
        onMoveIfNecessary = { draggedKey, targetKey ->
            val draggedKeyStr = draggedKey as? String ?: return@universalDragAndDrop false
            val targetKeyStr = targetKey as? String ?: return@universalDragAndDrop false

            if (!draggedKeyStr.startsWith("area_") || !targetKeyStr.startsWith("area_")) {
                return@universalDragAndDrop false
            }

            val draggedId = draggedKeyStr.removePrefix("area_")
            val targetId = targetKeyStr.removePrefix("area_")

            val fromIndex = currentLocalAreasList.indexOfFirst { it.id == draggedId }
            val toIndex = currentLocalAreasList.indexOfFirst { it.id == targetId }

            if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) {
                return@universalDragAndDrop false
            }

            val list = currentLocalAreasList.toMutableList()
            val moved = list.removeAt(fromIndex)
            list.add(toIndex, moved)
            currentOnLocalAreasListChange(list)
            true
        },
        onMoveCommitted = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        },
        onDragEnd = {
            // Если порядок сфер изменился относительно исходного, сохраняем в БД
            val originalIds = currentOriginalAreas.map { it.id }
            val currentIds = currentLocalAreasList.map { it.id }
            if (originalIds != currentIds) {
                currentOnAreasReordered(currentLocalAreasList)
            }
        }
    )
}
