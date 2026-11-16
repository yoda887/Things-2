package com.example.domain.edits

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Выбор в списках, где проекты стоят строками вместе с задачами («Сегодня», «Планы», область, тег, «Когда-нибудь»):
 * проекты и задачи отмечаются вместе, а пакетные действия делятся — к проектам их правила (завершение и удаление —
 * вместе с задачами проекта, «Переместить» — только в область), к задачам — задачные.
 * Закреплено тестами MixedSelectionTest.
 */
object MixedSelection {

    /** Строка — проект (шаблоны повторяющихся проектов — нет: у них свои правила) */
    fun isProject(item: Item): Boolean = item.type == Item.TYPE_PROJECT

    data class Parts(val projects: List<Item>, val tasks: List<ItemWithChecklist>) {
        val hasProjects: Boolean get() = projects.isNotEmpty()
        val hasTasks: Boolean get() = tasks.isNotEmpty()
    }

    fun split(selected: List<ItemWithChecklist>): Parts {
        val (projects, tasks) = selected.partition { isProject(it.item) }
        return Parts(projects.map { it.item }, tasks)
    }

    /**
     * Куда переносятся задачи, если среди выбранного есть проекты: окно предлагает только области («Без области» —
     * null), задачи идут туда же — в область, без проекта
     */
    fun taskTargetWithProjects(areaId: String?): MoveTarget.Place = MoveTarget.Place(projectId = null, areaId = areaId)

    /** Проекты, завершение которых нужно подтвердить: в них есть открытые задачи ([allTasks]) */
    fun projectsWithOpenTasks(projects: List<Item>, allTasks: List<ItemWithChecklist>): List<Item> {
        val ids = projects.mapTo(HashSet()) { it.id }
        val withOpen = allTasks.asSequence()
            .filter { it.item.projectId in ids && it.item.status == Item.STATUS_OPEN && !it.item.trashed }
            .mapTo(HashSet()) { it.item.projectId }
        return projects.filter { it.id in withOpen }
    }
}
