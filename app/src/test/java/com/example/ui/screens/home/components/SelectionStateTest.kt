package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Правила множественного выбора задач и проектов (SelectionState.kt). */
class SelectionStateTest {

    private fun project(id: String) = Item(id = id, title = id, type = Item.TYPE_PROJECT)
    private fun task(id: String) = ItemWithChecklist(Item(id = id, title = id), emptyList())

    // ── Задачи ──

    @Test
    fun `задачи - свайп вне режима входит в режим с этой задачей`() {
        val s = TaskSelectionState()
        s.swipe("a")
        assertTrue(s.isActive)
        assertEquals(setOf("a"), s.selectedIds)
    }

    @Test
    fun `задачи - свайп в режиме отмечает и снимает, снятие последней закрывает режим`() {
        val s = TaskSelectionState()
        s.swipe("a")
        s.swipe("b")
        assertEquals(setOf("a", "b"), s.selectedIds)
        s.swipe("a")
        assertEquals(setOf("b"), s.selectedIds)
        assertTrue(s.isActive)
        s.swipe("b")
        assertFalse(s.isActive)
        assertEquals(emptySet<String>(), s.selectedIds)
    }

    @Test
    fun `задачи - нажатие снимает последнюю, но режим остаётся`() {
        val s = TaskSelectionState()
        s.enter("a")
        s.toggle("a")
        assertTrue(s.isActive)
        assertEquals(emptySet<String>(), s.selectedIds)
    }

    @Test
    fun `задачи - вход из меню без выбранных, выбрать все, снять все, выход`() {
        val s = TaskSelectionState()
        s.enter()
        assertTrue(s.isActive)
        assertEquals(emptySet<String>(), s.selectedIds)
        s.selectAll(listOf("a", "b", "c"))
        assertEquals(setOf("a", "b", "c"), s.selectedIds)
        s.deselectAll()
        assertTrue(s.isActive)
        assertEquals(emptySet<String>(), s.selectedIds)
        s.selectAll(listOf("a"))
        s.exit()
        assertFalse(s.isActive)
        assertEquals(emptySet<String>(), s.selectedIds)
    }

    @Test
    fun `задачи - протяжка ставит состояние, выбранные берутся из списка в его порядке`() {
        val s = TaskSelectionState()
        s.enter()
        s.set("c", true)
        s.set("a", true)
        s.set("c", false)
        s.set("b", true)
        assertEquals(listOf("a", "b"), s.selected(listOf(task("a"), task("b"), task("c"))).map { it.item.id })
    }

    // ── Проекты ──

    @Test
    fun `проекты главного экрана - режим это есть выбранные`() {
        val s = ProjectSelectionState()
        assertFalse(s.isSelectionMode)
        s.swipeSelect(project("p1"))
        assertTrue(s.isSelectionMode)
        // Нажатие снимает последний — режим закрывается сам
        s.toggle(project("p1"))
        assertFalse(s.isSelectionMode)
        // Вход «из меню» на главном экране режим не держит
        s.enter()
        assertFalse(s.isSelectionMode)
    }

    @Test
    fun `пізніші проєкти - режим держится без выбранных, свайп последнего закрывает`() {
        val s = ProjectSelectionState(keepsModeWhenEmpty = true)
        s.enter()
        assertTrue(s.isSelectionMode)
        s.toggle(project("p1"))
        s.toggle(project("p1"))
        assertTrue("нажатие режим не закрывает", s.isSelectionMode)
        s.swipeSelect(project("p2"))
        s.swipeSelect(project("p2"))
        assertFalse("свайп, снявший последний, закрывает режим", s.isSelectionMode)
    }

    @Test
    fun `проекты - выбрать все, снять все, выход, выбранные из списка`() {
        val s = ProjectSelectionState(keepsModeWhenEmpty = true)
        s.selectAll(listOf("p1", "p2"))
        assertTrue(s.isSelectionMode)
        assertEquals(listOf("p1", "p2"), s.selected(listOf(project("p1"), project("x"), project("p2"))).map { it.id })
        s.deselectAll()
        assertTrue(s.isSelectionMode)
        s.set("p2", true)
        s.exit()
        assertFalse(s.isSelectionMode)
        assertEquals(emptySet<String>(), s.selectedIds)
    }

    @Test
    fun `проекты - Когда по свайпу вправо открывается и закрывается`() {
        val s = ProjectSelectionState()
        val p = project("p1")
        s.openWhen(p, null)
        assertEquals(p, s.whenProject)
        s.closeWhen()
        assertEquals(null, s.whenProject)
    }
}
