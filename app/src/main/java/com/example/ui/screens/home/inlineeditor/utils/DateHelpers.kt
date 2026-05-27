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
