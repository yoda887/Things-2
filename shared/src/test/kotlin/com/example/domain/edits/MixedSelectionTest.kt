package com.example.domain.edits

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проекты и задачи в одном выборе: деление, перенос задач вместе с проектами, что подтверждать при завершении. */
class MixedSelectionTest {

    private val project = Item(id = "p", type = Item.TYPE_PROJECT, title = "Проект")
    private val template = Item(id = "pt", type = Item.TYPE_PROJECT_TEMPLATE, title = "Шаблон")
    private val task = Item(id = "t", title = "Задача")
    private fun w(item: Item) = ItemWithChecklist(item)

    @Test
    fun split_projectsAndTasks() {
        val parts = MixedSelection.split(listOf(w(project), w(task), w(template)))
        assertEquals(listOf("p"), parts.projects.map { it.id })
        // Шаблон повторяющегося проекта — не проект выбора: у него свои правила
        assertEquals(listOf("t", "pt"), parts.tasks.map { it.item.id })
        assertTrue(parts.hasProjects)
        assertFalse(MixedSelection.split(listOf(w(task))).hasProjects)
    }

    @Test
    fun tasksWithProjects_goToAreaWithoutProject() {
        val target = MixedSelection.taskTargetWithProjects("a")
        assertNull(target.projectId)
        assertEquals("a", target.areaId)
        val moved = TaskMoves.moved(task.copy(projectId = "q", headingId = "h"), target)
        assertNull(moved.projectId)
        assertEquals("a", moved.areaId)
        assertNull(moved.headingId)
    }

    @Test
    fun confirm_onlyProjectsWithOpenTasks() {
        val other = Item(id = "p2", type = Item.TYPE_PROJECT, title = "Пустой")
        val all = listOf(
            w(Item(id = "a", title = "открытая", projectId = "p")),
            w(Item(id = "b", title = "сделанная", projectId = "p2", status = Item.STATUS_COMPLETED))
        )
        assertEquals(listOf("p"), MixedSelection.projectsWithOpenTasks(listOf(project, other), all).map { it.id })
    }
}
