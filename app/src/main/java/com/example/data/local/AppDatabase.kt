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

// [ИЗМЕНЕНИЕ]: Увеличена версия базы данных до 6 для поддержки таблицы recurrence_rules
@Database(entities = [Area::class, Item::class, Tag::class, ItemTag::class, ChecklistItem::class, RecurrenceRule::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
