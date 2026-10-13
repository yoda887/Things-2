package com.example.domain.reminders

import com.example.data.model.Item
import com.example.data.model.TaskSection
import com.example.domain.edits.TaskEditorFields
import com.example.domain.edits.TaskEdits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/** Напоминания: когда срабатывают и когда снимаются при правке «Когда». */
class RemindersTest {

    private fun at(d: Int, h: Int = 0, m: Int = 0) =
        Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, d, h, m) }.timeInMillis
    private val now = at(10, 9, 30)
    private val minutes1430 = 14 * 60 + 30

    @Test
    fun fireAt_onTaskDay_atReminderTime() {
        // Дата старта — в её день, в указанное время (время самой даты старта не важно)
        val dated = Item(id = "t", startDate = at(12, 8), start = Item.START_ANYTIME, reminderTime = minutes1430)
        assertEquals(at(12, 14, 30), Reminders.fireAt(dated, now))
        // «Сегодня» без даты — сегодня
        val today = Item(id = "t", start = Item.START_TODAY, reminderTime = minutes1430)
        assertEquals(at(10, 14, 30), Reminders.fireAt(today, now))
    }

    @Test
    fun fireAt_noDayOrClosed_never() {
        assertNull(Reminders.fireAt(Item(id = "t", start = Item.START_ANYTIME, reminderTime = 60), now))
        assertNull(Reminders.fireAt(Item(id = "t", start = Item.START_SOMEDAY, reminderTime = 60), now))
        assertNull(Reminders.fireAt(Item(id = "t", start = Item.START_TODAY), now))
        val today = Item(id = "t", start = Item.START_TODAY, reminderTime = 60)
        assertNull(Reminders.fireAt(today.copy(status = Item.STATUS_COMPLETED), now))
        assertNull(Reminders.fireAt(today.copy(status = Item.STATUS_CANCELLED), now))
        assertNull(Reminders.fireAt(today.copy(trashed = true), now))
        assertNull(Reminders.fireAt(today.copy(type = Item.TYPE_PROJECT), now))
    }

    @Test
    fun upcoming_onlyFuture_sortedByTime() {
        val past = Item(id = "past", start = Item.START_TODAY, reminderTime = 8 * 60)
        val later = Item(id = "later", startDate = at(11), start = Item.START_ANYTIME, reminderTime = 9 * 60)
        val soon = Item(id = "soon", start = Item.START_TODAY, reminderTime = 10 * 60)
        assertEquals(listOf("soon", "later"), Reminders.upcoming(listOf(past, later, soon), now).map { it.first.id })
    }

    @Test
    fun defaultTime_nextFullHour() {
        assertEquals(10 * 60, Reminders.defaultTime(now))
        assertEquals(0, Reminders.defaultTime(at(10, 23, 50)))
    }

    @Test
    fun editorSave_keepsReminderOnlyWithDay() {
        val task = Item(id = "t", title = "t")
        fun fields(section: TaskSection, startDate: Long?) =
            TaskEditorFields("t", "", section, false, startDate, null, emptyList(), null, 0, reminderTime = minutes1430)
        assertEquals(minutes1430, TaskEdits.apply(task, fields(TaskSection.TODAY, null), "u").reminderTime)
        assertEquals(minutes1430, TaskEdits.apply(task, fields(TaskSection.UPCOMING, at(12)), "u").reminderTime)
        // Дату убрали — напоминание снимается
        assertNull(TaskEdits.apply(task, fields(TaskSection.SOMEDAY, null), "u").reminderTime)
        assertNull(TaskEdits.apply(task, fields(TaskSection.ANYTIME, null), "u").reminderTime)
    }

    @Test
    fun backgroundDraft_keepsSavedReminder() {
        val saved = TaskEditorFields("", "", TaskSection.TODAY, false, null, null, emptyList(), null, 0, reminderTime = 60)
        val edited = saved.copy(title = "new", reminderTime = 120)
        assertEquals(60, TaskEdits.backgroundDraft(saved, edited).reminderTime)
        assertEquals("new", TaskEdits.backgroundDraft(saved, edited).title)
    }
}
