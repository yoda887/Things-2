package com.example.ui.screens.home.inlineeditor.utils

import android.content.Context
import java.util.Calendar

/** Время напоминания (минуты от полуночи) в формате часов телефона — 24-часовом или с AM/PM. */
fun formatReminderTime(context: Context, minutes: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
    }
    return android.text.format.DateFormat.getTimeFormat(context).format(cal.time)
}
