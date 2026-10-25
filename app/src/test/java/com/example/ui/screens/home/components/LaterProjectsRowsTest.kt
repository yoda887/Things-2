package com.example.ui.screens.home.components

import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Test

/** «Пізніші проєкти»: разделы «Плани» (с повторяющимися) и «Колись», как Later Projects в Things. */
class LaterProjectsRowsTest {

    private fun row(r: Any): String = when (r) {
        is SearchSectionHeaderItem -> "#" + r.kind.name
        is Item -> r.id
        else -> "?"
    }

    @Test
    fun sectionsInOrder_emptyOnesSkipped() {
        val scheduled = Item(id = "s1", type = Item.TYPE_PROJECT, startDate = 1_000L)
        val someday = Item(id = "d1", type = Item.TYPE_PROJECT, start = Item.START_SOMEDAY)
        // Шаблон повторяющегося проекта — в «Планах», после запланированных
        val repeating = Item(id = "r1", type = Item.TYPE_PROJECT_TEMPLATE)
        val rows = AllProjectsScreen.laterRows(listOf(scheduled, repeating, someday), "U", "S")
        assertEquals(listOf("#UPCOMING", "s1", "r1", "#SOMEDAY", "d1"), rows.map(::row))
    }

    @Test
    fun empty_noHeaders() {
        assertEquals(emptyList<Any>(), AllProjectsScreen.laterRows(emptyList(), "U", "S"))
    }
}
