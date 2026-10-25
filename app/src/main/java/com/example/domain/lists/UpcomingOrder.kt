package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import java.time.Instant
import java.time.ZoneId

/**
 * Порядок внутри дня в «Планах», как в Things: задачи и проекты дня — одним списком по общему полю
 * [Item.dayOrder] (меньше — выше); их sortOrder — порядок других списков, он здесь не участвует. Перенесённое
 * на другой день встаёт в нём первым; перетаскивание в «Планах» нумерует день заново. У записей без порядка
 * (null) день выглядит как прежде: проекты, затем задачи. Закреплено тестами UpcomingOrderTest.
 */
object UpcomingOrder {

    /** Шаг порядка: перенумерованные строки дня — подряд, а «первым» потом всё равно встаёт ниже их */
    private const val STEP = 1000L

    /** Ключ строки: без порядка — после всех с порядком */
    fun key(item: Item): Long = item.dayOrder ?: Long.MAX_VALUE

    /**
     * Порядок «первым в дне»: тем меньше, чем позже — перенесённое позже встаёт выше перенесённого раньше
     * и выше любого перенумерованного до того дня ([renumber])
     */
    fun top(now: Long = System.currentTimeMillis()): Long = -now * STEP

    /**
     * Запись [new] вместо [old]: если её перенесли на другой день, а порядок в дне не задан заново (это делает
     * перетаскивание в «Планах»), — она встаёт в новом дне первой.
     */
    fun rescheduled(old: Item?, new: Item, zone: ZoneId = ZoneId.systemDefault(), now: Long = System.currentTimeMillis()): Item {
        if (old == null || new.startDate == null) return new
        if (new.dayOrder != old.dayOrder) return new
        if (old.startDate != null && dayOf(old.startDate, zone) == dayOf(new.startDate, zone)) return new
        return new.copy(dayOrder = top(now))
    }

    /** Задачи «Планов» в порядке дней: по [key], при равном — как были (sortOrder) */
    fun orderTasks(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.sortedBy { key(it.item) }

    /**
     * Строки дня: проекты и задачи вперемешку. Задачи — в своём порядке ([tasks], во время перетаскивания —
     * как тянут), проекты встают перед первой задачей с ключом больше своего; при равном (без порядка) — проект
     * раньше, как было до общего порядка.
     */
    fun merge(projects: List<Item>, tasks: List<ItemWithChecklist>): List<Any> {
        val sorted = projects.sortedBy { key(it) }
        val rows = ArrayList<Any>(sorted.size + tasks.size)
        var next = 0
        for (task in tasks) {
            while (next < sorted.size && key(sorted[next]) <= key(task.item)) rows.add(sorted[next++])
            rows.add(task)
        }
        while (next < sorted.size) rows.add(sorted[next++])
        return rows
    }

    /**
     * Строки раздела месяца или года: по дню старта, внутри дня — как в дне ([key]; при равном — проект раньше,
     * задачи в своём порядке)
     */
    fun mergeByDay(projects: List<Item>, tasks: List<ItemWithChecklist>, zone: ZoneId = ZoneId.systemDefault()): List<Any> {
        val all: List<Pair<Item, Any>> = projects.map { it to (it as Any) } + tasks.map { it.item to (it as Any) }
        return all.withIndex().sortedWith(
            compareBy<IndexedValue<Pair<Item, Any>>> { it.value.first.startDate?.let { d -> dayOf(d, zone) } ?: Long.MAX_VALUE }
                .thenBy { key(it.value.first) }
                .thenBy { if (it.value.second is Item) 0 else 1 }
                .thenBy { it.index }
        ).map { it.value.second }
    }

    /** Порядок дня заново по строкам [rows] (задачи и проекты, сверху вниз): записи, у которых он изменился */
    fun renumber(rows: List<Item>, now: Long = System.currentTimeMillis()): List<Item> {
        val base = top(now)
        return rows.mapIndexedNotNull { index, item ->
            val order = base + index
            if (item.dayOrder == order) null else item.copy(dayOrder = order)
        }
    }

    /** День «Планов» по дням: начало суток и его проекты */
    data class DayRows(val dayStart: Long, val projects: List<Item>)

    /**
     * После перетаскивания задач в «Планах»: дни, где что-то изменилось ([changed] — сохраняемые задачи), нумеруются
     * заново по тому, как они видны: задачи в порядке [tasks] (итог перетаскивания), проекты — между ними, как стояли.
     * @return задачи и проекты с новым порядком дня (только изменившиеся)
     */
    fun afterDrag(
        tasks: List<ItemWithChecklist>,
        changed: List<Item>,
        days: List<DayRows>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Long = System.currentTimeMillis()
    ): List<Item> {
        val changedDays = changed.mapNotNullTo(HashSet()) { it.startDate?.let { d -> dayOf(d, zone) } }
        return days.filter { dayOf(it.dayStart, zone) in changedDays }.flatMap { day ->
            val d = dayOf(day.dayStart, zone)
            val dayTasks = tasks.filter { it.item.startDate?.let { s -> dayOf(s, zone) == d } == true }
            renumber(merge(day.projects, dayTasks).map { (it as? ItemWithChecklist)?.item ?: it as Item }, now)
        }
    }

    private fun dayOf(millis: Long, zone: ZoneId): Long = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
}
