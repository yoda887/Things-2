package com.example.domain.repeat

import com.example.data.model.ChecklistItem
import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Повторяющиеся задачи: правило, даты повторов, какие копии создать и как они выглядят. */
class RepeatTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val wed = d(2026, 10, 14)  // среда

    private fun dates(rule: RepeatRule, from: LocalDate, n: Int): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var cur: LocalDate? = from
        while (out.size < n) {
            cur = RepeatDates.nextOccurrence(rule, cur!!) ?: break
            out += cur
            cur = cur.plusDays(1)
        }
        return out
    }

    @Test
    fun encodeDecode_roundTrip() {
        val rules = listOf(
            RepeatRule(afterCompletion = true, interval = 2, unit = RepeatUnit.WEEK, start = wed),
            RepeatRule(
                afterCompletion = false, interval = 1, unit = RepeatUnit.YEAR, start = wed,
                on = listOf(RepeatOn(RepeatDay.OfMonth(14), 2), RepeatOn(RepeatDay.Weekday(1, 1), 11), RepeatOn(RepeatDay.OfMonth(RepeatDay.LAST))),
                endDate = d(2027, 12, 31), endCount = 5, deadlineDaysBefore = 2
            )
        )
        rules.forEach { assertEquals(it, RepeatRule.decode(it.encode())) }
        assertNull(RepeatRule.decode("garbage"))
    }

    @Test
    fun daily_everyTwoDays_fromStart() {
        val rule = RepeatRule(afterCompletion = false, interval = 2, unit = RepeatUnit.DAY, start = wed)
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 16), d(2026, 10, 18)), dates(rule, d(2026, 10, 1), 3))
    }

    @Test
    fun weekly_selectedDays_everyOtherWeek() {
        val days = listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY).map { RepeatOn(RepeatDay.Weekday(it.value)) }
        val weekly = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed, on = days)
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 17), d(2026, 10, 20), d(2026, 10, 21)), dates(weekly, wed, 4))
        // Каждые 2 недели: неделя с 19-го пропускается целиком
        val biweekly = weekly.copy(interval = 2)
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 17), d(2026, 10, 27), d(2026, 10, 28)), dates(biweekly, wed, 4))
        // Без дней — тот же день недели, что у начала
        val plain = RepeatRule(afterCompletion = false, unit = RepeatUnit.WEEK, start = wed)
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 21)), dates(plain, wed, 2))
    }

    @Test
    fun monthly_31st_isLastDayInShortMonths() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = d(2027, 1, 31))
        assertEquals(listOf(d(2027, 1, 31), d(2027, 2, 28), d(2027, 3, 31), d(2027, 4, 30)), dates(rule, d(2027, 1, 1), 4))
        val leap = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = d(2028, 1, 1), on = listOf(RepeatOn(RepeatDay.OfMonth(RepeatDay.LAST))))
        assertEquals(listOf(d(2028, 1, 31), d(2028, 2, 29)), dates(leap, d(2028, 1, 1), 2))
    }

    @Test
    fun monthly_nthAndLastWeekday() {
        val firstMonday = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = wed, on = listOf(RepeatOn(RepeatDay.Weekday(1, 1))))
        assertEquals(listOf(d(2026, 11, 2), d(2026, 12, 7)), dates(firstMonday, wed, 2))
        val lastFriday = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = wed, on = listOf(RepeatOn(RepeatDay.Weekday(5, RepeatDay.LAST))))
        assertEquals(listOf(d(2026, 10, 30), d(2026, 11, 27)), dates(lastFriday, wed, 2))
        // Каждые 3 месяца: октябрь, январь
        val quarterly = RepeatRule(afterCompletion = false, interval = 3, unit = RepeatUnit.MONTH, start = wed)
        assertEquals(listOf(d(2026, 10, 14), d(2027, 1, 14)), dates(quarterly, wed, 2))
    }

    @Test
    fun yearly_severalDays() {
        val rule = RepeatRule(
            afterCompletion = false, unit = RepeatUnit.YEAR, start = wed,
            on = listOf(RepeatOn(RepeatDay.OfMonth(14), 2), RepeatOn(RepeatDay.Weekday(1, 1), 11))
        )
        assertEquals(listOf(d(2026, 11, 2), d(2027, 2, 14), d(2027, 11, 1)), dates(rule, wed, 3))
    }

    @Test
    fun endDate_stopsRepeats() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed, endDate = d(2026, 10, 15))
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 15)), dates(rule, wed, 5))
    }

    @Test
    fun due_regular_stacksMissedDays_andHonorsCount() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed, endCount = 3)
        val state = RepeatPlan.start(rule, wed)
        assertEquals(wed, state.nextOccurrence)
        // Приложение не открывали 4 дня: подошедшие копии копятся, но не больше числа повторов
        val (created, after) = RepeatPlan.due(state, d(2026, 10, 18))
        assertEquals(listOf(d(2026, 10, 14), d(2026, 10, 15), d(2026, 10, 16)), created)
        assertEquals(3, after.generated)
        assertNull(after.nextOccurrence)
        assertTrue(RepeatPlan.due(after, d(2026, 10, 30)).first.isEmpty())
    }

    @Test
    fun due_catchUpIsLimited() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed)
        val (created, after) = RepeatPlan.due(RepeatPlan.start(rule, wed), wed.plusDays(100))
        assertEquals(RepeatPlan.MAX_CATCH_UP, created.size)
        assertEquals(wed.plusDays(RepeatPlan.MAX_CATCH_UP.toLong()), after.nextOccurrence)
    }

    @Test
    fun due_pausedCreatesNothing() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.DAY, start = wed)
        val paused = RepeatPlan.start(rule, wed).copy(active = false)
        assertTrue(RepeatPlan.due(paused, wed.plusDays(3)).first.isEmpty())
    }

    @Test
    fun afterCompletion_firstCopyToday_nextAfterCheckingOff() {
        val rule = RepeatRule(afterCompletion = true, interval = 1, unit = RepeatUnit.WEEK, start = wed)
        val (first, waiting) = RepeatPlan.due(RepeatPlan.start(rule, wed), wed)
        assertEquals(listOf(wed), first)
        assertNull(waiting.nextOccurrence)
        // Отметили в субботу — следующая копия через неделю, в следующую субботу
        val next = RepeatPlan.completed(waiting, d(2026, 10, 17))
        assertEquals(d(2026, 10, 24), next.nextOccurrence)
        assertTrue(RepeatPlan.due(next, d(2026, 10, 23)).first.isEmpty())
        assertEquals(listOf(d(2026, 10, 24)), RepeatPlan.due(next, d(2026, 10, 24)).first)
    }

    @Test
    fun deadline_copyAppearsEarlier_withDueOnRepeatDay() {
        val rule = RepeatRule(afterCompletion = false, unit = RepeatUnit.MONTH, start = d(2026, 10, 15), deadlineDaysBefore = 5)
        val state = RepeatPlan.start(rule, d(2026, 10, 1))
        assertTrue(RepeatPlan.due(state, d(2026, 10, 9)).first.isEmpty())
        assertEquals(listOf(d(2026, 10, 15)), RepeatPlan.due(state, d(2026, 10, 10)).first)

        val zone = ZoneId.of("Europe/Kyiv")
        val template = Item(id = "tpl", type = Item.TYPE_TEMPLATE, title = "Bill", notes = "n", cachedTags = "home", projectId = "p", reminderTime = 9 * 60)
        val checklist = listOf(ChecklistItem(id = "c", itemId = "tpl", title = "a", isCompleted = true))
        var n = 0
        val (copy, copyChecklist) = RepeatPlan.copyOf(template, checklist, rule, d(2026, 10, 15), zone, now = 1L) { "id${n++}" }
        assertEquals(Item.TYPE_TASK, copy.type)
        assertEquals("tpl", copy.repeatTemplateId)
        assertEquals("Bill", copy.title)
        assertEquals("p", copy.projectId)
        assertEquals(9 * 60, copy.reminderTime)
        assertEquals(d(2026, 10, 10).atStartOfDay(zone).toInstant().toEpochMilli(), copy.startDate)
        assertEquals(d(2026, 10, 15).atStartOfDay(zone).toInstant().toEpochMilli(), copy.dueDate)
        assertEquals(copy.id, copyChecklist.single().itemId)
        assertFalse(copyChecklist.single().isCompleted)
    }

    @Test
    fun setup_taskBecomesTemplate_copiesOnlyFromRegularTasks() {
        val task = Item(id = "t", title = "Water", start = Item.START_TODAY, startDate = 5L, dueDate = 7L, isTonight = true, cachedTags = "home")
        assertTrue(RepeatSetup.canRepeat(task))
        assertFalse(RepeatSetup.canRepeat(task.copy(repeatTemplateId = "tpl")))
        assertFalse(RepeatSetup.canRepeat(task.copy(type = Item.TYPE_PROJECT)))
        assertFalse(RepeatSetup.canRepeat(task.copy(status = Item.STATUS_COMPLETED)))
        val template = RepeatSetup.template(task, reminderTime = 9 * 60, now = 1L)
        assertEquals(Item.TYPE_TEMPLATE, template.type)
        assertEquals("t", template.id)
        assertEquals("Water", template.title)
        assertEquals("home", template.cachedTags)
        assertNull(template.startDate)
        assertNull(template.dueDate)
        assertFalse(template.isTonight)
        assertEquals(9 * 60, template.reminderTime)
    }
}
