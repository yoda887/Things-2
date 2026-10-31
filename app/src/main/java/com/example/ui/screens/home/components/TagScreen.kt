package com.example.ui.screens.home.components

import com.example.domain.lists.PlaceGroups
import com.example.domain.lists.TagList

object TagScreen {

    /**
     * Задачи и проекты по месту — под заголовком своего проекта или области; задачи без проекта и области и
     * проекты без области — отдельными блоками, с промежутком между ними. Поздние — сразу, в тех же группах после
     * текущих (TagList.Content.groups): заголовок проекта или области не повторяется. Плашек нет.
     */
    fun rows(content: TagList.Content): List<Any> = groupRows(content.groups())

    private fun groupRows(groups: List<PlaceGroups.Group>): List<Any> =
        PlaceGroupRows.rows(groups) { group ->
            val loose = group.project == null && group.area == null
            if (loose && group.tasks.isNotEmpty() && group.projectRows.isNotEmpty()) {
                group.tasks + TaskListKeys.AREA_PROJECTS_SPACER + group.projectRows
            } else group.tasks + group.projectRows
        }
}
