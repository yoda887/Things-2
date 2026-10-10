package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item

/**
 * Особый список «Все проекты», как All Projects в Things: открытые проекты по областям (в порядке
 * главного экрана), в каждой группе сначала активные ([HomeList.showsProject]), затем более поздние —
 * запланированные по дате старта и отложенные в «Когда-нибудь»; внизу — завершённые и отменённые,
 * свежие сверху.
 * Закреплено тестами AllProjectsListTest.
 */
object AllProjectsList {

    /** Группа: проекты области [area] (null — проекты без области). */
    data class Group(val area: Area?, val active: List<Item>, val later: List<Item>)

    data class Content(val groups: List<Group>, val logged: List<Item>)

    fun content(projects: List<Item>, areas: List<Area>, bounds: DayBounds = DayBounds.now()): Content {
        val liveAreas = areas.filter { !it.trashed }
        val areaIds = liveAreas.map { it.id }.toSet()
        val open = projects.filter { it.type == Item.TYPE_PROJECT && ListRules.isOpenProject(it) }
        fun group(area: Area?, list: List<Item>): Group? {
            if (list.isEmpty()) return null
            val (active, later) = list.partition { HomeList.showsProject(it, bounds) }
            // Более поздние, как в Things: сначала запланированные по дате старта, затем отложенные
            val (scheduled, someday) = later.partition { it.start != Item.START_SOMEDAY && it.startDate != null }
            return Group(area, active, scheduled.sortedBy { it.startDate } + someday)
        }
        val groups = buildList {
            // Проекты без области (или чья область удалена) — сверху, как на главном экране
            group(null, open.filter { it.areaId == null || it.areaId !in areaIds })?.let(::add)
            liveAreas.forEach { area -> group(area, open.filter { it.areaId == area.id })?.let(::add) }
        }
        val logged = projects.filter { LogbookList.includesProject(it) }.sortedByDescending { LogbookList.closedAt(it) }
        return Content(groups, logged)
    }

    /** Есть ли что показать. */
    fun hasContent(content: Content): Boolean = content.groups.isNotEmpty() || content.logged.isNotEmpty()
}
