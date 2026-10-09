package com.example.ui.screens.home.components

import com.example.domain.lists.UpcomingList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Горизонт планирования экрана «Предстоящие» в днях: неделя вперёд, начиная с завтра */
private const val UPCOMING_DAYS_HORIZON = 7

/** Сколько целых месяцев показывать после остатка месяца, в котором закончился горизонт */
private const val UPCOMING_MONTHS_HORIZON = 4

/**
 * Экран «Предстоящие»: расписание (неделя по дням, затем месяцы), строки списка и признак пустого
 * экрана; какие задачи на нём — UpcomingList. Перетаскивание по дням и месяцам — в TaskDrops.
 * Закреплено тестами UpcomingScreenTest.
 */
object UpcomingScreen {

    /**
     * Вычисляет полное расписание экрана «Предстоящие»:
     * 1. Неделя подряд начиная с завтра (включая пустые дни).
     * 2. Остаток месяца, в котором закончилась неделя, и следующие [UPCOMING_MONTHS_HORIZON]
     *    месяцев — только те, где есть дела. Дальше этого горизонта экран ничего не показывает.
     */
    fun schedule(
        localTasksList: List<ItemWithChecklist>,
        calendarEvents: List<Item>,
        tomorrowLabel: String,
        now: Calendar = Calendar.getInstance()
    ): UpcomingSchedule {
        val daysList = mutableListOf<UpcomingDay>()

        val cursor = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val locale = Locale.getDefault()
        val weekdayFormat = SimpleDateFormat("EEEE", locale)
        val monthFormat = SimpleDateFormat("MMMM", locale)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", locale)
        // Плашка задачи в месячных разделах — число и месяц, как на экране проекта
        val dayBadgeFormat = SimpleDateFormat("d MMM", locale)
        val dayNumFormat = SimpleDateFormat("d", locale)

        val currentYear = now.get(Calendar.YEAR)

        // Первые 14 дней отображаются ВСЕГДА, даже если они пустые
        for (offset in 0 until UPCOMING_DAYS_HORIZON) {
            val dayStart = cursor.timeInMillis
            val dayOfMonthLabel = cursor.get(Calendar.DAY_OF_MONTH).toString()

            cursor.add(Calendar.DAY_OF_YEAR, 1)
            val nextDayStart = cursor.timeInMillis

            val dayEvents = calendarEvents.filter { event ->
                val eventStart = event.eventStartMillis
                eventStart != null && eventStart >= dayStart && eventStart < nextDayStart
            }.map { ItemWithChecklist(item = it, checklist = emptyList()) }

            val dayTasks = localTasksList.filter { wrapper ->
                val taskStart = wrapper.item.startDate
                taskStart != null && taskStart >= dayStart && taskStart < nextDayStart
            }

            val dayOfWeekLabel = when (offset) {
                0 -> tomorrowLabel
                else -> weekdayFormat.format(Date(dayStart))
            }

            daysList.add(
                UpcomingDay(
                    dateMillis = dayStart,
                    dayOfMonth = dayOfMonthLabel,
                    dayOfWeekLabel = dayOfWeekLabel,
                    calendarEvents = dayEvents,
                    tasks = dayTasks
                )
            )
        }

        // Недельный горизонт закончился в cursor.timeInMillis
        val horizonEndMillis = cursor.timeInMillis

        // Дальняя граница экрана: остаток месяца, в котором закончилась неделя, плюс следующие
        // UPCOMING_MONTHS_HORIZON месяцев. Всё, что позже, на этом экране не показывается
        val laterLimitMillis = Calendar.getInstance().apply {
            timeInMillis = horizonEndMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, UPCOMING_MONTHS_HORIZON + 1)
        }.timeInMillis

        // Задачи и события позже недели, но в пределах месячного горизонта
        val laterTasks = localTasksList.filter { wrapper ->
            val taskStart = wrapper.item.startDate
            taskStart != null && taskStart >= horizonEndMillis && taskStart < laterLimitMillis
        }

        val laterEvents = calendarEvents.filter { event ->
            val eventStart = event.eventStartMillis
            eventStart != null && eventStart >= horizonEndMillis && eventStart < laterLimitMillis
        }

        val monthTaskBadges = mutableMapOf<String, String>()
        laterTasks.forEach { wrapper ->
            val taskStart = wrapper.item.startDate ?: 0L
            monthTaskBadges[wrapper.item.id] = dayBadgeFormat.format(Date(taskStart))
        }

