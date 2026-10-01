package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/** Промежуток в списке на месте пальца, пока тянут кнопку «+» */
data class FabGapItem(val asHeading: Boolean) {
    val key: String get() = KEY

    companion object {
        const val KEY = "fab_gap"
    }
}

/**
 * Место промежутка: перед элементом плоского списка с индексом [index]
 * ([index] == размеру списка — в конце).
 */
data class FabSlot(val index: Int, val asHeading: Boolean)

/** Строка списка на экране без промежутка: индекс в плоском списке и её отрезок по вертикали */
data class FabRow(val index: Int, val top: Float, val size: Float)

/** Куда встанет новая задача */
data class FabTaskPlacement(
    /** Позиция в общем списке задач экрана */
    val taskIndex: Int,
    /** Заголовок проекта, под которым окажется задача (null — без заголовка) */
    val headingId: String?,
    /** Задача окажется в вечерней секции «Сегодня» */
    val isTonight: Boolean,
    /** «Предстоящие»: дата дня или месяца, под заголовком которого встала задача; на других экранах null */
    val startDate: Long? = null,
)

/** Новый заголовок: его место среди заголовков и задачи, которые уйдут под него */
data class FabHeadingPlacement(val headingIndex: Int, val movedTaskIds: List<String>)

/**
 * Правила добавления перетаскиванием кнопки «+» — обычные функции над данными,
 * закреплены тестами FabInsertionTest.
 */
object FabInsertion {

    /** Строки, между которыми может раскрыться промежуток: задачи, заголовки проекта, «Вечер» */
    fun isAnchor(element: Any): Boolean =
        element is ItemWithChecklist || element is ProjectHeadingItem || element == TaskListKeys.EVENING_HEADER ||
            element is UpcomingHeaderItem || element is UpcomingMonthHeaderItem

    /**
     * Место промежутка по положению пальца [y] в координатах списка.
     *
     * [rows] — видимые строки-якоря, геометрия без промежутка (его высота уже вычтена у строк ниже),
     * поэтому место не дёргается от того, что промежуток сам сдвигает строки.
     * Промежуток встаёт перед первой строкой, середина которой ниже пальца; ниже всех — после
     * последней видимой строки-якоря.
     */
    fun slotAt(y: Float, rows: List<FabRow>, asHeading: Boolean): FabSlot? {
        if (rows.isEmpty()) return null
        val before = rows.firstOrNull { y < it.top + it.size / 2f }
        val index = before?.index ?: (rows.last().index + 1)
        return FabSlot(index, asHeading)
    }

    /**
     * Куда встанет новая задача, если промежуток — перед элементом [slotIndex] плоского списка [flattened].
     * [tasks] — общий список задач экрана в текущем порядке.
     */
    fun taskPlacement(flattened: List<Any>, slotIndex: Int, tasks: List<ItemWithChecklist>): FabTaskPlacement {
        val above = flattened.subList(0, slotIndex.coerceIn(0, flattened.size))
        val headingId = (above.lastOrNull { it is ProjectHeadingItem } as? ProjectHeadingItem)?.heading?.id
        val isTonight = above.any { it == TaskListKeys.EVENING_HEADER }
        // Перед первой задачей ниже промежутка; задач ниже нет — после последней задачи выше
        val nextTask = flattened.drop(slotIndex).firstOrNull { it is ItemWithChecklist } as? ItemWithChecklist
        val prevTask = above.lastOrNull { it is ItemWithChecklist } as? ItemWithChecklist
        val taskIndex = when {
            nextTask != null -> tasks.indexOfFirst { it.item.id == nextTask.item.id }.takeIf { it != -1 }
            prevTask != null -> tasks.indexOfFirst { it.item.id == prevTask.item.id }.takeIf { it != -1 }?.plus(1)
            else -> null
        } ?: tasks.size
        return FabTaskPlacement(taskIndex, headingId, isTonight, upcomingDate(flattened, above))
    }

    /**
     * «Предстоящие»: дата раздела, в который попал промежуток, — день заголовка дня или начало месяца
     * заголовка месяца (в полдень, как при переносе задачи на заголовок). Выше первого заголовка —
     * его же раздел. Нет заголовков (другой экран) — null.
     */
    private fun upcomingDate(flattened: List<Any>, above: List<Any>): Long? {
        val header = above.lastOrNull { it is UpcomingHeaderItem || it is UpcomingMonthHeaderItem }
            ?: flattened.firstOrNull { it is UpcomingHeaderItem || it is UpcomingMonthHeaderItem }
            ?: return null
        val millis = when (header) {
            is UpcomingHeaderItem -> header.dateMillis
            is UpcomingMonthHeaderItem -> header.monthMillis
            else -> return null
        }
        return java.util.Calendar.getInstance().run {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            timeInMillis
        }
    }

    /**
     * Новый заголовок в промежутке перед элементом [slotIndex]: встаёт после заголовков выше,
     * и задачи той же группы ниже промежутка (до следующего заголовка) уходят под него —
     * заголовок делит группу, как в Things.
     */
    fun headingPlacement(flattened: List<Any>, slotIndex: Int): FabHeadingPlacement {
        val clamped = slotIndex.coerceIn(0, flattened.size)
        val headingIndex = flattened.subList(0, clamped).count { it is ProjectHeadingItem }
        val moved = flattened.drop(clamped)
            .takeWhile { it !is ProjectHeadingItem }
            .filterIsInstance<ItemWithChecklist>()
            .map { it.item.id }
        return FabHeadingPlacement(headingIndex, moved)
    }

    /** Плоский список с промежутком на месте [slot] */
    fun withGap(flattened: List<Any>, slot: FabSlot?): List<Any> {
        if (slot == null) return flattened
        val index = slot.index.coerceIn(0, flattened.size)
        return flattened.toMutableList().apply { add(index, FabGapItem(slot.asHeading)) }
    }

    /** Заголовки проекта с новым [heading] на месте [index]; sortOrder — по порядку */
    fun headingsWith(headings: List<Item>, heading: Item, index: Int): List<Item> =
        headings.toMutableList()
            .apply { add(index.coerceIn(0, size), heading) }
            .mapIndexed { i, h -> if (h.sortOrder == i) h else h.copy(sortOrder = i) }
}
