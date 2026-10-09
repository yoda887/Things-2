package com.example.ui.screens.home.components

import com.example.domain.lists.LogbookList
import com.example.domain.lists.InboxList
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
        assertTrue(InboxList.includes(inbox))
        assertFalse(InboxList.includes(inbox.copy(status = Item.STATUS_COMPLETED)))
        assertFalse(InboxList.includes(inbox.copy(status = Item.STATUS_CANCELLED)))
        assertFalse(InboxList.includes(inbox.copy(trashed = true)))
        assertFalse(InboxList.includes(inbox.copy(start = Item.START_ANYTIME)))
        assertFalse(InboxList.includes(inbox.copy(startDate = 1L)))
        assertFalse(InboxList.includes(inbox.copy(dueDate = 1L)))
        assertFalse(InboxList.includes(inbox.copy(projectId = "p")))
        assertFalse(InboxList.includes(inbox.copy(type = Item.TYPE_PROJECT)))
    }

    @Test
    fun logbook_closedTasksOnly() {
        assertTrue(LogbookList.includes(inbox.copy(status = Item.STATUS_COMPLETED)))
        assertTrue(LogbookList.includes(inbox.copy(status = Item.STATUS_CANCELLED)))
        assertFalse(LogbookList.includes(inbox))
        assertFalse(LogbookList.includes(inbox.copy(status = Item.STATUS_COMPLETED, trashed = true)))
        // Завершённый проект — не задача: он идёт в «Журнал» строкой проекта
        assertFalse(LogbookList.includes(Item(id = "p", title = "p", type = Item.TYPE_PROJECT, status = Item.STATUS_COMPLETED)))
    }

    @Test
    fun logbook_rows_tasksAndProjectsNewestFirst() {
        val old = ItemWithChecklist(Item(id = "old", title = "old", status = Item.STATUS_COMPLETED, stopDate = 100))
        val new = ItemWithChecklist(Item(id = "new", title = "new", status = Item.STATUS_CANCELLED, stopDate = 300))
        val project = Item(id = "p", title = "p", type = Item.TYPE_PROJECT, status = Item.STATUS_COMPLETED, stopDate = 200)
        val openProject = Item(id = "open", title = "open", type = Item.TYPE_PROJECT)
        val rows = LogbookList.entries(listOf(old, new), listOf(project, openProject))
        assertEquals(listOf("new", "project:p", "old"), rows.map { if (it is ItemWithChecklist) it.item.id else "project:" + (it as Item).id })
        assertEquals(listOf("new", "old"), LogbookList.order(listOf(old, new)).map { it.item.id })
    }
}
