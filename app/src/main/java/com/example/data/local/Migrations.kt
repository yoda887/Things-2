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
