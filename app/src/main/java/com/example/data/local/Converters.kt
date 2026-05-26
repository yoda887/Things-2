package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ChecklistItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value) ?: "[]"
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromChecklistList(value: List<ChecklistItem>): String {
        val type = Types.newParameterizedType(List::class.java, ChecklistItem::class.java)
        val adapter = moshi.adapter<List<ChecklistItem>>(type)
        return adapter.toJson(value) ?: "[]"
    }

    @TypeConverter
    fun toChecklistList(value: String): List<ChecklistItem> {
        val type = Types.newParameterizedType(List::class.java, ChecklistItem::class.java)
        val adapter = moshi.adapter<List<ChecklistItem>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }
}
