package com.example.ui.screens.home.components

import com.example.domain.lists.AreaList

/** Строки экрана области (правила — в [AreaList]). */
object AreaScreen {

    /** Проекты, отступ, текущие задачи, затем «Планы», «Когда-нибудь» и переключатель «Скрыть/Показать». */
    fun rows(content: AreaList.Content, isLaterHidden: Boolean): List<Any> = buildList {
        addAll(content.projects)
        val hasTasks = content.current.isNotEmpty() || content.hasLater
        if (content.projects.isNotEmpty() && hasTasks) add(TaskListKeys.AREA_PROJECTS_SPACER)
        addAll(content.current)
        if (content.hasLater) {
            if (!isLaterHidden) {
                if (content.upcoming.isNotEmpty()) {
                    add(TaskListKeys.AREA_UPCOMING_HEADING)
                    addAll(content.upcoming)
                }
                if (content.someday.isNotEmpty()) {
                    add(TaskListKeys.AREA_SOMEDAY_HEADING)
                    addAll(content.someday)
                }
            }
            add(TaskListKeys.AREA_LATER_TOGGLE)
        }
    }
}
