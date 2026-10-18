package com.example.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Резервная копия в Google Drive: разбор ответов Drive и правила восстановления. */
class DriveBackupTest {

    @Test
    fun parseList_newestFirst_ignoresBrokenEntries() {
        val body = """
            {"files": [
              {"id": "old", "modifiedTime": "2026-10-16T08:00:00.000Z", "size": "1000"},
              {"name": "no id"},
              {"id": "new", "modifiedTime": "2026-10-18T09:15:02.123Z", "size": "2048"}
            ]}
        """.trimIndent()
        val files = DriveFiles.parseList(body)
        assertEquals(listOf("new", "old"), files.map { it.id })
        assertEquals(2048L, files.first().size)
    }

    @Test
    fun parseList_emptyOrUnexpected() {
        assertEquals(emptyList<DriveFile>(), DriveFiles.parseList("""{"files": []}"""))
        assertEquals(emptyList<DriveFile>(), DriveFiles.parseList("""{"kind": "drive#fileList"}"""))
    }

    @Test
    fun parseIdAndTime() {
        assertEquals("abc", DriveFiles.parseId("""{"id": "abc"}"""))
        assertNull(DriveFiles.parseId("""{"name": "x"}"""))
        assertEquals(1792314902123L, DriveFiles.parseTime("2026-10-18T09:15:02.123Z"))
        assertNull(DriveFiles.parseTime("not a date"))
        assertNull(DriveFiles.parseTime(null))
    }

    @Test
    fun restorable_onlyAppDatabaseNotNewerThanCurrent() {
        val tables = setOf("items", "areas", "tags", "checklist_items")
        assertTrue(BackupRules.isRestorable(9, 9, tables))
        // Старую копию доведут миграции Room
        assertTrue(BackupRules.isRestorable(7, 9, tables))
        // Копия из более новой версии приложения — не восстанавливать
        assertFalse(BackupRules.isRestorable(10, 9, tables))
        // Не база приложения
        assertFalse(BackupRules.isRestorable(9, 9, setOf("android_metadata")))
        assertFalse(BackupRules.isRestorable(0, 9, tables))
    }
}
