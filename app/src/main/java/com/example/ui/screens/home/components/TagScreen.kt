package com.example.ui.screens.home.components

import com.example.domain.lists.PlaceGroups
import com.example.domain.lists.TagList

object TagScreen {

    /** Ключ плашки поздних задач и проектов (состояние — как у «Всех проектов», набор раскрытых) */
    val LATER_KEY: String = TaskListKeys.LIST_TOGGLE_PREFIX + "tag_later"

    /**
     * Текущие задачи и проекты по месту — под заголовком своего проекта или области; задачи без проекта и
     * области и проекты без области — отдельными блоками, с промежутком между ними. Затем плашка поздних
     * («Показать более поздние объекты») и они сами, если раскрыты; затем плашка выполненных и они сами.
     */
    fun rows(
        content: TagList.Content,
        showLater: Boolean = true,
        showLogged: Boolean = true,
        laterLabel: (shown: Boolean) -> String = { "" },
        loggedLabel: (shown: Boolean, count: Int) -> String = { _, _ -> "" }
    ): List<Any> = buildList {
        addAll(groupRows(content.groups))
        if (content.laterCount > 0) {
            if (showLater) addAll(groupRows(content.laterGroups))
            add(ListToggleItem(LATER_KEY, laterLabel(showLater)))
        }
        if (content.logbook.isNotEmpty()) {
            add(ListToggleItem(AllProjectsScreen.LOGGED_KEY, loggedLabel(showLogged, content.logbook.size)))
            if (showLogged) addAll(content.logbook)
        }
    }

    private fun groupRows(groups: List<PlaceGroups.Group>): List<Any> =
        PlaceGroupRows.rows(groups) { group ->
            val loose = group.project == null && group.area == null
            if (loose && group.tasks.isNotEmpty() && group.projectRows.isNotEmpty()) {
                group.tasks + TaskListKeys.AREA_PROJECTS_SPACER + group.projectRows
            } else group.tasks + group.projectRows
        }
}
