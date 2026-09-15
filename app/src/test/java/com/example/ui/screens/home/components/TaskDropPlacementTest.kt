package com.example.ui.screens.home.components

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Закрепляет правила, по которым задача приземляется при перетаскивании через заголовки.
 *
 * Ключевая особенность, которую проверяют эти тесты: секции экранов — это фильтры и группировки
 * поверх общего списка, упорядоченного по sortOrder глобально, поэтому элементы одной секции
 * лежат в списке ВПЕРЕМЕШКУ с чужими. Списки здесь специально собраны так, чтобы наивный поиск
 * места вставки «по первому элементу с подходящей датой или признаком» давал неверный ответ:
 * именно так выглядели два реальных бага, когда задача приземлялась на несколько позиций выше
 * нужного места.
 */
class TaskDropPlacementTest {

    private val bounds = DayBounds.now()

    /** Дни-заголовки экрана «Предстоящие»: 14 суток начиная с завтра */
    private val upcomingDayStarts: List<Long> = (1..14).map { dayStart(it) }

    private val horizonEnd: Long = upcomingDayStarts.last() + MS_PER_DAY

    // ─── Заголовок «Вечер» (экран «Сегодня») ──────────────────────────────────

    @Test
    fun `вверх через заголовок вечера — задача встаёт последней в дневной секции`() {
        // Дневные и вечерние задачи вперемешку: первая вечерняя лежит в середине дневных
        val list = listOf(day("d1"), evening("t1"), day("d2"), day("d3"), evening("t2"))

        val result = planTaskDrop(
            list = list,
            draggedId = "t2",
            targetId = TaskListKeys.EVENING_HEADER,
            screen = ActiveScreen.TODAY,
            movingDown = false,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )

        // Как это видно на экране: дневная секция — фильтр !isTonight
        assertEquals(listOf("d1", "d2", "d3", "t2"), ids(result!!.filter { !it.item.isTonight }))
        assertEquals(listOf("t1"), ids(result.filter { it.item.isTonight }))
    }

    @Test
    fun `вниз через заголовок вечера — задача встаёт первой в вечерней секции`() {
        val list = listOf(day("d1"), evening("t1"), day("d2"), evening("t2"))

        val result = planTaskDrop(
            list = list,
            draggedId = "d2",
            targetId = TaskListKeys.EVENING_HEADER,
            screen = ActiveScreen.TODAY,
            movingDown = true,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )

        assertEquals(listOf("d2", "t1", "t2"), ids(result!!.filter { it.item.isTonight }))
        assertEquals(listOf("d1"), ids(result.filter { !it.item.isTonight }))
    }

    @Test
    fun `главный заголовок — вечерняя задача поднимается в начало дневной секции`() {
        val list = listOf(day("d1"), day("d2"), evening("t1"))

        val result = planTaskDrop(
            list = list,
            draggedId = "t1",
            targetId = TaskListKeys.MAIN_HEADER,
            screen = ActiveScreen.TODAY,
            movingDown = false,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )

        assertEquals(listOf("t1", "d1", "d2"), ids(result!!.filter { !it.item.isTonight }))
    }

    @Test
    fun `главный заголовок — дневную задачу переносить некуда`() {
        val list = listOf(day("d1"), day("d2"))

        assertNull(
            planTaskDrop(
                list = list,
                draggedId = "d2",
                targetId = TaskListKeys.MAIN_HEADER,
                screen = ActiveScreen.TODAY,
                movingDown = false,
                upcomingDayStarts = upcomingDayStarts,
                bounds = bounds,
            )
        )
    }

    // ─── Заголовок дня (экран «Предстоящие») ──────────────────────────────────

