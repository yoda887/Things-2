package com.example.ui.screens.home.components

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * «Когда-нибудь» — его правила в одном месте: какие задачи на нём, порядок и строки по группам
 * «по месту» (PlaceGroups) — по областям, внутри области отложенные проекты строками и задачи —
 * и признак пустого экрана. Закреплено тестами PlaceGroupsTest.
 */
object SomedayScreen {

    fun includes(item: Item, bounds: DayBounds): Boolean = bounds.isSomeday(item)

    fun order(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<ItemWithChecklist> =
        PlaceGroups.order(tasks, projects, areas)

    fun rows(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<Any> =
        PlaceGroups.rows(PlaceGroups.group(tasks, projects, areas, PlaceGroups.somedayProjects(projects)))

    /** Экран не пуст и без задач, если есть отложенные проекты. */
    fun isEmpty(tasks: List<ItemWithChecklist>, projects: List<Item>): Boolean =
        tasks.isEmpty() && PlaceGroups.somedayProjects(projects).isEmpty()
}
