package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * «В любое время»: какие задачи (все доступные сейчас, в том числе на сегодня и сегодня вечером, — как
 * Anytime в Things; без задач проектов, отложенных в «Когда-нибудь») и их группы «по месту» — заголовок
 * области только если у неё есть свои задачи. Закреплено тестами PlaceGroupsTest и AnytimeListTest.
 */
object AnytimeList {

    /** Проекты, отложенные в «Когда-нибудь»: их задачи здесь не показываются. */
    fun somedayProjectIds(projects: List<Item>): Set<String> =
        projects.filter { it.start == Item.START_SOMEDAY }.map { it.id }.toSet()

    /**
     * Задача доступна сейчас: открыта и не удалена, не отложена в «Когда-нибудь», не разобрана во «Входящих» и не
     * запланирована на будущее. Задачи «Сегодня» и «Сегодня вечером» сюда тоже входят (в строке — звезда
     * или луна), горящий дедлайн задачу отсюда не убирает.
     */
    fun includes(item: Item, bounds: DayBounds, somedayProjectIds: Set<String>): Boolean {
        if (item.type != Item.TYPE_TASK || item.trashed || ListRules.isClosed(item)) return false
        if (item.start == Item.START_SOMEDAY || item.projectId in somedayProjectIds) return false
        val startDate = item.startDate
        if (startDate != null) return startDate <= bounds.endOfToday
        // Без даты: «В любое время», «Сегодня» или задача проекта; задача «Входящих» без проекта — не разобрана
        return item.start == Item.START_ANYTIME || item.start == Item.START_TODAY || item.projectId != null
    }

    fun order(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<ItemWithChecklist> =
        PlaceGroups.order(tasks, projects, areas)

    /** Сколько задач видно в разделе проекта или области, пока его не раскрыли (как в Things). */
    const val GROUP_LIMIT = 3

    /**
     * Видимые задачи раздела и сколько скрыто. Задачи без проекта и области ([loose]) видны все; в разделе
     * проекта или области — первые [GROUP_LIMIT], пока раздел не раскрыт ([expanded]). Задачи из [keep] (раскрытая в
     * редакторе) видны всегда — сверх лимита.
     */
    fun visible(
        tasks: List<ItemWithChecklist>,
        loose: Boolean,
        expanded: Boolean,
        keep: Set<String> = emptySet()
    ): Pair<List<ItemWithChecklist>, Int> {
        if (loose || expanded || tasks.size <= GROUP_LIMIT) return tasks to 0
        val shown = tasks.filterIndexed { index, task -> index < GROUP_LIMIT || task.item.id in keep }
        return shown to (tasks.size - shown.size)
    }

    /** Раздел длиннее лимита — его можно раскрыть и свернуть обратно; задачи без проекта и области видны всегда */
    fun collapsible(tasks: List<ItemWithChecklist>, loose: Boolean): Boolean = !loose && tasks.size > GROUP_LIMIT

    fun groups(
        tasks: List<ItemWithChecklist>,
        projects: List<Item>,
        areas: List<Area>,
        pinned: Map<String, PlaceGroups.Place> = emptyMap()
    ): List<PlaceGroups.Group> =
        PlaceGroups.group(tasks, projects, areas, areaHeaderOnlyWithOwnItems = true, pinned = pinned)
}
