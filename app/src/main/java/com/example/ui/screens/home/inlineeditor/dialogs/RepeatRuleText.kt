package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.domain.repeat.RepeatDay
import com.example.domain.repeat.RepeatRule
import com.example.domain.repeat.RepeatUnit
import com.example.domain.repeat.RepeatWording
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Регулярное правило обычной фразой, с заглавной: «Щотижня по середах», «Кожні 2 тижні по буднях», «Щомісяця 1-го і
 * 15-го числа», «Щомісяця у 2-й вівторок», «Щороку в останній понеділок травня». Все семь дней недели — «Щодня».
 * Порядковые номера — цифрами; форма (будни, выходные, числа вместе) — RepeatWording.
 */
@Composable
internal fun repeatRulePhrase(rule: RepeatRule): String {
    val weekGroup = if (rule.unit == RepeatUnit.WEEK) RepeatWording.weekGroup(RepeatWording.weekdays(rule)) else null
    // Каждый день недели раз в неделю — это просто «щодня»
    if (weekGroup == RepeatWording.WeekGroup.ALL && rule.interval == 1) return stringResource(R.string.repeat_every_one_day)
    val every = if (rule.interval == 1) {
        stringResource(
            when (rule.unit) {
                RepeatUnit.DAY -> R.string.repeat_every_one_day
                RepeatUnit.WEEK -> R.string.repeat_every_one_week
                RepeatUnit.MONTH -> R.string.repeat_every_one_month
                RepeatUnit.YEAR -> R.string.repeat_every_one_year
            }
        )
    } else {
        val unit = pluralStringResource(
            when (rule.unit) {
                RepeatUnit.DAY -> R.plurals.repeat_unit_day
                RepeatUnit.WEEK -> R.plurals.repeat_unit_week
                RepeatUnit.MONTH -> R.plurals.repeat_unit_month
                RepeatUnit.YEAR -> R.plurals.repeat_unit_year
            },
            rule.interval
        )
        "${stringResource(R.string.repeat_every)} ${rule.interval} $unit"
    }
    val and = stringResource(R.string.repeat_list_and)
    val on: String = when (rule.unit) {
        RepeatUnit.DAY -> ""
        RepeatUnit.WEEK -> when (weekGroup) {
            RepeatWording.WeekGroup.ALL -> ", " + stringResource(R.string.repeat_every_one_day).replaceFirstChar { it.lowercase() }
            RepeatWording.WeekGroup.WORKDAYS -> stringResource(R.string.repeat_on_workdays)
            RepeatWording.WeekGroup.WEEKENDS -> stringResource(R.string.repeat_on_weekends)
            RepeatWording.WeekGroup.LIST ->
                stringResource(R.string.repeat_on_weekdays, joinAnd(RepeatWording.weekdays(rule).map { weekdayPlural(it) }, and))
            else -> ""
        }
        RepeatUnit.MONTH -> joinAnd(monthPhrases(RepeatWording.monthParts(rule.on), month = null), and)
        RepeatUnit.YEAR -> joinAnd(RepeatWording.byMonth(rule.on).flatMap { (month, parts) -> monthPhrases(parts, month) }, and)
    }
    return when {
        on.isEmpty() -> every
        on.startsWith(",") -> every + on
        else -> "$every $on"
    }
}

/** Части дня месяца фразами; [month] — у ежегодных правил: «15 березня», «в останній день лютого» */
@Composable
private fun monthPhrases(parts: RepeatWording.MonthParts, month: Int?): List<String> {
    val and = stringResource(R.string.repeat_list_and)
    val monthName = month?.let { Month.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    val out = ArrayList<String>()
    if (parts.numbers.isNotEmpty()) {
        out += if (monthName != null) {
            stringResource(R.string.repeat_on_year_date, joinAnd(parts.numbers.map { it.toString() }, and), monthName)
        } else {
            stringResource(R.string.repeat_on_month_days, joinAnd(parts.numbers.map { stringResource(R.string.repeat_on_month_day, ordinalArg(it)) }, and))
        }
    }
    if (parts.lastDay) out += withMonth(stringResource(R.string.repeat_on_last_day), monthName)
    parts.weekdays.forEach { day ->
        val phrase = if (day.ordinal == RepeatDay.LAST) {
            stringResource(LAST_WEEKDAY[day.dayOfWeek - 1])
        } else {
            stringResource(NTH_WEEKDAY[day.dayOfWeek - 1], ordinalArg(day.ordinal.coerceAtLeast(1)))
        }
        out += withMonth(phrase, monthName)
    }
    return out
}

@Composable
private fun withMonth(phrase: String, monthName: String?): String =
    if (monthName == null) phrase else stringResource(R.string.repeat_in_month, phrase, monthName)

@Composable
private fun weekdayPlural(dayOfWeek: Int): String = stringResource(WEEKDAY_PLURAL[dayOfWeek - 1])

/** «a», «a і b», «a, b і c» */
private fun joinAnd(parts: List<String>, and: String): String = when (parts.size) {
    0 -> ""
    1 -> parts[0]
    else -> parts.dropLast(1).joinToString(", ") + " $and " + parts.last()
}

/** Номер для строк с порядковым числом: по-английски с окончанием («1st»), в остальных языках окончание — в строке */
private fun ordinalArg(n: Int): String {
    if (Locale.getDefault().language != "en") return n.toString()
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$n$suffix"
}

private val WEEKDAY_PLURAL = listOf(
    R.string.repeat_weekday_plural_1, R.string.repeat_weekday_plural_2, R.string.repeat_weekday_plural_3,
    R.string.repeat_weekday_plural_4, R.string.repeat_weekday_plural_5, R.string.repeat_weekday_plural_6,
    R.string.repeat_weekday_plural_7
)

/** «у 2-й вівторок» — у каждого дня свой род, поэтому строка на день */
private val NTH_WEEKDAY = listOf(
    R.string.repeat_on_nth_weekday_1, R.string.repeat_on_nth_weekday_2, R.string.repeat_on_nth_weekday_3,
    R.string.repeat_on_nth_weekday_4, R.string.repeat_on_nth_weekday_5, R.string.repeat_on_nth_weekday_6,
    R.string.repeat_on_nth_weekday_7
)

private val LAST_WEEKDAY = listOf(
    R.string.repeat_on_last_weekday_1, R.string.repeat_on_last_weekday_2, R.string.repeat_on_last_weekday_3,
    R.string.repeat_on_last_weekday_4, R.string.repeat_on_last_weekday_5, R.string.repeat_on_last_weekday_6,
    R.string.repeat_on_last_weekday_7
)
