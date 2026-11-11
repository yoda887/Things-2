package com.example.domain.repeat

import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Сохранение после перетаскивания в «Планах» с шаблонами повтора на днях следующей копии. */
class RepeatDropsTest {

    private val zone = ZoneId.of("Europe/Kiev")
    private fun day(d: Int) = LocalDate.of(2026, 11, d).atStartOfDay(zone).toInstant().toEpochMilli()
    private fun template(start: Long?, order: Long? = null) =
        Item(id = "T", title = "T", type = Item.TYPE_TEMPLATE, startDate = start, dayOrder = order)
    private fun task(id: String, start: Long?, order: Long? = null) = Item(id = id, title = id, startDate = start, dayOrder = order)

    // В базе у шаблона своя дата — день, с которого идёт правило; в «Планах» он стоит 7-го
    private val inDb = mapOf("T" to template(day(1)))
    private val shown = listOf(task("a", day(7)), template(day(7)))

    @Test
    fun templateWithOwnDate_savedAsReorder() {
        val result = RepeatDrops.afterDrag(listOf(template(day(1), order = 3), task("a", day(7), 4)), inDb::get, shown, zone)
        assertEquals(listOf("T", "a"), result.save.map { it.id })
        assertNull(result.moved)
    }

    @Test
    fun templateWithShownDate_onItsDay_notSaved_notMoved() {
        val result = RepeatDrops.afterDrag(listOf(template(day(7), order = 3), task("a", day(7), 4)), inDb::get, shown, zone)
        assertEquals(listOf("a"), result.save.map { it.id })
        assertNull(result.moved)
    }

    @Test
    fun templateDroppedOnAnotherDay_movesNearestRepeat() {
        val result = RepeatDrops.afterDrag(listOf(template(day(9)), task("a", day(7), 4)), inDb::get, shown, zone)
        assertEquals(listOf("a"), result.save.map { it.id })
        assertEquals("T" to day(9), result.moved)
    }

    @Test
    fun withoutTemplates_everythingSaved() {
        val items = listOf(task("a", day(7), 1), task("b", day(8), 2))
        val result = RepeatDrops.afterDrag(items, inDb::get, shown, zone)
        assertEquals(items, result.save)
        assertNull(result.moved)
    }
}
