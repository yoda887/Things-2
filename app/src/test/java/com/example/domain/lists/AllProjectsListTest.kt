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
}
