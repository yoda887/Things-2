package com.example.domain.lists

import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Test

/** Поиск находит подзаголовки проектов: открытые, в открытых неудалённых проектах, по названию. */
class SearchHeadingsTest {

    private fun project(id: String, status: Int = Item.STATUS_OPEN, trashed: Boolean = false) =
        Item(id = id, type = Item.TYPE_PROJECT, title = id, status = status, trashed = trashed)

    private fun heading(id: String, title: String, projectId: String, status: Int = Item.STATUS_OPEN, trashed: Boolean = false) =
        Item(id = id, type = Item.TYPE_HEADING, title = title, projectId = projectId, status = status, trashed = trashed)

    @Test
    fun openHeadingsOfOpenProjects_byTitle() {
        val projects = listOf(project("p"), project("done", status = Item.STATUS_COMPLETED), project("bin", trashed = true))
        val headings = listOf(
            heading("h1", "Planning", "p"),
            heading("h2", "Shopping plan", "p"),
            heading("archived", "Planning old", "p", status = Item.STATUS_COMPLETED),
            heading("trashed", "Planning trash", "p", trashed = true),
            heading("inDone", "Planning", "done"),
            heading("inBin", "Planning", "bin"),
            heading("orphan", "Planning", "missing")
        )
        assertEquals(listOf("h1", "h2"), SearchList.headings(headings, projects, "plan").map { it.id })
        assertEquals(emptyList<Item>(), SearchList.headings(headings, projects, "  "))
    }
}
