package com.example.domain.reminders

import com.example.data.model.Item
import java.util.Calendar

/**
 * Когда срабатывает напоминание задачи. Закреплено тестами RemindersTest.
 *
 * Напоминание — время дня ([Item.reminderTime], минуты от полуночи) в день задачи из «Когда»: в день
 * даты старта, а у задачи «Сегодня» без даты — сегодня. Без дня (без даты, «Когда-нибудь», «В любое
 * время») напоминание не срабатывает. У выполненной, отменённой и удалённой задачи — тоже.
 */
object Reminders {

    /** Минут в сутках: время напоминания — от 0 до этого значения, не включая его */
    const val MINUTES_PER_DAY = 24 * 60

    /** Момент срабатывания напоминания задачи (мс) или null, если оно не сработает ни в какой день. */
    fun fireAt(item: Item, now: Long = System.currentTimeMillis()): Long? {
        val minutes = item.reminderTime ?: return null
        if (item.type != Item.TYPE_TASK || item.isClosed || item.trashed) return null
        val day = item.startDate ?: if (item.start == Item.START_TODAY) now else return null
        return Calendar.getInstance().apply {
            timeInMillis = day
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Напоминания, которые ещё впереди: задача и момент срабатывания, по времени. */
    fun upcoming(items: List<Item>, now: Long = System.currentTimeMillis()): List<Pair<Item, Long>> =
        items.mapNotNull { item -> fireAt(item, now)?.takeIf { it > now }?.let { item to it } }
            .sortedBy { it.second }

    /** У задачи есть день, в который сработает напоминание: дата старта или «Сегодня». */
    fun hasDay(startDate: Long?, start: Int): Boolean = startDate != null || start == Item.START_TODAY

    /** Время по умолчанию для нового напоминания — следующий целый час (полночь — как 0:00). */
    fun defaultTime(now: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return ((cal.get(Calendar.HOUR_OF_DAY) + 1) % 24) * 60
    }
}
