package com.example.ui.screens.home.components

import com.example.domain.lists.ProjectList
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Экран проекта: задачи, заголовки, порядок и строки. */
class ProjectScreenTest {

    private fun task(id: String, heading: String? = null, project: String = "p", status: Int = Item.STATUS_OPEN) =
        ItemWithChecklist(Item(id = id, title = id, projectId = project, headingId = heading, status = status))

    private fun heading(id: String, order: Int, status: Int = Item.STATUS_OPEN) =
        Item(id = id, title = id, type = Item.TYPE_HEADING, projectId = "p", sortOrder = order, status = status)

    private fun ids(rows: List<Any>) = rows.map {
        when (it) {
            is ItemWithChecklist -> it.item.id
            is ProjectHeadingItem -> "H:" + it.heading.id
            else -> it.toString()
        }
    }

    @Test
    fun includes_openTasksOfTheProject() {
        assertTrue(ProjectList.includes(task("a").item, "p"))
        assertFalse(ProjectList.includes(task("b", project = "other").item, "p"))
        assertFalse(ProjectList.includes(task("c", status = Item.STATUS_COMPLETED).item, "p"))
        assertFalse(ProjectList.includes(task("d").item, null))
    }

    @Test
    fun headings_openOnlyInOrder() {
        val all = listOf(heading("h2", 2), heading("h1", 1), heading("done", 0, Item.STATUS_COMPLETED),
            heading("other", 0).copy(projectId = "x"))
        assertEquals(listOf("h1", "h2"), ProjectList.headings(all, "p").map { it.id })
    }

    @Test
    fun rows_looseThenHeadingsWithTasks_draggedHeadingCollapsed() {
        val headings = listOf(heading("h1", 1), heading("h2", 2))
        val tasks = listOf(task("x", "h2"), task("loose"), task("y", "h1"))
        val ordered = ProjectList.order(tasks, headings)
        assertEquals(listOf("loose", "H:h1", "y", "H:h2", "x"), ids(ProjectScreen.rows(ordered, headings, null)))
        assertEquals(listOf("loose", "H:h1", "H:h2", "x"),
            ids(ProjectScreen.rows(ordered, headings, TaskListKeys.HEADING_PREFIX + "h1")))
        assertEquals(listOf("x", "loose"), ids(ProjectScreen.rows(listOf(task("x"), task("loose")), emptyList(), null)))
    }

    @Test
    fun isEmpty() {
        assertTrue(ProjectScreen.isEmpty(emptyList(), emptyList()))
        assertFalse(ProjectScreen.isEmpty(emptyList(), listOf(heading("h", 0))))
    }
}
