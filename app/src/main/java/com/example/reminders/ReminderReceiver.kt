package com.example.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.TaskDao
import com.example.di.ApplicationScope
import com.example.domain.reminders.Reminders
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * Срабатывание напоминания: показывает уведомление с названием задачи. Перед показом сверяется с
 * базой — задачу могли выполнить, удалить или перенести напоминание, пока процесс не работал.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var dao: TaskDao
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                val item = dao.getItemById(taskId)
                val now = System.currentTimeMillis()
                // Напоминание ещё в силе и это его время (а не будильник, оставшийся от прежнего времени)
                val fireAt = item?.let { Reminders.fireAt(it, now) }
                if (item != null && fireAt != null && abs(fireAt - now) <= STALE_TOLERANCE_MS) {
                    show(context, taskId, item.title)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, taskId: String, title: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(title.ifBlank { context.getString(R.string.untitled_task) })
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(taskId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Разрешение на уведомления отозвали между проверкой и показом
        }
    }

    companion object {
        const val ACTION_REMIND = "com.bgdlab.things.action.REMIND"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
        private const val CHANNEL_ID = "reminders"

        /** Будильник считается своим, если сработал не позже этого после назначенного времени (Doze откладывает) */
        private const val STALE_TOLERANCE_MS = 15 * 60 * 1000L

        /** Канал уведомлений «Напоминания» — создаётся при первом показе, повторное создание ничего не меняет */
        fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.reminder_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

/**
 * Перезагрузка телефона, обновление приложения, смена времени или часового пояса: будильники
 * пропадают или сдвигаются — ставим заново по базе.
 */
@AndroidEntryPoint
class ReminderRescheduleReceiver : BroadcastReceiver() {

    @Inject lateinit var sync: ReminderSync
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                sync.syncNow()
            } finally {
                pending.finish()
            }
        }
    }
}
