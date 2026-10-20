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

/**
 * Экран «Предстоящие»: подписи расписания, строки списка и признак пустого экрана. Какие задачи на
 * экране и как они разложены по дням и месяцам — UpcomingList. Перетаскивание по дням и месяцам —
 * в TaskDrops. Закреплено тестами UpcomingScreenTest.
 */
object UpcomingScreen {

    /** Расписание экрана ([UpcomingList.schedule]) с подписями дней, разделов и плашками дат задач. */
    fun schedule(
        localTasksList: List<ItemWithChecklist>,
        calendarEvents: List<Item>,
        tomorrowLabel: String,
        now: Calendar = Calendar.getInstance(),
        projects: List<Item> = emptyList()
    ): UpcomingSchedule {
        val plan = UpcomingList.schedule(localTasksList, calendarEvents, now, projects)

        val locale = Locale.getDefault()
        val weekdayFormat = SimpleDateFormat("EEEE", locale)
        val monthFormat = SimpleDateFormat("LLLL", locale)
        val monthYearFormat = SimpleDateFormat("LLLL yyyy", locale)
        // Плашка задачи в месячных разделах — число и месяц, как на экране проекта
        val dayBadgeFormat = SimpleDateFormat("d MMM", locale)
        val dayNumFormat = SimpleDateFormat("d", locale)
        val currentYear = now.get(Calendar.YEAR)
        val cal = Calendar.getInstance()

        val days = plan.days.mapIndexed { offset, day ->
            cal.timeInMillis = day.dayStart
            UpcomingDay(
                dateMillis = day.dayStart,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString(),
                dayOfWeekLabel = if (offset == 0) tomorrowLabel else weekdayFormat.format(Date(day.dayStart)),
                calendarEvents = day.events.map { ItemWithChecklist(item = it, checklist = emptyList()) },
                tasks = day.tasks,
                projects = day.projects
            )
        }

        val months = plan.periods.map { period ->
            val label = if (period.isRemainder) {
                // Остаток месяца: «16 – 31 октября», один день — просто дата
                cal.clear()
                cal.set(period.year, period.month, period.lastDay)
                val pattern = if (period.year == currentYear) "d MMMM" else "d MMMM yyyy"
                val endFormatted = SimpleDateFormat(pattern, locale).format(cal.time)
                if (period.firstDay == period.lastDay) endFormatted else "${period.firstDay} – $endFormatted"
            } else {
                val raw = (if (period.year == currentYear) monthFormat else monthYearFormat).format(Date(period.start))
                raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            }
            UpcomingMonth(
                monthMillis = period.start,
                monthLabel = label,
                isRemainder = period.isRemainder,
                calendarEvents = period.events.map { event ->
                    val start = event.eventStartMillis ?: 0L
                    UpcomingEventItem(event = event, dateMillis = start, dayOfMonthLabel = dayNumFormat.format(Date(start)))
                },
                tasks = period.tasks,
                projects = period.projects
            )
        }

        // Плашки с датой в месячных разделах — у задач и у проектов. Ключ — id и день ([monthBadgeKey]): шаблон
        // повторяющейся задачи стоит на экране несколько раз (в дни своих повторов) с одним id, и плашка месяца
        // не должна доставаться его строкам в дневных разделах
        val monthTaskBadges = plan.periods.flatMap { it.tasks }
            .associate { monthBadgeKey(it.item) to dayBadgeFormat.format(Date(it.item.startDate ?: 0L)) } +
            plan.periods.flatMap { it.projects }.associate { monthBadgeKey(it) to dayBadgeFormat.format(Date(it.startDate ?: 0L)) }

        return UpcomingSchedule(days = days, months = months, monthTaskBadges = monthTaskBadges)
    }

    /** Ключ плашки даты месячного раздела: id и день (у шаблона повтора — день его повтора на этом месте) */
    fun monthBadgeKey(item: Item): String = "${item.id}@${item.startDate}"

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
            // Проекты дня — после событий, перед задачами
            addAll(day.projects)
            day.tasks.forEach { task ->
                add(task)
            }
        }
        upcomingMonths.forEach { month ->
            add(UpcomingMonthHeaderItem(month.monthMillis, month.monthLabel, month.isRemainder))
            // События календаря месяца выводятся первыми, не смешиваясь с задачами
            month.calendarEvents.forEachIndexed { index, event ->
                // Как и у дней: отступ не зависит от наличия задач, иначе высота строки
                // менялась бы во время перетаскивания
                val isLast = index == month.calendarEvents.lastIndex
                add(event.copy(isLastBeforeTasks = isLast))
            }
            addAll(month.projects)
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
    calendarEvents: List<Item>,
    projects: List<Item> = emptyList()
): UpcomingSchedule {
    val tomorrowLabel = stringResource(R.string.tomorrow)
    return remember(localTasksList, calendarEvents, projects, tomorrowLabel) {
        // Запланированные проекты — на своём дне или в своём месяце (UpcomingList.includesProject)
        val bounds = com.example.data.model.DayBounds.now()
        UpcomingScreen.schedule(
            localTasksList, calendarEvents, tomorrowLabel,
            projects = projects.filter { UpcomingList.includesProject(it, bounds) }
        )
    }
}
