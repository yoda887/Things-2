package com.example.ui.screens.home.components

import com.example.domain.lists.TagList
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Правила экрана тега: какие задачи и проекты на нём и в каком порядке. */
class TagScreenTest {

    private fun task(id: String, tags: String = "work", status: Int = Item.STATUS_OPEN, trashed: Boolean = false, stop: Long? = null) =
        ItemWithChecklist(Item(id = id, title = id, cachedTags = tags, status = status, trashed = trashed, stopDate = stop))

    private fun project(id: String, tags: String = "work", status: Int = Item.STATUS_OPEN) =
        Item(id = id, type = Item.TYPE_PROJECT, title = id, cachedTags = tags, status = status)

    private fun ids(rows: List<Any>) = rows.map {
        when (it) {
            is ItemWithChecklist -> it.item.id
            is Item -> "project:${it.id}"
            else -> it.toString()
        }
    }

    @Test
    fun isActiveTask() {
        assertTrue(TagList.isActiveTask(task("a").item, "work"))
        assertFalse(TagList.isActiveTask(task("b", tags = "home").item, "work"))
        assertFalse(TagList.isActiveTask(task("c", status = Item.STATUS_COMPLETED).item, "work"))
        assertFalse(TagList.isActiveTask(task("d", status = Item.STATUS_CANCELLED).item, "work"))
        assertFalse(TagList.isActiveTask(task("e", trashed = true).item, "work"))
        assertFalse(TagList.isActiveTask(project("p").copy(), "work"))
    }

    @Test
    fun rows_openProjects_spacer_openTasks_thenLogbookNewestFirst() {
        val open = task("open")
        val oldDone = task("old", status = Item.STATUS_COMPLETED, stop = 100)
        val newCancelled = task("new", status = Item.STATUS_CANCELLED, stop = 200)
        val otherTagDone = task("other", tags = "home", status = Item.STATUS_COMPLETED, stop = 300)
        val trashedDone = task("trash", status = Item.STATUS_COMPLETED, trashed = true, stop = 400)
        val content = TagList.content(
            "work",
            tasks = listOf(open),
            allTasks = listOf(open, oldDone, newCancelled, otherTagDone, trashedDone),
            projects = listOf(project("p"), project("done", status = Item.STATUS_COMPLETED), project("home", tags = "home"))
        )
        assertEquals(
            listOf("project:p", TaskListKeys.AREA_PROJECTS_SPACER, "open", "new", "old"),
            ids(TagScreen.rows(content))
        )
    }

    @Test
    fun noSpacerWithoutOpenTasks_andEmpty() {
        val onlyProject = TagList.content("work", emptyList(), emptyList(), listOf(project("p")))
        assertEquals(listOf("project:p"), ids(TagScreen.rows(onlyProject)))
        assertTrue(TagList.content("work", emptyList(), listOf(task("x", tags = "home")), emptyList()).isEmpty)
    }
}
