package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "areas")
data class Area(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sortOrder: Int = 0,
    val trashed: Boolean = false,
    val creationDate: Long = System.currentTimeMillis(),
    /** Когда запись менялась последний раз (мс) — ставит триггер базы (SyncTriggers), для синхронизации устройств */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
    /**
     * Теги области — строкой названий, как у задачи ([Item.cachedTags]). Задачи и проекты области их наследуют
     * ([com.example.domain.tag.TagInheritance]): на экране тега и в фильтре по тегу они видны вместе с областью
     */
    @ColumnInfo(defaultValue = "") val cachedTags: String = ""
) {
    @get:Ignore
    val tags: List<String>
        get() = com.example.domain.tag.TagTitles.parse(cachedTags)
}
