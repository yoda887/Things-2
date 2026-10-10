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

    fun groups(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<PlaceGroups.Group> =
        PlaceGroups.group(tasks, projects, areas, areaHeaderOnlyWithOwnItems = true)
}