    @Test
    fun `вверх через заголовок дня — задача встаёт последней в предыдущем дне`() {
        // Список упорядочен по sortOrder, а не по дате: задача дальнего дня лежит первой,
        // поэтому «первый элемент с датой не раньше целевой» указывает мимо нужного дня
        val list = listOf(
            dated("far", dayStart(3) + HOUR * 10),
            dated("b", dayStart(1) + HOUR * 9),
            dated("c", dayStart(1) + HOUR * 11),
            dated("dragged", dayStart(2) + HOUR * 9),
        )

        val result = planTaskDrop(
            list = list,
            draggedId = "dragged",
            targetId = TaskListKeys.DAY_HEADER_PREFIX + dayStart(2),
            screen = ActiveScreen.UPCOMING,
            movingDown = false,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )!!

        assertEquals(listOf("far", "b", "c", "dragged"), ids(result))
        // Задача переехала в предыдущий день и скопировала точное время соседа по дню
        assertEquals(dayStart(1) + HOUR * 11, result.last().item.startDate)
    }

    @Test
    fun `вниз через заголовок дня — задача встаёт первой в целевом дне`() {
        val list = listOf(
            dated("far", dayStart(5) + HOUR * 10),
            dated("b", dayStart(2) + HOUR * 9),
            dated("c", dayStart(2) + HOUR * 11),
            dated("dragged", dayStart(1) + HOUR * 9),
        )

        val result = planTaskDrop(
            list = list,
            draggedId = "dragged",
            targetId = TaskListKeys.DAY_HEADER_PREFIX + dayStart(2),
            screen = ActiveScreen.UPCOMING,
            movingDown = true,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )!!

        assertEquals(listOf("far", "dragged", "b", "c"), ids(result))
        assertEquals(dayStart(2) + HOUR * 9, result[1].item.startDate)
    }

    @Test
    fun `заголовок своего же дня — перенос не нужен`() {
        val list = listOf(dated("a", dayStart(2) + HOUR * 9))

        assertNull(
            planTaskDrop(
                list = list,
                draggedId = "a",
                targetId = TaskListKeys.DAY_HEADER_PREFIX + dayStart(2),
                screen = ActiveScreen.UPCOMING,
                movingDown = true,
                upcomingDayStarts = upcomingDayStarts,
                bounds = bounds,
            )
        )
    }

    @Test
    fun `вверх в пустой день — задача получает полдень этого дня`() {
        val list = listOf(
            dated("far", dayStart(7) + HOUR * 10),
            dated("dragged", dayStart(3) + HOUR * 9),
        )

        val result = planTaskDrop(
            list = list,
            draggedId = "dragged",
            targetId = TaskListKeys.DAY_HEADER_PREFIX + dayStart(3),
            screen = ActiveScreen.UPCOMING,
            movingDown = false,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )!!

        assertEquals(dayStart(2) + HOUR * 12, result.first { it.item.id == "dragged" }.item.startDate)
    }

    // ─── Заголовок месяца (экран «Предстоящие») ───────────────────────────────

    @Test
    fun `вверх из первого месячного раздела — задача уходит в конец последнего дня горизонта`() {
        // У первого месячного раздела (остаток текущего месяца) заголовок несёт конец горизонта,
        // поэтому вверх задача попадает не в месяц, а в ДЕНЬ, где порядок задаёт сам список
        val list = listOf(
            dated("month", horizonEnd + MS_PER_DAY * 5),
            dated("lastDay", upcomingDayStarts.last() + HOUR * 10),
            dated("dragged", horizonEnd + MS_PER_DAY * 2),
        )

        val result = planTaskDrop(
            list = list,
            draggedId = "dragged",
            targetId = TaskListKeys.MONTH_HEADER_PREFIX + horizonEnd,
            screen = ActiveScreen.UPCOMING,
            movingDown = false,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )!!

        assertEquals(listOf("month", "lastDay", "dragged"), ids(result))
        val movedStart = result.last().item.startDate!!
        assertEquals(upcomingDayStarts.last() + HOUR * 12, movedStart)
    }

