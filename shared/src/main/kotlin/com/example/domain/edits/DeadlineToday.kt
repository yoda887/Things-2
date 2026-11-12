package com.example.domain.edits

import com.example.data.model.DayBounds
import com.example.data.model.Item

/**
 * Дедлайн приводит задачу или проект в «Сегодня», как в Things: дата старта становится сегодняшней.
 *
 * - Поставили дедлайн на сегодня или на прошедший день — старт сразу сегодня ([afterDeadlineEdit]).
 * - Наступил день дедлайна — при первом пересчёте в этот день старт ставится на сегодня ([dueNow]). «Первый» —
 *   значит, с начала дня дедлайна задачу не меняли: если её потом сами перенесли на другой день, она там и
 *   остаётся, а не возвращается при каждом пересчёте.
 *
 * Шаблоны повтора не трогаются — дни им задаёт правило. Закреплено тестами DeadlineTodayTest.
 */
object DeadlineToday {

    /** Открытая задача или проект с дедлайном сегодня или раньше, ещё не стоящие на сегодня. */
    fun pulls(item: Item, bounds: DayBounds): Boolean {
        if (item.type != Item.TYPE_TASK && item.type != Item.TYPE_PROJECT) return false
        if (item.isClosed || item.trashed) return false
        val due = item.dueDate ?: return false
        if (due > bounds.endOfToday) return false
        return !startsToday(item, bounds)
    }

    /** Старт сегодня или раньше: дата старта не позже сегодня, а без даты — раздел «Сегодня». */
    private fun startsToday(item: Item, bounds: DayBounds): Boolean =
        item.startDate?.let { it <= bounds.endOfToday } ?: (item.start == Item.START_TODAY)

    /** Задача или проект на сегодня — как выбор «Сегодня» в окне «Когда?». */
    fun today(item: Item, now: Long): Item =
        item.copy(start = Item.START_TODAY, startDate = now, isTonight = false, modificationDate = now)

    /** После правки: дедлайн изменили на сегодня или прошедший день — старт тоже сегодня. */
    fun afterDeadlineEdit(before: Item, after: Item, bounds: DayBounds, now: Long): Item =
        if (after.dueDate != before.dueDate && pulls(after, bounds)) today(after, now) else after

    /** Новая задача или проект с дедлайном на сегодня или раньше — сразу на сегодня. */
    fun forNew(item: Item, bounds: DayBounds, now: Long): Item =
        if (pulls(item, bounds)) today(item, now) else item

    /**
     * Задачи и проекты, у которых наступил день дедлайна, — уже поставленные на сегодня. Только те, что не меняли с
     * начала дня дедлайна: в этот день их ещё не пересчитывали и сами не переносили.
     */
    fun dueNow(items: List<Item>, bounds: DayBounds, now: Long): List<Item> =
        items.filter { item ->
            val due = item.dueDate
            due != null && pulls(item, bounds) && item.modificationDate < dayStart(due)
        }.map { today(it, now) }

    /** Начало дня, в который попадает момент [millis]. */
    private fun dayStart(millis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
