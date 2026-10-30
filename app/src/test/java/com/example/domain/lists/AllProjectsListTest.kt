package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.DayBounds
import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Test

/** «Все проекты»: открытые по областям — активные, затем более поздние; внизу — закрытые. */
class AllProjectsListTest {

    private val bounds = DayBounds.now()
    private val day = 24 * 3600 * 1000L
    private fun project(id: String, area: String? = null, start: Int = Item.START_ANYTIME, startDate: Long? = null,
                        status: Int = Item.STATUS_OPEN, stopDate: Long? = null, trashed: Boolean = false) =
        Item(id = id, title = id, type = Item.TYPE_PROJECT, areaId = area, start = start, startDate = startDate,
            status = status, stopDate = stopDate, trashed = trashed)

    private val home = Area(id = "home", title = "Home", sortOrder = 0)
    private val work = Area(id = "work", title = "Work", sortOrder = 1)

    @Test
    fun groupsByArea_activeThenLater_loggedNewestFirst() {
        val projects = listOf(
            project("w1", "work"), project("loose"), project("hSomeday", "home", start = Item.START_SOMEDAY),
            project("h1", "home"), project("wFuture", "work", startDate = bounds.endOfToday + day),
            project("done1", "work", status = Item.STATUS_COMPLETED, stopDate = 1000L),
            project("done2", status = Item.STATUS_CANCELLED, stopDate = 2000L),
            project("gone", "home", trashed = true)
        )
        val content = AllProjectsList.content(projects, listOf(home, work), bounds)
        assertEquals(
            listOf(Triple(null, listOf("loose"), emptyList()), Triple("home", listOf("h1"), listOf("hSomeday")),
                Triple("work", listOf("w1"), listOf("wFuture"))),
            content.groups.map { g -> Triple(g.area?.id, g.active.map { it.id }, g.later.map { it.id }) }
        )
        assertEquals(listOf("done2", "done1"), content.logged.map { it.id })
    }

    @Test
    fun areaWithoutOpenProjects_hasNoGroup() {
        val content = AllProjectsList.content(listOf(project("w1", "work")), listOf(home, work), bounds)
        assertEquals(listOf("work"), content.groups.map { it.area?.id })
    }

    @Test
    fun repeatingProjectTemplates_inLaterOfTheirArea() {
        val template = Item(id = "tpl", title = "tpl", type = Item.TYPE_PROJECT_TEMPLATE, areaId = "work")
        val gone = Item(id = "tplGone", title = "tplGone", type = Item.TYPE_PROJECT_TEMPLATE, trashed = true)
        val content = AllProjectsList.content(
            listOf(project("w1", "work"), project("wSomeday", "work", start = Item.START_SOMEDAY)),
            listOf(home, work), bounds, templates = listOf(template, gone, project("w1", "work"))
        )
        assertEquals(listOf("w1"), content.groups.single().active.map { it.id })
        assertEquals(listOf("tpl", "wSomeday"), content.groups.single().later.map { it.id })
    }

    @Test
    fun reorderLater_somedayAmongSomeday_dayAmongSameDay_notAcross() {
        val tomorrow = bounds.endOfToday + day
        val later = listOf(
            project("d1", startDate = tomorrow).copy(dayOrder = 10), project("d2", startDate = tomorrow).copy(dayOrder = 20),
            project("far", startDate = tomorrow + 3 * day),
            project("s1", start = Item.START_SOMEDAY).copy(sortOrder = 5), project("s2", start = Item.START_SOMEDAY).copy(sortOrder = 9)
        )
        // Отложенные меняются местами и номерами
        val someday = AllProjectsList.reorderLater(later, "s2", "s1")!!
        assertEquals(listOf("d1", "d2", "far", "s2", "s1"), someday.map { it.id })
        assertEquals(listOf(5, 9), someday.drop(3).map { it.sortOrder })
        // Проекты одного дня — порядком дня
        val sameDay = AllProjectsList.reorderLater(later, "d2", "d1")!!
        assertEquals(listOf("d2" to 10L, "d1" to 20L), sameDay.take(2).map { it.id to it.dayOrder })
        // В другой день и в другой раздел — нельзя
        assertEquals(null, AllProjectsList.reorderLater(later, "d1", "far"))
        assertEquals(null, AllProjectsList.reorderLater(later, "s1", "d1"))
    }
}
