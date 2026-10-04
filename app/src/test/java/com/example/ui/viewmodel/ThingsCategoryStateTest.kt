package com.example.ui.viewmodel

import com.example.data.model.Area
import com.example.data.model.ChecklistItem
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.components.ProjectProgress
import com.example.ui.screens.home.components.ThingsCategoryListState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Проверяет фильтрацию задач по экранам и вычисление производных данных
 * в [ThingsViewModel.computeCategoryListState].
 */
class ThingsCategoryStateTest {

    /** Полдень дня, отстоящего от сегодняшнего на [daysFromToday] суток. */
    private fun noonInDays(daysFromToday: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Задача-обёртка с явным id и необязательным чек-листом. */
    private fun task(
        id: String,
        start: Int = 0,
        status: Int = 0,
        type: Int = 0,
        title: String = id,
        notes: String = "",
        startDate: Long? = null,
        projectId: String? = null,
        areaId: String? = null,
        headingId: String? = null,
        tags: String = "",
        trashed: Boolean = false,
        sortOrder: Int = 0,
        checklist: List<String> = emptyList()
    ) = ItemWithChecklist(
        item = Item(
            id = id,
            type = type,
            title = title,
            notes = notes,
            status = status,
            start = start,
            startDate = startDate,
            projectId = projectId,
            areaId = areaId,
            headingId = headingId,
            cachedTags = tags,
            trashed = trashed,
            sortOrder = sortOrder
        ),
        checklist = checklist.mapIndexed { i, t -> ChecklistItem(id = "$id-c$i", itemId = id, title = t) }
    )

    /** Вызов тестируемой функции с нейтральными значениями для несущественных параметров. */
    private fun state(
        screen: ActiveScreen,
        tasks: List<ItemWithChecklist>,
        project: Item? = null,
        area: Area? = null,
        tag: Tag? = null,
        expandedTaskId: String? = null,
        selectedTag: String? = null,
        query: String = "",
        headings: List<Item> = emptyList()
    ): ThingsCategoryListState = ThingsViewModel.computeCategoryListState(
        screen = screen,
        project = project,
        area = area,
        tag = tag,
        expandedTaskId = expandedTaskId,
        selectedTag = selectedTag,
        taskList = tasks,
        projectList = emptyList(),
        areaList = emptyList(),
        calEvents = emptyList(),
        savedTags = emptyList(),
        savedTagObjs = emptyList(),
        allTagsSet = emptySet(),
        highlighted = null,
        query = query,
        headingList = headings
    )

    private fun ids(s: ThingsCategoryListState) = s.displayTasks.map { it.item.id }

    // ─── Экраны ─────────────────────────────────────────────────────

    @Test
    fun `Inbox показывает только открытые задачи без дат и проекта`() {
        val tasks = listOf(
            task("inbox"),
            task("inboxDone", status = 3),
            task("today", start = 1),
            task("inProject", projectId = "p1"),
            task("scheduled", startDate = noonInDays(2))
        )
        assertEquals(listOf("inbox"), ids(state(ActiveScreen.INBOX, tasks)))
    }

    @Test
    fun `Today показывает задачи на сегодня`() {
        val tasks = listOf(
            task("manual", start = 1),
            task("startsToday", start = 2, startDate = noonInDays(0)),
            task("tomorrow", start = 2, startDate = noonInDays(1)),
            task("doneToday", start = 1, status = 3)
        )
        assertEquals(listOf("manual", "startsToday"), ids(state(ActiveScreen.TODAY, tasks)))
    }

    @Test
    fun `Upcoming показывает задачи с будущей датой старта`() {
        val tasks = listOf(
            task("tomorrow", start = 2, startDate = noonInDays(1)),
            task("today", start = 1),
            task("anytime", start = 2)
        )
        assertEquals(listOf("tomorrow"), ids(state(ActiveScreen.UPCOMING, tasks)))
    }

    @Test
    fun `Anytime исключает Inbox Someday и будущие задачи`() {
        val tasks = listOf(
            task("anytime", start = 2),
            task("projectTask", projectId = "p1"),
            task("inbox"),
            task("someday", start = 3),
            task("future", start = 2, startDate = noonInDays(5))
        )
        assertEquals(listOf("anytime", "projectTask"), ids(state(ActiveScreen.ANYTIME, tasks)))
    }

    @Test
    fun `Someday показывает только отложенные задачи`() {
        val tasks = listOf(task("someday", start = 3), task("anytime", start = 2), task("inbox"))
        assertEquals(listOf("someday"), ids(state(ActiveScreen.SOMEDAY, tasks)))
    }

    @Test
    fun `Logbook показывает только выполненные задачи`() {
        val tasks = listOf(task("done", status = 3), task("open"), task("cancelled", status = 2))
        assertEquals(listOf("done"), ids(state(ActiveScreen.LOGBOOK, tasks)))
    }

    @Test
    fun `экран проекта показывает открытые задачи только этого проекта`() {
        val project = Item(id = "p1", type = Item.TYPE_PROJECT, title = "P1")
        val tasks = listOf(
            task("mine", projectId = "p1"),
            task("mineDone", projectId = "p1", status = 3),
            task("other", projectId = "p2"),
            task("loose")
        )
        assertEquals(listOf("mine"), ids(state(ActiveScreen.PROJECT_DETAIL, tasks, project = project)))
    }

    @Test
    fun `экран области показывает открытые задачи области но не проекты`() {
        val area = Area(id = "a1", title = "A1")
        val tasks = listOf(
            task("mine", areaId = "a1"),
            task("projectInArea", areaId = "a1", type = Item.TYPE_PROJECT),
            task("mineDone", areaId = "a1", status = 3),
            task("other", areaId = "a2")
        )
        assertEquals(listOf("mine"), ids(state(ActiveScreen.AREA_DETAIL, tasks, area = area)))
    }

    @Test
    fun `экран тега показывает открытые задачи с этим тегом`() {
        val tag = Tag(id = "t", title = "work")
        val tasks = listOf(
            task("work", tags = "work"),
            task("workHome", tags = "home,work"),
            task("home", tags = "home"),
            task("workDone", tags = "work", status = 3)
        )
        assertEquals(listOf("work", "workHome"), ids(state(ActiveScreen.TAG_DETAIL, tasks, tag = tag)))
    }

    @Test
    fun `экран тега без тега пуст`() {
        val tasks = listOf(task("work", tags = "work"))
        assertTrue(state(ActiveScreen.TAG_DETAIL, tasks, tag = null).displayTasks.isEmpty())
    }

    @Test
    fun `поиск находит по названию заметкам и чек-листу без учёта регистра`() {
        val tasks = listOf(
            task("byTitle", title = "Купить МОЛОКО"),
            task("byNotes", notes = "не забыть молоко"),
            task("byChecklist", checklist = listOf("Молоко 2 л")),
            task("doneMatch", title = "молоко", status = 3),
            task("trashedMatch", title = "молоко", trashed = true),
            task("projectMatch", title = "молоко", type = Item.TYPE_PROJECT),
            task("noMatch", title = "хлеб")
        )
        assertEquals(
            listOf("byTitle", "byNotes", "byChecklist", "doneMatch"),
            ids(state(ActiveScreen.SEARCH, tasks, query = "  молоко "))
        )
    }

    @Test
    fun `пустой поисковый запрос ничего не находит`() {
        val tasks = listOf(task("a"), task("b"))
        assertTrue(state(ActiveScreen.SEARCH, tasks, query = "   ").displayTasks.isEmpty())
    }

    @Test
    fun `экран HOME не показывает задач`() {
        assertTrue(state(ActiveScreen.HOME, listOf(task("inbox"), task("today", start = 1))).displayTasks.isEmpty())
    }

    // ─── Открытая задача, теги, сортировка ──────────────────────────

    @Test
    fun `открытая в редакторе задача остаётся в списке даже вне фильтра экрана`() {
        // Задачу отметили выполненной в инлайн-редакторе — из Inbox она не должна пропасть
        val tasks = listOf(task("a"), task("edited", status = 3))
        assertEquals(listOf("a", "edited"), ids(state(ActiveScreen.INBOX, tasks, expandedTaskId = "edited")))
    }

    @Test
    fun `фильтр по тегу оставляет задачи с тегом и открытую задачу`() {
        val tasks = listOf(
            task("work", tags = "work", sortOrder = 0),
            task("home", tags = "home", sortOrder = 1),
            task("edited", sortOrder = 2)
        )
        val s = state(ActiveScreen.INBOX, tasks, expandedTaskId = "edited", selectedTag = "work")
        assertEquals(listOf("work", "edited"), ids(s))
        assertEquals("work", s.selectedTagFilter)
    }

    @Test
    fun `задачи сортируются по sortOrder`() {
        val tasks = listOf(task("c", sortOrder = 3), task("a", sortOrder = 1), task("b", sortOrder = 2))
        assertEquals(listOf("a", "b", "c"), ids(state(ActiveScreen.INBOX, tasks)))
    }

    // ─── Заголовки проекта ──────────────────────────────────────────

    @Test
    fun `экран проекта группирует задачи по активным заголовкам`() {
        val project = Item(id = "p1", type = Item.TYPE_PROJECT, title = "P1")
        val headings = listOf(
            Item(id = "hB", type = Item.TYPE_HEADING, projectId = "p1", sortOrder = 2),
            Item(id = "hA", type = Item.TYPE_HEADING, projectId = "p1", sortOrder = 1),
            Item(id = "hArchived", type = Item.TYPE_HEADING, projectId = "p1", status = 3),
            Item(id = "hTrashed", type = Item.TYPE_HEADING, projectId = "p1", trashed = true),
            Item(id = "hOther", type = Item.TYPE_HEADING, projectId = "p2")
        )
        val tasks = listOf(
            task("b1", projectId = "p1", headingId = "hB", sortOrder = 0),
            task("a1", projectId = "p1", headingId = "hA", sortOrder = 1),
            task("loose", projectId = "p1", sortOrder = 2)
        )
        val s = state(ActiveScreen.PROJECT_DETAIL, tasks, project = project, headings = headings)
        assertEquals(listOf("hA", "hB"), s.headings.map { it.id })
        assertEquals(listOf("loose", "a1", "b1"), ids(s))
    }

    @Test
    fun `заголовки не передаются на экраны кроме проекта`() {
        val headings = listOf(Item(id = "h", type = Item.TYPE_HEADING, projectId = "p1"))
        assertTrue(state(ActiveScreen.INBOX, listOf(task("a")), headings = headings).headings.isEmpty())
    }

    // ─── Прогресс проектов ──────────────────────────────────────────

    @Test
    fun `прогресс проекта считается по всем задачам независимо от экрана`() {
        val tasks = listOf(
            task("p1a", projectId = "p1"),
            task("p1b", projectId = "p1", status = 3),
            task("p1c", projectId = "p1", status = 3),
            task("p2a", projectId = "p2"),
            task("loose")
        )
        val progress = state(ActiveScreen.INBOX, tasks).projectProgressMap
        assertEquals(ProjectProgress(completed = 2, total = 3), progress["p1"])
        assertEquals(ProjectProgress(completed = 0, total = 1), progress["p2"])
        assertEquals(2, progress.size)
    }

    @Test
    fun `пустой projectId не попадает в прогресс`() {
        val progress = state(ActiveScreen.INBOX, listOf(task("x", projectId = ""))).projectProgressMap
        assertFalse(progress.containsKey(""))
        assertNull(progress["x"])
    }

    @Test
    fun `состояние переносит входные данные экрана`() {
        val all = listOf(task("a"), task("done", status = 3))
        val s = state(ActiveScreen.INBOX, all, expandedTaskId = "a", query = "q")
        assertEquals(ActiveScreen.INBOX, s.screen)
        assertEquals("a", s.inlineExpandedTaskId)
        assertEquals("q", s.searchQuery)
        assertEquals(all, s.allTasks)
    }
}
