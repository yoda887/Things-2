package com.example.domain.edits

import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Test

/** Копии выбранных задач — группой над верхней выбранной; порядок списков — sortOrder, затем более новая выше. */
class BatchDuplicatesTest {

    private val listOrder = compareBy<Item> { it.sortOrder }.thenByDescending { it.creationDate }

    private fun task(id: String, sortOrder: Int, created: Long) =
        Item(id = id, title = id, sortOrder = sortOrder, creationDate = created)

    private fun withCopies(all: List<Item>, selected: List<Item>): List<String> {
        val copies = BatchDuplicates.places(selected).map { (item, place) ->
            item.copy(id = item.id + "'", sortOrder = place.sortOrder, creationDate = place.creationDate, dayOrder = place.dayOrder)
        }
        return (all + copies).sortedWith(listOrder).map { it.id }
    }

    @Test
    fun copiesAsGroupAboveTheTopSelected_sameSortOrder() {
        val x = task("x", 0, 500); val a = task("a", 0, 400); val y = task("y", 0, 300); val b = task("b", 0, 200)
        assertEquals(listOf("x", "a'", "b'", "a", "y", "b"), withCopies(listOf(x, a, y, b), listOf(a, b)))
    }

    @Test
    fun copiesAsGroupAboveTheTopSelected_ownSortOrders() {
        val x = task("x", 1, 0); val a = task("a", 2, 0); val y = task("y", 3, 0); val b = task("b", 4, 0)
        assertEquals(listOf("x", "a'", "b'", "a", "y", "b"), withCopies(listOf(x, a, y, b), listOf(a, b)))
    }

    @Test
    fun dayOrder_ofTheTopOnlyForTheSameDay() {
        val a = task("a", 0, 0).copy(startDate = 10, dayOrder = 5)
        val b = task("b", 0, 0).copy(startDate = 10, dayOrder = 9)
        val c = task("c", 0, 0).copy(startDate = 20, dayOrder = 1)
        val places = BatchDuplicates.places(listOf(a, b, c)).map { it.second.dayOrder }
        assertEquals(listOf(5L, 5L, 1L), places)
    }
}
