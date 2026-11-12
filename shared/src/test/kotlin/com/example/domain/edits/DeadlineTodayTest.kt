package com.example.domain.edits

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.TaskSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Дедлайн приводит задачу и проект в «Сегодня»: при правке и в день дедлайна — один раз. */
class DeadlineTodayTest {

    private val day = 24 * 60 * 60 * 1000L
    private val bounds: DayBounds = DayBounds.now()
    private val today: Long = bounds.todayStart
    private val now: Long = today + 10 * 60 * 60 * 1000L
    private val tomorrow: Long = bounds.endOfToday + 1
    private val yesterday: Long = today - day

    private fun task(
        dueDate: Long? = null,
        start: Int = Item.START_ANYTIME,
        startDate: Long? = null,
        changed: Long = today - 3 * day,
        type: Int = Item.TYPE_TASK,
        status: Int = Item.STATUS_OPEN
    ) = Item(id = "t", title = "t", type = type, status = status, start = start, startDate = startDate,
        dueDate = dueDate, modificationDate = changed)

    private fun isTodayStart(item: Item) {
        assertEquals(Item.START_TODAY, item.start)
        assertEquals(now, item.startDate)
        assertFalse(item.isTonight)
    }

    @Test
    fun deadlineSetToToday_startsToday() {
        val before = task(start = Item.START_SOMEDAY)
        isTodayStart(DeadlineToday.afterDeadlineEdit(before, before.copy(dueDate = today), bounds, now))
    }

    @Test
    fun deadlineSetToPastDay_startsToday() {
        val before = task(startDate = tomorrow + day)
        isTodayStart(DeadlineToday.afterDeadlineEdit(before, before.copy(dueDate = yesterday), bounds, now))
    }

    @Test
    fun deadlineSetLater_startUnchanged() {
        val before = task(startDate = tomorrow + day)
        val after = before.copy(dueDate = tomorrow)
        assertSame(after, DeadlineToday.afterDeadlineEdit(before, after, bounds, now))
    }

    @Test
    fun deadlineNotChanged_startUnchanged() {
        // Перенесли задачу с дедлайном сегодня на завтра — дедлайн тот же, задача остаётся на завтра
        val before = task(dueDate = today, startDate = null)
        val after = before.copy(startDate = tomorrow)
        assertSame(after, DeadlineToday.afterDeadlineEdit(before, after, bounds, now))
    }

    @Test
    fun alreadyToday_notTouched() {
        assertFalse(DeadlineToday.pulls(task(dueDate = today, start = Item.START_TODAY), bounds))
        assertFalse(DeadlineToday.pulls(task(dueDate = today, startDate = yesterday), bounds))
    }

    @Test
    fun projects_tooButNotTemplatesClosedOrHeadings() {
        assertTrue(DeadlineToday.pulls(task(dueDate = today, type = Item.TYPE_PROJECT), bounds))
        assertFalse(DeadlineToday.pulls(task(dueDate = today, type = Item.TYPE_TEMPLATE), bounds))
        assertFalse(DeadlineToday.pulls(task(dueDate = today, type = Item.TYPE_PROJECT_TEMPLATE), bounds))
        assertFalse(DeadlineToday.pulls(task(dueDate = today, type = Item.TYPE_HEADING), bounds))
        assertFalse(DeadlineToday.pulls(task(dueDate = today, status = Item.STATUS_COMPLETED), bounds))
    }

    @Test
    fun deadlineArrived_firstPassStartsToday() {
        val due = DeadlineToday.dueNow(listOf(task(dueDate = today, startDate = tomorrow + day)), bounds, now)
        assertEquals(1, due.size)
        isTodayStart(due.single())
        assertEquals(now, due.single().modificationDate)
    }

    @Test
    fun deadlineArrived_changedSinceDayStart_notAgain() {
        // Уже поставлена (или сама перенесена) сегодня — второй раз не трогается
        val moved = task(dueDate = today, startDate = tomorrow, changed = today + 60_000)
        assertTrue(DeadlineToday.dueNow(listOf(moved), bounds, now).isEmpty())
    }

    @Test
    fun overdueUntouchedSinceDeadline_startsToday() {
        // День дедлайна был вчера, приложение не открывали — задача встаёт на сегодня
        val due = DeadlineToday.dueNow(listOf(task(dueDate = yesterday, changed = yesterday - day)), bounds, now)
        assertEquals(1, due.size)
    }

    @Test
    fun noDeadlineOrLater_notTouched() {
        assertTrue(DeadlineToday.dueNow(listOf(task(), task(dueDate = tomorrow)), bounds, now).isEmpty())
    }

    @Test
    fun editorSave_deadlineToday_startsToday() {
        val saved = task(start = Item.START_INBOX)
        val fields = TaskEditorFields(
            title = "t", notes = "", section = TaskSection.INBOX, isTonight = false, startDate = null,
            dueDate = today, tags = emptyList(), projectId = null, priority = Item.PRIORITY_NONE
        )
        isTodayStart(TaskEdits.apply(saved, fields, "Untitled", now, bounds))
    }

    @Test
    fun projectDeadlineEdit_startsToday() {
        val project = task(type = Item.TYPE_PROJECT)
        isTodayStart(ProjectEdits.apply(project, ProjectEdit.Deadline(today), now, bounds))
        assertNull(ProjectEdits.apply(project, ProjectEdit.Deadline(tomorrow), now, bounds).startDate)
    }
}
