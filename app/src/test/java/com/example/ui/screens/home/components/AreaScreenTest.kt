package com.example.ui.screens.home.components

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Правила экрана области: разделы, порядок задач и строки списка. */
class AreaScreenTest {

    private val bounds = DayBounds.now()
    private val dayMs = 24 * 3600 * 1000L
    private val tomorrow = bounds.endOfToday + dayMs / 2

    private fun task(id: String, area: String? = "a", project: String? = null, start: Int = Item.START_ANYTIME, startDate: Long? = null) =
        ItemWithChecklist(Item(id = id, title = id, areaId = area, projectId = project, start = start, startDate = startDate))

    private val current = task("now")
    private val later = task("later", startDate = tomorrow)
    private val someday = task("someday", start = Item.START_SOMEDAY)
    private val inProject = task("in-project", project = "p")
    private val otherArea = task("other", area = "b")
    private val project = Item(id = "p", type = Item.TYPE_PROJECT, title = "p", areaId = "a")

    private fun ids(rows: List<Any>) = rows.map {
        when (it) {
            is ItemWithChecklist -> it.item.id
            is Item -> "project:${it.id}"
            else -> it.toString()
        }
    }

    @Test
    fun sections() {
        assertEquals(AreaScreen.Section.CURRENT, AreaScreen.sectionOf(current.item, bounds))
        assertEquals(AreaScreen.Section.UPCOMING, AreaScreen.sectionOf(later.item, bounds))
        assertEquals(AreaScreen.Section.SOMEDAY, AreaScreen.sectionOf(someday.item, bounds))
    }

    @Test
    fun content_onlyOwnTasksOfTheArea() {
        val content = AreaScreen.content("a", listOf(someday, inProject, current, otherArea, later), listOf(project), bounds)
        assertEquals(listOf("now"), content.current.map { it.item.id })
        assertEquals(listOf("later"), content.upcoming.map { it.item.id })
        assertEquals(listOf("someday"), content.someday.map { it.item.id })
        assertEquals(listOf(project), content.projects)
    }

    @Test
    fun order_ownTasksBySectionThenTheRest() {
        val order = AreaScreen.order(listOf(someday, inProject, later, current), "a", bounds)
        assertEquals(listOf("now", "later", "someday", "in-project"), order.map { it.item.id })
    }

    @Test
    fun rows_withLaterShown_andHidden() {
        val content = AreaScreen.content("a", listOf(current, later, someday), listOf(project), bounds)
        assertEquals(
            listOf("project:p", TaskListKeys.AREA_PROJECTS_SPACER, "now",
                TaskListKeys.AREA_UPCOMING_HEADING, "later", TaskListKeys.AREA_SOMEDAY_HEADING, "someday",
                TaskListKeys.AREA_LATER_TOGGLE),
            ids(AreaScreen.rows(content, isLaterHidden = false))
        )
        assertEquals(
            listOf("project:p", TaskListKeys.AREA_PROJECTS_SPACER, "now", TaskListKeys.AREA_LATER_TOGGLE),
            ids(AreaScreen.rows(content, isLaterHidden = true))
        )
    }

    @Test
    fun rows_noSpacerWithoutTasks_noToggleWithoutLater() {
        assertEquals(listOf("project:p"), ids(AreaScreen.rows(AreaScreen.content("a", emptyList(), listOf(project), bounds), false)))
        assertEquals(listOf("now"), ids(AreaScreen.rows(AreaScreen.content("a", listOf(current), emptyList(), bounds), false)))
    }

    @Test
    fun isEmpty() {
        assertTrue(AreaScreen.content("a", listOf(inProject, otherArea), emptyList(), bounds).isEmpty)
        assertFalse(AreaScreen.content("a", emptyList(), listOf(project), bounds).isEmpty)
    }
}
