package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Какие задачи в «В любое время»: все доступные сейчас, в том числе на сегодня — как Anytime в Things. */
class AnytimeListTest {

    private val bounds = DayBounds.now()
    private val day = 24 * 3600 * 1000L
    private fun includes(item: Item, someday: Set<String> = emptySet()) = AnytimeList.includes(item, bounds, someday)
    private fun task(start: Int = Item.START_ANYTIME, startDate: Long? = null, projectId: String? = null) =
        Item(id = "t", title = "t", start = start, startDate = startDate, projectId = projectId)

    @Test
    fun todayAndTonightTasks_areIncluded() {
        assertTrue(includes(task(start = Item.START_TODAY)))
        assertTrue(includes(task(startDate = bounds.todayStart + 3_600_000L)))
        assertTrue(includes(task(startDate = bounds.todayStart + 3_600_000L).copy(isTonight = true)))
        // Дата старта в прошлом — задача тоже «на сегодня»
        assertTrue(includes(task(startDate = bounds.todayStart - 3 * day)))
    }

    @Test
    fun plainAnytimeAndProjectTasks_areIncluded() {
        assertTrue(includes(task()))
        assertTrue(includes(task(start = Item.START_INBOX, projectId = "p")))
        // Горящий дедлайн задачу отсюда не убирает
        assertTrue(includes(task().copy(dueDate = bounds.todayStart)))
    }

    @Test
    fun futureSomedayInboxClosedAndTrashed_areExcluded() {
        assertFalse(includes(task(startDate = bounds.endOfToday + day)))
        assertFalse(includes(task(start = Item.START_SOMEDAY)))
        assertFalse(includes(task(start = Item.START_INBOX)))
        assertFalse(includes(task(projectId = "sp"), someday = setOf("sp")))
        assertFalse(includes(task().copy(status = Item.STATUS_COMPLETED)))
        assertFalse(includes(task().copy(status = Item.STATUS_CANCELLED)))
        assertFalse(includes(task().copy(trashed = true)))
        assertFalse(includes(task().copy(type = Item.TYPE_PROJECT)))
    }
}
