package com.example.data.sync

import com.example.data.sync.SyncMerge.Key
import com.example.data.sync.SyncMerge.State
import com.example.data.sync.SyncMerge.SyncDeletionMark
import com.example.data.sync.SyncMerge.SyncRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/** Файл общего состояния: записи и удаления переживают запись и чтение без потерь. */
class SyncCodecTest {

    @Test
    fun roundTrip_keepsValuesAndTypes() {
        val item = SyncRow(
            Key("items", "a"), 123L,
            mapOf("id" to "a", "title" to "Купить «хліб»", "startDate" to null, "sortOrder" to 7L, "updatedAt" to 123L, "score" to 1.5)
        )
        val link = SyncRow(Key("item_tags", "a|t"), 50L, mapOf("itemId" to "a", "tagId" to "t", "updatedAt" to 50L))
        val gone = SyncDeletionMark(Key("tags", "old"), 99L)
        val state = State(listOf(item, link).associateBy { it.key }, mapOf(gone.key to gone))

        val decoded = SyncCodec.decode(SyncCodec.encode(state))!!
        assertEquals(state.rows, decoded.rows)
        assertEquals(state.deletions, decoded.deletions)
    }

    @Test
    fun newerFormatOrGarbage_ignored() {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write("""{"v": 99, "rows": {}}""".toByteArray()) }
        assertNull(SyncCodec.decode(out.toByteArray()))
        assertNull(SyncCodec.decode("not gzip".toByteArray()))
    }

    @Test
    fun keys() {
        assertEquals("i|t", SyncTables.keyOf("item_tags", mapOf("itemId" to "i", "tagId" to "t")))
        assertEquals("x", SyncTables.keyOf("items", mapOf("id" to "x")))
        assertNull(SyncTables.keyOf("items", mapOf("title" to "no id")))
    }
}