    @Test
    fun `вниз через заголовок месяца — задача встаёт в начало этого месяца`() {
        val monthStart = firstDayOfMonthAfter(horizonEnd)
        val list = listOf(
            dated("m1", monthStart + MS_PER_DAY * 3),
            dated("m2", monthStart + MS_PER_DAY * 6),
            dated("dragged", upcomingDayStarts.last() + HOUR * 9),
        )

        val result = planTaskDrop(
            list = list,
            draggedId = "dragged",
            targetId = TaskListKeys.MONTH_HEADER_PREFIX + monthStart,
            screen = ActiveScreen.UPCOMING,
            movingDown = true,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )!!

        assertEquals(listOf("dragged", "m1", "m2"), ids(result))
        assertEquals(noonOf(monthStart), result.first().item.startDate)
    }

    @Test
    fun `заголовок своего же месяца при движении вниз — перенос не нужен`() {
        val monthStart = firstDayOfMonthAfter(horizonEnd)
        val list = listOf(dated("a", monthStart + MS_PER_DAY * 3))

        assertNull(
            planTaskDrop(
                list = list,
                draggedId = "a",
                targetId = TaskListKeys.MONTH_HEADER_PREFIX + monthStart,
                screen = ActiveScreen.UPCOMING,
                movingDown = true,
                upcomingDayStarts = upcomingDayStarts,
                bounds = bounds,
            )
        )
    }

    // ─── Обычный обмен задач ──────────────────────────────────────────────────

    @Test
    fun `на экране сферы обмен между разными секциями не принимается`() {
        // Секции экрана сферы задаются свойствами самой задачи, и обмен их не меняет
        val list = listOf(current("cur"), dated("soon", dayStart(10) + HOUR * 12))

        assertNull(
            planTaskDrop(
                list = list,
                draggedId = "soon",
                targetId = "cur",
                screen = ActiveScreen.AREA_DETAIL,
                movingDown = false,
                upcomingDayStarts = upcomingDayStarts,
                bounds = bounds,
            )
        )
    }

    @Test
    fun `на экране сферы обмен внутри одной секции работает`() {
        val list = listOf(current("a"), current("b"), current("c"))

        val result = planTaskDrop(
            list = list,
            draggedId = "c",
            targetId = "a",
            screen = ActiveScreen.AREA_DETAIL,
            movingDown = false,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )

        assertEquals(listOf("c", "a", "b"), ids(result!!))
    }

    @Test
    fun `обмен на экране «Сегодня» переносит признак вечера`() {
        val list = listOf(day("d1"), evening("t1"))

        val result = planTaskDrop(
            list = list,
            draggedId = "d1",
            targetId = "t1",
            screen = ActiveScreen.TODAY,
            movingDown = true,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )!!

        assertEquals(true, result.first { it.item.id == "d1" }.item.isTonight)
    }

    @Test
    fun `обмен на экране «Предстоящие» переносит дату соседа`() {
        val list = listOf(dated("a", dayStart(1) + HOUR * 9), dated("b", dayStart(4) + HOUR * 15))

        val result = planTaskDrop(
            list = list,
            draggedId = "a",
            targetId = "b",
            screen = ActiveScreen.UPCOMING,
            movingDown = true,
            upcomingDayStarts = upcomingDayStarts,
            bounds = bounds,
        )!!

        assertEquals(dayStart(4) + HOUR * 15, result.first { it.item.id == "a" }.item.startDate)
    }

    // ─── Фикстуры ─────────────────────────────────────────────────────────────

    private fun day(id: String) = wrap(Item(id = id, title = id))

    private fun evening(id: String) = wrap(Item(id = id, title = id, isTonight = true))

    /** Задача без даты и без «когда-нибудь» — текущая секция экрана сферы */
    private fun current(id: String) = wrap(Item(id = id, title = id, start = 2))

    private fun dated(id: String, startDate: Long) = wrap(Item(id = id, title = id, startDate = startDate))

    private fun wrap(item: Item) = ItemWithChecklist(item = item, checklist = emptyList())

    private fun ids(list: List<ItemWithChecklist>) = list.map { it.item.id }

    private fun dayStart(daysFromToday: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun noonOf(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Первое число месяца, следующего за месяцем [millis] */
    private fun firstDayOfMonthAfter(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.MONTH, 1)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val HOUR = 3600 * 1000L
    }
}
