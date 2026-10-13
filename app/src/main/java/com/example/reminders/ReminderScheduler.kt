package com.example.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.data.model.Item
import com.example.domain.reminders.Reminders
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Будильники напоминаний в AlarmManager. [sync] приводит их к списку задач: ставит предстоящие
 * ([Reminders.upcoming]) и снимает те, что больше не нужны (задача выполнена, удалена, напоминание
 * убрано или перенесено). Поставленные запоминаются, чтобы снять их и после перезапуска процесса.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Что поставлено в этом процессе: задача → момент и название. Без изменений будильники не трогаем */
    private var scheduled: Map<String, Pair<Long, String>>? = null

    @Synchronized
    fun sync(items: List<Item>, now: Long = System.currentTimeMillis()) {
        val wanted = Reminders.upcoming(items, now).associate { (item, at) -> item.id to (at to item.title) }
        if (wanted == scheduled) return
        val previousIds = scheduled?.keys ?: prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
        (previousIds - wanted.keys).forEach { id -> alarmManager.cancel(pendingIntent(id, null)) }
        wanted.forEach { (id, value) ->
            val (at, title) = value
            if (scheduled?.get(id) == value) return@forEach
            val intent = pendingIntent(id, title)
            // Точное время, как у будильника; если система не даёт точных — ближайшее разрешённое
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        }
        scheduled = wanted
        prefs.edit().putStringSet(KEY_IDS, wanted.keys).apply()
    }

    /** Будильник задачи. Адрес с id делает намерения разных задач разными для AlarmManager. */
    private fun pendingIntent(taskId: String, title: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            .setData(Uri.parse("things://reminder/$taskId"))
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderReceiver.EXTRA_TITLE, title)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val PREFS = "reminders"
        const val KEY_IDS = "scheduled_ids"
    }
}
