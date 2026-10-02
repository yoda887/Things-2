package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FabInsertionTest {

    private fun task(id: String, heading: String? = null, tonight: Boolean = false) =
        ItemWithChecklist(item = Item(id = id, title = id, headingId = heading, isTonight = tonight), checklist = emptyList())

    private fun heading(id: String) = ProjectHeadingItem(Item(id = id, title = id, type = Item.TYPE_HEADING))

    // t1 | A: a1 a2 | B: b1
    private val t1 = task("t1")
    private val a1 = task("a1", "A")
    private val a2 = task("a2", "A")
    private val b1 = task("b1", "B")
    private val project: List<Any> = listOf(t1, heading("A"), a1, a2, heading("B"), b1)
    private val tasks = listOf(t1, a1, a2, b1)

    @Test
    fun slotAt_beforeRowWhoseMiddleIsBelowFinger() {
        val rows = listOf(FabRow(0, 0f, 100f), FabRow(1, 100f, 100f), FabRow(2, 200f, 100f))
        assertEquals(0, FabInsertion.slotAt(10f, rows, false)!!.index)
        assertEquals(1, FabInsertion.slotAt(60f, rows, false)!!.index)
        assertEquals(3, FabInsertion.slotAt(280f, rows, false)!!.index)
        assertNull(FabInsertion.slotAt(10f, emptyList(), false))
    }

    @Test
    fun taskPlacement_insideHeadingGroup() {
        // между a1 и a2
        val p = FabInsertion.taskPlacement(project, 3, tasks)
        assertEquals(2, p.taskIndex)
        assertEquals("A", p.headingId)
        assertFalse(p.isTonight)
    }

    @Test
    fun taskPlacement_rightAfterHeading_belongsToIt() {
        val p = FabInsertion.taskPlacement(project, 5, tasks)
        assertEquals(3, p.taskIndex)
        assertEquals("B", p.headingId)
    }

    @Test
    fun taskPlacement_beforeFirstHeading_hasNoHeading() {
        val p = FabInsertion.taskPlacement(project, 1, tasks)
        assertEquals(1, p.taskIndex)
        assertNull(p.headingId)
    }

    @Test
    fun taskPlacement_atEnd() {
        val p = FabInsertion.taskPlacement(project, project.size, tasks)
        assertEquals(4, p.taskIndex)
        assertEquals("B", p.headingId)
    }

    @Test
    fun taskPlacement_afterEveningHeader_isTonight() {
        val day = task("d")
        val eve = task("e", tonight = true)
        val today: List<Any> = listOf(day, TaskListKeys.EVENING_HEADER, eve)
        assertTrue(FabInsertion.taskPlacement(today, 2, listOf(day, eve)).isTonight)
        assertFalse(FabInsertion.taskPlacement(today, 1, listOf(day, eve)).isTonight)
    }

    @Test
    fun headingPlacement_splitsGroup() {
        // новый заголовок между a1 и a2: a2 уходит под него, заголовок — второй
        val p = FabInsertion.headingPlacement(project, 3)
        assertEquals(1, p.headingIndex)
        assertEquals(listOf("a2"), p.movedTaskIds)

        // перед первым заголовком: под него уходят задачи без заголовка ниже — их нет
        val top = FabInsertion.headingPlacement(project, 1)
        assertEquals(0, top.headingIndex)
        assertEquals(emptyList<String>(), top.movedTaskIds)

        // в самом начале: под него уходит t1
        assertEquals(listOf("t1"), FabInsertion.headingPlacement(project, 0).movedTaskIds)
    }

    @Test
    fun withGap_insertsPlaceholder() {
        val gapped = FabInsertion.withGap(project, FabSlot(2, asHeading = true))
        assertEquals(FabGapItem(true), gapped[2])
        assertEquals(project.size + 1, gapped.size)
    }

    @Test
    fun headingsWith_renumbers() {
        val hs = listOf(Item(id = "A", type = 2, sortOrder = 0), Item(id = "B", type = 2, sortOrder = 1))
        val result = FabInsertion.headingsWith(hs, Item(id = "N", type = 2, sortOrder = -1), 1)
        assertEquals(listOf("A", "N", "B"), result.map { it.id })
        assertEquals(listOf(0, 1, 2), result.map { it.sortOrder })
    }

    @Test
    fun taskPlacement_upcoming_takesDateOfSectionAboveGap() {
        val day1 = UpcomingHeaderItem(1_700_000_000_000L, "1", "Mon")
        val day2 = UpcomingHeaderItem(1_700_086_400_000L, "2", "Tue")
        val a = task("a")
        val b = task("b")
        val list: List<Any> = listOf(day1, a, day2, b)
        fun hourOf(millis: Long?) = java.util.Calendar.getInstance().apply { timeInMillis = millis!! }.get(java.util.Calendar.HOUR_OF_DAY)
        // между a и day2 — день 1; после day2 — день 2
        assertEquals(1, FabInsertion.taskPlacement(list, 2, listOf(a, b)).taskIndex)
        val under1 = FabInsertion.taskPlacement(list, 2, listOf(a, b)).startDate!!
        val under2 = FabInsertion.taskPlacement(list, 3, listOf(a, b)).startDate!!
        assertEquals(12, hourOf(under1))
        assertTrue(under2 - under1 in 82_800_000L..90_000_000L)
        // выше первого заголовка — дата первого
        assertEquals(under1, FabInsertion.taskPlacement(list, 0, listOf(a, b)).startDate)
    }

    @Test
    fun taskPlacement_withoutUpcomingHeaders_hasNoDate() {
        assertNull(FabInsertion.taskPlacement(project, 3, tasks).startDate)
    }

    @Test
    fun upcomingHeadersAreAnchors() {
        assertTrue(FabInsertion.isAnchor(UpcomingHeaderItem(0L, "1", "Mon")))
        assertTrue(FabInsertion.isAnchor(UpcomingMonthHeaderItem(0L, "June")))
    }

    @Test
    fun freeSortOrder_usesGapBetweenNeighbours() {
        assertEquals(6, FabInsertion.freeSortOrder(listOf(5, 10), 1))
        assertEquals(11, FabInsertion.freeSortOrder(listOf(5, 10), 2))
        assertEquals(4, FabInsertion.freeSortOrder(listOf(5, 10), 0))
        assertEquals(0, FabInsertion.freeSortOrder(emptyList(), 0))
    }

    @Test
    fun freeSortOrder_nullWhenNoRoomOrOrderIsNotAscending() {
        assertNull(FabInsertion.freeSortOrder(listOf(3, 3, 3), 1))
        assertNull(FabInsertion.freeSortOrder(listOf(3, 4), 1))
        assertNull(FabInsertion.freeSortOrder(listOf(9, 2, 5), 1))
    }

    @Test
    fun taskPlacement_areaSections() {
        val a = task("a")
        val u = task("u")
        val s = task("s")
        val list: List<Any> = listOf(a, TaskListKeys.AREA_UPCOMING_HEADING, u, TaskListKeys.AREA_SOMEDAY_HEADING, s)
        val all = listOf(a, u, s)
        assertFalse(FabInsertion.taskPlacement(list, 1, all).let { it.inAreaUpcoming || it.inAreaSomeday })
        assertTrue(FabInsertion.taskPlacement(list, 2, all).inAreaUpcoming)
        assertTrue(FabInsertion.taskPlacement(list, 4, all).inAreaSomeday)
        assertTrue(FabInsertion.isAnchor(TaskListKeys.AREA_SOMEDAY_HEADING))
    }

    @Test
    fun slotAt_centersGapOnButton() {
        val rows = listOf(FabRow(0, 0f, 100f), FabRow(1, 100f, 100f), FabRow(2, 200f, 100f))
        // Промежуток высотой 100: его центр = граница + 50, поэтому палец на 120 — граница на 100 (центр 150)
        assertEquals(1, FabInsertion.slotAt(120f, rows, false, gapHeight = 100f)!!.index)
        // Палец на 160 — граница на 100 даёт центр 150, на 200 — 250: ближе первая
        assertEquals(1, FabInsertion.slotAt(160f, rows, false, gapHeight = 100f)!!.index)
        assertEquals(2, FabInsertion.slotAt(210f, rows, false, gapHeight = 100f)!!.index)
        // Без поправки на высоту выбирается ближайшая граница
        assertEquals(2, FabInsertion.slotAt(160f, rows, false)!!.index)
    }

    @Test
    fun taskPlacement_rightAboveHeading_isLastInSection() {
        // В общем списке задачи секций вперемешку: d1 e1 d2 — d2 последняя дневная, e1 вечерняя.
        // Промежуток сразу над «Вечером» — после d2, а не перед e1 (между d1 и d2)
        val d1 = task("d1"); val d2 = task("d2"); val e1 = task("e1", tonight = true)
        val today: List<Any> = listOf(d1, d2, TaskListKeys.EVENING_HEADER, e1)
        val p = FabInsertion.taskPlacement(today, 2, listOf(d1, e1, d2))
        assertEquals(3, p.taskIndex)
        assertFalse(p.isTonight)
        // Пустая секция под заголовком — ориентир из соседней
        val empty: List<Any> = listOf(d1, TaskListKeys.EVENING_HEADER)
        assertEquals(1, FabInsertion.taskPlacement(empty, 2, listOf(d1)).taskIndex)
        // В проекте над заголовком B — после a2, последней задачи группы A
        assertEquals(3, FabInsertion.taskPlacement(project, 4, tasks).taskIndex)
    }
}
