package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.ui.Modifier
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop

/** Зоны автопрокрутки сверху и снизу: список тегов в диалоге короткий, широкие зоны сразу утаскивали бы его */
private const val TAGS_SCROLL_ZONE_FRACTION = 0.12f

/**
 * Обёртка над универсальным [universalDragAndDrop] для списка «Manage Tags» — тот же движок,
 * что у задач, проектов и областей. Правила перестановки — в [TagReorder]:
 * - группа (корневой тег) меняется местами только с группами и переезжает вместе со своими тегами,
 *   которые на время жеста убраны из списка (см. [TagReorder.withoutChildrenOf]); теги чужих групп
 *   для неё — продолжение блока своей группы, и она встаёт за ними;
 * - тег внутри группы меняется местами с любыми строками и переходит в соседнюю группу через её заголовок.
 *
 * @param rows текущий порядок строк, включая теги группы, которую сейчас несут
 * @param canBeGroup может ли тег стать группой для перенесённого тега
 * @param onRowsChange новый порядок строк после перестановки
 * @param onDragEnd палец отпущен — порядок пора сохранить
 */
fun Modifier.tagDragAndDrop(
    state: GenericDragDropState,
    row: TagRow,
    rows: List<TagRow>,
    canBeGroup: (String) -> Boolean,
    onRowsChange: (List<TagRow>) -> Unit,
    onDragEnd: () -> Unit
): Modifier {
    val lazyListState = state.lazyListState
    val isGroup = !row.second
    val childIds = rows.filter { it.second }.map { it.first.id }.toSet()

    return this.universalDragAndDrop(
        state = state,
        key = row.first.id,
        topScrollZoneFraction = TAGS_SCROLL_ZONE_FRACTION,
        bottomScrollZoneFraction = TAGS_SCROLL_ZONE_FRACTION,
        canDropOver = { targetKey -> !isGroup || targetKey !in childIds },
        isBlockContinuation = { targetKey -> isGroup && targetKey in childIds },
        onMoveIfNecessary = { draggedKey, targetKey ->
            val draggedId = draggedKey as? String ?: return@universalDragAndDrop false
            val targetId = targetKey as? String ?: return@universalDragAndDrop false
            val newRows = if (isGroup) {
                TagReorder.moveGroup(rows, draggedId, targetId)
            } else {
                val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                val draggedIndex = visibleItems.firstOrNull { it.key == draggedKey }?.index
                val targetIndex = visibleItems.firstOrNull { it.key == targetKey }?.index
                if (draggedIndex == null || targetIndex == null) return@universalDragAndDrop false
                TagReorder.moveChild(rows, draggedId, targetId, movingDown = targetIndex > draggedIndex, canBeGroup = canBeGroup)
            } ?: return@universalDragAndDrop false
            onRowsChange(newRows)
            true
        },
        onDragEnd = onDragEnd
    )
}
