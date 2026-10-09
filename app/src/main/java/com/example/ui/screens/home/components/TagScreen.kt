package com.example.ui.screens.home.components

import com.example.domain.lists.TagList

/** Строки экрана тега (правила — в [TagList]). */
object TagScreen {

    /** Проекты, отступ, открытые задачи, затем Logbook. */
    fun rows(content: TagList.Content): List<Any> = buildList {
        addAll(content.projects)
        if (content.projects.isNotEmpty() && content.active.isNotEmpty()) add(TaskListKeys.AREA_PROJECTS_SPACER)
        addAll(content.active)
        addAll(content.logbook)
    }
}
