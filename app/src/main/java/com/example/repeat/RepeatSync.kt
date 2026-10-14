package com.example.repeat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.TaskDao
import com.example.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Когда создаются копии повторяющихся задач: при запуске приложения, при изменении задач (закрыли копию
 * «после выполнения»), сразу после полуночи — будильником, чтобы копия с напоминанием появилась вовремя,
 * даже если приложение не открывали, — и после перезагрузки или смены времени.
 */
@Singleton
class RepeatSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TaskDao,
    private val generator: RepeatGenerator,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var started = false

    @OptIn(FlowPreview::class)
    @Synchronized
    fun start() {
        if (started) return
        started = true
        scheduleNextDay()
        scope.launch {
            // Первое значение — сразу при запуске, дальше — после изменений задач и правил (новое правило,
            // пауза и возобновление меняют только правила). Генератор пишет правило, лишь когда что-то создал,
            // поэтому сам себя не перезапускает бесконечно
            merge(dao.getAllItems().map { }, dao.getAllRecurrenceRulesFlow().map { })
                .debounce(CHANGE_DEBOUNCE_MS)
                .collect { generator.run() }
        }
    }

    /** Один прогон — по будильнику полуночи, после перезагрузки или смены времени. */
    suspend fun runNow() {
        generator.run()
        scheduleNextDay()
    }

    /** Будильник сразу после ближайшей полуночи: копии нового дня создаются, даже если приложение закрыто. */
    fun scheduleNextDay() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val at = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + AFTER_MIDNIGHT_MS
        val intent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, RepeatDayReceiver::class.java).setAction(ACTION_NEW_DAY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    companion object {
        const val ACTION_NEW_DAY = "com.bgdlab.things.action.REPEAT_NEW_DAY"
        private const val CHANGE_DEBOUNCE_MS = 1_000L
        /** Чуть позже полуночи, чтобы «сегодня» уже наступило на всех часах */
        private const val AFTER_MIDNIGHT_MS = 5_000L
    }
}

/** Будильник новой дня: создать копии повторяющихся задач на сегодня и поставить будильник на следующую полночь. */
@AndroidEntryPoint
class RepeatDayReceiver : BroadcastReceiver() {

    @Inject lateinit var sync: RepeatSync
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RepeatSync.ACTION_NEW_DAY) return
        val pending = goAsync()
        scope.launch {
            try {
                sync.runNow()
            } finally {
                pending.finish()
            }
        }
    }
}
