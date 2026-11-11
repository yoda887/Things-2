package com.example.ui.screens.home.components

import androidx.compose.foundation.lazy.LazyListState
import com.example.data.model.Item
import com.example.ui.components.dragdrop.GenericDragDropState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Экран «Пізніші проєкти»: выбор, верхний бар и перетаскивание стопкой (LaterProjects.kt). */
class LaterProjectsControllerTest {

    private fun someday(id: String, order: Int) =
        Item(id = id, title = id, type = Item.TYPE_PROJECT, start = Item.START_SOMEDAY, sortOrder = order)

    private var stored = listOf(someday("p1", 1), someday("p2", 2), someday("p3", 3))
    private val controller = LaterProjectsController(enabled = true) {
        LaterProjectsSource(stored, areas = emptyList(), templates = emptyList(), rules = emptyMap())
    }
    private fun drag() = GenericDragDropState(LazyListState())
    private fun ids() = controller.now().map { it.id }

    @Test
    fun `список - отложенные проекты без области в их порядке`() {
        assertEquals(listOf("p1", "p2", "p3"), ids())
    }

    @Test
    fun `перетаскивание одного - новый порядок сразу на экране и в сохранении`() {
        val state = drag()
        controller.onDragStarted(state, "p3")
        assertTrue(controller.move("p3", "p1"))
        assertEquals(listOf("p3", "p1", "p2"), ids())
        val changed = controller.onDragEnd(state)
        assertEquals(setOf("p1", "p2", "p3"), changed.map { it.id }.toSet())
        // База вернула сохранённое — временный порядок снимается
        stored = controller.now()
        controller.onProjectsStored()
        assertNull(controller.orderOverride)
        assertEquals(listOf("p3", "p1", "p2"), ids())
    }

    @Test
    fun `перетаскивание без переноса ничего не сохраняет`() {
        val state = drag()
        controller.onDragStarted(state, "p2")
        assertEquals(emptyList<Item>(), controller.onDragEnd(state))
        assertNull(controller.orderOverride)
    }

    @Test
    fun `выбранные тянутся стопкой - режим выбора закрывается, остальные встают за ведущим`() {
        val state = drag()
        controller.selection.swipeSelect(stored[0])
        controller.selection.swipeSelect(stored[2])
        controller.onDragStarted(state, "p3")
        assertEquals(listOf<Any>("p3", "p1"), state.stackedDragKeys)
        assertFalse(controller.selection.isSelectionMode)
        val changed = controller.onDragEnd(state)
        assertEquals(listOf("p2", "p3", "p1"), ids())
        assertTrue(changed.isNotEmpty())
    }

    @Test
    fun `тянут невыбранный - стопки нет, выбор всё равно закрывается`() {
        val state = drag()
        controller.selection.swipeSelect(stored[0])
        controller.selection.swipeSelect(stored[1])
        controller.onDragStarted(state, "p3")
        assertEquals(emptyList<Any>(), state.stackedDragKeys)
        assertFalse(controller.selection.isSelectionMode)
    }

    @Test
    fun `верхний бар - выбрать все отмечает все проекты, крестик закрывает режим`() {
        val bar = controller.topBarSelection()
        assertFalse(bar.isSelectionMode)
        bar.onEnterSelectionMode()
        assertTrue(controller.topBarSelection().isSelectionMode)
        bar.onSelectAllClick()
        val all = controller.topBarSelection()
        assertTrue(all.isAllSelected)
        assertEquals(3, all.selectedCount)
        all.onDeselectAllClick()
        assertTrue("снятие выбора режим не закрывает", controller.topBarSelection().isSelectionMode)
        all.onCancelSelection()
        assertFalse(controller.topBarSelection().isSelectionMode)
    }
}
