package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Тег: открытые задачи и проекты с тегом — по месту, как на главном экране ([PlaceGroups]): сначала без
 * проекта и области, потом под своим проектом или областью. Более поздние (старт после сегодня или
 * «Когда-нибудь») — отдельно, тоже по месту ([Content.laterGroups]); закрытые задачи с тегом — Logbook, свежие
 * сверху. Экран прячет поздние и выполненные под плашками «Показать». Закреплено тестами TagScreenTest.
 */
object TagList {

    fun hasTag(item: Item, tag: String): Boolean = item.tags.contains(tag)

    /** Задача списка экрана: открытая, не удалённая задача с тегом — и шаблон повторяющейся задачи (копии — задачи). */
    fun isActiveTask(item: Item, tag: String): Boolean =
        (item.type == Item.TYPE_TASK || RepeatingList.includes(item)) && !item.trashed && !ListRules.isClosed(item) && hasTag(item, tag)

    data class Content(
        val projects: List<Item>,
        val active: List<ItemWithChecklist>,
        val logbook: List<ItemWithChecklist>,
        /** Текущие открытые задачи и проекты по месту: задачи — [PlaceGroups.Group.tasks], проекты — строками */
        val groups: List<PlaceGroups.Group> = emptyList(),
        /** Более поздние — со стартом после сегодня и «Когда-нибудь», тоже по месту */
        val laterGroups: List<PlaceGroups.Group> = emptyList(),
        /** Сколько поздних задач и проектов */
        val laterCount: Int = 0,
        /** Для [groups]: текущие и поздние задачи и проекты, все проекты и области, закреплённые места */
        private val currentTasks: List<ItemWithChecklist> = emptyList(),
        private val laterTasks: List<ItemWithChecklist> = emptyList(),
        private val currentProjects: List<Item> = emptyList(),
        private val laterProjects: List<Item> = emptyList(),
        private val allProjects: List<Item> = emptyList(),
        private val areas: List<Area> = emptyList(),
        private val pinned: Map<String, PlaceGroups.Place> = emptyMap()
    ) {
        val isEmpty: Boolean get() = projects.isEmpty() && active.isEmpty() && logbook.isEmpty()

        /**
         * Группы экрана: текущие, а показанные поздние ([showLater]) и выполненные ([showLogged]) — в тех же группах
         * своего проекта или области, после текущих (один заголовок на проект или область, не второй такой же).
         */
        fun groups(showLater: Boolean, showLogged: Boolean): List<PlaceGroups.Group> {
            val tasks = currentTasks + (if (showLater) laterTasks else emptyList()) + (if (showLogged) logbook else emptyList())
            val rows = currentProjects + (if (showLater) laterProjects else emptyList())
            return PlaceGroups.group(tasks, allProjects, areas, projectRows = rows, areaHeaderOnlyWithOwnItems = true, pinned = pinned)
        }
    }

    /** Поздняя задача или проект: старт после сегодня или «Когда-нибудь». Шаблон повтора — не поздний (у него свой день повтора) */
    fun isLater(item: Item, bounds: DayBounds): Boolean =
        !RepeatingList.includes(item) && !RepeatingList.includesProject(item) &&
            (item.start == Item.START_SOMEDAY || bounds.isUpcoming(item))

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
        pinned: Map<String, PlaceGroups.Place> = emptyMap(),
        bounds: DayBounds = DayBounds.now()
    ): Content {
        val title = tag.orEmpty()
        // Проекты с тегом — и шаблоны повторяющихся проектов (их нет среди проектов, они среди всех задач)
        val projectTemplates = allTasks.map { it.item }.filter { RepeatingList.includesProject(it) && hasTag(it, title) }
        val taggedProjects = projects.filter { ListRules.isOpenProject(it) && hasTag(it, title) } +
            projectTemplates.filter { template -> projects.none { it.id == template.id } }
        val active = tasks.filter { !it.item.trashed && !ListRules.isClosed(it.item) }
        // Задача в открытом редакторе — на месте, даже если ей только что поставили позднюю дату
        val (current, later) = active.partition { it.item.id in pinned || !isLater(it.item, bounds) }
        val (currentProjects, laterProjects) = taggedProjects.partition { !isLater(it, bounds) }
        return Content(
            projects = taggedProjects,
            active = active,
            groups = PlaceGroups.group(current, projects, areas, projectRows = currentProjects, areaHeaderOnlyWithOwnItems = true, pinned = pinned),
            laterGroups = PlaceGroups.group(later, projects, areas, projectRows = laterProjects, areaHeaderOnlyWithOwnItems = true),
            laterCount = later.size + laterProjects.size,
            currentTasks = current,
            laterTasks = later,
            currentProjects = currentProjects,
            laterProjects = laterProjects,
            allProjects = projects,
            areas = areas,
            pinned = pinned,
            logbook = allTasks
                .filter { !it.item.trashed && ListRules.isClosed(it.item) && hasTag(it.item, title) }
                .sortedByDescending { it.item.stopDate ?: it.item.modificationDate }
        )
    }
}
