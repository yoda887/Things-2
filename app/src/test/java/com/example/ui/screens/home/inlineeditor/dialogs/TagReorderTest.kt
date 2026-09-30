package com.example.ui.screens.home.inlineeditor.dialogs

import com.example.data.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagReorderTest {

    private fun group(id: String) = Tag(id = id, title = id) to false
    private fun child(id: String, parent: String) = Tag(id = id, title = id, parentId = parent) to true

    private fun ids(rows: List<TagRow>?) = rows!!.map { it.first.id }
    private fun parentOf(rows: List<TagRow>?, id: String) = rows!!.first { it.first.id == id }.first.parentId

    // A: a1 a2 | B: b1 | C
    private val rows = listOf(
        group("A"), child("a1", "A"), child("a2", "A"),
        group("B"), child("b1", "B"),
        group("C"),
    )

    @Test
    fun moveGroup_carriesItsTags() {
        assertEquals(listOf("B", "b1", "A", "a1", "a2", "C"), ids(TagReorder.moveGroup(rows, "A", "B")))
        assertEquals(listOf("C", "A", "a1", "a2", "B", "b1"), ids(TagReorder.moveGroup(rows, "C", "A")))
    }

    @Test
    fun moveGroup_toTopmostGroup() {
        assertEquals(listOf("B", "b1", "A", "a1", "a2", "C"), ids(TagReorder.moveGroup(rows, "B", "A")))
    }

    @Test
    fun moveGroup_ignoresSelfAndChildren() {
        assertNull(TagReorder.moveGroup(rows, "A", "A"))
        assertNull(TagReorder.moveGroup(rows, "A", "b1"))
    }

    @Test
    fun moveChild_swapsWithinGroup() {
        val down = TagReorder.moveChild(rows, "a1", "a2", movingDown = true)
        assertEquals(listOf("A", "a2", "a1", "B", "b1", "C"), ids(down))
        val up = TagReorder.moveChild(rows, "a2", "a1", movingDown = false)
        assertEquals(listOf("A", "a2", "a1", "B", "b1", "C"), ids(up))
    }

    @Test
    fun moveChild_downAcrossGroupHeader_becomesFirstOfThatGroup() {
        val result = TagReorder.moveChild(rows, "a2", "B", movingDown = true)
        assertEquals(listOf("A", "a1", "B", "a2", "b1", "C"), ids(result))
        assertEquals("B", parentOf(result, "a2"))
    }

    @Test
    fun moveChild_upAcrossOwnHeader_becomesLastOfPreviousGroup() {
        val result = TagReorder.moveChild(rows, "b1", "B", movingDown = false)
        assertEquals(listOf("A", "a1", "a2", "b1", "B", "C"), ids(result))
        assertEquals("A", parentOf(result, "b1"))
    }

    @Test
    fun moveChild_upAcrossTopmostHeader_isRejected() {
        assertNull(TagReorder.moveChild(rows, "a1", "A", movingDown = false))
    }

    @Test
    fun moveChild_intoEmptyGroup() {
        val result = TagReorder.moveChild(rows, "b1", "C", movingDown = true)
        assertEquals(listOf("A", "a1", "a2", "B", "C", "b1"), ids(result))
        assertEquals("C", parentOf(result, "b1"))
    }

    @Test
    fun moveChild_ontoChildOfOtherGroup_takesItsGroup() {
        val result = TagReorder.moveChild(rows, "a2", "b1", movingDown = true)
        assertEquals(listOf("A", "a1", "B", "b1", "a2", "C"), ids(result))
        assertEquals("B", parentOf(result, "a2"))
    }

    @Test
    fun moveChild_respectsAllowedGroups() {
        assertNull(TagReorder.moveChild(rows, "b1", "C", movingDown = true, canBeGroup = { it != "C" }))
    }

    @Test
    fun withoutChildrenOf_hidesOnlyThatGroup() {
        assertEquals(listOf("A", "B", "b1", "C"), ids(TagReorder.withoutChildrenOf(rows, "A")))
        assertEquals(ids(rows), ids(TagReorder.withoutChildrenOf(rows, null)))
    }
}