        // Собираем все уникальные месяцы, где есть хотя бы одна задача или событие
        val monthCal = Calendar.getInstance()
        data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
            override fun compareTo(other: MonthKey): Int {
                return if (year != other.year) year.compareTo(other.year) else month.compareTo(other.month)
            }
        }

        val monthsMap = sortedMapOf<MonthKey, Pair<MutableList<ItemWithChecklist>, MutableList<UpcomingEventItem>>>()

        laterTasks.forEach { task ->
            val start = task.item.startDate ?: 0L
            monthCal.timeInMillis = start
            val key = MonthKey(monthCal.get(Calendar.YEAR), monthCal.get(Calendar.MONTH))
            val pair = monthsMap.getOrPut(key) { Pair(mutableListOf(), mutableListOf()) }
            pair.first.add(task)
        }

        laterEvents.forEach { event ->
            val start = event.eventStartMillis ?: 0L
            monthCal.timeInMillis = start
            val key = MonthKey(monthCal.get(Calendar.YEAR), monthCal.get(Calendar.MONTH))
            val pair = monthsMap.getOrPut(key) { Pair(mutableListOf(), mutableListOf()) }
            val dayNum = dayNumFormat.format(Date(start))
            pair.second.add(UpcomingEventItem(event = event, dateMillis = start, dayOfMonthLabel = dayNum))
        }

        val horizonCal = Calendar.getInstance().apply { timeInMillis = horizonEndMillis }

        val monthsList = mutableListOf<UpcomingMonth>()
        monthsMap.forEach { (key, pair) ->
            monthCal.set(Calendar.YEAR, key.year)
            monthCal.set(Calendar.MONTH, key.month)
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            monthCal.set(Calendar.HOUR_OF_DAY, 0)
            monthCal.set(Calendar.MINUTE, 0)
            monthCal.set(Calendar.SECOND, 0)
            monthCal.set(Calendar.MILLISECOND, 0)
            val monthStart = monthCal.timeInMillis

            // Проверяем, является ли секция остатком месяца после 14-дневного горизонта
            val isHorizonMonth = key.year == horizonCal.get(Calendar.YEAR) && key.month == horizonCal.get(Calendar.MONTH)
            val startDay = if (isHorizonMonth) horizonCal.get(Calendar.DAY_OF_MONTH) else 1

            val (label, sectionStartMillis) = if (isHorizonMonth && startDay > 1) {
                val endDayCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, key.year)
                    set(Calendar.MONTH, key.month)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                val pattern = if (key.year == currentYear) "d MMMM" else "d MMMM yyyy"
                val endFormatted = SimpleDateFormat(pattern, locale).format(endDayCal.time)
                val endDay = endDayCal.get(Calendar.DAY_OF_MONTH)
                val periodLabel = if (startDay == endDay) {
                    endFormatted
                } else {
                    "$startDay – $endFormatted"
                }
                Pair(periodLabel, horizonEndMillis)
            } else {
                val rawLabel = if (key.year == currentYear) {
                    monthFormat.format(Date(monthStart))
                } else {
                    monthYearFormat.format(Date(monthStart))
                }
                val capitalizedLabel = rawLabel.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
                Pair(capitalizedLabel, monthStart)
            }

            pair.first.sortBy { it.item.startDate ?: 0L }
            pair.second.sortBy { it.dateMillis }

            monthsList.add(
                UpcomingMonth(
                    monthMillis = sectionStartMillis,
                    monthLabel = label,
                    calendarEvents = pair.second,
                    tasks = pair.first
                )
            )
        }

        return UpcomingSchedule(
            days = daysList,
            months = monthsList,
            monthTaskBadges = monthTaskBadges
        )
    }

    /** Строки списка экрана: заголовок дня, события календаря, задачи; затем месяцы так же. */
    fun rows(upcomingDays: List<UpcomingDay>, upcomingMonths: List<UpcomingMonth>): List<Any> = buildList {
        upcomingDays.forEach { day ->
            add(UpcomingHeaderItem(day.dateMillis, day.dayOfMonth, day.dayOfWeekLabel))
            day.calendarEvents.forEachIndexed { index, event ->
                // Отступ после последнего события дня — всегда, а не только когда за ним есть задачи.
                // Наличие задач меняется прямо во время перетаскивания, и вместе с ним менялась бы
                // высота этой строки: движок, рассчитывающий компенсацию прыжка при неизменных
                // высотах, промахивался бы на величину отступа, и карточка дёргалась бы на переходе дня.
                val isLast = index == day.calendarEvents.lastIndex
                add(UpcomingEventItem(event.item, day.dateMillis, isLastBeforeTasks = isLast))
            }
            day.tasks.forEach { task ->
                add(task)
            }
        }
        upcomingMonths.forEach { month ->
            add(UpcomingMonthHeaderItem(month.monthMillis, month.monthLabel))
            // События календаря месяца выводятся первыми, не смешиваясь с задачами
            month.calendarEvents.forEachIndexed { index, event ->
                // Как и у дней: отступ не зависит от наличия задач, иначе высота строки
                // менялась бы во время перетаскивания
                val isLast = index == month.calendarEvents.lastIndex
                add(event.copy(isLastBeforeTasks = isLast))
            }
            // Задачи месяца выводятся строго после событий календаря
            month.tasks.forEach { task ->
                add(task)
            }
        }
    }

    /** Экран пуст, если в расписании нет ни дней, ни месяцев (неделя по дням показывается всегда). */
    fun isEmpty(schedule: UpcomingSchedule): Boolean = schedule.days.isEmpty() && schedule.months.isEmpty()
}

/** Запоминает расписание экрана «Предстоящие» (дни и месяцы). */
@Composable
fun rememberUpcomingSchedule(
    localTasksList: List<ItemWithChecklist>,
    calendarEvents: List<Item>
): UpcomingSchedule {
    val tomorrowLabel = stringResource(R.string.tomorrow)
    return remember(localTasksList, calendarEvents, tomorrowLabel) {
        UpcomingScreen.schedule(localTasksList, calendarEvents, tomorrowLabel)
    }
}
