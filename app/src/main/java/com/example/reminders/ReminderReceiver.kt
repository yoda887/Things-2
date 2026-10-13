package com.example.reminders

import kotlinx.coroutines.flow.first
import com.example.data.model.Item
import com.example.domain.usecase.ProjectUseCases
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
import com.example.data.model.ItemWithChecklist
import com.example.domain.usecase.TaskUseCases
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
    @Inject lateinit var taskUseCases: TaskUseCases
    @Inject lateinit var projectUseCases: ProjectUseCases
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        when (intent.action) {
            ACTION_REMIND -> remind(context, taskId)
            ACTION_COMPLETE, ACTION_SNOOZE -> act(context, intent, taskId)
        }
    }

    /**
     * Кнопки уведомления: «Выполнено» отмечает задачу, «Через 15 мин» и «Через час» откладывают
     * напоминание ([Reminders.snoozed]). Будильники пересчитает ReminderSync — задача изменилась в базе.
     */
    private fun act(context: Context, intent: Intent, taskId: String) {
        NotificationManagerCompat.from(context).cancel(taskId.hashCode())
        val pending = goAsync()
        scope.launch {
            try {
                val item = dao.getItemById(taskId) ?: return@launch
                if (item.isClosed || item.trashed) return@launch
                if (intent.action == ACTION_COMPLETE && item.type == Item.TYPE_PROJECT) {
                    // Проект завершается вместе с открытыми задачами — как «Завершить проект» в приложении
                    val projectTasks = dao.getAllItems().first()
                        .filter { it.projectId == item.id && it.type == Item.TYPE_TASK }
                        .map { ItemWithChecklist(it) }
                    projectUseCases.completeProject(item, projectTasks)
                } else if (intent.action == ACTION_COMPLETE) {
                    taskUseCases.toggleTaskCompletion(ItemWithChecklist(item, dao.getChecklistItemsByItemId(taskId)))
                } else {
                    val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, SNOOZE_SHORT_MIN)
                    taskUseCases.updateTask(Reminders.snoozed(item, minutes))
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun remind(context: Context, taskId: String) {
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
            // Как в Things: отметить выполненной или напомнить позже — прямо из уведомления
            .addAction(0, context.getString(R.string.reminder_action_complete), actionIntent(context, taskId, ACTION_COMPLETE))
            .addAction(0, context.getString(R.string.reminder_action_snooze_short), actionIntent(context, taskId, ACTION_SNOOZE, SNOOZE_SHORT_MIN))
            // Кнопок не больше трёх: остальные сроки (час, 3 часа, завтра) — в окне «Позже…»
            .addAction(0, context.getString(R.string.reminder_action_snooze_more), laterIntent(context, taskId, title))
            .build()
        try {
            manager.notify(taskId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Разрешение на уведомления отозвали между проверкой и показом
        }
    }

    /** «Позже…» — окно выбора срока поверх всего */
    private fun laterIntent(context: Context, taskId: String, title: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            ("later" + taskId).hashCode(),
            ReminderSnoozeActivity.intent(context, taskId, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Нажатие кнопки уведомления; адрес с задачей и действием делает намерения разных кнопок разными */
    private fun actionIntent(context: Context, taskId: String, action: String, minutes: Int = 0): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(android.net.Uri.parse("things://reminder/$taskId/$action/$minutes"))
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_SNOOZE_MINUTES, minutes)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_REMIND = "com.bgdlab.things.action.REMIND"
        const val ACTION_COMPLETE = "com.bgdlab.things.action.REMINDER_COMPLETE"
        const val ACTION_SNOOZE = "com.bgdlab.things.action.REMINDER_SNOOZE"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"

        /** «Напомнить позже» кнопкой уведомления — через 15 минут */
        const val SNOOZE_SHORT_MIN = 15
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
