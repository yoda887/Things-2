package com.example.ui.screens.home.components

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Группировка задач «по месту» — по проекту или области, в порядке главного экрана: сначала задачи
 * без проекта и области, затем проекты без области, затем каждая область — её заголовок, её задачи
 * без проекта, потом её проекты. Это правило только экранов «В любое время» и «Когда-нибудь»
 * (у поиска свой порядок результатов). На «Когда-нибудь» в группы добавляются ещё и сами отложенные
 * проекты — строками ([Group.projectRows]).
 *
 * Проект или область, которых нет или которые удалены, задачу не уносят — она среди задач без места.
 */
object PlaceGroups {

    /**
     * Группа: задачи без места ([project] и [area] — null), задачи проекта или задачи области без проекта.
     * [projectRows] — проекты строками в этой группе (на «Когда-нибудь» — отложенные проекты своей области).
     */
    data class Group(
        val project: Item?,
        val area: Area?,
        val tasks: List<ItemWithChecklist>,
        val projectRows: List<Item> = emptyList()
    )

    /** Ключ группы задачи: перенос перетаскиванием допустим только внутри одной группы. */
    fun keyOf(task: Item, projects: List<Item>, areas: List<Area>): String? {
        val project = task.projectId?.let { id -> projects.firstOrNull { it.id == id && !it.trashed } }
        if (project != null) return "project:${project.id}"
        val area = task.areaId?.let { id -> areas.firstOrNull { it.id == id && !it.trashed } }
        return area?.let { "area:${it.id}" }
    }

    /**
     * @param projectRows проекты, которые показываются строками внутри групп: без области — среди задач
     * без места, с областью — под заголовком своей области
     * @param areaHeaderOnlyWithOwnItems заголовок области — только когда у неё есть свои задачи без проекта
     * (или строки проектов); её проекты с задачами показываются и без него («В любое время»)
     */
    fun group(
        tasks: List<ItemWithChecklist>,
        projects: List<Item>,
        areas: List<Area>,
        projectRows: List<Item> = emptyList(),
        areaHeaderOnlyWithOwnItems: Boolean = false
    ): List<Group> {
        val liveProjects = projects.filter { it.type == Item.TYPE_PROJECT && !it.trashed }
        val liveAreas = areas.filter { !it.trashed }
        val projectsById = liveProjects.associateBy { it.id }
        val areasById = liveAreas.associateBy { it.id }
        fun ItemWithChecklist.project() = item.projectId?.let { projectsById[it] }
        fun ItemWithChecklist.area() = item.areaId?.let { areasById[it] }

        val loose = tasks.filter { it.project() == null && it.area() == null }
        val byProject = tasks.filter { it.project() != null }.groupBy { it.item.projectId!! }
        val byArea = tasks.filter { it.project() == null && it.area() != null }.groupBy { it.item.areaId!! }

        val rows = projectRows.filter { it.type == Item.TYPE_PROJECT && !it.trashed }
        val rowsWithoutArea = rows.filter { it.areaId.isNullOrEmpty() || it.areaId !in areasById }
        val rowsByArea = (rows - rowsWithoutArea.toSet()).groupBy { it.areaId!! }

        return buildList {
            if (loose.isNotEmpty() || rowsWithoutArea.isNotEmpty()) add(Group(null, null, loose, rowsWithoutArea))
            fun addProject(project: Item) {
                byProject[project.id]?.let { add(Group(project, null, it)) }
            }
            // Проект, чьей области нет, на главном экране стоит среди проектов без области
            liveProjects.filter { it.areaId.isNullOrEmpty() || it.areaId !in areasById }.forEach(::addProject)
            liveAreas.forEach { area ->
                val areaProjects = liveProjects.filter { it.areaId == area.id }
                val areaTasks = byArea[area.id].orEmpty()
                val areaRows = rowsByArea[area.id].orEmpty()
                // Заголовок области стоит над её задачами и, если не сказано иначе, над её проектами —
                // даже если своих задач у неё нет
                val hasOwnItems = areaTasks.isNotEmpty() || areaRows.isNotEmpty()
                if (hasOwnItems || (!areaHeaderOnlyWithOwnItems && areaProjects.any { it.id in byProject })) {
                    add(Group(null, area, areaTasks, areaRows))
                }
                areaProjects.forEach(::addProject)
            }
        }
    }

    /** Задачи в порядке групп — так список экрана совпадает с тем, что видно на экране. */
    fun order(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<ItemWithChecklist> =
        group(tasks, projects, areas).flatMap { it.tasks }

    /** Отложенные проекты для «Когда-нибудь»: открытые, не удалённые, со стартом «Когда-нибудь». */
    fun somedayProjects(projects: List<Item>): List<Item> = projects.filter {
        it.type == Item.TYPE_PROJECT && !it.trashed && it.status == Item.STATUS_OPEN && it.start == Item.START_SOMEDAY
    }
}
