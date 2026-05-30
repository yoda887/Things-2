package com.example.data.model

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
data class RecurrenceRule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val rrule: String,
    val nextGenerationDate: Long,
    val isActive: Boolean = true
)
