package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Проверяет, что пакетная классификация задач через [DayBounds] даёт те же результаты,
 * что и одиночные геттеры [Item.isToday], [Item.isUpcoming] и т.д.
 */
class DayBoundsTest {

    private val bounds = DayBounds.now()

    /** Полдень дня, отстоящего от сегодняшнего на [daysFromToday] суток. */
    private fun noonInDays(daysFromToday: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun task(
        start: Int = 0,
        status: Int = 0,
        startDate: Long? = null,
        dueDate: Long? = null,
        projectId: String? = null,
        type: Int = 0,
        repeatTemplateId: String? = null
    ) = Item(
        type = type,
        repeatTemplateId = repeatTemplateId,
        start = start,
        status = status,
        startDate = startDate,
        dueDate = dueDate,
        projectId = projectId
    )

    @Test
    fun `задача помещённая в Today вручную попадает в Today`() {
        val item = task(start = 1)
        assertTrue(bounds.isToday(item))
        assertFalse(bounds.isUpcoming(item))
        assertFalse(bounds.isAnytime(item))
    }

    @Test
    fun `дата старта сегодня попадает в Today`() {
        val item = task(start = 2, startDate = noonInDays(0))
        assertTrue(bounds.isToday(item))
        assertFalse(bounds.isUpcoming(item))
    }

    @Test
    fun `просроченная дата старта попадает в Today`() {
        val item = task(start = 2, startDate = noonInDays(-5))
        assertTrue(bounds.isToday(item))
    }

    @Test
    fun `дата старта завтра попадает в Upcoming а не в Today`() {
        val item = task(start = 2, startDate = noonInDays(1))
        assertFalse(bounds.isToday(item))
        assertTrue(bounds.isUpcoming(item))
        assertFalse(bounds.isAnytime(item))
    }

    @Test
    fun `дедлайн обычной задачи не поднимает её в Today`() {
        // Дедлайн и старт независимы: дедлайн на показ обычной задачи в «Сегодня» не влияет
        assertTrue(bounds.isDueSoon(noonInDays(0)))
        assertFalse(bounds.isToday(task(start = 2, dueDate = noonInDays(0))))
        assertFalse(bounds.isToday(task(start = 2, dueDate = noonInDays(-2))))
        assertFalse(bounds.isToday(task(start = 2, dueDate = noonInDays(3))))
    }

    @Test
    fun `дедлайн раньше старта не поднимает задачу в Today — она приходит в день старта`() {
        val item = task(start = 2, startDate = noonInDays(2), dueDate = noonInDays(0))
        assertFalse(bounds.isToday(item))
        assertTrue(bounds.isUpcoming(item))
        assertTrue(bounds.isToday(item.copy(startDate = noonInDays(0))))
    }

    @Test
    fun `горящий дедлайн копии повтора поднимает её в Today`() {
        assertFalse(bounds.isDueSoon(noonInDays(1)))
        assertTrue(bounds.isDueSoon(noonInDays(-2)))
        assertTrue(bounds.isToday(task(start = 2, startDate = noonInDays(1), dueDate = noonInDays(0), repeatTemplateId = "t")))
        assertFalse(bounds.isToday(task(start = 2, startDate = noonInDays(1), dueDate = noonInDays(1), repeatTemplateId = "t")))
    }

    @Test
    fun `задача из Inbox не попадает в Anytime`() {
        val item = task(start = 0)
        assertTrue(item.isInbox)
        assertFalse(bounds.isAnytime(item))
        assertFalse(bounds.isToday(item))
    }

    @Test
    fun `задача из Inbox с дальним дедлайном остаётся во Входящих`() {
        // Иначе она не видна ни в одном списке до самого срока
        val item = task(start = 0, dueDate = noonInDays(30))
        assertTrue(item.isInbox)
        assertTrue(com.example.domain.lists.InboxList.includes(item))
        assertFalse(bounds.isAnytime(item))
        assertFalse(bounds.isToday(item))
    }

    @Test
    fun `задача проекта без дат попадает в Anytime`() {
        val item = task(start = 2, projectId = "p1")
        assertTrue(bounds.isAnytime(item))
        assertFalse(bounds.isToday(item))
        assertFalse(bounds.isSomeday(item))
    }

    @Test
    fun `отложенная задача попадает только в Someday`() {
        val item = task(start = 3)
        assertTrue(bounds.isSomeday(item))
        assertFalse(bounds.isAnytime(item))
        assertFalse(bounds.isToday(item))
    }

    @Test
    fun `выполненная задача не попадает ни в одну активную категорию`() {
        val item = task(start = 1, status = 3, startDate = noonInDays(0))
        assertTrue(item.isCompleted)
        assertFalse(bounds.isToday(item))
        assertFalse(bounds.isUpcoming(item))
        assertFalse(bounds.isAnytime(item))
        assertFalse(bounds.isSomeday(item))
    }

    @Test
    fun `проект не классифицируется как задача`() {
        val project = task(type = 1, start = 1)
        assertFalse(bounds.isToday(project))
        assertFalse(bounds.isAnytime(project))
    }

    @Test
    fun `sectionOf соответствует полю start когда дат нет`() {
        assertEquals(TaskSection.INBOX, bounds.sectionOf(task(start = 0)))
        assertEquals(TaskSection.TODAY, bounds.sectionOf(task(start = 1)))
        assertEquals(TaskSection.ANYTIME, bounds.sectionOf(task(start = 2)))
        assertEquals(TaskSection.SOMEDAY, bounds.sectionOf(task(start = 3)))
    }

    @Test
    fun `sectionOf учитывает даты старта и дедлайна`() {
        assertEquals(TaskSection.UPCOMING, bounds.sectionOf(task(start = 2, startDate = noonInDays(5))))
        assertEquals(TaskSection.TODAY, bounds.sectionOf(task(start = 2, startDate = noonInDays(0))))
        // Дедлайн обычной задачи раздел не меняет; у копии повтора горящий — поднимает в «Сегодня»
        assertEquals(TaskSection.SOMEDAY, bounds.sectionOf(task(start = 3, dueDate = noonInDays(0))))
        assertEquals(TaskSection.UPCOMING, bounds.sectionOf(task(start = 2, startDate = noonInDays(5), dueDate = noonInDays(0))))
        assertEquals(TaskSection.TODAY, bounds.sectionOf(task(start = 2, startDate = noonInDays(5), dueDate = noonInDays(0), repeatTemplateId = "t")))
    }

    @Test
    fun `пакетная классификация совпадает с одиночными геттерами`() {
        val samples = listOf(
            task(start = 1),
            task(start = 2, startDate = noonInDays(0)),
            task(start = 2, startDate = noonInDays(1)),
            task(start = 2, startDate = noonInDays(10)),
            task(start = 2, dueDate = noonInDays(2)),
            task(start = 2, dueDate = noonInDays(9)),
            task(start = 3),
            task(start = 0),
            task(start = 2, projectId = "p1"),
            task(start = 1, status = 3)
        )
        samples.forEachIndexed { index, item ->
            assertEquals("isToday, образец #$index", item.isToday, bounds.isToday(item))
            assertEquals("isUpcoming, образец #$index", item.isUpcoming, bounds.isUpcoming(item))
            assertEquals("isAnytime, образец #$index", item.isAnytime, bounds.isAnytime(item))
            assertEquals("isSomeday, образец #$index", item.isSomeday, bounds.isSomeday(item))
            assertEquals("section, образец #$index", item.section, bounds.sectionOf(item))
        }
    }
}
