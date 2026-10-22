package com.example.ui.screens.home.components

import com.example.domain.lists.TodayList
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Area
import com.example.domain.lists.PlaceGroups

/** Строки экрана «Сегодня» (правила — в TodayList). Перенос между днём и вечером — в TaskDrops. */
object TodayScreen {

    /**
     * Проекты и задачи дня, затем заголовок «Вечер», вечерние проекты и задачи. Проекты — строками над задачами,
     * как в Things. Пока задачу несут, заголовок виден и без вечерних задач — на него можно перенести задачу в вечер.
     * [groupedDay] (настройка) — дневная часть по проектам и областям, как «В любое время»: сначала задачи без
     * проекта и области, затем проекты дня без области, затем группы под заголовком проекта или области; вечер — как
     * прежде. [allProjects] и [areas] — для заголовков групп.
     */
    fun rows(
        day: List<ItemWithChecklist>,
        evening: List<ItemWithChecklist>,
        isDragging: Boolean,
        projects: Pair<List<Item>, List<Item>> = emptyList<Item>() to emptyList(),
        groupedDay: Boolean = false,
        allProjects: List<Item> = emptyList(),
        areas: List<Area> = emptyList()
    ): List<Any> = buildList {
        if (groupedDay) {
            val groups = PlaceGroups.group(day, allProjects, areas, projectRows = projects.first, areaHeaderOnlyWithOwnItems = true)
            addAll(PlaceGroupRows.rows(groups) { group ->
                val loose = group.project == null && group.area == null
                if (loose && group.tasks.isNotEmpty() && group.projectRows.isNotEmpty()) {
                    group.tasks + TaskListKeys.AREA_PROJECTS_SPACER + group.projectRows
                } else group.tasks + group.projectRows
            })
        } else {
            addAll(projects.first)
            addAll(day)
        }
        if (evening.isNotEmpty() || projects.second.isNotEmpty() || isDragging) {
            add(TaskListKeys.EVENING_HEADER)
            addAll(projects.second)
            addAll(evening)
        }
    }

    fun isEmpty(
        day: List<ItemWithChecklist>,
        evening: List<ItemWithChecklist>,
        isDragging: Boolean,
        projects: Pair<List<Item>, List<Item>> = emptyList<Item>() to emptyList()
    ): Boolean = day.isEmpty() && evening.isEmpty() && projects.first.isEmpty() && projects.second.isEmpty() && !isDragging
}
