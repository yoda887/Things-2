package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.theme.ThingsTheme

/**
 * Колонка дня закрытия в «Журнале» — в размерах шрифта даты: самая широкая дата «28.10.» занимает
 * около трёх, «28 жовт.» — около пяти. Ширина одна на все строки — названия задач и проектов стоят ровно,
 * а с размером шрифта колонка растёт сама.
 */
private const val LOGBOOK_DATE_COLUMN_EM = 3.4f
private const val LOGBOOK_DATE_COLUMN_MONTH_NAME_EM = 4.9f

/**
 * Дата закрытия перед названием — как в Logbook эталона: мельче названия, цветом значка проекта. В «Журнале» ([inLogbook])
 * над строками уже стоит месяц: день и месяц словом («9 жовт.») или цифрами («9.10.») — настройка
 * [com.example.ui.components.LocalDatesWithMonthName], в колонке одной ширины для задач и проектов. В поиске — полная короткая дата в формате системы (28.04.24).
 */
@Composable
fun ClosedDateLabel(closedAt: Long, inLogbook: Boolean) {
    val monthName = com.example.ui.components.LocalDatesWithMonthName.current
    val text = remember(closedAt, inLogbook, monthName) {
        if (inLogbook) formatLogbookDay(closedAt, monthName) else formatSearchLogbookDate(closedAt)
    }
    val columnEm = if (monthName) LOGBOOK_DATE_COLUMN_MONTH_NAME_EM else LOGBOOK_DATE_COLUMN_EM
    // Мельче названия и цветом значка проекта, как в Logbook Things
    val style = ThingsTheme.type.taskSubtitleStrong.copy(color = ThingsTheme.colors.project)
    val fontSize = style.fontSize
    val density = LocalDensity.current
    Text(
        text = text,
        style = style,
        maxLines = 1,
        modifier = if (inLogbook) Modifier.width(with(density) { (fontSize * columnEm).toDp() }) else Modifier
    )
}

/** День закрытия в «Журнале»: «9 жовт.» ([monthName]) или цифрами, как в Logbook Things, — «9.10.». */
private fun formatLogbookDay(millis: Long, monthName: Boolean): String =
    java.text.SimpleDateFormat(if (monthName) "d MMM" else "d.M.", java.util.Locale.getDefault()).format(java.util.Date(millis))

/** Короткая числовая дата в формате системы: 04/28/24 для en-US, 28.04.24 для ru/uk. */
private fun formatSearchLogbookDate(millis: Long): String {
    val locale = java.util.Locale.getDefault()
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "ddMMyy")
    return java.text.SimpleDateFormat(pattern, locale).format(java.util.Date(millis))
}
