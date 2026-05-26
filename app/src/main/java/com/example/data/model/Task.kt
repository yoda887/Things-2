package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskSection {
    INBOX, TODAY, UPCOMING, ANYTIME, SOMEDAY
}

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val section: TaskSection = TaskSection.INBOX,
    val isTonight: Boolean = false,
    val isCompleted: Boolean = false,
    val completedDate: Long? = null,
    val dueDate: Long? = null,
    val tags: List<String> = emptyList(),
    val projectId: String? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val googleTaskId: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val calendarColor: Int? = null,
    val calendarDisplayName: String? = null,
    val eventStartMillis: Long? = null,
    val isAllDay: Boolean = false,
    val priority: Int = 0 // 0 = None, 1 = Low, 2 = Medium, 3 = High
)
