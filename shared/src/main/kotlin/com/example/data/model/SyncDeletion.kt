package com.example.data.model

import androidx.room.Entity

/**
 * Журнал удалений для синхронизации устройств: запись [recordId] таблицы [tableName] удалена насовсем в [deletedAt]
 * (мс). Без него другое устройство не отличит «удалили здесь» от «создали там и ещё не прислали». Пишут триггеры
 * базы (SyncTriggers); снова вставленная запись из журнала уходит.
 */
@Entity(tableName = "sync_deletions", primaryKeys = ["tableName", "recordId"])
data class SyncDeletion(
    val tableName: String,
    val recordId: String,
    val deletedAt: Long
)
