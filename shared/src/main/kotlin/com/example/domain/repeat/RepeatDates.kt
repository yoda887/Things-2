package com.example.domain.repeat

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Даты повторов по правилу. Закреплено тестами RepeatTest. */
object RepeatDates {

    /** Дальше этого правило без подходящих дат считается исчерпанным (защита от бесконечного поиска) */
    private const val SEARCH_LIMIT_DAYS = 366L * 8

    /**
     * Ближайшая дата повтора регулярного правила не раньше [from] (включительно) или null — повторов больше
     * нет (прошла дата окончания или подходящей даты не нашлось).
     */
    fun nextOccurrence(rule: RepeatRule, from: LocalDate): LocalDate? {
        var date = maxOf(from, rule.start)
        val limit = date.plusDays(SEARCH_LIMIT_DAYS)
        while (!date.isAfter(limit)) {
            if (rule.endDate != null && date.isAfter(rule.endDate)) return null
            if (matches(rule, date)) return date
            date = date.plusDays(1)
        }
        return null
    }

    /** «После выполнения»: следующая копия — через интервал правила после дня выполнения. */
    fun afterCompletion(rule: RepeatRule, completedOn: LocalDate): LocalDate = when (rule.unit) {
        RepeatUnit.DAY -> completedOn.plusDays(rule.interval.toLong())
        RepeatUnit.WEEK -> completedOn.plusWeeks(rule.interval.toLong())
        RepeatUnit.MONTH -> completedOn.plusMonths(rule.interval.toLong())
        RepeatUnit.YEAR -> completedOn.plusYears(rule.interval.toLong())
    }

    /** Подходит ли дата регулярному правилу: попадает в его период («каждые N …») и в его дни. */
    fun matches(rule: RepeatRule, date: LocalDate): Boolean {
        if (date.isBefore(rule.start)) return false
        val start = rule.start
        val periodOk = when (rule.unit) {
            RepeatUnit.DAY -> ChronoUnit.DAYS.between(start, date) % rule.interval == 0L
            RepeatUnit.WEEK -> ChronoUnit.WEEKS.between(mondayOf(start), mondayOf(date)) % rule.interval == 0L
            RepeatUnit.MONTH -> ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(date)) % rule.interval == 0L
            RepeatUnit.YEAR -> (date.year - start.year) % rule.interval == 0
        }
        if (!periodOk) return false
        return when (rule.unit) {
            RepeatUnit.DAY -> true
            RepeatUnit.WEEK -> {
                val days = rule.on.mapNotNull { (it.day as? RepeatDay.Weekday)?.dayOfWeek }
                if (days.isEmpty()) date.dayOfWeek == start.dayOfWeek else date.dayOfWeek.value in days
            }
            RepeatUnit.MONTH -> {
                val on = rule.on.ifEmpty { listOf(RepeatOn(RepeatDay.OfMonth(start.dayOfMonth))) }
                on.any { dayMatches(it.day, date) }
            }
            RepeatUnit.YEAR -> {
                val on = rule.on.ifEmpty { listOf(RepeatOn(RepeatDay.OfMonth(start.dayOfMonth), start.monthValue)) }
                on.any { (it.month ?: start.monthValue) == date.monthValue && dayMatches(it.day, date) }
            }
        }
    }

    /** День повтора внутри месяца: число (31-е в коротком месяце — его последний день) или N-й день недели. */
    internal fun dayMatches(day: RepeatDay, date: LocalDate): Boolean = when (day) {
        is RepeatDay.OfMonth -> {
            val last = date.lengthOfMonth()
            val target = if (day.day == RepeatDay.LAST) last else minOf(day.day, last)
            date.dayOfMonth == target
        }
        is RepeatDay.Weekday -> date.dayOfWeek.value == day.dayOfWeek && when (day.ordinal) {
            0 -> true
            RepeatDay.LAST -> date.plusWeeks(1).month != date.month
            else -> (date.dayOfMonth - 1) / 7 + 1 == day.ordinal
        }
    }

    private fun mondayOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
