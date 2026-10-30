package com.example.ui.screens.home.components

import com.example.domain.lists.TagList
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Правила экрана тега: какие задачи и проекты на нём и в каком порядке. */
class TagScreenTest {

    private fun task(
        id: String, tags: String = "work", status: Int = Item.STATUS_OPEN, trashed: Boolean = false, stop: Long? = null,
        projectId: String? = null, areaId: String? = null
    ) = ItemWithChecklist(
        Item(id = id, title = id, cachedTags = tags, status = status, trashed = trashed, stopDate = stop, projectId = projectId, areaId = areaId)
    )

    private fun project(id: String, tags: String = "work", status: Int = Item.STATUS_OPEN, areaId: String? = null) =
        Item(id = id, type = Item.TYPE_PROJECT, title = id, cachedTags = tags, status = status, areaId = areaId)

    private fun ids(rows: List<Any>) = rows.map {
        when (it) {
            is ItemWithChecklist -> it.item.id
            is Item -> "project:${it.id}"
            is SearchSectionHeaderItem -> "header:${it.title}"
            is ListToggleItem -> if (it.key == AllProjectsScreen.LOGGED_KEY) "toggle:logged" else "toggle:later"
            else -> it.toString()
        }
    }

    @Test
    fun isActiveTask() {
        assertTrue(TagList.isActiveTask(task("a").item, "work"))
        assertFalse(TagList.isActiveTask(task("b", tags = "home").item, "work"))
        assertFalse(TagList.isActiveTask(task("c", status = Item.STATUS_COMPLETED).item, "work"))
        assertFalse(TagList.isActiveTask(task("d", status = Item.STATUS_CANCELLED).item, "work"))
        assertFalse(TagList.isActiveTask(task("e", trashed = true).item, "work"))
        assertFalse(TagList.isActiveTask(project("p").copy(), "work"))
    }

    @Test
    fun rows_openTasks_spacer_openProjects_thenLogbookNewestFirst() {
        val open = task("open")
        val oldDone = task("old", status = Item.STATUS_COMPLETED, stop = 100)
        val newCancelled = task("new", status = Item.STATUS_CANCELLED, stop = 200)
        val otherTagDone = task("other", tags = "home", status = Item.STATUS_COMPLETED, stop = 300)
        val trashedDone = task("trash", status = Item.STATUS_COMPLETED, trashed = true, stop = 400)
        val content = TagList.content(
            "work",
            tasks = listOf(open),
            allTasks = listOf(open, oldDone, newCancelled, otherTagDone, trashedDone),
            projects = listOf(project("p"), project("done", status = Item.STATUS_COMPLETED), project("home", tags = "home"))
        )
        assertEquals(
            // Выполненные — в той же группе, что открытые, после них; плашка — внизу
            listOf("open", "new", "old", TaskListKeys.AREA_PROJECTS_SPACER, "project:p", "toggle:logged"),
            ids(TagScreen.rows(content))
        )
    }

    @Test
    fun laterAndCompleted_underToggles_hiddenUntilShown() {
        val bounds = com.example.data.model.DayBounds.now()
        val tomorrow = bounds.endOfToday + 3_600_000L
        val now = task("now")
        val planned = ItemWithChecklist(Item(id = "planned", title = "planned", cachedTags = "work", startDate = tomorrow))
        val someday = ItemWithChecklist(Item(id = "someday", title = "someday", cachedTags = "work", start = Item.START_SOMEDAY))
        val done = task("done", status = Item.STATUS_COMPLETED, stop = 5)
        val laterProject = project("lp").copy(start = Item.START_SOMEDAY)
        val content = TagList.content(
            "work", tasks = listOf(now, planned, someday), allTasks = listOf(now, planned, someday, done),
            projects = listOf(laterProject), bounds = bounds
        )
        assertEquals(3, content.laterCount)
        // По умолчанию скрыты: видны только текущие и плашки
        assertEquals(listOf("now", "toggle:later", "toggle:logged"), ids(TagScreen.rows(content, showLater = false, showLogged = false)))
        // Раскрыты — поздние по месту перед своей плашкой, выполненные после своей
        assertEquals(
            // Поздние и выполненные — в тех же группах, что текущие (без второго заголовка); плашки — внизу
            listOf("now", "planned", "someday", "done", TaskListKeys.AREA_PROJECTS_SPACER, "project:lp", "toggle:later", "toggle:logged"),
            ids(TagScreen.rows(content, showLater = true, showLogged = true))
        )
        // Задача в открытом редакторе не уходит под плашку, даже с поздней датой
        val pinned = TagList.content(
            "work", tasks = listOf(planned), allTasks = listOf(planned), projects = emptyList(),
            pinned = mapOf("planned" to com.example.domain.lists.PlaceGroups.Place(null, null)), bounds = bounds
        )
        assertEquals(listOf("planned"), ids(TagScreen.rows(pinned, showLater = false, showLogged = false)))
    }

    @Test
    fun noSpacerWithoutOpenTasks_andEmpty() {
        val onlyProject = TagList.content("work", emptyList(), emptyList(), listOf(project("p")))
        assertEquals(listOf("project:p"), ids(TagScreen.rows(onlyProject)))
        assertTrue(TagList.content("work", emptyList(), listOf(task("x", tags = "home")), emptyList()).isEmpty)
    }

