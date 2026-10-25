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
        hideLaterLabel: (Int) -> String,
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
                add(ListToggleItem(key, if (expanded) hideLaterLabel(group.later.size) else showLaterLabel(group.later.size)))
            }
        }
        if (content.logged.isNotEmpty()) {
            add(ListToggleItem(LOGGED_KEY, if (showLogged) hideLoggedLabel else showLoggedLabel))
            if (showLogged) addAll(content.logged)
        }
    }

    /**
     * Плашка даты у запланированного проекта среди более поздних, как в Things: в ближайшую неделю —
     * день недели («пн»), дальше — число и месяц словом («18 жовт.») или цифрами («18.10.», [monthName] — настройка).
     * У отложенных плашки нет.
     */
    fun laterDateBadge(project: com.example.data.model.Item, now: Long = System.currentTimeMillis(), monthName: Boolean = true): String? {
        val start = project.startDate ?: return null
        if (project.start == com.example.data.model.Item.START_SOMEDAY) return null
        val weekAhead = now + 6 * 24 * 3600 * 1000L
        val pattern = if (start <= weekAhead) "EEE" else if (monthName) "d MMM" else "d.M."
        return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(java.util.Date(start))
    }

    /**
     * Строки экрана «Пізніші проєкти», как Later Projects в Things: разделы «Плани» (запланированные по дате, затем
     * шаблоны повторяющихся проектов) и «Колись» (отложенные) — заголовок со значком списка и линией; пустой раздел
     * не показывается. [later] — более поздние проекты в их порядке (AllProjectsList).
     */
    fun laterRows(later: List<com.example.data.model.Item>, upcomingTitle: String, somedayTitle: String): List<Any> {
        val (upcoming, someday) = later.partition {
            com.example.domain.lists.RepeatingList.includesProject(it) ||
                (it.start != com.example.data.model.Item.START_SOMEDAY && it.startDate != null)
        }
        return buildList {
            listOf(
                Triple(SearchSectionKind.UPCOMING, upcomingTitle, upcoming),
                Triple(SearchSectionKind.SOMEDAY, somedayTitle, someday)
            ).forEach { (kind, title, rows) ->
                if (rows.isEmpty()) return@forEach
                add(SearchSectionHeaderItem("later_hdr_${kind.name.lowercase()}", title, kind))
                addAll(rows)
            }
        }
    }
}
