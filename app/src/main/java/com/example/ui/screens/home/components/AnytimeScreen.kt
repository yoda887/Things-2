package com.example.ui.screens.home.components

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * «В любое время» — его правила в одном месте: какие задачи на нём (без задач проектов, отложенных
 * в «Когда-нибудь»), порядок и строки по группам «по месту» (PlaceGroups) и признак пустого экрана.
 * Заголовок области — только если у неё есть свои задачи. Закреплено тестами PlaceGroupsTest.
 */
object AnytimeScreen {

    /** Проекты, отложенные в «Когда-нибудь»: их задачи здесь не показываются. */
    fun somedayProjectIds(projects: List<Item>): Set<String> =
        projects.filter { it.start == Item.START_SOMEDAY }.map { it.id }.toSet()

    fun includes(item: Item, bounds: DayBounds, somedayProjectIds: Set<String>): Boolean =
        bounds.isAnytime(item) && item.projectId !in somedayProjectIds

    fun order(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<ItemWithChecklist> =
        PlaceGroups.order(tasks, projects, areas)

    fun rows(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<Any> =
        PlaceGroups.rows(PlaceGroups.group(tasks, projects, areas, areaHeaderOnlyWithOwnItems = true))

    fun isEmpty(tasks: List<ItemWithChecklist>): Boolean = tasks.isEmpty()
}
