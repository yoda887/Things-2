package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/** «Журнал» по разделам: сегодня, вчера, затем месяцы — свежие сверху. */
class LogbookListTest {

    /** 10 октября 2026, 15:00 */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 10, 15, 0) }

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = Calendar.getInstance().apply { clear(); set(y, m, d, h, 0) }.timeInMillis
    private fun done(id: String, at: Long) =
        ItemWithChecklist(Item(id = id, title = id, status = Item.STATUS_COMPLETED, stopDate = at))
    private fun closedProject(id: String, at: Long) =
        Item(id = id, title = id, type = Item.TYPE_PROJECT, status = Item.STATUS_COMPLETED, stopDate = at)

    @Test
    fun sections_todayYesterdayThenMonths_newestFirst() {
        val tasks = listOf(
            done("sep", at(2026, Calendar.SEPTEMBER, 20)),
            done("today", at(2026, Calendar.OCTOBER, 10, 9)),
            done("oct3", at(2026, Calendar.OCTOBER, 3)),
            done("yesterday", at(2026, Calendar.OCTOBER, 9, 23)),
            done("oct8", at(2026, Calendar.OCTOBER, 8)),
            done("lastYear", at(2025, Calendar.DECEMBER, 31))
        )
        val projects = listOf(closedProject("pToday", at(2026, Calendar.OCTOBER, 10, 11)))
        val sections = LogbookList.sections(tasks, projects, now)

        fun ids(s: LogbookList.Section) = s.entries.map { (it as? ItemWithChecklist)?.item?.id ?: (it as Item).id }
        assertEquals(
            listOf(LogbookList.Period.TODAY, LogbookList.Period.YESTERDAY, LogbookList.Period.MONTH, LogbookList.Period.MONTH, LogbookList.Period.MONTH),
            sections.map { it.period }
        )
        assertEquals(listOf("pToday", "today"), ids(sections[0]))
        assertEquals(listOf("yesterday"), ids(sections[1]))
        // Октябрь — только то, что закрыто раньше вчерашнего дня
        assertEquals(listOf("oct8", "oct3"), ids(sections[2]))
        assertEquals(Calendar.OCTOBER, sections[2].month)
        assertEquals(at(2026, Calendar.OCTOBER, 1, 0), sections[2].start)
        assertEquals(listOf("sep"), ids(sections[3]))
        assertEquals(2025, sections[4].year)
        assertEquals(Calendar.DECEMBER, sections[4].month)
    }

    @Test
    fun sections_emptyWhenNothingClosed() {
        assertEquals(emptyList<LogbookList.Section>(), LogbookList.sections(emptyList(), emptyList(), now))
    }
}
