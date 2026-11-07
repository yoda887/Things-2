package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.components.ProjectScreen
import com.example.ui.screens.home.components.TaskListKeys
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Как в Things: в «В любое время» в разделе проекта или области видно три задачи («Показати ще N»), задачи без места
 * — все; в проекте более поздние задачи — в конце своей части по дате, их можно скрыть.
 */
class SectionLimitsTest {

    private val bounds = DayBounds.now()
    private val day = 24 * 3600 * 1000L
    private fun t(id: String, startDate: Long? = null, headingId: String? = null, start: Int = Item.START_ANYTIME) =
        ItemWithChecklist(Item(id = id, title = id, projectId = "p", start = start, startDate = startDate, headingId = headingId))

    @Test
    fun projectSection_showsThree_looseShowsAll() {
        val tasks = (1..5).map { t("t$it") }
        val (shown, hidden) = AnytimeList.visible(tasks, loose = false, expanded = false)
        assertEquals(listOf("t1", "t2", "t3"), shown.map { it.item.id })
        assertEquals(2, hidden)
        assertEquals(5 to 0, AnytimeList.visible(tasks, loose = true, expanded = false).let { it.first.size to it.second })
        assertEquals(5 to 0, AnytimeList.visible(tasks, loose = false, expanded = true).let { it.first.size to it.second })
        // Раскрытый раздел длиннее лимита сворачивается обратно; короткий и задачи без места — нет
        assertEquals(true, AnytimeList.collapsible(tasks, loose = false))
        assertEquals(false, AnytimeList.collapsible(tasks.take(3), loose = false))
        assertEquals(false, AnytimeList.collapsible(tasks, loose = true))
    }

    @Test
    fun taskInEditor_staysVisibleBeyondLimit() {
        val tasks = (1..5).map { t("t$it") }
        val (shown, hidden) = AnytimeList.visible(tasks, loose = false, expanded = false, keep = setOf("t5"))
        assertEquals(listOf("t1", "t2", "t3", "t5"), shown.map { it.item.id })
        assertEquals(1, hidden)
    }

    @Test
    fun projectLaterTasks_goToEndOfTheirPart_byDate() {
        val h = Item(id = "h", title = "h", type = Item.TYPE_HEADING, projectId = "p")
        val tasks = listOf(
            t("late2", startDate = bounds.endOfToday + 5 * day),
            t("a"),
            t("late1", startDate = bounds.endOfToday + 1 * day),
            t("b"),
            t("hLate", startDate = bounds.endOfToday + 2 * day, headingId = "h"),
            t("hA", headingId = "h")
        )
        val ordered = ProjectList.order(tasks, listOf(h), bounds)
        assertEquals(listOf("a", "b", "late1", "late2", "hA", "hLate"), ordered.map { it.item.id })

        val rows = ProjectScreen.rows(ordered, listOf(h), null, laterHidden = false, bounds = bounds)
        assertEquals(TaskListKeys.AREA_LATER_TOGGLE, rows.last())
        val hiddenRows = ProjectScreen.rows(ordered, listOf(h), null, laterHidden = true, bounds = bounds)
        assertEquals(listOf("a", "b", "hA"), hiddenRows.filterIsInstance<ItemWithChecklist>().map { it.item.id })
        assertEquals(3, ProjectScreen.laterCount(ordered, bounds))
    }

    @Test
    fun projectSomedayTasks_hiddenWithLater_afterScheduled() {
        val tasks = listOf(
            t("some", start = Item.START_SOMEDAY),
            t("a"),
            t("late", startDate = bounds.endOfToday + 2 * day)
        )
        val ordered = ProjectList.order(tasks, emptyList(), bounds)
        assertEquals(listOf("a", "late", "some"), ordered.map { it.item.id })
        val hiddenRows = ProjectScreen.rows(ordered, emptyList(), null, laterHidden = true, bounds = bounds)
        assertEquals(listOf("a"), hiddenRows.filterIsInstance<ItemWithChecklist>().map { it.item.id })
        assertEquals(TaskListKeys.AREA_LATER_TOGGLE, hiddenRows.last())
        assertEquals(2, ProjectScreen.laterCount(ordered, bounds))
    }

    @Test
    fun projectWithoutLater_hasNoToggle() {
        val rows = ProjectScreen.rows(listOf(t("a"), t("b")), emptyList(), null, bounds = bounds)
        assertEquals(listOf("a", "b"), rows.filterIsInstance<ItemWithChecklist>().map { it.item.id })
        assertEquals(2, rows.size)
    }
}
