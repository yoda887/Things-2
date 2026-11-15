package com.example.domain.tag

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.lists.AreaList
import com.example.domain.lists.TagList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Задачи и проекты наследуют теги проекта и области. */
class TagInheritanceTest {

    private val home = Area(id = "a", title = "Дом", cachedTags = "Home")
    private val project = Item(id = "p", type = Item.TYPE_PROJECT, title = "Ремонт", areaId = "a", cachedTags = "Work")
    private val inProject = Item(id = "t1", title = "Краска", projectId = "p", start = Item.START_ANYTIME, cachedTags = "Errand")
    private val inArea = Item(id = "t2", title = "Полить", areaId = "a", start = Item.START_ANYTIME)
    private val loose = Item(id = "t3", title = "Другое", start = Item.START_ANYTIME)
    private val index = TagInheritance.Index(listOf(project), listOf(home))

    @Test
    fun taskInProject_getsProjectAndAreaTags() {
        assertEquals(listOf("Errand", "Work", "Home"), index.tagsOf(inProject))
    }

    @Test
    fun taskInArea_getsAreaTags_caseInsensitive() {
        assertTrue(index.has(inArea, "home"))
        assertFalse(index.has(inArea, "Work"))
        assertFalse(index.has(loose, "Home"))
    }

    @Test
    fun projectInArea_getsAreaTags() {
        assertEquals(listOf("Work", "Home"), index.tagsOf(project))
    }

    @Test
    fun noRepeats() {
        val both = inArea.copy(cachedTags = "HOME")
        assertEquals(listOf("HOME"), index.tagsOf(both))
    }

    @Test
    fun tagScreen_showsInheritedTasksAndProjects() {
        assertTrue(TagList.isActiveTask(inArea, "Home", index))
        assertFalse(TagList.isActiveTask(inArea, "Home"))
        val content = TagList.content("Home", listOf(ItemWithChecklist(inArea)), emptyList(), listOf(project), listOf(home))
        assertEquals(listOf("p"), content.projects.map { it.id })
    }

    @Test
    fun areaFilterByAreaTag_keepsEverything() {
        val tasks = listOf(ItemWithChecklist(inArea), ItemWithChecklist(inArea.copy(id = "t4", cachedTags = "Other")))
        val content = AreaList.content("a", tasks, listOf(project), tagFilter = TagFilter(tags = listOf("Home")), inheritance = index)
        assertEquals(2, content.current.size)
        assertEquals(listOf("p"), content.projects.map { it.id })
        val filtered = AreaList.content("a", tasks, listOf(project), tagFilter = TagFilter(tags = listOf("Other")), inheritance = index)
        assertEquals(listOf("t4"), filtered.current.map { it.item.id })
        assertTrue(filtered.projects.isEmpty())
    }
}