    @Test
    fun rows_groupedByPlace_likeHome() {
        val area = com.example.data.model.Area(id = "a", title = "Area")
        val inArea = project("pa", tags = "home", areaId = "a")
        val taggedInArea = project("tagged", areaId = "a")
        val looseProject = project("loose", tags = "home")
        val content = TagList.content(
            "work",
            tasks = listOf(task("t1"), task("t2", projectId = "pa"), task("t3", areaId = "a"), task("t4", projectId = "loose")),
            allTasks = emptyList(),
            projects = listOf(looseProject, inArea, taggedInArea),
            areas = listOf(area)
        )
        assertEquals(
            listOf(
                "t1",
                "header:loose", "t4",
                "header:Area", "t3", "project:tagged",
                "header:pa", "t2"
            ),
            ids(TagScreen.rows(content))
        )
    }

    @Test
    fun pinnedTask_staysInItsGroup() {
        val moved = task("m", projectId = "p")
        val content = TagList.content(
            "work", tasks = listOf(moved), allTasks = emptyList(), projects = listOf(project("p", tags = "home")),
            pinned = mapOf("m" to com.example.domain.lists.PlaceGroups.Place(null, null))
        )
        assertEquals(listOf("m"), ids(TagScreen.rows(content)))
    }

    @Test
    fun repeatingTemplatesAndCopies_onTagScreen() {
        val template = Item(id = "tpl", type = Item.TYPE_TEMPLATE, title = "tpl", cachedTags = "work")
        val part = Item(id = "part", type = Item.TYPE_TEMPLATE, title = "part", cachedTags = "work", repeatTemplateId = "ptpl")
        val copy = Item(id = "copy", title = "copy", cachedTags = "work", repeatTemplateId = "tpl")
        assertTrue(TagList.isActiveTask(template, "work"))
        assertTrue(TagList.isActiveTask(copy, "work"))
        assertFalse(TagList.isActiveTask(part, "work"))

        val projectTemplate = Item(id = "ptpl", type = Item.TYPE_PROJECT_TEMPLATE, title = "ptpl", cachedTags = "work")
        val content = TagList.content(
            "work",
            tasks = listOf(ItemWithChecklist(template), ItemWithChecklist(copy)),
            allTasks = listOf(ItemWithChecklist(projectTemplate), ItemWithChecklist(template)),
            projects = emptyList()
        )
        assertEquals(listOf("tpl", "copy", TaskListKeys.AREA_PROJECTS_SPACER, "project:ptpl"), ids(TagScreen.rows(content)))
    }

    @Test
    fun shownLaterAndCompleted_sameProjectOneHeader() {
        val p = Item(id = "p", type = Item.TYPE_PROJECT, title = "P")
        val tomorrow = com.example.data.model.DayBounds.now().endOfToday + 1
        val now = task("now", projectId = "p")
        val later = ItemWithChecklist(Item(id = "later", title = "later", cachedTags = "work", projectId = "p", start = Item.START_ANYTIME, startDate = tomorrow))
        val done = task("done", status = Item.STATUS_COMPLETED, stop = 1L, projectId = "p")
        val content = TagList.content("work", listOf(now, later), listOf(now, later, done), listOf(p))
        assertEquals(
            listOf("header:P", "now", "later", "done", "toggle:later", "toggle:logged"),
            ids(TagScreen.rows(content, showLater = true, showLogged = true))
        )
    }

    @Test
    fun repeatTemplate_laterByNextDay_likeItsCopies_laterSortedByDay() {
        val bounds = com.example.data.model.DayBounds.now()
        val day = 86_400_000L
        val template = ItemWithChecklist(Item(id = "tpl", title = "tpl", type = Item.TYPE_TEMPLATE, cachedTags = "work", start = Item.START_ANYTIME))
        val todayTemplate = ItemWithChecklist(Item(id = "tplNow", title = "tplNow", type = Item.TYPE_TEMPLATE, cachedTags = "work", start = Item.START_ANYTIME))
        val copy = ItemWithChecklist(Item(id = "copy", title = "copy", cachedTags = "work", repeatTemplateId = "tpl", start = Item.START_ANYTIME, startDate = bounds.endOfToday + 1 + day))
        val rules = mapOf(
            "tpl" to com.example.data.model.RecurrenceRule(templateId = "tpl", rrule = "", nextGenerationDate = bounds.endOfToday + 1 + 3 * day),
            "tplNow" to com.example.data.model.RecurrenceRule(templateId = "tplNow", rrule = "", nextGenerationDate = bounds.endOfToday - 1000)
        )
        val tasks = listOf(template, todayTemplate, copy)
        val content = TagList.content("work", tasks, tasks, emptyList(), bounds = bounds, rules = rules)
        // Шаблон на послезавтра+ — поздний, как и его копия; копия раньше — выше
        assertEquals(listOf("tplNow", "toggle:later"), ids(TagScreen.rows(content, showLater = false)))
        assertEquals(listOf("tplNow", "copy", "tpl", "toggle:later"), ids(TagScreen.rows(content, showLater = true)))
    }

    @Test
    fun repeatingProjectCopies_oneHeader() {
        val now = project("cur").copy(title = "Weekly", repeatTemplateId = "pt", status = Item.STATUS_OPEN)
        val old = project("old").copy(title = "Weekly", repeatTemplateId = "pt", status = Item.STATUS_COMPLETED)
        val open = task("a", projectId = "cur")
        val done = task("b", projectId = "old", status = Item.STATUS_COMPLETED, stop = 100)
        val content = TagList.content("work", listOf(open), listOf(open, done), listOf(now.copy(cachedTags = ""), old.copy(cachedTags = "")))
        assertEquals(listOf("header:Weekly", "a", "b", "toggle:logged"), ids(TagScreen.rows(content, showLogged = true)))
    }
}
