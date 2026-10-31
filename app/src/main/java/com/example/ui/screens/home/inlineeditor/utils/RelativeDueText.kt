package com.example.ui.screens.home.inlineeditor.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.R

/** Срок относительно сегодня на языке системы: «просрочено», «сегодня», «завтра», «через N дней». */
@Composable
fun relativeDueText(deltaDays: Int): String = when {
    deltaDays < 0 -> stringResource(R.string.due_overdue)
    deltaDays == 0 -> stringResource(R.string.due_today)
    deltaDays == 1 -> stringResource(R.string.due_tomorrow)
    else -> pluralStringResource(R.plurals.due_in_days, deltaDays, deltaDays)
}

/**
 * Дедлайн на карточке в списке: позже, чем через месяц (после того же дня следующего месяца), — датой, как даты в
 * списках («5 груд.»; другой год — с годом); ближе — относительно сегодня ([relativeDueText]).
 */
@Composable
fun listDueText(dueDate: Long): String {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    val due = java.time.Instant.ofEpochMilli(dueDate).atZone(zone).toLocalDate()
    return if (DueLabels.showsDate(today, due)) {
        DueLabels.dateText(due, today, com.example.ui.components.LocalDatesWithMonthName.current, java.util.Locale.getDefault())
    } else {
        relativeDueText(java.time.temporal.ChronoUnit.DAYS.between(today, due).toInt())
    }
}

/** Правило подписи дедлайна в списках. Закреплено тестами DueLabelsTest. */
object DueLabels {

    /** Датой — дедлайн позже того же дня следующего месяца; до него включительно — днями */
    fun showsDate(today: java.time.LocalDate, due: java.time.LocalDate): Boolean = due.isAfter(today.plusMonths(1))

    /** «5 груд.» или цифрами «5.12.», как даты в списках (настройка [monthName]); другой год — с годом */
    fun dateText(due: java.time.LocalDate, today: java.time.LocalDate, monthName: Boolean, locale: java.util.Locale): String {
        val sameYear = due.year == today.year
        val pattern = when {
            monthName && sameYear -> "d MMM"
            monthName -> "d MMM yyyy"
            sameYear -> "d.M."
            else -> "d.M.yy"
        }
        val date = java.util.Date.from(due.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        return java.text.SimpleDateFormat(pattern, locale).format(date)
    }
}
