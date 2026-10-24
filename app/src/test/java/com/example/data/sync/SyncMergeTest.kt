package com.example.data.sync

import com.example.data.sync.SyncMerge.Key
import com.example.data.sync.SyncMerge.State
import com.example.data.sync.SyncMerge.SyncDeletionMark
import com.example.data.sync.SyncMerge.SyncRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Слияние состояний двух устройств: побеждает самое позднее событие — правка или удаление; правки на обоих — по полям. */
class SyncMergeTest {

    private fun key(id: String) = Key("items", id)
    private fun row(id: String, at: Long, title: String = id) = SyncRow(key(id), at, mapOf("id" to id, "title" to title, "updatedAt" to at))
    private fun deletion(id: String, at: Long) = SyncDeletionMark(key(id), at)
    private fun state(rows: List<SyncRow> = emptyList(), deletions: List<SyncDeletionMark> = emptyList()) =
        State(rows.associateBy { it.key }, deletions.associateBy { it.key })

    @Test
    fun newerRemoteEdit_appliedLocally() {
        val result = SyncMerge.merge(state(listOf(row("a", 10, "old"))), state(listOf(row("a", 20, "new"))))
        assertEquals(listOf("new"), result.applyLocally.map { it.values["title"] })
        assertFalse(result.remoteChanged)
    }

    @Test
    fun newerLocalEdit_uploaded() {
        val result = SyncMerge.merge(state(listOf(row("a", 30, "mine"))), state(listOf(row("a", 20, "theirs"))))
        assertTrue(result.applyLocally.isEmpty())
        assertTrue(result.remoteChanged)
        assertEquals("mine", result.merged.rows[key("a")]?.values?.get("title"))
    }

    @Test
    fun newRecordsBothWays() {
        val result = SyncMerge.merge(state(listOf(row("mine", 5))), state(listOf(row("theirs", 5))))
        assertEquals(setOf("mine", "theirs"), result.merged.rows.keys.map { it.id }.toSet())
        assertEquals(listOf("theirs"), result.applyLocally.map { it.key.id })
        assertTrue(result.remoteChanged)
    }

    @Test
    fun deletionAfterEdit_wins_editAfterDeletion_wins() {
        // Там удалили после моей правки — удаляю у себя
        val deleted = SyncMerge.merge(state(listOf(row("a", 10))), state(deletions = listOf(deletion("a", 20))))
        assertEquals(listOf(key("a")), deleted.deleteLocally)
        assertTrue(deleted.merged.rows.isEmpty())
        assertFalse(deleted.remoteChanged)

        // Я правил позже, чем там удалили — запись остаётся и уходит в общий файл
        val revived = SyncMerge.merge(state(listOf(row("a", 30))), state(deletions = listOf(deletion("a", 20))))
        assertTrue(revived.deleteLocally.isEmpty())
        assertEquals(30L, revived.merged.rows[key("a")]?.updatedAt)
        assertTrue(revived.remoteChanged)
    }

    @Test
    fun localDeletion_propagates() {
        val result = SyncMerge.merge(state(deletions = listOf(deletion("a", 20))), state(listOf(row("a", 10))))
        assertTrue(result.merged.rows.isEmpty())
        assertTrue(result.remoteChanged)
        assertTrue(result.applyLocally.isEmpty())
    }

    @Test
    fun nothingNew_noUpload_noApply() {
        val same = state(listOf(row("a", 10)), listOf(deletion("b", 5)))
        val result = SyncMerge.merge(same, same)
        assertFalse(result.remoteChanged)
        assertTrue(result.applyLocally.isEmpty())
        assertTrue(result.deleteLocally.isEmpty())
    }

    @Test
    fun pruneOldDeletions() {
        val now = SyncMerge.DELETION_KEEP_MS + 1000
        val pruned = SyncMerge.pruneDeletions(state(deletions = listOf(deletion("old", 0), deletion("new", now - 10))), now)
        assertEquals(setOf("new"), pruned.deletions.keys.map { it.id }.toSet())
    }

