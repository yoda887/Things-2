package com.example.ui.screens.home.inlineeditor.dialogs

import com.example.data.model.Tag

/**
 * Строка списка «Manage Tags»: тег и признак вложенного тега (true — строка внутри группы).
 * Группа — корневой тег; её теги идут в списке сразу за ней.
 */
typealias TagRow = Pair<Tag, Boolean>

/**
 * Правила перестановки тегов при перетаскивании — как у областей и проектов на главном экране:
 * группа переезжает целиком, вместе со своими тегами, и меняется местами только с другими группами;
 * тег внутри группы меняется местами с тегами и может перейти в соседнюю группу через её заголовок.
 *
 * Обычные функции над списком строк: их поведение закреплено тестами TagReorderTest.
 */
object TagReorder {

    /**
     * Ставит группу [draggedId] на место группы [targetId], перенося с ней её теги.
     * @return новый порядок строк либо null, если перестановки нет
     */
    fun moveGroup(rows: List<TagRow>, draggedId: String, targetId: String): List<TagRow>? {
        val blocks = blocks(rows)
        val from = blocks.indexOfFirst { !it.first().second && it.first().first.id == draggedId }
        val to = blocks.indexOfFirst { !it.first().second && it.first().first.id == targetId }
        if (from == -1 || to == -1 || from == to) return null
        val result = blocks.toMutableList()
        result.add(to, result.removeAt(from))
        return result.flatten()
    }

    /**
     * Переносит вложенный тег [draggedId] на место строки [targetId].
     *
     * - На другой вложенный тег — встаёт на его место и в его группу.
     * - Вниз через заголовок группы — становится первым тегом этой группы.
     * - Вверх через заголовок своей группы — становится последним тегом предыдущей группы.
     *
     * @param canBeGroup может ли тег быть группой (в справочнике есть не все строки диалога)
     * @return новый порядок строк либо null, если перестановки нет
     */
    fun moveChild(
        rows: List<TagRow>,
        draggedId: String,
        targetId: String,
        movingDown: Boolean,
        canBeGroup: (String) -> Boolean = { true },
    ): List<TagRow>? {
        val from = rows.indexOfFirst { it.first.id == draggedId }
        val to = rows.indexOfFirst { it.first.id == targetId }
        if (from == -1 || to == -1 || from == to || !rows[from].second) return null
        val target = rows[to]
        val dragged = rows[from].first

        val list = rows.toMutableList()
        list.removeAt(from)

        val newParentId: String?
        val insertAt: Int
        if (target.second) {
            // Место соседнего тега: при движении вниз — за ним, вверх — перед ним
            newParentId = target.first.parentId
            insertAt = to
        } else {
            val targetAt = list.indexOfFirst { it.first.id == target.first.id }
            when {
                movingDown -> {
                    newParentId = target.first.id
                    insertAt = targetAt + 1
                }
                target.first.id == dragged.parentId -> {
                    // Вверх через заголовок своей группы — в конец предыдущей
                    val previousGroup = list.subList(0, targetAt).lastOrNull { !it.second } ?: return null
                    newParentId = previousGroup.first.id
                    insertAt = targetAt
                }
                else -> {
                    // Вверх в чужую группу снизу — в её конец
                    newParentId = target.first.id
                    var end = targetAt + 1
                    while (end < list.size && list[end].second) end++
                    insertAt = end
                }
            }
        }
        if (newParentId == null || !canBeGroup(newParentId)) return null
        list.add(insertAt, dragged.copy(parentId = newParentId) to true)
        return list
    }

    /** Строки без тегов группы [groupId] — так список выглядит, пока группу несут под пальцем. */
    fun withoutChildrenOf(rows: List<TagRow>, groupId: String?): List<TagRow> =
        if (groupId == null) rows else rows.filterNot { it.second && it.first.parentId == groupId }

    /** Группы со своими тегами, по порядку. */
    private fun blocks(rows: List<TagRow>): List<List<TagRow>> {
        val result = mutableListOf<MutableList<TagRow>>()
        for (row in rows) {
            if (!row.second || result.isEmpty()) result.add(mutableListOf(row)) else result.last().add(row)
        }
        return result
    }
}
