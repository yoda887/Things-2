package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 7 → 8: у задачи появилось напоминание — время дня в минутах (items.reminderTime, может быть пустым).
 * Только добавляется столбец: существующие задачи остаются как были, без напоминаний.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `items` ADD COLUMN `reminderTime` INTEGER")
    }
}

/**
 * 8 → 9: повторяющиеся задачи. Копия ссылается на свой шаблон (items.repeatTemplateId, с индексом), у правила
 * повтора — счётчик созданных копий (recurrence_rules.generatedCount, для окончания «после N повторов»).
 * Только добавляются столбцы и индекс: существующие задачи остаются как были.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `items` ADD COLUMN `repeatTemplateId` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_repeatTemplateId` ON `items` (`repeatTemplateId`)")
        db.execSQL("ALTER TABLE `recurrence_rules` ADD COLUMN `generatedCount` INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Версия 10: синхронизация устройств — у каждой записи время последнего изменения (`updatedAt`), журнал удалений
 * (`sync_deletions`) и триггеры, которые их ведут (SyncTriggers). Время уже существующих записей — время миграции
 * (у задач — их modificationDate): до синхронизации все записи «изменены» одинаково.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        listOf("items", "areas", "tags", "item_tags", "checklist_items", "recurrence_rules").forEach { table ->
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `$table` SET updatedAt = $now")
        }
        db.execSQL("UPDATE `items` SET updatedAt = modificationDate WHERE modificationDate > 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_deletions` (`tableName` TEXT NOT NULL, `recordId` TEXT NOT NULL, " +
                "`deletedAt` INTEGER NOT NULL, PRIMARY KEY(`tableName`, `recordId`))"
        )
        SyncTriggers.create(db)
    }
}
