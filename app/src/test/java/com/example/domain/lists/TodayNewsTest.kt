package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Test

/** Новое в «Сегодня»: что помечается точкой, что — нет, и что снимает «OK». */
class TodayNewsTest {

    private val bounds: DayBounds = DayBounds.now()
    private val nowMs: Long = bounds.todayStart + 10 * 60 * 60 * 1000L
    private val old: Long = nowMs - 3 * 24 * 60 * 60 * 1000L

    private fun task(id: String, startDate: Long?, changed: Long = old, created: Long = old, repeatOf: String? = null) =
        ItemWithChecklist(
            Item(
                id = id, title = id, start = if (startDate != null) Item.START_ANYTIME else Item.START_INBOX,
                startDate = startDate, modificationDate = changed, updatedAt = changed, creationDate = created,
                repeatTemplateId = repeatOf
            )
        )

    private val tomorrow: Long = bounds.endOfToday + 1
    private val today: Long = bounds.todayStart + 1

    @Test
    fun firstRun_everythingInTodayIsSeen() {
        val list = listOf(task("a", today), task("b", tomorrow))
        val todayIds = TodayNews.todayIds(list, bounds)
        val known = TodayNews.update(todayIds, null, list, bounds, nowMs)
        assertEquals(emptySet<String>(), TodayNews.newIds(known, todayIds))
    }

    @Test
    fun arrivedWhileClosed_isNew_untilOk() {
        // Вчера «b» была завтрашней; приложение запустили сегодня — её день настал
        val list = listOf(task("a", today), task("b", today))
        val known = TodayNews.update(setOf("a"), null, list, bounds, nowMs)
        val todayIds = TodayNews.todayIds(list, bounds)
        assertEquals(setOf("b"), TodayNews.newIds(known, todayIds))
        // «OK» — всё виденное
        assertEquals(emptySet<String>(), TodayNews.newIds(known + todayIds, todayIds))
    }

    @Test
    fun placedOnTodayJustNow_isNotNew_butArrivedByDateIs() {
        val before = listOf(task("a", today), task("b", tomorrow), task("c", null))
        val previous = TodayNews.todayIds(before, DayBounds.now())
        // «c» поставили на сегодня только что; «b» вошла без правки — настал день (смена суток при открытом приложении)
        val after = listOf(task("a", today), task("b", today), task("c", today, changed = nowMs))
        val known = TodayNews.update(setOf("a"), previous, after, bounds, nowMs)
        assertEquals(setOf("b"), TodayNews.newIds(known, TodayNews.todayIds(after, bounds)))
    }

    @Test
    fun freshRepeatCopy_isNew() {
        val before = listOf(task("a", today))
        val after = before + task("copy", today, changed = nowMs, created = nowMs, repeatOf = "template")
        val known = TodayNews.update(setOf("a"), TodayNews.todayIds(before, bounds), after, bounds, nowMs)
        assertEquals(setOf("copy"), TodayNews.newIds(known, TodayNews.todayIds(after, bounds)))
    }

    @Test
    fun leftTodayAndCameBack_isNewAgain() {
        val known = setOf("a")
        // Перенесли на завтра — из виденного выбывает
        val moved = listOf(task("a", tomorrow, changed = nowMs))
        val afterMove = TodayNews.update(known, setOf("a"), moved, bounds, nowMs)
        assertEquals(emptySet<String>(), afterMove)
        // Назавтра пришла сама — снова новая
        val back = listOf(task("a", today))
        val knownBack = TodayNews.update(afterMove, null, back, bounds, nowMs)
        assertEquals(setOf("a"), TodayNews.newIds(knownBack, TodayNews.todayIds(back, bounds)))
    }

    @Test
    fun projects_count_closedAndFutureNot() {
        val project = ItemWithChecklist(Item(id = "p", title = "p", type = Item.TYPE_PROJECT, start = Item.START_ANYTIME, startDate = today))
        val done = ItemWithChecklist(Item(id = "d", title = "d", startDate = today, start = Item.START_ANYTIME, status = Item.STATUS_COMPLETED))
        assertEquals(setOf("p"), TodayNews.todayIds(listOf(project, done, task("f", tomorrow)), bounds))
    }
}
