package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskSection {
    INBOX, TODAY, UPCOMING, ANYTIME, SOMEDAY
}

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Area::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["headingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["areaId"]),
        Index(value = ["projectId"]),
        Index(value = ["status", "start", "trashed"])
    ]
)
data class Item(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: Int, // 0=task, 1=project, 2=heading, 3=template
    val title: String,
    val notes: String = "",
    val status: Int = 0, // 0=open, 2=cancelled, 3=completed
    val start: Int = 0, // 0=inbox, 1=today, 2=anytime, 3=someday
    val isTonight: Boolean = false,
    val completedDate: Long? = null,
    val dueDate: Long? = null,
    val areaId: String? = null,
    val projectId: String? = null,
    val headingId: String? = null,
    val cachedTags: String = "",
    val checklistItemsCount: Int = 0,
    val openChecklistItemsCount: Int = 0,
    val googleTaskId: String? = null,
    val googleTaskListId: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val calendarColor: Int? = null,
    val calendarDisplayName: String? = null,
    val eventStartMillis: Long? = null,
    val isAllDay: Boolean = false,
    val priority: Int = 0, // 0=None, 1=Low, 2=Medium, 3=High
    val trashed: Boolean = false
) {
    @Ignore
    var checklist: List<ChecklistItem> = emptyList()

    @get:Ignore
    val isCompleted: Boolean
        get() = status == 3

    @get:Ignore
    val name: String
        get() = title

    @get:Ignore
    val section: TaskSection
        get() {
            if (dueDate != null) {
                val todayStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val tomorrowStart = todayStart + (24 * 60 * 60 * 1000)
                if (dueDate >= tomorrowStart) {
                    return TaskSection.UPCOMING
                }
            }
            return when (start) {
                0 -> TaskSection.INBOX
                1 -> TaskSection.TODAY
                2 -> TaskSection.ANYTIME
                3 -> TaskSection.SOMEDAY
                else -> TaskSection.INBOX
            }
        }

    @get:Ignore
    val tags: List<String>
        get() = if (cachedTags.isBlank()) emptyList() else cachedTags.split(", ").map { it.trim() }
}

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["itemId"])
    ]
)
data class ChecklistItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)
