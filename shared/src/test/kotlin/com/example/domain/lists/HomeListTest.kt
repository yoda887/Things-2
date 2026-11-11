package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проекты главного экрана: только те, с которыми можно работать сейчас. */
class HomeListTest {

    private val bounds = DayBounds.now()
    private val day = 24 * 3600 * 1000L
    private fun project(start: Int = Item.START_ANYTIME, startDate: Long? = null) =
        Item(id = "p", title = "p", type = Item.TYPE_PROJECT, start = start, startDate = startDate)

    @Test
    fun anytimeTodayAndStartedProjects_areShown() {
        assertTrue(HomeList.showsProject(project(), bounds))
        assertTrue(HomeList.showsProject(project(start = Item.START_TODAY), bounds))
        assertTrue(HomeList.showsProject(project(startDate = bounds.todayStart + 3_600_000L), bounds))
        assertTrue(HomeList.showsProject(project(startDate = bounds.todayStart - 5 * day), bounds))
    }

    @Test
    fun somedayFutureAndClosedProjects_areHidden() {
        assertFalse(HomeList.showsProject(project(start = Item.START_SOMEDAY), bounds))
        assertFalse(HomeList.showsProject(project(startDate = bounds.endOfToday + day), bounds))
        assertFalse(HomeList.showsProject(project().copy(status = Item.STATUS_COMPLETED), bounds))
        assertFalse(HomeList.showsProject(project().copy(trashed = true), bounds))
    }
}
