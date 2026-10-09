package com.example.ui.screens.home.components

import com.example.data.model.Item
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Входящие» и «Журнал»: какие элементы на экране. */
class SimpleScreensTest {

    private val inbox = Item(id = "i", title = "i", start = Item.START_INBOX)

    @Test
    fun inbox() {
        assertTrue(InboxScreen.includes(inbox))
        assertFalse(InboxScreen.includes(inbox.copy(status = Item.STATUS_COMPLETED)))
        assertFalse(InboxScreen.includes(inbox.copy(start = Item.START_ANYTIME)))
        assertFalse(InboxScreen.includes(inbox.copy(startDate = 1L)))
        assertFalse(InboxScreen.includes(inbox.copy(dueDate = 1L)))
        assertFalse(InboxScreen.includes(inbox.copy(projectId = "p")))
        assertFalse(InboxScreen.includes(inbox.copy(type = Item.TYPE_PROJECT)))
    }

    @Test
    fun logbook() {
        assertTrue(LogbookScreen.includes(inbox.copy(status = Item.STATUS_COMPLETED)))
        assertFalse(LogbookScreen.includes(inbox))
    }
}