    @Test
    fun newDevice_dropsSeedsMissingFromShared_keepsSharedOnes() {
        val seedKept = row("seed_kept", SyncMerge.SEED_TIME)
        val seedDeletedThere = row("seed_deleted", SyncMerge.SEED_TIME)
        val seedNeverShared = row("seed_never", SyncMerge.SEED_TIME)
        val local = state(listOf(seedKept, seedDeletedThere, seedNeverShared))
        val remote = state(listOf(row("seed_kept", 500, "edited"), row("mine_elsewhere", 600)), listOf(deletion("seed_deleted", 400)))

        val result = SyncMerge.merge(local, remote)
        // Образец, которого в общем файле нет, не уходит туда, а убирается здесь; удалённый там — тоже
        assertEquals(setOf("seed_never", "seed_deleted"), result.deleteLocally.map { it.id }.toSet())
        assertFalse(result.merged.rows.containsKey(key("seed_never")))
        assertEquals("edited", result.merged.rows[key("seed_kept")]?.values?.get("title"))
        assertFalse(result.remoteChanged)
    }

    @Test
    fun firstDevice_uploadsItsSeeds() {
        // Общего файла ещё нет — образцы первого устройства становятся общими
        val result = SyncMerge.merge(state(listOf(row("seed", SyncMerge.SEED_TIME))), State.EMPTY)
        assertTrue(result.merged.rows.containsKey(key("seed")))
        assertTrue(result.remoteChanged)
    }

    // ---------------------------------------------------------------- слияние по полям

    private fun item(at: Long, vararg fields: Pair<String, Any?>) =
        SyncRow(key("a"), at, mapOf("id" to "a", "updatedAt" to at) + fields)
    private fun other(table: String, id: String, at: Long, vararg fields: Pair<String, Any?>) =
        SyncRow(Key(table, id), at, mapOf<String, Any?>("updatedAt" to at) + fields)

    @Test
    fun differentFieldsOnTwoDevices_bothKept() {
        val base = state(listOf(item(10, "title" to "old", "startDate" to null)))
        // Здесь поменяли название, там — дату
        val local = state(listOf(item(20, "title" to "mine", "startDate" to null)))
        val remote = state(listOf(item(30, "title" to "old", "startDate" to 500L)))
        val result = SyncMerge.merge(local, remote, base)
        val merged = result.merged.rows.getValue(key("a"))
        assertEquals("mine", merged.values["title"])
        assertEquals(500L, merged.values["startDate"])
        // Новая запись — позже обеих: применяется здесь и уходит в общий файл
        assertEquals(31L, merged.updatedAt)
        assertEquals(31L, merged.values["updatedAt"])
        assertEquals(listOf(merged), result.applyLocally)
        assertTrue(result.remoteChanged)
    }

    @Test
    fun sameFieldOnTwoDevices_laterWins() {
        val base = state(listOf(item(10, "title" to "old", "notes" to "")))
        val theirsLater = SyncMerge.merge(
            state(listOf(item(20, "title" to "mine", "notes" to ""))),
            state(listOf(item(30, "title" to "theirs", "notes" to ""))),
            base
        )
        assertEquals("theirs", theirsLater.merged.rows.getValue(key("a")).values["title"])
        assertEquals(30L, theirsLater.merged.rows.getValue(key("a")).updatedAt)
        assertFalse(theirsLater.remoteChanged)

        val mineLater = SyncMerge.merge(
            state(listOf(item(40, "title" to "mine", "notes" to ""))),
            state(listOf(item(30, "title" to "theirs", "notes" to ""))),
            base
        )
        assertEquals("mine", mineLater.merged.rows.getValue(key("a")).values["title"])
        assertTrue(mineLater.applyLocally.isEmpty())
        assertTrue(mineLater.remoteChanged)
    }

