package com.example.ui.screens.home.components

import com.example.domain.edits.Headings
import androidx.compose.ui.Modifier
import com.example.data.model.Item
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop

/** Полосы автопрокрутки у краёв — те же, что у задач */
private const val HEADING_SCROLL_TOP_ZONE_FRACTION = 0.11f
private const val HEADING_SCROLL_BOTTOM_ZONE_FRACTION = 0.09f

/**
 * Перетаскивание заголовка проекта — на общем движке списков, как области на главном экране:
 * заголовок меняется местами только с другими заголовками и переезжает вместе со своими задачами,
 * которые на время жеста убраны из списка (см. rememberFlattenedList). Задачи чужого заголовка для
 * него — продолжение блока этого заголовка, и он встаёт за ними.
 *
 * @param headings текущий порядок заголовков проекта
 * @param onHeadingsChange новый порядок после перестановки
 * @param onDragEnd палец отпущен — порядок пора сохранить
 */
fun Modifier.headingDragAndDrop(
    state: GenericDragDropState,
    heading: Item,
    headings: List<Item>,
    onHeadingsChange: (List<Item>) -> Unit,
    onDragEnd: () -> Unit
): Modifier = universalDragAndDrop(
    state = state,
    key = TaskListKeys.HEADING_PREFIX + heading.id,
    topScrollZoneFraction = HEADING_SCROLL_TOP_ZONE_FRACTION,
    bottomScrollZoneFraction = HEADING_SCROLL_BOTTOM_ZONE_FRACTION,
    canDropOver = { targetKey -> (targetKey as? String)?.startsWith(TaskListKeys.HEADING_PREFIX) == true },
    isBlockContinuation = { targetKey ->
        val key = targetKey as? String
        key != null && !key.startsWith(TaskListKeys.HEADING_PREFIX) &&
            key != TaskListKeys.MAIN_HEADER && key !in TaskListKeys.nonDroppable
    },
    onMoveIfNecessary = { draggedKey, targetKey ->
        val draggedId = (draggedKey as? String)?.removePrefix(TaskListKeys.HEADING_PREFIX)
            ?: return@universalDragAndDrop false
        val targetId = (targetKey as? String)?.removePrefix(TaskListKeys.HEADING_PREFIX)
            ?: return@universalDragAndDrop false
        val reordered = Headings.move(headings, draggedId, targetId)
            ?: return@universalDragAndDrop false
        onHeadingsChange(reordered)
        true
    },
    onDragEnd = onDragEnd
)
