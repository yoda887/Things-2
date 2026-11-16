package com.example.domain.edits

import com.example.data.model.Item

/**
 * Подзаголовки проекта в множественном выборе, как в Things: подзаголовок отмечается наравне с задачами, а
 * «Переместить», «Удалить», «Дублировать» и «Поделиться» действуют на него вместе с его задачами. «Когда», «Срок»,
 * «Теги» и «Завершить» — только к выбранным задачам. Закреплено тестами HeadingBatchTest.
 */
object HeadingBatch {

    /**
     * Выбранное: [headings] — подзаголовки (в порядке проекта), [tasks] — задачи, выбранные сами по себе (кроме
     * задач выбранных подзаголовков: те идут вместе с ними), [headingTasks] — задачи выбранных подзаголовков.
     */
    data class Selection(val headings: List<Item>, val tasks: List<Item>, val headingTasks: Map<String, List<Item>>) {
        val isEmpty: Boolean get() = headings.isEmpty() && tasks.isEmpty()
    }

    /** Разобрать выбранное ([selectedIds]) на подзаголовки ([headings] — все известные) и задачи ([tasks]). */
    fun split(selectedIds: Set<String>, tasks: List<Item>, headings: List<Item>): Selection {
        val chosen = headings.filter { it.id in selectedIds }.sortedBy { it.sortOrder }
        val chosenIds = chosen.mapTo(HashSet()) { it.id }
        val headingTasks = tasks.filter { it.headingId in chosenIds }.groupBy { it.headingId!! }
        val own = tasks.filter { it.id in selectedIds && it.headingId !in chosenIds }
        return Selection(chosen, own, headingTasks)
    }

    /**
     * Подзаголовки в другой проект ([projectId], его область — [areaId]) вместе с задачами: подзаголовки встают в
     * конце его подзаголовков ([targetHeadings]) в прежнем порядке, задачи остаются под своим подзаголовком.
     * Перенос в тот же проект — ничего.
     * @return что сохранить: подзаголовки, затем задачи (задачи ссылаются на подзаголовок)
     */
    fun moved(
        selection: Selection,
        projectId: String,
        areaId: String?,
        targetHeadings: List<Item>,
        now: Long = System.currentTimeMillis()
    ): List<Item> {
        val moving = selection.headings.filter { it.projectId != projectId }
        if (moving.isEmpty()) return emptyList()
        var next = (targetHeadings.filter { it.projectId == projectId }.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val headings = moving.map { it.copy(projectId = projectId, sortOrder = next++, modificationDate = now) }
        val tasks = moving.flatMap { heading ->
            selection.headingTasks[heading.id].orEmpty().map {
                TaskMoves.moved(it, MoveTarget.Place(projectId, areaId, heading.id), now)
            }
        }
        return headings + tasks
    }

    /**
     * Копия подзаголовка [heading] в конце его проекта ([projectHeadings] — подзаголовки проекта) с id [newId];
     * его задачи дублируются под неё (задаче копии — headingId = [newId]).
     */
    fun copy(heading: Item, projectHeadings: List<Item>, newId: String, now: Long = System.currentTimeMillis()): Item {
        val last = projectHeadings.filter { it.projectId == heading.projectId }.maxOfOrNull { it.sortOrder } ?: heading.sortOrder
        return heading.copy(id = newId, sortOrder = last + 1, creationDate = now, modificationDate = now)
    }

    /** Текст «Поделиться»: подзаголовок, под ним его задачи; затем выбранные задачи — по «• » в строке. */
    fun shareText(selection: Selection): String = buildList {
        selection.headings.forEach { heading ->
            add(heading.title)
            selection.headingTasks[heading.id].orEmpty().sortedBy { it.sortOrder }.forEach { add("• " + it.title) }
        }
        selection.tasks.forEach { add("• " + it.title) }
    }.joinToString("\n")
}
