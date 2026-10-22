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

    /** Запланированный проект: открытый, не «Когда-нибудь», с датой старта позже сегодняшнего дня. */
    fun includesProject(project: Item, bounds: DayBounds): Boolean =
        ListRules.isOpenProject(project) && project.start != Item.START_SOMEDAY &&
            project.startDate.let { it != null && it > bounds.endOfToday }

    /**
     * День недели расписания: начало суток, события календаря, проекты, которые стартуют в этот день,
     * и задачи (в порядке списка).
     */
    data class Day(
        val dayStart: Long,
        val events: List<Item>,
        val tasks: List<ItemWithChecklist>,
        val projects: List<Item> = emptyList()
    )

    /**
     * Раздел после недели: месяц [year]/[month] (месяц по Calendar, с 0) — целый или, если неделя
     * закончилась внутри него, остаток с [firstDay] по [lastDay] ([isRemainder]). [start] — начало раздела.
     * Задачи — по дате старта, события — по времени начала. [isYear] — раздел года [year] за горизонтом месяцев:
     * задачи и проекты по дате старта, без событий календаря; [start] — начало года или конец горизонта.
     */
    data class Period(
        val start: Long,
        val year: Int,
        val month: Int,
        val firstDay: Int,
        val lastDay: Int,
        val isRemainder: Boolean,
        val events: List<Item>,
        val tasks: List<ItemWithChecklist>,
        val projects: List<Item> = emptyList(),
        val isYear: Boolean = false
    )

    data class Schedule(val days: List<Day>, val periods: List<Period>)

    /**
     * Расписание экрана:
     * 1. Неделя подряд, начиная с завтра, — все дни, даже пустые.
     * 2. Остаток месяца, в котором закончилась неделя, и следующие [MONTHS_HORIZON] месяцев — только те,
     *    где есть задачи или события.
     * 3. Дальше — по годам: разделы лет, где есть задачи или проекты (события календаря туда не попадают).
     */
    fun schedule(
        tasks: List<ItemWithChecklist>,
        events: List<Item>,
        now: Calendar = Calendar.getInstance(),
        // Запланированные проекты ([includesProject]) — на своём дне или в своём месяце, как задачи
        projects: List<Item> = emptyList()
    ): Schedule {
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
                tasks = tasks.filter { it.item.startDate.inRange(dayStart, nextDayStart) },
                projects = projects.filter { it.startDate.inRange(dayStart, nextDayStart) }.sortedBy { it.startDate }
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
        val laterProjects = projects.filter { it.startDate.inRange(horizonEnd, laterLimit) }
            .groupBy { monthKey(it.startDate!!) }

        val horizonKey = monthKey(horizonEnd)
        cal.timeInMillis = horizonEnd
        val horizonDay = cal.get(Calendar.DAY_OF_MONTH)

        val periods = (laterTasks.keys + laterEvents.keys + laterProjects.keys).sorted().map { key ->
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
                tasks = laterTasks[key].orEmpty().sortedBy { it.item.startDate },
                projects = laterProjects[key].orEmpty().sortedBy { it.startDate }
            )
        }
        // За горизонтом месяцев — по годам: задачи и проекты, без событий календаря
        fun yearOf(millis: Long): Int {
            cal.timeInMillis = millis
            return cal.get(Calendar.YEAR)
        }
        val yearTasks = tasks.filter { (it.item.startDate ?: Long.MIN_VALUE) >= laterLimit }.groupBy { yearOf(it.item.startDate!!) }
        val yearProjects = projects.filter { (it.startDate ?: Long.MIN_VALUE) >= laterLimit }.groupBy { yearOf(it.startDate!!) }
        val years = (yearTasks.keys + yearProjects.keys).sorted().map { year ->
            cal.clear()
            cal.set(year, Calendar.JANUARY, 1)
            Period(
                start = maxOf(cal.timeInMillis, laterLimit),
                year = year,
                month = Calendar.JANUARY,
                firstDay = 1,
                lastDay = 31,
                isRemainder = false,
                events = emptyList(),
                tasks = yearTasks[year].orEmpty().sortedBy { it.item.startDate },
                projects = yearProjects[year].orEmpty().sortedBy { it.startDate },
                isYear = true
            )
        }
        return Schedule(days, periods + years)
    }

    private fun Long?.inRange(from: Long, until: Long): Boolean = this != null && this >= from && this < until
}