    @Test
    fun sameFieldConflict_otherFieldsStillMerged() {
        val base = state(listOf(item(10, "title" to "old", "notes" to "", "priority" to 0L)))
        val result = SyncMerge.merge(
            state(listOf(item(20, "title" to "mine", "notes" to "note", "priority" to 0L))),
            state(listOf(item(30, "title" to "theirs", "notes" to "", "priority" to 2L))),
            base
        )
        val merged = result.merged.rows.getValue(key("a")).values
        assertEquals("theirs", merged["title"])
        assertEquals("note", merged["notes"])
        assertEquals(2L, merged["priority"])
    }

    @Test
    fun withoutBase_wholeRecordLaterWins() {
        val result = SyncMerge.merge(
            state(listOf(item(20, "title" to "mine", "startDate" to null))),
            state(listOf(item(30, "title" to "old", "startDate" to 500L)))
        )
        assertEquals("old", result.merged.rows.getValue(key("a")).values["title"])
        assertFalse(result.remoteChanged)
    }

    @Test
    fun editedOnOneSideOnly_noFieldMerge() {
        val was = item(10, "title" to "old")
        // Там не правили с прошлого обмена — моя правка целиком
        val result = SyncMerge.merge(state(listOf(item(20, "title" to "mine"))), state(listOf(was)), state(listOf(was)))
        assertEquals(20L, result.merged.rows.getValue(key("a")).updatedAt)
        assertTrue(result.applyLocally.isEmpty())
        assertTrue(result.remoteChanged)
    }

    @Test
    fun mergedRecord_takenWholeByOtherDevice() {
        // Другое устройство: своё не правило с прошлого обмена, в общем файле — слитая запись
        val was = item(20, "title" to "mine", "startDate" to null)
        val merged = item(31, "title" to "mine", "startDate" to 500L)
        val result = SyncMerge.merge(state(listOf(was)), state(listOf(merged)), state(listOf(was)))
        assertEquals(listOf(merged), result.applyLocally)
        assertFalse(result.remoteChanged)
    }

    @Test
    fun editAfterDeletion_withBase_editWins() {
        val was = item(10, "title" to "old")
        val result = SyncMerge.merge(
            state(listOf(item(30, "title" to "mine"))),
            state(deletions = listOf(deletion("a", 20))),
            state(listOf(was))
        )
        assertEquals("mine", result.merged.rows.getValue(key("a")).values["title"])
        assertTrue(result.deleteLocally.isEmpty())
        assertTrue(result.remoteChanged)
    }

    @Test
    fun tagsAndChecklistOnTwoDevices_recountedFromLinks() {
        val tagA = other("tags", "A", 5, "id" to "A", "title" to "Дом", "sortOrder" to 0L)
        val tagB = other("tags", "B", 5, "id" to "B", "title" to "Работа", "sortOrder" to 1L)
        fun task(at: Long, tags: String, total: Long, open: Long) =
            item(at, "title" to "t", "cachedTags" to tags, "checklistItemsCount" to total, "openChecklistItemsCount" to open)
        val base = state(listOf(tagA, tagB, task(10, "", 0, 0)))
        // Здесь — тег «Дом» и открытый пункт, там — тег «Работа» и выполненный пункт
        val local = state(listOf(
            tagA, tagB, task(20, "Дом", 1, 1),
            other("item_tags", "a|A", 20, "itemId" to "a", "tagId" to "A"),
            other("checklist_items", "c1", 20, "id" to "c1", "itemId" to "a", "isCompleted" to 0L)
        ))
        val remote = state(listOf(
            tagA, tagB, task(30, "Работа", 1, 0),
            other("item_tags", "a|B", 30, "itemId" to "a", "tagId" to "B"),
            other("checklist_items", "c2", 30, "id" to "c2", "itemId" to "a", "isCompleted" to 1L)
        ))
        val result = SyncMerge.merge(local, remote, base)
        val merged = result.merged.rows.getValue(key("a")).values
        assertEquals("Дом, Работа", merged["cachedTags"])
        assertEquals(2L, merged["checklistItemsCount"])
        assertEquals(1L, merged["openChecklistItemsCount"])
        assertTrue(result.applyLocally.any { it.key == key("a") })
        assertTrue(result.remoteChanged)
    }
}
