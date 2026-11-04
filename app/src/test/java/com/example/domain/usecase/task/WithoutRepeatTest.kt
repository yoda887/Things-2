package com.example.domain.usecase.task

import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Дубликат повторяющейся задачи или проекта — обычная задача или проект, без повтора. */
class WithoutRepeatTest {

    @Test
    fun taskTemplate_becomesTask() {
        val plain = Item(id = "t", type = Item.TYPE_TEMPLATE, title = "Полить цветы").withoutRepeat()
        assertEquals(Item.TYPE_TASK, plain.type)
        assertNull(plain.repeatTemplateId)
        assertEquals("Полить цветы", plain.title)
    }

    @Test
    fun projectTemplateAndItsTask_becomePlain() {
        assertEquals(Item.TYPE_PROJECT, Item(id = "p", type = Item.TYPE_PROJECT_TEMPLATE).withoutRepeat().type)
        val part = Item(id = "x", type = Item.TYPE_TEMPLATE, repeatTemplateId = "p", projectId = "p").withoutRepeat()
        assertEquals(Item.TYPE_TASK, part.type)
        assertNull(part.repeatTemplateId)
    }

    @Test
    fun repeatCopies_loseTheirTemplate() {
        val task = Item(id = "c", type = Item.TYPE_TASK, repeatTemplateId = "t", startDate = 500L).withoutRepeat()
        assertEquals(Item.TYPE_TASK, task.type)
        assertNull(task.repeatTemplateId)
        assertEquals(500L, task.startDate)
        val project = Item(id = "pc", type = Item.TYPE_PROJECT, repeatTemplateId = "p").withoutRepeat()
        assertEquals(Item.TYPE_PROJECT, project.type)
        assertNull(project.repeatTemplateId)
    }

    @Test
    fun plainTask_unchanged() {
        val task = Item(id = "a", title = "a")
        assertEquals(task, task.withoutRepeat())
    }
}
