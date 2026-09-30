package com.example.domain.tag

import com.example.data.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagTitlesTest {

    @Test
    fun parse_understandsBothSeparatorsAndDropsDuplicates() {
        assertEquals(listOf("Work", "Home"), TagTitles.parse("Work, Home"))
        // старый формат из раскрытого редактора — без пробела
        assertEquals(listOf("Work", "Home"), TagTitles.parse("Work,Home"))
        assertEquals(listOf("Work"), TagTitles.parse(" Work , work ,, "))
        assertEquals(emptyList<String>(), TagTitles.parse(""))
    }

    @Test
    fun join_usesCanonicalSeparator() {
        assertEquals("a, b", TagTitles.join(listOf("a", "b")))
        assertEquals(TagTitles.parse(TagTitles.join(listOf("a", "b"))), listOf("a", "b"))
    }

    @Test
    fun resolve_reusesExistingTagsIgnoringCase() {
        val work = Tag(id = "1", title = "Work", sortOrder = 0)
        val home = Tag(id = "2", title = "Home", sortOrder = 5)
        val result = TagTitles.resolve(listOf("work", "HOME"), listOf(work, home))
        assertEquals(listOf(work, home), result.tags)
        assertTrue(result.created.isEmpty())
    }

    @Test
    fun resolve_createsMissingTagsOnceAndAfterExistingOrder() {
        val work = Tag(id = "1", title = "Work", sortOrder = 3)
        val result = TagTitles.resolve(listOf("New", "new", "Work"), listOf(work))
        assertEquals(1, result.created.size)
        val created = result.created.single()
        assertEquals("New", created.title)
        assertEquals(4, created.sortOrder)
        assertEquals(listOf(created, work), result.tags)
    }

    @Test
    fun find_skipsExceptId() {
        val a = Tag(id = "1", title = "Work")
        assertEquals(a, TagTitles.find(listOf(a), " work "))
        assertNull(TagTitles.find(listOf(a), "work", exceptId = "1"))
    }

    @Test
    fun duplicates_keepsRootAndFirstBySortOrder() {
        val child = Tag(id = "c", title = "Work", sortOrder = 0, parentId = "g")
        val rootLate = Tag(id = "r2", title = "work", sortOrder = 9)
        val rootEarly = Tag(id = "r1", title = "WORK", sortOrder = 2)
        val unique = Tag(id = "u", title = "Home")
        val plan = TagTitles.duplicates(listOf(child, rootLate, rootEarly, unique))
        assertEquals(mapOf(child to rootEarly, rootLate to rootEarly), plan)
    }

    @Test
    fun rename_replacesIgnoringCaseAndMergesDuplicates() {
        assertEquals(listOf("Office", "Home"), TagTitles.rename(listOf("work", "Home"), "Work", "Office"))
        // задача уже была с новым названием — повтора не будет
        assertEquals(listOf("Home"), TagTitles.rename(listOf("Work", "home"), "work", "Home"))
    }

    @Test
    fun remove_dropsTitlesIgnoringCase() {
        assertEquals(listOf("Home"), TagTitles.remove(listOf("Work", "Home"), listOf("work")))
    }
}
