package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import java.util.Calendar

/**
 * «Предстоящие»: какие задачи и расписание — неделя по дням, затем месяцы. Подписи дней и месяцев —
 * в UpcomingScreen. Закреплено тестами UpcomingListTest и UpcomingScreenTest.
 */
object UpcomingList {

    /** Горизонт по дням: неделя, начиная с завтра */
    const val DAYS_HORIZON = 7

    /** Сколько целых месяцев показывать после остатка месяца, в котором закончилась неделя */
    const val MONTHS_HORIZON = 4

    fun includes(item: Item, bounds: DayBounds): Boolean = bounds.isUpcoming(item)

    /** День недели расписания: начало суток, события календаря и задачи этого дня (в порядке списка). */
    data class Day(val dayStart: Long, val events: List<Item>, val tasks: List<ItemWithChecklist>)

    /**
     * Раздел после недели: месяц [year]/[month] (месяц по Calendar, с 0) — целый или, если неделя
     * закончилась внутри него, остаток с [firstDay] по [lastDay] ([isRemainder]). [start] — начало раздела.
     * Задачи — по дате старта, события — по времени начала.
     */
    data class Period(
        val start: Long,
        val year: Int,
        val month: Int,
        val firstDay: Int,
        val lastDay: Int,
        val isRemainder: Boolean,
        val events: List<Item>,
        val tasks: List<ItemWithChecklist>
    )

    data class Schedule(val days: List<Day>, val periods: List<Period>)

    /**
     * Расписание экрана:
     * 1. Неделя подряд, начиная с завтра, — все дни, даже пустые.
     * 2. Остаток месяца, в котором закончилась неделя, и следующие [MONTHS_HORIZON] месяцев — только те,
     *    где есть задачи или события. Дальше этого горизонта экран ничего не показывает.
     */
    fun schedule(tasks: List<ItemWithChecklist>, events: List<Item>, now: Calendar = Calendar.getInstance()): Schedule {
        val cursor = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val days = (0 until DAYS_HORIZON).map {
            val dayStart = cursor.timeInMillis
            cursor.add(Calendar.DAY_OF_YEAR, 1)
            val nextDayStart = cursor.timeInMillis
            Day(
                dayStart = dayStart,
                events = events.filter { it.eventStartMillis.inRange(dayStart, nextDayStart) },
                tasks = tasks.filter { it.item.startDate.inRange(dayStart, nextDayStart) }
            )
        }

        // Неделя закончилась в cursor; дальняя граница — остаток этого месяца и MONTHS_HORIZON месяцев
        val horizonEnd = cursor.timeInMillis
        val laterLimit = Calendar.getInstance().apply {
            timeInMillis = horizonEnd
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, MONTHS_HORIZON + 1)
        }.timeInMillis

        val cal = Calendar.getInstance()
        fun monthKey(millis: Long): Int {
            cal.timeInMillis = millis
            return cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
        }
        val laterTasks = tasks.filter { it.item.startDate.inRange(horizonEnd, laterLimit) }
            .groupBy { monthKey(it.item.startDate!!) }
        val laterEvents = events.filter { it.eventStartMillis.inRange(horizonEnd, laterLimit) }
            .groupBy { monthKey(it.eventStartMillis!!) }

        val horizonKey = monthKey(horizonEnd)
        cal.timeInMillis = horizonEnd
        val horizonDay = cal.get(Calendar.DAY_OF_MONTH)

        val periods = (laterTasks.keys + laterEvents.keys).sorted().map { key ->
            cal.clear()
            cal.set(key / 12, key % 12, 1)
            val monthStart = cal.timeInMillis
            val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            // Остаток месяца, в котором закончилась неделя, начинается с конца недели
            val isRemainder = key == horizonKey && horizonDay > 1
            Period(
                start = if (isRemainder) horizonEnd else monthStart,
                year = key / 12,
                month = key % 12,
                firstDay = if (isRemainder) horizonDay else 1,
                lastDay = lastDay,
                isRemainder = isRemainder,
                events = laterEvents[key].orEmpty().sortedBy { it.eventStartMillis },
                tasks = laterTasks[key].orEmpty().sortedBy { it.item.startDate }
            )
        }
        return Schedule(days, periods)
    }

    private fun Long?.inRange(from: Long, until: Long): Boolean = this != null && this >= from && this < until
}
