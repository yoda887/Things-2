package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.Tag
import com.example.data.model.ItemTag
import com.example.data.model.ChecklistItem
import com.example.data.model.RecurrenceRule

// Версия 8: напоминание задачи (items.reminderTime) — миграция MIGRATION_7_8
// Версия 9: повторяющиеся задачи — копия ссылается на шаблон, у правила счётчик копий — MIGRATION_8_9
@Database(entities = [Area::class, Item::class, Tag::class, ItemTag::class, ChecklistItem::class, RecurrenceRule::class], version = 9, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
