package com.example.domain.edits

import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Test

/** Новая задача из «+» и быстрого добавления — первой в своём списке. */
class NewTasksTopTest {
    @Test
    fun aboveAllTasks_evenNegative_projectsIgnored() {
        val items = listOf(
            Item(id = "a", sortOrder = 0), Item(id = "b", sortOrder = -3),
            Item(id = "p", type = Item.TYPE_PROJECT, sortOrder = -10)
        )
        assertEquals(-4, NewTasks.topSortOrder(items))
        assertEquals(0, NewTasks.topSortOrder(emptyList()))
    }
}
