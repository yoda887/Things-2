package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * «Когда-нибудь»: какие задачи и их группы «по месту» — по областям, внутри области отложенные
 * проекты строками и задачи. Заголовок области — только если у неё есть свои отложенные проекты или
 * задачи; её активные проекты с отложенными задачами показываются и без него. Закреплено тестами PlaceGroupsTest.
 */
object SomedayList {

    /**
     * Отложенная задача: открыта и не удалена. Задачи отложенного проекта отдельно не показываются —
     * проект целиком стоит строкой (как в Things), его задачи видны в самом проекте.
     */
    fun includes(item: Item, bounds: DayBounds, somedayProjectIds: Set<String>): Boolean =
        bounds.isSomeday(item) && !item.trashed && !ListRules.isClosed(item) && item.projectId !in somedayProjectIds

    fun order(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<ItemWithChecklist> =
        PlaceGroups.order(tasks, projects, areas)

    fun groups(tasks: List<ItemWithChecklist>, projects: List<Item>, areas: List<Area>): List<PlaceGroups.Group> =
        PlaceGroups.group(tasks, projects, areas, PlaceGroups.somedayProjects(projects), areaHeaderOnlyWithOwnItems = true)

    /** Есть ли что показать: задачи или отложенные проекты. */
    fun hasContent(tasks: List<ItemWithChecklist>, projects: List<Item>): Boolean =
        tasks.isNotEmpty() || PlaceGroups.somedayProjects(projects).isNotEmpty()
}
