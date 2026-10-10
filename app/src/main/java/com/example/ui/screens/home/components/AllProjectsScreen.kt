package com.example.ui.screens.home.components

import com.example.domain.lists.AllProjectsList

/** Строка-переключатель списка (серым текстом): «Показати пізніші проєкти», «Показати завершені проєкти». */
data class ListToggleItem(val key: String, val label: String)

/**
 * Строки особого списка «Все проекты» (правила — в [AllProjectsList]): заголовок области, её активные
 * проекты, затем переключатель более поздних (и они сами, если раскрыты); внизу — переключатель
 * завершённых проектов и они сами.
 */
object AllProjectsScreen {

    /** Ключ переключателя поздних проектов группы (null — проекты без области). */
    fun laterKey(areaId: String?): String = TaskListKeys.LIST_TOGGLE_PREFIX + "later_" + (areaId ?: "none")

    val LOGGED_KEY: String = TaskListKeys.LIST_TOGGLE_PREFIX + "logged"

    fun rows(
        content: AllProjectsList.Content,
        expandedLater: Set<String>,
        showLogged: Boolean,
        showLaterLabel: (Int) -> String,
        hideLaterLabel: String,
        showLoggedLabel: String,
        hideLoggedLabel: String
    ): List<Any> = buildList {
        content.groups.forEach { group ->
            group.area?.let { area ->
                add(SearchSectionHeaderItem("ap_area_${area.id}", area.title, SearchSectionKind.AREA, area = area))
            }
            addAll(group.active)
            if (group.later.isNotEmpty()) {
                val key = laterKey(group.area?.id)
                val expanded = key in expandedLater
                if (expanded) addAll(group.later)
                add(ListToggleItem(key, if (expanded) hideLaterLabel else showLaterLabel(group.later.size)))
            }
        }
        if (content.logged.isNotEmpty()) {
            add(ListToggleItem(LOGGED_KEY, if (showLogged) hideLoggedLabel else showLoggedLabel))
            if (showLogged) addAll(content.logged)
        }
    }
}
