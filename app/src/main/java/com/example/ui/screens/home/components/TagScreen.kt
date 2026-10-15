package com.example.ui.screens.home.components

import com.example.domain.lists.TagList

/** Строки экрана тега (правила — в [TagList]). */
object TagScreen {

    /**
     * Открытые задачи и проекты по месту — под заголовком своего проекта или области; задачи без проекта и
     * области и проекты без области — отдельными блоками, с промежутком между ними. Затем Logbook.
     */
    fun rows(content: TagList.Content): List<Any> = buildList {
        addAll(
            PlaceGroupRows.rows(content.groups) { group ->
                val loose = group.project == null && group.area == null
                if (loose && group.tasks.isNotEmpty() && group.projectRows.isNotEmpty()) {
                    group.tasks + TaskListKeys.AREA_PROJECTS_SPACER + group.projectRows
                } else group.tasks + group.projectRows
            }
        )
        addAll(content.logbook)
    }
}
