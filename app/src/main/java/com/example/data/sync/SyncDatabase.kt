package com.example.data.sync

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.SyncTriggers
import com.example.data.sync.SyncMerge.Key
import com.example.data.sync.SyncMerge.State
import com.example.data.sync.SyncMerge.SyncDeletionMark
import com.example.data.sync.SyncMerge.SyncRow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * База для синхронизации: прочитать своё состояние (все записи шести таблиц и журнал удалений) и применить итог
 * слияния. Применение — одной транзакцией Room (экраны обновятся по её окончании), с флагом «изменения с другого
 * устройства»: триггеры не перебивают пришедшее время изменения.
 */
@Singleton
class SyncDatabase @Inject constructor(private val database: AppDatabase) {

    private val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase

    suspend fun read(): State = database.withTransaction {
        val rows = HashMap<Key, SyncRow>()
        for (table in SyncTables.ORDER) {
            db.query("SELECT * FROM `$table`").use { cursor ->
                while (cursor.moveToNext()) {
                    val values = rowValues(cursor)
                    val id = SyncTables.keyOf(table, values) ?: continue
                    val key = Key(table, id)
                    rows[key] = SyncRow(key, (values["updatedAt"] as? Long) ?: 0L, values)
                }
            }
        }
        val deletions = HashMap<Key, SyncDeletionMark>()
        db.query("SELECT tableName, recordId, deletedAt FROM sync_deletions").use { cursor ->
            while (cursor.moveToNext()) {
                val key = Key(cursor.getString(0), cursor.getString(1))
                if (key.table in SyncTables.ORDER) deletions[key] = SyncDeletionMark(key, cursor.getLong(2))
            }
        }
        State(rows, deletions)
    }

    /**
     * Применить пришедшее: удалить [deletions] (сначала зависимые таблицы), вставить или заменить [rows] (сначала те,
     * на кого ссылаются). Запись, которой не на что сослаться (её задачу здесь удалили), пропускается.
     * Журнал удалений чистится от записей старше [pruneBefore].
     */
    suspend fun apply(rows: List<SyncRow>, deletions: List<Key>, pruneBefore: Long) = database.withTransaction {
        SyncTriggers.beginApplyingRemote(db)
        try {
            for (table in SyncTables.ORDER.reversed()) {
                deletions.filter { it.table == table }.forEach { key ->
                    val (where, args) = whereOf(key)
                    db.delete("`$table`", where, args)
                }
            }
            for (table in SyncTables.ORDER) {
                rows.filter { it.key.table == table }.forEach { row -> upsert(table, row) }
            }
            db.delete("sync_deletions", "deletedAt < ?", arrayOf<Any>(pruneBefore))
        } finally {
            SyncTriggers.endApplyingRemote(db)
        }
    }

    /** Заменить запись (UPDATE) или вставить новую; UPSERT в SQLite старых Android нет */
    private fun upsert(table: String, row: SyncRow) {
        val values = ContentValues().apply {
            row.values.forEach { (column, value) ->
                when (value) {
                    null -> putNull(column)
                    is String -> put(column, value)
                    is Long -> put(column, value)
                    is Double -> put(column, value)
                    else -> put(column, value.toString())
                }
            }
        }
        val (where, args) = whereOf(row.key)
        val updated = runCatching { db.update("`$table`", SQLiteDatabase.CONFLICT_ABORT, values, where, args) }.getOrDefault(0)
        if (updated == 0) {
            runCatching { db.insert("`$table`", SQLiteDatabase.CONFLICT_ABORT, values) }
        }
    }

    private fun whereOf(key: Key): Pair<String, Array<Any>> =
        if (key.table == "item_tags") {
            val (item, tag) = key.id.split('|', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            "itemId = ? AND tagId = ?" to arrayOf(item, tag)
        } else "id = ?" to arrayOf(key.id)

    private fun rowValues(cursor: Cursor): Map<String, Any?> {
        val values = LinkedHashMap<String, Any?>(cursor.columnCount)
        for (i in 0 until cursor.columnCount) {
            values[cursor.getColumnName(i)] = when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> null
                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                else -> null
            }
        }
        return values
    }
}
