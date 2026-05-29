package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.Tag
import com.example.data.model.ItemTag
import com.example.data.model.ChecklistItem

// [ИЗМЕНЕНИЕ]: Увеличена версия базы данных с 1 до 2, чтобы Room корректно сбросил схему
// и пересоздал таблицы при переходе от старых моделей (Task/Project) к новым (Area/Item/etc).
@Database(entities = [Area::class, Item::class, Tag::class, ItemTag::class, ChecklistItem::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
