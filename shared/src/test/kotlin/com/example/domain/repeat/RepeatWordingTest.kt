package com.example.domain.repeat

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Из каких частей правило повтора складывается фразой */
class RepeatWordingTest {

    private val start = LocalDate.of(2026, 11, 18)
    private fun weekly(vararg days: Int) = RepeatRule(
        afterCompletion = false, unit = RepeatUnit.WEEK, start = start,
        on = days.map { RepeatOn(RepeatDay.Weekday(it)) }
    )

    @Test
    fun `дни недели — все, будни, выходные или список`() {
        assertEquals(RepeatWording.WeekGroup.NONE, RepeatWording.weekGroup(emptyList()))
        assertEquals(RepeatWording.WeekGroup.ALL, RepeatWording.weekGroup((1..7).toList()))
        assertEquals(RepeatWording.WeekGroup.WORKDAYS, RepeatWording.weekGroup(listOf(5, 1, 2, 3, 4)))
        assertEquals(RepeatWording.WeekGroup.WEEKENDS, RepeatWording.weekGroup(listOf(7, 6)))
        assertEquals(RepeatWording.WeekGroup.LIST, RepeatWording.weekGroup(listOf(3)))
        assertEquals(RepeatWording.WeekGroup.LIST, RepeatWording.weekGroup(listOf(1, 2, 3, 4)))
        assertEquals(RepeatWording.WeekGroup.LIST, RepeatWording.weekGroup(listOf(5, 6, 7)))
    }

    @Test
    fun `дни недели правила — по порядку недели, без повторов`() {
        assertEquals(listOf(1, 3, 5), RepeatWording.weekdays(weekly(5, 1, 3, 1)))
    }

    @Test
    fun `дни месяца — числа вместе по возрастанию, последний день после них, дни недели по порядку правила`() {
        val parts = RepeatWording.monthParts(
            listOf(
                RepeatOn(RepeatDay.OfMonth(15)),
                RepeatOn(RepeatDay.Weekday(5, 3)),
                RepeatOn(RepeatDay.OfMonth(RepeatDay.LAST)),
                RepeatOn(RepeatDay.OfMonth(1)),
                RepeatOn(RepeatDay.Weekday(1, RepeatDay.LAST))
            )
        )
        assertEquals(listOf(1, 15), parts.numbers)
        assertEquals(true, parts.lastDay)
        assertEquals(listOf(RepeatDay.Weekday(5, 3), RepeatDay.Weekday(1, RepeatDay.LAST)), parts.weekdays)
    }

    @Test
    fun `ежегодное — по месяцам в порядке года`() {
        val groups = RepeatWording.byMonth(
            listOf(
                RepeatOn(RepeatDay.OfMonth(20), month = 5),
                RepeatOn(RepeatDay.OfMonth(15), month = 3),
                RepeatOn(RepeatDay.OfMonth(1), month = 5)
            )
        )
        assertEquals(listOf(3, 5), groups.map { it.first })
        assertEquals(listOf(1, 20), groups[1].second.numbers)
    }
}
