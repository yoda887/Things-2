package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "recurrence_rules",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["nextGenerationDate"])
    ]
)
/**
 * Повтор шаблона [templateId]: правило [rrule] (RepeatRule.encode), следующий повтор
 * [nextGenerationDate] — день повтора в мс начала дня, 0 — повторов пока нет («после выполнения» ждёт
 * выполнения текущей копии или повторы закончились), [isActive] — false на паузе, [generatedCount] —
 * сколько копий уже создано (для окончания «после N повторов»).
 */
data class RecurrenceRule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val rrule: String,
    val nextGenerationDate: Long,
    val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0") val generatedCount: Int = 0,
    /** Когда запись менялась последний раз (мс) — ставит триггер базы (SyncTriggers), для синхронизации устройств */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
)
