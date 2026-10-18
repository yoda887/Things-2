package com.example.data.sync

import com.example.data.sync.SyncMerge.Key
import com.example.data.sync.SyncMerge.State
import com.example.data.sync.SyncMerge.SyncDeletionMark
import com.example.data.sync.SyncMerge.SyncRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Слияние состояний двух устройств: побеждает самое позднее событие — правка или удаление. */
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
}
