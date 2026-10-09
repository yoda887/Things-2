package com.example.ui.screens.home.components

import com.example.domain.lists.SearchList
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag

/**
 * Строки результатов экрана «Поиск» (какие задачи находятся — SearchList). Порядок результатов — свой,
 * не правило групп «по месту». Быстрый поиск (Quick Find) строит свои результаты сам, в SearchOverlay.
 */
object SearchScreen {

    fun rows(
        matchedTasks: List<ItemWithChecklist>,
        projects: List<Item>,
        areas: List<Area>,
        savedTags: List<Tag>,
        searchQuery: String,
        headerLogbook: String
    ): List<Any> = buildList { addSearchResults(matchedTasks, projects, areas, savedTags, searchQuery, headerLogbook) }
}

/**
 * Результаты поиска: открытые задачи без проекта и области, найденные проекты, области и теги (строками, без заголовка),
 * открытые задачи по проектам и областям — в порядке главного экрана, затем выполненное (Logbook).
 */
private fun MutableList<Any>.addSearchResults(
    matchedTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    savedTags: List<Tag>,
    searchQuery: String,
    headerLogbook: String
) {
    val q = searchQuery.trim()
    if (q.isEmpty()) return

    fun Item.isDone() = isCompleted || status == Item.STATUS_CANCELLED
    val projectsById = projects.filter { it.type == Item.TYPE_PROJECT }.associateBy { it.id }
    val areasById = areas.associateBy { it.id }
    // Задача в удалённом проекте или области — в корзине вместе с ними, в поиске её нет
    val active = matchedTasks.filter { task ->
        !task.item.isDone() &&
            task.item.projectId?.let { projectsById[it]?.trashed } != true &&
            task.item.areaId?.let { areasById[it]?.trashed } != true
    }
    // Проект и область задачи, если они есть и не удалены; иначе задача — среди задач без проекта
    fun ItemWithChecklist.projectOrNull() = item.projectId?.let { projectsById[it] }
    fun ItemWithChecklist.areaOrNull() = item.areaId?.let { areasById[it] }

    // 1. Открытые задачи без проекта и области — сразу под строкой поиска, без заголовка.
    // Сюда же — задачи, чьих проекта или области больше нет: иначе они пропали бы из поиска
    addAll(active.filter { it.projectOrNull() == null && it.areaOrNull() == null })

    // 2. Найденные открытые проекты, области и теги — строками, без заголовка секции
    val matchedProjects = projects.filter {
        it.type == Item.TYPE_PROJECT && !it.trashed && !it.isDone() &&
            (it.title.contains(q, ignoreCase = true) || it.notes.contains(q, ignoreCase = true))
    }
    addAll(matchedProjects)
    val matchedAreas = areas.filter { !it.trashed && it.title.contains(q, ignoreCase = true) }
    addAll(matchedAreas.map { SearchAreaItem(it) })
    val matchedTags = savedTags.filter { it.title.isNotBlank() && it.title.contains(q, ignoreCase = true) }
    addAll(matchedTags.map { SearchTagItem(it) })

    // 3. Открытые задачи по проектам и областям — в порядке главного экрана: сначала проекты
    // без области, затем каждая область — её задачи без проекта, потом её проекты
    val tasksByProject = active.filter { it.projectOrNull() != null }.groupBy { it.item.projectId!! }
    val tasksByArea = active.filter { it.projectOrNull() == null && it.areaOrNull() != null }
        .groupBy { it.item.areaId!! }
    fun addProjectGroup(project: Item) {
        val tasks = tasksByProject[project.id] ?: return
        add(SearchSectionHeaderItem("search_hdr_project_${project.id}", project.title, SearchSectionKind.PROJECT, project = project))
        addAll(tasks)
    }
    val groupProjects = projects.filter { it.id in tasksByProject }
    // Проект, чьей области нет, на главном экране стоит среди проектов без области
    groupProjects.filter { it.areaId.isNullOrEmpty() || it.areaId !in areasById }.forEach(::addProjectGroup)
    areas.filter { it.id in tasksByArea || groupProjects.any { p -> p.areaId == it.id } }.forEach { area ->
        tasksByArea[area.id]?.let { tasks ->
            add(SearchSectionHeaderItem("search_hdr_area_${area.id}", area.title, SearchSectionKind.AREA, area = area))
            addAll(tasks)
        }
        groupProjects.filter { it.areaId == area.id }.forEach(::addProjectGroup)
    }

    // 4. Выполненные и отменённые задачи и проекты — Logbook, свежие сверху
    val doneTasks = matchedTasks.filter { it.item.isDone() }
    val doneProjects = projects.filter {
        it.type == Item.TYPE_PROJECT && !it.trashed && it.isDone() &&
            (it.title.contains(q, ignoreCase = true) || it.notes.contains(q, ignoreCase = true))
    }
    val logbook = (doneTasks.map { it to it.item } + doneProjects.map { it to it })
        .sortedByDescending { (_, item) -> item.stopDate ?: item.modificationDate }
        .map { it.first }
    if (logbook.isNotEmpty()) {
        add(SearchSectionHeaderItem("search_hdr_logbook", headerLogbook, SearchSectionKind.LOGBOOK))
        addAll(logbook)
    }
}
