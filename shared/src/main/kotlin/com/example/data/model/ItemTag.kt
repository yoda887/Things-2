package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "item_tags",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["tagId"])
    ]
)
data class ItemTag(
    val itemId: String,
    val tagId: String,
    /** Когда запись менялась последний раз (мс) — ставит триггер базы (SyncTriggers), для синхронизации устройств */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
)
