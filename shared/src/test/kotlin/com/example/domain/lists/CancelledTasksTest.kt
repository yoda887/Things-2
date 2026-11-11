package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Отменённая задача, как и выполненная, — только в «Журнале»: ни в одном открытом списке её нет. */
class CancelledTasksTest {

    private val bounds = DayBounds.now()
    private val day = 24 * 3600 * 1000L

    private fun cases() = listOf(
        "today" to Item(id = "t", title = "t", start = Item.START_TODAY),
        "upcoming" to Item(id = "t", title = "t", start = Item.START_ANYTIME, startDate = bounds.endOfToday + day),
        "anytime" to Item(id = "t", title = "t", start = Item.START_ANYTIME),
        "someday" to Item(id = "t", title = "t", start = Item.START_SOMEDAY),
        "project" to Item(id = "t", title = "t", start = Item.START_ANYTIME, projectId = "p"),
        "area" to Item(id = "t", title = "t", start = Item.START_ANYTIME, areaId = "a")
    )

    private fun inItsList(name: String, item: Item): Boolean = when (name) {
        "today" -> TodayList.includes(item, bounds)
        "upcoming" -> UpcomingList.includes(item, bounds)
        "anytime" -> AnytimeList.includes(item, bounds, emptySet())
        "someday" -> SomedayList.includes(item, bounds, emptySet())
        "project" -> ProjectList.includes(item, "p")
        else -> AreaList.includes(item, "a")
    }

    @Test
    fun openTask_isInItsList_cancelledAndCompletedAreNot() {
        cases().forEach { (name, item) ->
            assertTrue("$name: открытая", inItsList(name, item))
            assertFalse("$name: отменённая", inItsList(name, item.copy(status = Item.STATUS_CANCELLED)))
            assertFalse("$name: выполненная", inItsList(name, item.copy(status = Item.STATUS_COMPLETED)))
        }
    }

    @Test
    fun cancelledTask_isInLogbook() {
        assertTrue(LogbookList.includes(Item(id = "t", title = "t", status = Item.STATUS_CANCELLED)))
    }
}
