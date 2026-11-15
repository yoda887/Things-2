package com.example.domain.tag

import com.example.data.model.Area
import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Фильтр по тегам, как в Things: все выбранные теги сразу, «Без тега», выбор одного снимает другое. */
class TagFilterTest {

    private val all = Item(id = "a", title = "Все", cachedTags = "Errand,Home,Office")
    private val noOffice = Item(id = "b", title = "Без Office", cachedTags = "Errand,Home")
    private val bare = Item(id = "c", title = "Без тегов")

    @Test
    fun severalTags_onlyItemsWithAllOfThem() {
        val filter = TagFilter(tags = listOf("Errand", "Office"))
        assertTrue(filter.matches(all))
        assertFalse(filter.matches(noOffice))
        assertFalse(filter.matches(bare))
    }

    @Test
    fun noFilter_everything() {
        assertTrue(TagFilter.NONE.matches(bare))
        assertFalse(TagFilter.NONE.isActive)
    }

    @Test
    fun untagged_onlyItemsWithoutTags_includingInherited() {
        val area = Area(id = "ar", title = "Дом", cachedTags = "Home")
        val inArea = Item(id = "d", title = "В области", areaId = "ar")
        val index = TagInheritance.Index(emptyList(), listOf(area))
        val filter = TagFilter(untagged = true)
        assertTrue(filter.matches(bare, index))
        assertFalse(filter.matches(all, index))
        // Тег области — тоже тег задачи
        assertFalse(filter.matches(inArea, index))
        // И наоборот: фильтр по тегу области находит задачу в ней
        assertTrue(TagFilter(tags = listOf("home")).matches(inArea, index))
    }

    @Test
    fun toggle_tagsAndUntaggedExcludeEachOther() {
        val withTags = TagFilter.NONE.toggle("Errand").toggle("Home")
        assertEquals(listOf("Errand", "Home"), withTags.tags)
        val untagged = withTags.toggleUntagged()
        assertEquals(TagFilter(untagged = true), untagged)
        assertEquals(TagFilter(tags = listOf("Office")), untagged.toggle("Office"))
        // Снятие — без учёта регистра
        assertEquals(listOf("Home"), withTags.toggle("errand").tags)
        assertEquals(TagFilter.NONE, TagFilter(untagged = true).toggleUntagged())
    }
}
