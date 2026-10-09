package com.example.domain.edits

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.TaskSection
import com.example.domain.lists.AreaList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Правила изменения данных: новые задачи и проекты, перемещение, правки, порядок, подзаголовки. */
class EditsTest {

    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 10, 9, 30) }.timeInMillis
    private fun day(d: Int, h: Int = 0) = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, d, h, 0) }.timeInMillis

    @Test
    fun newTask_fieldsByPlace() {
        assertEquals(Item.START_INBOX, NewTasks.create(NewTaskPlace.Inbox, "a", now).start)
        NewTasks.create(NewTaskPlace.Today, "b", now).let { assertEquals(Item.START_TODAY, it.start); assertEquals(now, it.startDate) }
        NewTasks.create(NewTaskPlace.Upcoming(day(12)), "c", now).let { assertEquals(Item.START_ANYTIME, it.start); assertEquals(day(12), it.startDate) }
        assertEquals(Item.START_SOMEDAY, NewTasks.create(NewTaskPlace.Someday, "d", now).start)
        NewTasks.create(NewTaskPlace.Project("p"), "e", now).let { assertEquals("p", it.projectId); assertEquals(Item.START_INBOX, it.start) }
        NewTasks.create(NewTaskPlace.Area("ar"), "f", now).let { assertEquals("ar", it.areaId); assertEquals(Item.START_ANYTIME, it.start) }
        assertEquals("work", NewTasks.create(NewTaskPlace.Tag("work"), "g", now).cachedTags)
        assertEquals("h", NewTasks.create(NewTaskPlace.Inbox, "h", now).id)
    }

    @Test
    fun newTask_atDrop() {
        val base = NewTasks.create(NewTaskPlace.Anytime, "x", now)
        assertEquals(Item.START_SOMEDAY, NewTasks.atDrop(base, DropSpot(areaSection = AreaList.Section.SOMEDAY), now).start)
        assertEquals(day(11, 12), NewTasks.atDrop(base, DropSpot(areaSection = AreaList.Section.UPCOMING), now).startDate)
        val project = Item(id = "p", type = Item.TYPE_PROJECT, title = "p", areaId = "ar")
        NewTasks.atDrop(base, DropSpot(groupProject = project), now).let { assertEquals("p", it.projectId); assertEquals("ar", it.areaId) }
        NewTasks.atDrop(base, DropSpot(groupAreaId = "ar2"), now).let { assertNull(it.projectId); assertEquals("ar2", it.areaId) }
        NewTasks.atDrop(base, DropSpot(headingId = "h", isTonight = true, startDate = day(15)), now).let {
            assertEquals("h", it.headingId); assertTrue(it.isTonight); assertEquals(day(15), it.startDate)
        }
    }

    @Test
    fun defaultUpcomingDate_earliestOrTomorrow() {
        assertEquals(day(13), NewTasks.defaultUpcomingDate(listOf(day(20), day(13)), now))
        assertEquals(day(11), NewTasks.defaultUpcomingDate(emptyList(), now))
    }

    @Test
    fun moveTask() {
        val inbox = Item(id = "t", title = "t", start = Item.START_INBOX)
        TaskMoves.moved(inbox, MoveTarget.Place("p", null), now).let { assertEquals("p", it.projectId); assertEquals(Item.START_ANYTIME, it.start) }
        val dated = Item(id = "t", title = "t", start = Item.START_ANYTIME, projectId = "p", startDate = day(12), dueDate = day(14))
        TaskMoves.moved(dated, MoveTarget.Inbox, now).let {
            assertNull(it.projectId); assertNull(it.startDate); assertNull(it.dueDate)
            assertEquals(Item.START_INBOX, it.start); assertEquals(now, it.modificationDate)
        }
    }

    @Test
    fun sortOrders_insertBetweenOrRenumber() {
        val a = Item(id = "a", title = "a", sortOrder = 0)
        val b = Item(id = "b", title = "b", sortOrder = 5)
        val n = Item(id = "n", title = "n")
        SortOrders.insert(listOf(a, b), 1, n).let { assertEquals(1, it.created.sortOrder); assertTrue(it.reordered.isEmpty()) }
        val c = Item(id = "c", title = "c", sortOrder = 1)
        SortOrders.insert(listOf(a, c), 1, n).let {
            assertEquals(1, it.created.sortOrder)
            assertEquals(listOf("c" to 2), it.reordered.map { r -> r.id to r.sortOrder })
        }
    }

    @Test
    fun sortOrders_afterReorder_savesOnlyChanged() {
        val a = ItemWithChecklist(Item(id = "a", title = "a", sortOrder = 0))
        val b = ItemWithChecklist(Item(id = "b", title = "b", sortOrder = 1))
        val c = ItemWithChecklist(Item(id = "c", title = "c", sortOrder = 2))
        val r = SortOrders.afterReorder(listOf(b, a, c), listOf(a, b, c), now)
        assertEquals(listOf("b" to 0, "a" to 1, "c" to 2), r.updated.map { it.item.id to it.item.sortOrder })
        assertEquals(listOf("b", "a"), r.changed.map { it.id })
    }

    @Test
    fun projectEdits() {
        val p = Item(id = "p", type = Item.TYPE_PROJECT, title = "Old", modificationDate = 1)
        assertEquals("New", ProjectEdits.apply(p, ProjectEdit.Rename("New"), now).title)
        assertSame(p, ProjectEdits.apply(p, ProjectEdit.Rename("Old"), now))
        assertEquals(Item.START_ANYTIME, ProjectEdits.apply(p, ProjectEdit.Start(TaskSection.INBOX, null, false), now).start)
        ProjectEdits.apply(p, ProjectEdit.Start(TaskSection.SOMEDAY, null, false), now).let {
            assertEquals(Item.START_SOMEDAY, it.start); assertEquals(now, it.modificationDate)
        }
        assertEquals("a, b", ProjectEdits.apply(p, ProjectEdit.Tags(listOf("a", "b")), now).cachedTags)
        assertEquals(day(20), ProjectEdits.apply(p, ProjectEdit.Deadline(day(20)), now).dueDate)
        assertEquals("ar", ProjectEdits.apply(p, ProjectEdit.MoveToArea("ar"), now).areaId)
        assertEquals("n", ProjectEdits.apply(p, ProjectEdit.Notes("n"), now).notes)
    }

    @Test
    fun taskEdits_sameRuleForSaveAndToggle() {
        val t = Item(id = "t", title = "t")
        val fields = TaskEditorFields(" ", "notes", TaskSection.INBOX, false, null, null, listOf("x"), "p", 2)
        TaskEdits.apply(t, fields, "Untitled", now).let {
            assertEquals("Untitled", it.title); assertEquals(Item.START_ANYTIME, it.start); assertEquals("p", it.projectId)
            assertEquals("x", it.cachedTags); assertEquals(2, it.priority); assertEquals(now, it.modificationDate)
        }
    }

    @Test
    fun headings() {
        val h1 = Item(id = "h1", type = Item.TYPE_HEADING, projectId = "p", sortOrder = 3)
        assertEquals(4, Headings.newAtEnd("p", listOf(h1), "n").sortOrder)
        assertEquals(listOf("n" to 0, "h1" to 1), Headings.insertAt(listOf(h1), Headings.newHeading("p", "n"), 0).map { it.id to it.sortOrder })
        assertEquals(listOf("g"), Headings.assign(listOf(Item(id = "t", title = "t")), "g", now).map { it.headingId })
    }
}
