package com.example.ui.screens.home.inlineeditor.utils

import java.util.Calendar

fun isPastDate(year: Int, month: Int, day: Int, today: java.util.Calendar): Boolean {
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    
    if (year < todayYear) return true
    if (year > todayYear) return false
    
    if (month < todayMonth) return true
    if (month > todayMonth) return false
    
    return day < todayDay
}

fun isTodayDate(timestamp: Long?): Boolean {
    if (timestamp == null) return false
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
           cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

fun isSameDay(t1: Long?, t2: Long): Boolean {
    if (t1 == null) return false
    val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isTodayDateOrPast(timestamp: Long?): Boolean {
    if (timestamp == null) return false
    val cal = Calendar.getInstance()
    val endOfToday = cal.apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    return timestamp <= endOfToday
}

/**
 * Форматирует надпись стартовой даты задачи по правилам:
 * • Сегодня (или просроченная дата старта) -> "Сегодня" / "Сегодня вечером"
 * • Завтра -> "Завтра"
 * • На текущей неделе (позже завтра) -> "Чт, 23 июл."
 * • Позже текущей недели (в текущем году) -> "15 авг."
 * • Позже текущего года -> "2027 г."
 */
fun formatStartDateLabel(
    startDate: Long?,
    section: com.example.data.model.TaskSection,
    isTonight: Boolean
): String {
    if (section == com.example.data.model.TaskSection.SOMEDAY) {
        val lang = java.util.Locale.getDefault().language
        return when (lang) {
            "ru" -> "Когда-нибудь"
            "uk" -> "Колись"
            else -> "Someday"
        }
    }

    val isToday = (startDate != null && isTodayDateOrPast(startDate)) || (startDate == null && section == com.example.data.model.TaskSection.TODAY)
    if (isToday) {
        val lang = java.util.Locale.getDefault().language
        return if (isTonight) {
            when (lang) {
                "ru" -> "Сегодня вечером"
                "uk" -> "Сьогодні ввечері"
                else -> "This Evening"
            }
        } else {
            when (lang) {
                "ru" -> "Сегодня"
                "uk" -> "Сьогодні"
                else -> "Today"
            }
        }
    }

    if (startDate == null) return ""

    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val targetCal = Calendar.getInstance().apply {
        timeInMillis = startDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
    val diffDays = (diffMillis / (24 * 60 * 60 * 1000L)).toInt()

    val lang = java.util.Locale.getDefault().language

    if (diffDays == 1) {
        return when (lang) {
            "ru" -> "Завтра"
            "uk" -> "Завтра"
            else -> "Tomorrow"
        }
    }

    val startOfWeek = (todayCal.clone() as Calendar).apply {
        firstDayOfWeek = Calendar.SUNDAY
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }
    val endOfWeek = (startOfWeek.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, 6)
    }

    val isThisWeek = targetCal.timeInMillis >= startOfWeek.timeInMillis && targetCal.timeInMillis <= endOfWeek.timeInMillis
    val isCurrentYear = targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)

    val locale = java.util.Locale.getDefault()
    return when {
        isThisWeek -> java.text.SimpleDateFormat("EEE, d MMM", locale).format(targetCal.time)
        isCurrentYear -> java.text.SimpleDateFormat("d MMM", locale).format(targetCal.time)
        else -> java.text.SimpleDateFormat("yyyy", locale).format(targetCal.time)
    }
}
