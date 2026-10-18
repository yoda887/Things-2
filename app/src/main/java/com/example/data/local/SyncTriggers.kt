package com.example.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Триггеры базы для синхронизации устройств — они, а не код приложения, следят за изменениями, поэтому ни один путь
 * записи не забыт (ни @Upsert, ни вставка с заменой, ни запросы UPDATE):
 * - вставка и изменение ставят `updatedAt` — «когда запись менялась последний раз»;
 * - удаление насовсем — запись в журнал `sync_deletions`; снова вставленная запись из журнала уходит.
 *
 * Пока синхронизация применяет изменения с другого устройства, она поднимает флаг в `sync_control`
 * ([beginApplyingRemote] / [endApplyingRemote]) — тогда время изменения остаётся тем, что пришло с того устройства.
 *
 * Триггеры пересоздаются при каждом открытии базы (DatabaseModule): так база всегда с их нынешней версией.
 *
 * Android включает рекурсивные триггеры: UPDATE внутри триггера снова вызывает триггер изменения. Поэтому время
 * ставится строго больше прежнего (`MAX(сейчас, прежнее + 1)`), а триггер изменения срабатывает, только пока время
 * не сдвинуто вперёд, — второй вызов сразу останавливается.
 */
object SyncTriggers {

    /** Сейчас в миллисекундах — средствами SQLite */
    private const val NOW_MS = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"

    /** Изменение сделано в приложении, а не пришло с другого устройства */
    private const val LOCAL_CHANGE = "NOT EXISTS (SELECT 1 FROM sync_control WHERE applyingRemote = 1)"

    /** Таблица → (ключ новой строки, ключ старой строки); у связей задача–тег ключ составной */
    val TABLES: Map<String, Pair<String, String>> = linkedMapOf(
        "items" to ("NEW.id" to "OLD.id"),
        "areas" to ("NEW.id" to "OLD.id"),
        "tags" to ("NEW.id" to "OLD.id"),
        "item_tags" to ("NEW.itemId || '|' || NEW.tagId" to "OLD.itemId || '|' || OLD.tagId"),
        "checklist_items" to ("NEW.id" to "OLD.id"),
        "recurrence_rules" to ("NEW.id" to "OLD.id")
    )

    /** Строка по ключу — для UPDATE изнутри триггера */
    private fun whereKey(table: String): String =
        if (table == "item_tags") "itemId = NEW.itemId AND tagId = NEW.tagId" else "id = NEW.id"

    fun statements(): List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS sync_control (id INTEGER PRIMARY KEY CHECK (id = 1), applyingRemote INTEGER NOT NULL DEFAULT 0)",
        "INSERT OR IGNORE INTO sync_control (id, applyingRemote) VALUES (1, 0)"
    ) + TABLES.flatMap { (table, keys) ->
        val (newKey, oldKey) = keys
        listOf(
            "DROP TRIGGER IF EXISTS `sync_${table}_insert`",
            "DROP TRIGGER IF EXISTS `sync_${table}_update`",
            "DROP TRIGGER IF EXISTS `sync_${table}_delete`",
            "DROP TRIGGER IF EXISTS `sync_${table}_insert_undelete`",
            // Новая запись: время изменения — сейчас; из журнала удалений она уходит
            """CREATE TRIGGER `sync_${table}_insert` AFTER INSERT ON `$table` WHEN $LOCAL_CHANGE BEGIN
                UPDATE `$table` SET updatedAt = MAX($NOW_MS, NEW.updatedAt + 1) WHERE ${whereKey(table)};
            END""",
            """CREATE TRIGGER `sync_${table}_insert_undelete` AFTER INSERT ON `$table` BEGIN
                DELETE FROM sync_deletions WHERE tableName = '$table' AND recordId = $newKey;
            END""",
            // Правка в приложении, время ещё не сдвинуто — сдвигаем; повторный вызов от этого UPDATE уже не проходит WHEN
            """CREATE TRIGGER `sync_${table}_update` AFTER UPDATE ON `$table`
                WHEN $LOCAL_CHANGE AND NEW.updatedAt <= OLD.updatedAt BEGIN
                UPDATE `$table` SET updatedAt = MAX($NOW_MS, OLD.updatedAt + 1) WHERE ${whereKey(table)};
            END""",
            // Удаление насовсем — в журнал
            """CREATE TRIGGER `sync_${table}_delete` AFTER DELETE ON `$table` BEGIN
                INSERT OR REPLACE INTO sync_deletions (tableName, recordId, deletedAt) VALUES ('$table', $oldKey, $NOW_MS);
            END"""
        )
    }

    fun create(db: SupportSQLiteDatabase) {
        statements().forEach { db.execSQL(it) }
    }

    /**
     * Новая база только что заполнена образцами: их время изменения — самое раннее ([com.example.data.sync.SyncMerge.SEED_TIME]),
     * журнал удалений пуст. Так любая правка или удаление на других устройствах их перебивает
     */
    fun markAllAsSeed(db: SupportSQLiteDatabase) {
        beginApplyingRemote(db)
        try {
            TABLES.keys.forEach { table -> db.execSQL("UPDATE `$table` SET updatedAt = ${com.example.data.sync.SyncMerge.SEED_TIME}") }
            db.execSQL("DELETE FROM sync_deletions")
        } finally {
            endApplyingRemote(db)
        }
    }

    /** Синхронизация начинает применять изменения с другого устройства: их время изменения сохраняется */
    fun beginApplyingRemote(db: SupportSQLiteDatabase) = db.execSQL("UPDATE sync_control SET applyingRemote = 1 WHERE id = 1")

    fun endApplyingRemote(db: SupportSQLiteDatabase) = db.execSQL("UPDATE sync_control SET applyingRemote = 0 WHERE id = 1")
}
