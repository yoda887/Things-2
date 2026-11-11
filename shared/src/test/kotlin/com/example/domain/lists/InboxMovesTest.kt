package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Test

/** Ушедшие из «Входящих» задачи: кто попадает под плашку и кто выбывает. */
class InboxMovesTest {

    private fun inbox(id: String) = ItemWithChecklist(Item(id = id, title = id, start = Item.START_INBOX))
    private fun withProject(id: String) = ItemWithChecklist(Item(id = id, title = id, start = Item.START_ANYTIME, projectId = "p"))

    @Test
    fun movedOutTask_staysUntilOk_closedAndReturnedLeave() {
        val before = listOf(inbox("a"), inbox("b"), inbox("c"), inbox("d"))
        val previous = InboxMoves.inboxIds(before)
        val after = listOf(
            withProject("a"),
            ItemWithChecklist(Item(id = "b", title = "b", start = Item.START_INBOX, status = Item.STATUS_COMPLETED)),
            ItemWithChecklist(Item(id = "c", title = "c", start = Item.START_INBOX, trashed = true)),
            inbox("d")
        )
        val moved = InboxMoves.update(emptySet(), previous, after)
        assertEquals(setOf("a"), moved)

        // Следующее изменение не снимает плашку
        val later = InboxMoves.update(moved, InboxMoves.inboxIds(after), after)
        assertEquals(setOf("a"), later)

        // Вернули во «Входящие» — снова обычная задача
        val back = listOf(inbox("a"), inbox("d"))
        assertEquals(emptySet<String>(), InboxMoves.update(later, InboxMoves.inboxIds(after), back))
    }

    @Test
    fun ghosts_skipTaskStillShown() {
        val moved = setOf("a", "x")
        val tasks = listOf(withProject("a"), withProject("x"))
        assertEquals(listOf("x"), InboxMoves.ghosts(moved, tasks, shown = listOf(withProject("a"))).map { it.item.id })
    }
}
