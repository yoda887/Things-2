package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.theme.ThingsTheme

/**
 * Колонка дня закрытия в «Журнале» — в размерах шрифта даты: «10 жовт.» занимает ~4, запас на длинные
 * сокращения месяцев. Ширина одна на все строки — названия задач и проектов стоят ровно, а с размером
 * шрифта колонка растёт сама.
 */
private const val LOGBOOK_DATE_COLUMN_EM = 4.4f

/**
 * Дата закрытия перед названием — как в Logbook эталона, фирменным голубым. В «Журнале» ([inLogbook])
 * над строками уже стоит месяц: день и месяц коротко («9 жовт.»), в колонке одной ширины для задач и
 * проектов. В поиске — полная короткая дата в формате системы (28.04.24).
 */
@Composable
fun ClosedDateLabel(closedAt: Long, inLogbook: Boolean) {
    val text = remember(closedAt, inLogbook) {
        if (inLogbook) formatLogbookDay(closedAt) else formatSearchLogbookDate(closedAt)
    }
    val fontSize = ThingsTheme.type.taskTitle.fontSize * 0.9f
    val density = LocalDensity.current
    Text(
        text = text,
        style = ThingsTheme.type.taskSubtitleStrong.copy(fontSize = fontSize, color = ThingsTheme.colors.accent),
        maxLines = 1,
        modifier = if (inLogbook) Modifier.width(with(density) { (fontSize * LOGBOOK_DATE_COLUMN_EM).toDp() }) else Modifier
    )
}

/** День закрытия в «Журнале»: «9 жовт.» — коротко, месяц и так в заголовке раздела. */
private fun formatLogbookDay(millis: Long): String =
    java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(millis))

/** Короткая числовая дата в формате системы: 04/28/24 для en-US, 28.04.24 для ru/uk. */
private fun formatSearchLogbookDate(millis: Long): String {
    val locale = java.util.Locale.getDefault()
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "ddMMyy")
    return java.text.SimpleDateFormat(pattern, locale).format(java.util.Date(millis))
}
