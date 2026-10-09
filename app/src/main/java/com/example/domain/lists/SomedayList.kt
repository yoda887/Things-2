package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * «Когда-нибудь»: какие задачи и их группы «по месту» — по областям, внутри области отложенные
 * проекты строками и задачи. Закреплено тестами PlaceGroupsTest.
 */
object SomedayList {

    fun includes(item: Item, bounds: DayBounds): Boolean = bounds.isSomeday(item)

    fun order(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<ItemWithChecklist> =
        PlaceGroups.order(tasks, projects, areas)

    fun groups(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<PlaceGroups.Group> =
        PlaceGroups.group(tasks, projects, areas, PlaceGroups.somedayProjects(projects))

    /** Есть ли что показать: задачи или отложенные проекты. */
    fun hasContent(tasks: List<ItemWithChecklist>, projects: List<Item>): Boolean =
        tasks.isNotEmpty() || PlaceGroups.somedayProjects(projects).isNotEmpty()
}
