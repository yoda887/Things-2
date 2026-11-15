package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.components.ProjectHeadingItem
import com.example.ui.screens.home.components.ProjectScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Как в Things: в проекте, отфильтрованном по тегам, видны только подзаголовки, под которыми остались задачи. */
class ProjectTagFilterTest {

    private val h1 = Item(id = "h1", title = "С задачей", type = Item.TYPE_HEADING, projectId = "p")
    private val h2 = Item(id = "h2", title = "Пустой", type = Item.TYPE_HEADING, projectId = "p")
    private val task = ItemWithChecklist(Item(id = "t", title = "t", projectId = "p", headingId = "h1", start = Item.START_ANYTIME))

    private fun headingIds(rows: List<Any>) = rows.filterIsInstance<ProjectHeadingItem>().map { it.heading.id }

    @Test
    fun filtered_headingsWithoutTasksHidden() {
        assertEquals(listOf("h1"), headingIds(ProjectScreen.rows(listOf(task), listOf(h1, h2), null, filtered = true)))
        // Без фильтра пустой подзаголовок на месте
        assertEquals(listOf("h1", "h2"), headingIds(ProjectScreen.rows(listOf(task), listOf(h1, h2), null)))
    }

    @Test
    fun filtered_nothingMatches_projectIsEmpty() {
        assertTrue(ProjectScreen.isEmpty(emptyList(), listOf(h1, h2), filtered = true))
        assertFalse(ProjectScreen.isEmpty(emptyList(), listOf(h1, h2)))
    }
}
