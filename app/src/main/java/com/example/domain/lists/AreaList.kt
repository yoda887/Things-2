package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Область: что в ней показывается, в каком порядке и на какие разделы делится — активные проекты
 * области, затем её задачи без проекта по разделам: текущие, «Планы», «Когда-нибудь». Проекты с датой
 * старта позже сегодняшнего дня стоят в «Планах» (по дате), отложенные — в «Когда-нибудь», перед задачами.
 * Закреплено тестами AreaScreenTest.
 */
object AreaList {

    /** Раздел задачи на экране области. */
    enum class Section { CURRENT, UPCOMING, SOMEDAY }

    fun sectionOf(item: Item, bounds: DayBounds): Section = when {
        item.start == Item.START_SOMEDAY -> Section.SOMEDAY
        // Шаблон повторяющейся задачи — в «Планах», как шаблоны повторяющихся проектов: его следующий повтор впереди
        RepeatingList.includes(item) -> Section.UPCOMING
        bounds.isUpcoming(item) -> Section.UPCOMING
        else -> Section.CURRENT
    }

    /**
     * Задачи списка экрана: невыполненные задачи области (и её проектов — они идут после своих, см. [order]) и шаблоны
     * её повторяющихся задач.
     */
    fun includes(item: Item, areaId: String?): Boolean =
        areaId != null && item.areaId == areaId && !item.isClosed &&
            (item.type == Item.TYPE_TASK || RepeatingList.includes(item))

    /** Задача стоит в области сама, а не в одном из её проектов. */
    fun isOwnTask(item: Item, areaId: String?): Boolean =
        areaId != null && item.areaId == areaId && item.projectId.isNullOrEmpty()

    /** Содержимое экрана, разложенное по разделам. */
    data class Content(
        val projects: List<Item>,
        val current: List<ItemWithChecklist>,
        val upcoming: List<ItemWithChecklist>,
        val someday: List<ItemWithChecklist>,
        /** Запланированные проекты — в «Планах», по дате старта */
        val upcomingProjects: List<Item> = emptyList(),
        /** Отложенные проекты — в «Когда-нибудь» */
        val somedayProjects: List<Item> = emptyList()
    ) {
        val isEmpty: Boolean get() = projects.isEmpty() && current.isEmpty() && !hasLater
        val hasLater: Boolean get() =
            upcoming.isNotEmpty() || someday.isNotEmpty() || upcomingProjects.isNotEmpty() || somedayProjects.isNotEmpty()
    }

    /** @param templates шаблоны повторяющихся проектов (их нет среди проектов) — в «Планах» области, за запланированными */
    fun content(
        areaId: String?,
        tasks: List<ItemWithChecklist>,
        projects: List<Item>,
        bounds: DayBounds = DayBounds.now(),
        templates: List<Item> = emptyList(),
        // Фильтр по тегу над списком: проекты (и шаблоны проектов) — только с этим тегом, как задачи
        tagFilter: String? = null
    ): Content {
        fun tagged(item: Item) = tagFilter == null ||
            item.tags.any { com.example.domain.tag.TagTitles.key(it) == com.example.domain.tag.TagTitles.key(tagFilter) }
        val own = tasks.filter { isOwnTask(it.item, areaId) && tagged(it.item) }
        val bySection = own.groupBy { sectionOf(it.item, bounds) }
        val areaProjects = projects.filter { areaId != null && it.areaId == areaId && ListRules.isOpenProject(it) && tagged(it) }
        val (active, later) = areaProjects.partition { HomeList.showsProject(it, bounds) }
        val (someday, scheduled) = later.partition { it.start == Item.START_SOMEDAY }
        return Content(
            projects = active,
            current = bySection[Section.CURRENT].orEmpty(),
            upcoming = bySection[Section.UPCOMING].orEmpty(),
            someday = bySection[Section.SOMEDAY].orEmpty(),
            upcomingProjects = scheduled.sortedBy { it.startDate } + templates.filter { t ->
                areaId != null && t.areaId == areaId && RepeatingList.includesProject(t) && tagged(t) && areaProjects.none { it.id == t.id }
            }.sortedBy { it.sortOrder },
            somedayProjects = someday
        )
    }

    /**
     * Задачи в порядке экрана: свои задачи области по разделам, за ними — задачи её проектов
     * (на экране их нет, но в списке экрана они остаются, как и раньше). Порядок внутри раздела не меняется.
     */
    fun order(tasks: List<ItemWithChecklist>, areaId: String?, bounds: DayBounds = DayBounds.now()): List<ItemWithChecklist> {
        val (own, rest) = tasks.partition { isOwnTask(it.item, areaId) }
        return own.sortedBy { sectionOf(it.item, bounds).ordinal } + rest
    }
}
