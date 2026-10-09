package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Входящие» и «Журнал»: какие элементы на экране и в каком порядке. */
class SimpleScreensTest {

    private val inbox = Item(id = "i", title = "i", start = Item.START_INBOX)

    @Test
    fun inbox() {
        assertTrue(InboxScreen.includes(inbox))
        assertFalse(InboxScreen.includes(inbox.copy(status = Item.STATUS_COMPLETED)))
        assertFalse(InboxScreen.includes(inbox.copy(status = Item.STATUS_CANCELLED)))
        assertFalse(InboxScreen.includes(inbox.copy(trashed = true)))
        assertFalse(InboxScreen.includes(inbox.copy(start = Item.START_ANYTIME)))
        assertFalse(InboxScreen.includes(inbox.copy(startDate = 1L)))
        assertFalse(InboxScreen.includes(inbox.copy(dueDate = 1L)))
        assertFalse(InboxScreen.includes(inbox.copy(projectId = "p")))
        assertFalse(InboxScreen.includes(inbox.copy(type = Item.TYPE_PROJECT)))
    }

    @Test
    fun logbook_closedTasksOnly() {
        assertTrue(LogbookScreen.includes(inbox.copy(status = Item.STATUS_COMPLETED)))
        assertTrue(LogbookScreen.includes(inbox.copy(status = Item.STATUS_CANCELLED)))
        assertFalse(LogbookScreen.includes(inbox))
        assertFalse(LogbookScreen.includes(inbox.copy(status = Item.STATUS_COMPLETED, trashed = true)))
        // Завершённый проект — не задача: он идёт в «Журнал» строкой проекта
        assertFalse(LogbookScreen.includes(Item(id = "p", title = "p", type = Item.TYPE_PROJECT, status = Item.STATUS_COMPLETED)))
    }

    @Test
    fun logbook_rows_tasksAndProjectsNewestFirst() {
        val old = ItemWithChecklist(Item(id = "old", title = "old", status = Item.STATUS_COMPLETED, stopDate = 100))
        val new = ItemWithChecklist(Item(id = "new", title = "new", status = Item.STATUS_CANCELLED, stopDate = 300))
        val project = Item(id = "p", title = "p", type = Item.TYPE_PROJECT, status = Item.STATUS_COMPLETED, stopDate = 200)
        val openProject = Item(id = "open", title = "open", type = Item.TYPE_PROJECT)
        val rows = LogbookScreen.rows(listOf(old, new), listOf(project, openProject))
        assertEquals(listOf("new", "project:p", "old"), rows.map { if (it is ItemWithChecklist) it.item.id else "project:" + (it as Item).id })
        assertEquals(listOf("new", "old"), LogbookScreen.order(listOf(old, new)).map { it.item.id })
    }
}
