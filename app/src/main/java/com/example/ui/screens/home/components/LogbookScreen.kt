package com.example.ui.screens.home.components

import com.example.domain.lists.LogbookList
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Заголовок раздела «Журнала»: «Сегодня», «Вчера» или месяц. */
data class LogbookHeaderItem(val start: Long, val label: String) {
    val key: String get() = TaskListKeys.LOGBOOK_HEADER_PREFIX + start
}

/** Строки экрана «Журнал»: заголовок раздела, затем закрытое в нём (правила — в [LogbookList]). */
object LogbookScreen {

    fun rows(
        sections: List<LogbookList.Section>,
        todayLabel: String,
        yesterdayLabel: String,
        now: Calendar = Calendar.getInstance()
    ): List<Any> = buildList {
        val locale = Locale.getDefault()
        val monthFormat = SimpleDateFormat("LLLL", locale)
        val monthYearFormat = SimpleDateFormat("LLLL yyyy", locale)
        val currentYear = now.get(Calendar.YEAR)
        sections.forEach { section ->
            val label = when (section.period) {
                LogbookList.Period.TODAY -> todayLabel
                LogbookList.Period.YESTERDAY -> yesterdayLabel
                LogbookList.Period.MONTH ->
                    (if (section.year == currentYear) monthFormat else monthYearFormat).format(Date(section.start))
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            }
            add(LogbookHeaderItem(section.start, label))
            addAll(section.entries)
        }
    }
}
