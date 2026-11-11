package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
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
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
)
