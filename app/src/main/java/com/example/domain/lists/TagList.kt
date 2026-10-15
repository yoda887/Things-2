package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Тег: открытые задачи и проекты с тегом — по месту, как на главном экране ([PlaceGroups]): сначала без
 * проекта и области, потом под своим проектом или областью; затем закрытые задачи с тегом (Logbook, свежие
 * сверху). Закреплено тестами TagScreenTest.
 */
object TagList {

    fun hasTag(item: Item, tag: String): Boolean = item.tags.contains(tag)

    /** Задача списка экрана: открытая, не удалённая задача с тегом. */
    fun isActiveTask(item: Item, tag: String): Boolean =
        item.type == Item.TYPE_TASK && !item.trashed && !ListRules.isClosed(item) && hasTag(item, tag)

    data class Content(
        val projects: List<Item>,
        val active: List<ItemWithChecklist>,
        val logbook: List<ItemWithChecklist>,
        /** Открытые задачи и проекты по месту: задачи — [PlaceGroups.Group.tasks], проекты — строками */
        val groups: List<PlaceGroups.Group> = emptyList()
    ) {
        val isEmpty: Boolean get() = projects.isEmpty() && active.isEmpty() && logbook.isEmpty()
    }

    /**
     * @param tasks задачи списка экрана (из ViewModel). Тег у них не перепроверяется: раскрытая
     * в редакторе задача остаётся на месте, даже если тег с неё только что сняли
     * @param allTasks все задачи — из них берётся Logbook тега
     * @param pinned место задачи в открытом редакторе — она стоит там до закрытия ([PlaceGroups.group])
     */
    fun content(
        tag: String?,
        tasks: List<ItemWithChecklist>,
        allTasks: List<ItemWithChecklist>,
        projects: List<Item>,
        areas: List<Area> = emptyList(),
        pinned: Map<String, PlaceGroups.Place> = emptyMap()
    ): Content {
        val title = tag.orEmpty()
        val taggedProjects = projects.filter { ListRules.isOpenProject(it) && hasTag(it, title) }
        val active = tasks.filter { !it.item.trashed && !ListRules.isClosed(it.item) }
        return Content(
            projects = taggedProjects,
            active = active,
            groups = PlaceGroups.group(active, projects, areas, projectRows = taggedProjects, areaHeaderOnlyWithOwnItems = true, pinned = pinned),
            logbook = allTasks
                .filter { !it.item.trashed && ListRules.isClosed(it.item) && hasTag(it.item, title) }
                .sortedByDescending { it.item.stopDate ?: it.item.modificationDate }
        )
    }
}
