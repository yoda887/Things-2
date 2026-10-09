package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * «В любое время»: какие задачи (без задач проектов, отложенных в «Когда-нибудь») и их группы
 * «по месту» — заголовок области только если у неё есть свои задачи. Закреплено тестами PlaceGroupsTest.
 */
object AnytimeList {

    /** Проекты, отложенные в «Когда-нибудь»: их задачи здесь не показываются. */
    fun somedayProjectIds(projects: List<Item>): Set<String> =
        projects.filter { it.start == Item.START_SOMEDAY }.map { it.id }.toSet()

    fun includes(item: Item, bounds: DayBounds, somedayProjectIds: Set<String>): Boolean =
        bounds.isAnytime(item) && item.projectId !in somedayProjectIds

    fun order(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<ItemWithChecklist> =
        PlaceGroups.order(tasks, projects, areas)

    fun groups(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<PlaceGroups.Group> =
        PlaceGroups.group(tasks, projects, areas, areaHeaderOnlyWithOwnItems = true)
}
