package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** «Предстоящие»: неделя по дням, затем месяцы до горизонта — при заданном «сейчас». */
class UpcomingScreenTest {

    /** 9 октября 2026, 10:00 */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 9, 10, 0) }

    private fun at(y: Int, m: Int, d: Int) = Calendar.getInstance().apply { clear(); set(y, m, d, 12, 0) }.timeInMillis
    private fun task(id: String, start: Long) = ItemWithChecklist(Item(id = id, title = id, startDate = start))

    private val tomorrow = task("tomorrow", at(2026, Calendar.OCTOBER, 10))
    private val inWeek = task("week", at(2026, Calendar.OCTOBER, 16))
    private val laterOctober = task("oct20", at(2026, Calendar.OCTOBER, 20))
    private val february = task("feb", at(2027, Calendar.FEBRUARY, 3))
    private val tooFar = task("march", at(2027, Calendar.MARCH, 3))

    private fun schedule() = UpcomingScreen.schedule(listOf(tooFar, february, laterOctober, inWeek, tomorrow), emptyList(), "Завтра", now)

    @Test
    fun weekOfDays_startingTomorrow_alwaysShown() {
        val days = schedule().days
        assertEquals(7, days.size)
        assertEquals("Завтра", days[0].dayOfWeekLabel)
        assertEquals("10", days[0].dayOfMonth)
        assertEquals(listOf("tomorrow"), days[0].tasks.map { it.item.id })
        assertEquals(listOf("week"), days[6].tasks.map { it.item.id })
        assertTrue(days.subList(1, 6).all { it.tasks.isEmpty() })
    }

    @Test
    fun months_restOfOctoberThenMonthsWithTasks_upToHorizon() {
        val months = schedule().months
        assertEquals(listOf(listOf("oct20"), listOf("feb")), months.map { m -> m.tasks.map { it.item.id } })
        // Остаток октября начинается сразу после недели — с 17-го
        assertEquals(Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 17) }.timeInMillis, months[0].monthMillis)
    }

    @Test
    fun rows_headersEventsAndTasksInOrder() {
        val rows = UpcomingScreen.rows(schedule().days, schedule().months)
        assertTrue(rows.first() is UpcomingHeaderItem)
        assertEquals(7, rows.count { it is UpcomingHeaderItem })
        assertEquals(2, rows.count { it is UpcomingMonthHeaderItem })
        assertEquals(listOf("tomorrow", "week", "oct20", "feb"), rows.filterIsInstance<ItemWithChecklist>().map { it.item.id })
        assertFalse(UpcomingScreen.isEmpty(schedule()))
    }
}
