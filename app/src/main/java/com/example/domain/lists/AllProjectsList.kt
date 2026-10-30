package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item

/**
 * Особый список «Все проекты», как All Projects в Things: открытые проекты по областям (в порядке
 * главного экрана), в каждой группе сначала активные ([HomeList.showsProject]), затем более поздние —
 * запланированные по дате старта, шаблоны повторяющихся проектов и отложенные в «Когда-нибудь»; внизу —
 * завершённые и отменённые, свежие сверху.
 * Закреплено тестами AllProjectsListTest.
 */
object AllProjectsList {

    /** Группа: проекты области [area] (null — проекты без области). */
    data class Group(val area: Area?, val active: List<Item>, val later: List<Item>)

    data class Content(val groups: List<Group>, val logged: List<Item>)

    /** @param templates шаблоны повторяющихся проектов (их нет среди проектов) — в более поздних своей области */
    fun content(projects: List<Item>, areas: List<Area>, bounds: DayBounds = DayBounds.now(), templates: List<Item> = emptyList()): Content {
        val liveAreas = areas.filter { !it.trashed }
        val areaIds = liveAreas.map { it.id }.toSet()
        val openProjects = projects.filter { it.type == Item.TYPE_PROJECT && ListRules.isOpenProject(it) }
        val liveTemplates = templates.filter { RepeatingList.includesProject(it) && openProjects.none { p -> p.id == it.id } }
        val open = openProjects + liveTemplates
        fun group(area: Area?, list: List<Item>): Group? {
            if (list.isEmpty()) return null
            val (repeating, plain) = list.partition { RepeatingList.includesProject(it) }
            val (active, later) = plain.partition { HomeList.showsProject(it, bounds) }
            // Более поздние, как в Things: сначала запланированные по дате старта, затем шаблоны повтора, затем отложенные
            val (scheduled, someday) = later.partition { it.start != Item.START_SOMEDAY && it.startDate != null }
            // Внутри дня — порядок дня, как в «Планах»; отложенные — в своём порядке (их можно переставлять перетаскиванием)
            return Group(
                area, active,
                scheduled.sortedWith(compareBy<Item>({ UpcomingOrder.dayOf(it.startDate!!) }, { UpcomingOrder.key(it) })) +
                    repeating.sortedBy { it.sortOrder } + someday.sortedBy { it.sortOrder }
            )
        }
        val groups = buildList {
            // Проекты без области (или чья область удалена) — сверху, как на главном экране
            group(null, open.filter { it.areaId == null || it.areaId !in areaIds })?.let(::add)
            liveAreas.forEach { area -> group(area, open.filter { it.areaId == area.id })?.let(::add) }
        }
        val logged = projects.filter { LogbookList.includesProject(it) }.sortedByDescending { LogbookList.closedAt(it) }
        return Content(groups, logged)
    }

    /**
     * Перетаскивание на «Пізніших проєктах»: проект [dragged] встаёт на место [target] — только в своём ряду: среди
     * отложенных (их sortOrder) или среди запланированных на тот же день (порядок дня, общий с «Планами»).
     * Шаблоны повторяющихся проектов не переставляются — их место задаёт правило.
     * @return более поздние проекты в новом порядке, с новыми номерами у переставленных; null — сюда нельзя
     */
    fun reorderLater(later: List<Item>, dragged: String, target: String, now: Long = System.currentTimeMillis()): List<Item>? {
        if (dragged == target) return null
        fun row(item: Item): Long? = when {
            item.type != Item.TYPE_PROJECT -> null
            item.start == Item.START_SOMEDAY || item.startDate == null -> Long.MIN_VALUE
            else -> UpcomingOrder.dayOf(item.startDate)
        }
        val from = later.firstOrNull { it.id == dragged } ?: return null
        val to = later.firstOrNull { it.id == target } ?: return null
        val fromRow = row(from) ?: return null
        if (row(to) != fromRow) return null
        val group = later.filter { row(it) == fromRow }.toMutableList()
        group.add(group.indexOfFirst { it.id == target }, group.removeAt(group.indexOfFirst { it.id == dragged }))
        val someday = fromRow == Long.MIN_VALUE
        // Прежние номера ряда переходят к новым местам; совпадающие или незаданные — нумеруются заново
        val numbered: List<Item> = if (someday) {
            val orders = group.map { it.sortOrder }.sorted()
            if (orders.distinct().size == orders.size) group.mapIndexed { i, p -> p.copy(sortOrder = orders[i]) }
            else group.mapIndexed { i, p -> p.copy(sortOrder = orders.first() + i) }
        } else {
            val orders = group.map { it.dayOrder }
            if (orders.none { it == null } && orders.distinct().size == orders.size) {
                val sorted = orders.filterNotNull().sorted()
                group.mapIndexed { i, p -> p.copy(dayOrder = sorted[i]) }
            } else {
                val renumbered = UpcomingOrder.renumber(group, now).associateBy { it.id }
                group.map { renumbered[it.id] ?: it }
            }
        }
        val byId = numbered.associateBy { it.id }
        val ids = group.map { it.id }
        var next = 0
        // Ряд встаёт на свои прежние места в общем списке
        return later.map { if (it.id in byId) byId.getValue(ids[next++]) else it }
    }

    /** Есть ли что показать. */
    fun hasContent(content: Content): Boolean = content.groups.isNotEmpty() || content.logged.isNotEmpty()
}
