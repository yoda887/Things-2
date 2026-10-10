package com.example.ui.screens.home.components

import com.example.domain.lists.PlaceGroups
import com.example.domain.lists.SomedayList
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Test

/** Группы «по месту» — в порядке главного экрана. */
class PlaceGroupsTest {

    private fun task(id: String, project: String? = null, area: String? = null) =
        ItemWithChecklist(Item(id = id, title = id, projectId = project, areaId = area))

    private fun project(id: String, area: String? = null, trashed: Boolean = false) =
        Item(id = id, type = Item.TYPE_PROJECT, title = id, areaId = area, trashed = trashed)

    private val work = Area(id = "work", title = "Work", sortOrder = 1)
    private val home = Area(id = "home", title = "Home", sortOrder = 0)
    // Порядок списков — как их отдаёт база (по sortOrder): Home, затем Work
    private val areas = listOf(home, work)
    private val projects = listOf(project("p1"), project("w1", "work"), project("h1", "home"), project("p2"))

    private fun shape(groups: List<PlaceGroups.Group>) = groups.map { g ->
        (g.project?.id ?: g.area?.id ?: "-") to g.tasks.map { it.item.id }
    }

    @Test
    fun order_looseThenProjectsWithoutAreaThenAreasWithTheirProjects() {
        val tasks = listOf(
            task("t-w1", project = "w1"), task("t-loose"), task("t-work", area = "work"),
            task("t-p2", project = "p2"), task("t-h1", project = "h1"), task("t-p1", project = "p1")
        )
        assertEquals(
            listOf(
                "-" to listOf("t-loose"),
                "p1" to listOf("t-p1"), "p2" to listOf("t-p2"),
                "home" to emptyList<String>(), "h1" to listOf("t-h1"),
                "work" to listOf("t-work"), "w1" to listOf("t-w1")
            ),
            shape(PlaceGroups.group(tasks, projects, areas))
        )
    }

    @Test
    fun areaWithoutAnyTasks_hasNoHeader() {
        val groups = PlaceGroups.group(listOf(task("t-p1", project = "p1")), projects, areas)
        assertEquals(listOf("p1" to listOf("t-p1")), shape(groups))
    }

    @Test
    fun missingOrTrashedPlace_keepsTaskAmongLooseOnes() {
        val trashedProjects = projects + project("gone", trashed = true)
        val tasks = listOf(task("a", project = "gone"), task("b", project = "nowhere"), task("c", area = "nowhere"))
        assertEquals(listOf("-" to listOf("a", "b", "c")), shape(PlaceGroups.group(tasks, trashedProjects, areas)))
    }

    @Test
    fun order_keepsRelativeOrderInsideGroup() {
        val tasks = listOf(task("x", project = "p1"), task("y"), task("z", project = "p1"))
        assertEquals(listOf("y", "x", "z"), PlaceGroups.order(tasks, projects, areas).map { it.item.id })
    }

    @Test
    fun somedayProjectRows_goIntoTheirAreaOrAmongLooseOnes() {
        val rowNoArea = project("s0")
        val rowWork = project("s1", "work")
        val groups = PlaceGroups.group(listOf(task("t-p1", project = "p1")), projects, areas, listOf(rowNoArea, rowWork))
        assertEquals(
            listOf("-" to listOf("s0"), "p1" to emptyList(), "work" to listOf("s1")),
            groups.map { g -> (g.project?.id ?: g.area?.id ?: "-") to g.projectRows.map { it.id } }
        )
    }

    @Test
    fun anytime_areaHeaderOnlyWithItsOwnTasks_projectsStillShown() {
        val tasks = listOf(task("t-w1", project = "w1"), task("t-h1", project = "h1"), task("t-home", area = "home"))
        assertEquals(
            listOf("home" to listOf("t-home"), "h1" to listOf("t-h1"), "w1" to listOf("t-w1")),
            shape(PlaceGroups.group(tasks, projects, areas, areaHeaderOnlyWithOwnItems = true))
        )
    }

    @Test
    fun someday_areaHeaderOnlyWithOwnSomedayProjectsOrTasks() {
        val somedayHome = Item(id = "sh", type = Item.TYPE_PROJECT, title = "sh", areaId = "home", start = Item.START_SOMEDAY)
        // У Work только активный проект w1 с отложенной задачей — заголовка Work нет, проект w1 есть
        val groups = SomedayList.groups(listOf(task("t-w1", project = "w1")), projects + somedayHome, areas)
        assertEquals(
            listOf("home" to listOf("sh"), "w1" to emptyList()),
            groups.map { g -> (g.project?.id ?: g.area?.id ?: "-") to g.projectRows.map { it.id } }
        )
    }

    @Test
    fun someday_tasksOfSomedayProject_notListed_projectIsARow() {
        val bounds = com.example.data.model.DayBounds.now()
        val somedayIds = setOf("sp")
        fun t(project: String?) = Item(id = "t", title = "t", start = Item.START_SOMEDAY, projectId = project)
        assertEquals(false, SomedayList.includes(t("sp"), bounds, somedayIds))
        assertEquals(true, SomedayList.includes(t("active"), bounds, somedayIds))
        assertEquals(true, SomedayList.includes(t(null), bounds, somedayIds))
        assertEquals(false, SomedayList.includes(t(null).copy(status = Item.STATUS_CANCELLED), bounds, somedayIds))
        assertEquals(false, SomedayList.includes(t(null).copy(trashed = true), bounds, somedayIds))
    }
}
