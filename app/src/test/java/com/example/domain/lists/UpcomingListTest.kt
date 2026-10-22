package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Расписание «Предстоящих» (неделя по дням, затем месяцы) и события «Сегодня». */
class UpcomingListTest {

    /** 9 октября 2026, 10:00 */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 9, 10, 0) }

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = Calendar.getInstance().apply { clear(); set(y, m, d, h, 0) }.timeInMillis
    private fun task(id: String, start: Long) = ItemWithChecklist(Item(id = id, title = id, startDate = start))
    private fun event(id: String, start: Long) = Item(id = id, title = id, eventStartMillis = start)

    @Test
    fun days_weekFromTomorrow_withTasksAndEventsOfEachDay() {
        val schedule = UpcomingList.schedule(
            listOf(task("t11", at(2026, Calendar.OCTOBER, 11)), task("t10", at(2026, Calendar.OCTOBER, 10))),
            listOf(event("e10", at(2026, Calendar.OCTOBER, 10, 9))),
            now
        )
        assertEquals(7, schedule.days.size)
        assertEquals(at(2026, Calendar.OCTOBER, 10, 0), schedule.days[0].dayStart)
        assertEquals(listOf("t10"), schedule.days[0].tasks.map { it.item.id })
        assertEquals(listOf("e10"), schedule.days[0].events.map { it.id })
        assertEquals(listOf("t11"), schedule.days[1].tasks.map { it.item.id })
    }

    @Test
    fun periods_restOfMonthThenWholeMonths_sortedByDate_upToHorizon() {
        val schedule = UpcomingList.schedule(
            listOf(
                task("oct25", at(2026, Calendar.OCTOBER, 25)),
                task("oct20", at(2026, Calendar.OCTOBER, 20)),
                task("feb", at(2027, Calendar.FEBRUARY, 3)),
                task("march", at(2027, Calendar.MARCH, 3))
            ),
            listOf(event("nov", at(2026, Calendar.NOVEMBER, 5))),
            now
        )
        val periods = schedule.periods.filter { !it.isYear }
        // Неделя — 10–16 октября; остаток октября с 17-го, ноябрь (только событие), февраль; март — за горизонтом месяцев,
        // в разделе 2027 года
        assertEquals(listOf(Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.FEBRUARY), periods.map { it.month })
        assertEquals(listOf("march"), schedule.periods.single { it.isYear }.tasks.map { it.item.id })
        val october = periods[0]
        assertTrue(october.isRemainder)
        assertEquals(17, october.firstDay)
        assertEquals(31, october.lastDay)
        assertEquals(at(2026, Calendar.OCTOBER, 17, 0), october.start)
        assertEquals(listOf("oct20", "oct25"), october.tasks.map { it.item.id })
        assertFalse(periods[1].isRemainder)
        assertEquals(at(2026, Calendar.NOVEMBER, 1, 0), periods[1].start)
        assertEquals(listOf("nov"), periods[1].events.map { it.id })
        assertEquals(2027, periods[2].year)
    }

    @Test
    fun todayEvents_onlyThoseStartingToday() {
        val bounds = DayBounds.now()
        val today = event("today", bounds.todayStart + 3_600_000L)
        val tomorrow = event("tomorrow", bounds.endOfToday + 3_600_000L)
        val yesterday = event("yesterday", bounds.todayStart - 3_600_000L)
        assertEquals(listOf("today"), TodayList.events(listOf(yesterday, today, tomorrow), bounds).map { it.id })
    }

    @Test
    fun scheduledProjects_onTheirDayOrMonth_somedayAndPastNot() {
        fun project(id: String, start: Long?, startVal: Int = Item.START_ANYTIME) =
            Item(id = id, title = id, type = Item.TYPE_PROJECT, start = startVal, startDate = start)
        val tomorrowProject = project("pTomorrow", at(2026, Calendar.OCTOBER, 10))
        val novemberProject = project("pNov", at(2026, Calendar.NOVEMBER, 12))
        val schedule = UpcomingList.schedule(emptyList(), emptyList(), now, listOf(tomorrowProject, novemberProject))
        assertEquals(listOf("pTomorrow"), schedule.days[0].projects.map { it.id })
        assertEquals(listOf("pNov"), schedule.periods.single { it.month == Calendar.NOVEMBER }.projects.map { it.id })

        val bounds = DayBounds.now()
        val day = 24 * 3600 * 1000L
        assertTrue(UpcomingList.includesProject(project("f", bounds.endOfToday + day), bounds))
        assertFalse(UpcomingList.includesProject(project("today", bounds.todayStart + 3_600_000L), bounds))
        assertFalse(UpcomingList.includesProject(project("none", null), bounds))
        assertFalse(UpcomingList.includesProject(project("s", bounds.endOfToday + day, Item.START_SOMEDAY), bounds))
        assertFalse(UpcomingList.includesProject(project("c", bounds.endOfToday + day).copy(status = Item.STATUS_COMPLETED), bounds))
    }

    @Test
    fun beyondMonthHorizon_byYears_withoutEvents() {
        val schedule = UpcomingList.schedule(
            listOf(
                task("y2028", at(2028, Calendar.MAY, 1)),
                task("march27", at(2027, Calendar.MARCH, 3)),
                task("dec27", at(2027, Calendar.DECEMBER, 31))
            ),
            listOf(event("farEvent", at(2027, Calendar.JUNE, 1))),
            now,
            projects = listOf(Item(id = "p28", type = Item.TYPE_PROJECT, title = "p28", start = Item.START_ANYTIME, startDate = at(2028, Calendar.JANUARY, 9)))
        )
        val years = schedule.periods.filter { it.isYear }
        assertEquals(listOf(2027, 2028), years.map { it.year })
        assertEquals(listOf("march27", "dec27"), years[0].tasks.map { it.item.id })
        assertTrue(years.all { it.events.isEmpty() })
        assertEquals(listOf("p28"), years[1].projects.map { it.id })
        // 2027 начинается с конца горизонта месяцев (1 марта), 2028 — с 1 января
        assertEquals(at(2027, Calendar.MARCH, 1, 0), years[0].start)
        assertEquals(at(2028, Calendar.JANUARY, 1, 0), years[1].start)
    }
}
