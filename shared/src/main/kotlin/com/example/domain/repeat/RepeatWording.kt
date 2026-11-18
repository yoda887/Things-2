package com.example.domain.repeat

/**
 * Из каких частей складывается правило повтора фразой («щотижня по середах», «щомісяця 1-го і 15-го числа»,
 * «щороку в останній понеділок травня»). Здесь — только выбор формы; слова и падежи — в строках приложения.
 * Закреплено тестами RepeatWordingTest.
 */
object RepeatWording {

    /** Как назвать набор дней недели */
    enum class WeekGroup {
        /** Дни не выбраны — только «щотижня» */
        NONE,
        /** Все семь — «щодня» */
        ALL,
        /** Пн–пт — «по буднях» */
        WORKDAYS,
        /** Сб и нд — «по вихідних» */
        WEEKENDS,
        /** Остальное — перечислением, по порядку недели */
        LIST
    }

    private val WORKDAYS = setOf(1, 2, 3, 4, 5)
    private val WEEKENDS = setOf(6, 7)

    fun weekGroup(days: Collection<Int>): WeekGroup {
        val set = days.toSet()
        return when {
            set.isEmpty() -> WeekGroup.NONE
            set.size == 7 -> WeekGroup.ALL
            set == WORKDAYS -> WeekGroup.WORKDAYS
            set == WEEKENDS -> WeekGroup.WEEKENDS
            else -> WeekGroup.LIST
        }
    }

    /** Дни недели еженедельного правила — без повторов, по порядку недели */
    fun weekdays(rule: RepeatRule): List<Int> =
        rule.on.mapNotNull { (it.day as? RepeatDay.Weekday)?.dayOfWeek }.distinct().sorted()

    /**
     * Дни месяца одного месяца (или месяца без указания — у ежемесячных правил): числа — вместе по возрастанию
     * («1-го і 15-го числа»), последний день — после них, дни недели («у 2-й вівторок») — в порядке правила.
     */
    data class MonthParts(val numbers: List<Int>, val lastDay: Boolean, val weekdays: List<RepeatDay.Weekday>)

    fun monthParts(on: List<RepeatOn>): MonthParts {
        val numbers = on.mapNotNull { (it.day as? RepeatDay.OfMonth)?.day?.takeIf { d -> d != RepeatDay.LAST } }.distinct().sorted()
        val last = on.any { (it.day as? RepeatDay.OfMonth)?.day == RepeatDay.LAST }
        val weekdays = on.mapNotNull { it.day as? RepeatDay.Weekday }.distinct()
        return MonthParts(numbers, last, weekdays)
    }

    /** Ежегодное правило — по месяцам в порядке года; строки без месяца — в конце, отдельной группой (null) */
    fun byMonth(on: List<RepeatOn>): List<Pair<Int?, MonthParts>> =
        on.groupBy { it.month }
            .entries
            .sortedWith(compareBy(nullsLast()) { it.key })
            .map { (month, entries) -> month to monthParts(entries) }
}
