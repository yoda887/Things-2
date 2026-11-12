package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Test

/** Область: что удаляется вместе с ней и какие теги в строке фильтра её экрана. */
class AreaDeletionAndTagsTest {

    private val project = Item(id = "p", type = Item.TYPE_PROJECT, title = "Проект", areaId = "a", cachedTags = "Work")
    private val heading = Item(id = "h", type = Item.TYPE_HEADING, title = "Этап", projectId = "p")
    private val inProject = Item(id = "t1", title = "В проекте", projectId = "p", headingId = "h", start = Item.START_ANYTIME, cachedTags = "Deep")
    private val own = Item(id = "t2", title = "Своя", areaId = "a", start = Item.START_ANYTIME, cachedTags = "Home")
    private val done = Item(id = "t3", title = "Сделана", areaId = "a", status = Item.STATUS_COMPLETED, cachedTags = "Old")
    private val template = Item(id = "pt", type = Item.TYPE_PROJECT_TEMPLATE, title = "Шаблон", areaId = "a")
    private val inTemplate = Item(id = "t4", title = "В шаблоне", projectId = "pt")
    private val otherProject = Item(id = "q", type = Item.TYPE_PROJECT, title = "Чужой", areaId = "b")
    private val elsewhere = Item(id = "t5", title = "Чужая", projectId = "q", cachedTags = "Elsewhere")
    private val loose = Item(id = "t6", title = "Без места", cachedTags = "Loose")
    private val all = listOf(project, heading, inProject, own, done, template, inTemplate, otherProject, elsewhere, loose)

    @Test
    fun contents_everythingInsideArea_andNothingElse() {
        assertEquals(
            setOf("p", "h", "t1", "t2", "t3", "pt", "t4"),
            AreaList.contents("a", all).map { it.id }.toSet()
        )
    }

    @Test
    fun filterTags_onlyTagsOfThisArea() {
        val tasks = all.filter { it.type == Item.TYPE_TASK }.map { ItemWithChecklist(it) }
        val projects = all.filter { it.type == Item.TYPE_PROJECT }
        // Задачи внутри проекта и выполненные на экране области не видны — их тегов в фильтре нет
        assertEquals(setOf("Home", "Work"), AreaList.filterTags("a", tasks, projects))
        assertEquals(emptySet<String>(), AreaList.filterTags(null, tasks, projects))
    }
}
