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
    /** Экран области: промежуток в секции «Планы» (предстоящие) */
    val inAreaUpcoming: Boolean = false,
    /** Экран области: промежуток в секции «Когда-нибудь» */
    val inAreaSomeday: Boolean = false,
    /** Группа «по месту» («В любое время»), в которую попал промежуток: её заголовок; null — выше групп */
    val placeHeader: SearchSectionHeaderItem? = null,
)

/** Новый заголовок: его место среди заголовков и задачи, которые уйдут под него */
data class FabHeadingPlacement(val headingIndex: Int, val movedTaskIds: List<String>)

/**
 * Правила добавления перетаскиванием кнопки «+» — обычные функции над данными,
 * закреплены тестами FabInsertionTest.
 */
object FabInsertion {

    /** Строки, между которыми может раскрыться промежуток: задачи, заголовки проекта, «Вечер» */
    /** Подзаголовок, разделяющий секции списка: всё, что можно пометить промежутком, кроме задач */
    private fun isSectionBoundary(element: Any): Boolean =
        element !is ItemWithChecklist && (isAnchor(element) || element is SearchSectionHeaderItem)

    fun isAnchor(element: Any): Boolean =
        element is ItemWithChecklist || element is ProjectHeadingItem || element == TaskListKeys.EVENING_HEADER ||
            element is UpcomingHeaderItem || element is UpcomingMonthHeaderItem ||
            element == TaskListKeys.AREA_UPCOMING_HEADING || element == TaskListKeys.AREA_SOMEDAY_HEADING

    /**
     * Место промежутка по положению центра кнопки [y] в координатах списка.
     *
     * Промежуток занимает место между строками: его верх — на границе между ними, центр — на
     * [gapHeight]/2 ниже. Выбирается граница, при которой центр промежутка ближе всего к [y], —
     * тогда промежуток и кнопка сидят на одной линии, а не промежуток всегда ниже кнопки.
     *
     * [rows] — видимые строки-якоря, геометрия без промежутка (его высота уже вычтена у строк ниже),
     * поэтому место не дёргается от того, что промежуток сам сдвигает строки. Ниже всех — после
     * последней видимой строки-якоря.
     */
    fun slotAt(y: Float, rows: List<FabRow>, asHeading: Boolean, gapHeight: Float = 0f): FabSlot? {
        if (rows.isEmpty()) return null
        val target = y - gapHeight / 2f
        var bestIndex = rows.first().index
        var bestDistance = kotlin.math.abs(rows.first().top - target)
        for (row in rows) {
            val distance = kotlin.math.abs(row.top - target)
            if (distance < bestDistance) { bestDistance = distance; bestIndex = row.index }
        }
        val last = rows.last()
        if (kotlin.math.abs(last.top + last.size - target) < bestDistance) bestIndex = last.index + 1
        return FabSlot(bestIndex, asHeading)
    }

    /**
     * «Предстоящие»: промежуток стоит только внутри раздела дня или месяца — не выше первого заголовка
     * и не между заголовком и его событиями календаря. Такой промежуток переносится сразу за события
     * своего раздела. На других экранах заголовков дней нет, и [slot] не меняется.
     */
    fun normalizeSlot(flattened: List<Any>, slot: FabSlot): FabSlot {
        var index = slot.index.coerceIn(0, flattened.size)
        val firstHeader = flattened.indexOfFirst { it is UpcomingHeaderItem || it is UpcomingMonthHeaderItem }
        if (firstHeader != -1 && index <= firstHeader) index = firstHeader + 1
        while (index < flattened.size && flattened[index] is UpcomingEventItem) index++
        return if (index == slot.index) slot else slot.copy(index = index)
    }

    /**
     * Куда встанет новая задача, если промежуток — перед элементом [slotIndex] плоского списка [flattened].
     * [tasks] — общий список задач экрана в текущем порядке.
     */
    fun taskPlacement(flattened: List<Any>, slotIndex: Int, tasks: List<ItemWithChecklist>): FabTaskPlacement {
        val above = flattened.subList(0, slotIndex.coerceIn(0, flattened.size))
        val headingId = (above.lastOrNull { it is ProjectHeadingItem } as? ProjectHeadingItem)?.heading?.id
        val isTonight = above.any { it == TaskListKeys.EVENING_HEADER }
        // Соседи — только из той же секции: подзаголовок (заголовок проекта, «Вечер», день, месяц, раздел
        // области) — граница, за которую поиск не заходит. Задачи всех секций лежат в [tasks] вперемешку,
        // и ориентир «первая задача ниже» за подзаголовком — это задача чужой секции: новая задача вставала
        // перед ней, то есть в произвольное место своей секции, а не прямо над подзаголовком.
        val below = flattened.drop(slotIndex.coerceIn(0, flattened.size))
        val nextTask = below.takeWhile { !isSectionBoundary(it) }.firstOrNull { it is ItemWithChecklist } as? ItemWithChecklist
        val prevTask = above.takeLastWhile { !isSectionBoundary(it) }.lastOrNull { it is ItemWithChecklist } as? ItemWithChecklist
        // Секция пуста — ориентир из соседней: последняя задача выше или первая ниже
        val prevAny = above.lastOrNull { it is ItemWithChecklist } as? ItemWithChecklist
        val nextAny = below.firstOrNull { it is ItemWithChecklist } as? ItemWithChecklist
        fun indexOf(task: ItemWithChecklist) = tasks.indexOfFirst { it.item.id == task.item.id }.takeIf { it != -1 }
        val taskIndex = when {
            nextTask != null -> indexOf(nextTask)
            prevTask != null -> indexOf(prevTask)?.plus(1)
            prevAny != null -> indexOf(prevAny)?.plus(1)
            nextAny != null -> indexOf(nextAny)
            else -> null
        } ?: tasks.size
        // Секции экрана области задаются свойствами задачи, поэтому место промежутка — это секция,
        // под чьим заголовком он стоит
        val sectionHeader = above.lastOrNull {
            it == TaskListKeys.AREA_UPCOMING_HEADING || it == TaskListKeys.AREA_SOMEDAY_HEADING
        }
        return FabTaskPlacement(
            taskIndex, headingId, isTonight, upcomingDate(flattened, above),
            inAreaUpcoming = sectionHeader == TaskListKeys.AREA_UPCOMING_HEADING,
            inAreaSomeday = sectionHeader == TaskListKeys.AREA_SOMEDAY_HEADING,
            placeHeader = above.lastOrNull { it is SearchSectionHeaderItem } as? SearchSectionHeaderItem,
        )
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

    /**
     * Номер порядка новой задачи, вставляемой в позицию [index] списка [orders] (sortOrder задач экрана
     * в порядке показа).
     *
     * Если между соседями есть свободное число — это число, и остальные задачи не трогаются: перенумерация
     * всех задач экрана переписывала бы их в базе и на совпадающих номерах меняла их порядок местами.
     * Нет места (номера соседей совпадают или идут подряд) либо порядок показа не по возрастанию
     * (например, задачи сгруппированы по заголовкам проекта) — null: вызывающий перенумеровывает список.
     */
    fun freeSortOrder(orders: List<Int>, index: Int): Int? {
        if (orders.zipWithNext().any { (a, b) -> a > b }) return null
        val i = index.coerceIn(0, orders.size)
        val prev = orders.getOrNull(i - 1)
        val next = orders.getOrNull(i)
        return when {
            prev == null && next == null -> 0
            prev == null -> next!! - 1
            next == null -> prev + 1
            prev + 1 < next -> prev + 1
            else -> null
        }
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
