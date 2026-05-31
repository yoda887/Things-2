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
        Index(value = ["headingId"]),
        Index(value = ["status", "start", "trashed"])
    ]
)
data class Item(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: Int = 0, // 0=task, 1=project, 2=heading, 3=template
    val title: String = "",
    val notes: String = "",
    val status: Int = 0, // 0=open, 2=cancelled, 3=completed
    val start: Int = 0, // 0=inbox, 1=today, 2=anytime, 3=someday
    val isTonight: Boolean = false,
    val stopDate: Long? = null,
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
    val trashed: Boolean = false,
    val startDate: Long? = null,
    val modificationDate: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
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
            // First check the dueDate fallback: if dueDate is <= 3 days from today, it goes to TODAY
            val dDate = dueDate
            if (dDate != null) {
                val cal = java.util.Calendar.getInstance()
                val todayStart = cal.apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val calDue = java.util.Calendar.getInstance().apply {
                    timeInMillis = dDate
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val diffMs = calDue.timeInMillis - todayStart
                val diffDays = diffMs / (24 * 3600 * 1000)
                if (diffDays <= 3) {
                    return TaskSection.TODAY
                }
            }

            if (startDate != null) {
                val cal = java.util.Calendar.getInstance()
                val endOfToday = cal.apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }.timeInMillis

                if (startDate > endOfToday) {
                    return TaskSection.UPCOMING
                } else {
                    return TaskSection.TODAY
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

    /**
     * Determines if the task belongs to Inbox category.
     */
    @get:Ignore
    val isInbox: Boolean
        get() = type == 0 && start == 0 && startDate == null && dueDate == null

    /**
     * Determines if the task belongs to Today category.
     * This includes:
     * - Manually placed in Today (start == 1)
     * - Start date is today or in the past (startDate <= endOfToday)
     * - Start date is in the future but due date is in less than 3 days
     * - No start date but due date is within 3 days or less
     */
    @get:Ignore
    val isToday: Boolean
        get() {
            if (type != 0 || isCompleted) return false
            if (start == 1) return true
            val dDate = dueDate
            if (dDate != null) {
                val cal = java.util.Calendar.getInstance()
                val todayStart = cal.apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val calDue = java.util.Calendar.getInstance().apply {
                    timeInMillis = dDate
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val diffMs = calDue.timeInMillis - todayStart
                val diffDays = diffMs / (24 * 3600 * 1000)
                if (diffDays <= 3) return true
            }
            val sDate = startDate
            if (sDate != null) {
                val cal = java.util.Calendar.getInstance()
                val endOfToday = cal.apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }.timeInMillis

                if (sDate <= endOfToday) return true
            }
            return false
        }

    /**
     * Determines if the task belongs to Upcoming category.
     * Scheduled in the future (startDate > endOfToday) but due date
     * is NOT less than 3 days away.
     */
    @get:Ignore
    val isUpcoming: Boolean
        get() {
            if (type != 0 || isCompleted) return false
            if (isToday) return false
            val sDate = startDate ?: return false
            val cal = java.util.Calendar.getInstance()
            val endOfToday = cal.apply {
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }.timeInMillis

            return sDate > endOfToday
        }

    /**
     * Determines if the task belongs to Anytime category.
     * Tasks that are actionable (not in Inbox, not Someday, not in the future).
     * Includes active tasks with startDate <= endOfToday.
     */
    @get:Ignore
    val isAnytime: Boolean
        get() {
            if (type != 0 || isCompleted) return false
            if (isToday) return false
            if (start == 0 && startDate == null) return false
            if (start == 3) return false

            val sDate = startDate
            if (sDate != null) {
                val cal = java.util.Calendar.getInstance()
                val endOfToday = cal.apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }.timeInMillis
                if (sDate <= endOfToday) return true
                return false
            }
            return start == 2
        }

    /**
     * Determines if the task belongs to Someday category.
     */
    @get:Ignore
    val isSomeday: Boolean
        get() = type == 0 && !isCompleted && !isToday && start == 3

    @get:Ignore
    val tags: List<String>
        get() = if (cachedTags.isBlank()) emptyList() else cachedTags.split(", ").map { it.trim() }

    /**
     * Helper to perform item copying while preserving ignored fields like checklist
     */
    fun copyTask(
        id: String = this.id,
        type: Int = this.type,
        title: String = this.title,
        notes: String = this.notes,
        status: Int = this.status,
        start: Int = this.start,
        isTonight: Boolean = this.isTonight,
        stopDate: Long? = this.stopDate,
        dueDate: Long? = this.dueDate,
        areaId: String? = this.areaId,
        projectId: String? = this.projectId,
        headingId: String? = this.headingId,
        cachedTags: String = this.cachedTags,
        checklistItemsCount: Int = this.checklistItemsCount,
        openChecklistItemsCount: Int = this.openChecklistItemsCount,
        googleTaskId: String? = this.googleTaskId,
        googleTaskListId: String? = this.googleTaskListId,
        creationDate: Long = this.creationDate,
        calendarColor: Int? = this.calendarColor,
        calendarDisplayName: String? = this.calendarDisplayName,
        eventStartMillis: Long? = this.eventStartMillis,
        isAllDay: Boolean = this.isAllDay,
        priority: Int = this.priority,
        trashed: Boolean = this.trashed,
        startDate: Long? = this.startDate,
        modificationDate: Long = this.modificationDate,
        sortOrder: Int = this.sortOrder,
        checklist: List<ChecklistItem> = this.checklist
    ): Item {
        return this.copy(
            id = id,
            type = type,
            title = title,
            notes = notes,
            status = status,
            start = start,
            isTonight = isTonight,
            stopDate = stopDate,
            dueDate = dueDate,
            areaId = areaId,
            projectId = projectId,
            headingId = headingId,
            cachedTags = cachedTags,
            checklistItemsCount = checklistItemsCount,
            openChecklistItemsCount = openChecklistItemsCount,
            googleTaskId = googleTaskId,
            googleTaskListId = googleTaskListId,
            creationDate = creationDate,
            calendarColor = calendarColor,
            calendarDisplayName = calendarDisplayName,
            eventStartMillis = eventStartMillis,
            isAllDay = isAllDay,
            priority = priority,
            trashed = trashed,
            startDate = startDate,
            modificationDate = modificationDate,
            sortOrder = sortOrder
        ).apply {
            this.checklist = checklist
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Item

        if (id != other.id) return false
        if (type != other.type) return false
        if (title != other.title) return false
        if (notes != other.notes) return false
        if (status != other.status) return false
        if (start != other.start) return false
        if (isTonight != other.isTonight) return false
        if (stopDate != other.stopDate) return false
        if (dueDate != other.dueDate) return false
        if (areaId != other.areaId) return false
        if (projectId != other.projectId) return false
        if (headingId != other.headingId) return false
        if (cachedTags != other.cachedTags) return false
        if (checklistItemsCount != other.checklistItemsCount) return false
        if (openChecklistItemsCount != other.openChecklistItemsCount) return false
        if (googleTaskId != other.googleTaskId) return false
        if (googleTaskListId != other.googleTaskListId) return false
        if (creationDate != other.creationDate) return false
        if (calendarColor != other.calendarColor) return false
        if (calendarDisplayName != other.calendarDisplayName) return false
        if (eventStartMillis != other.eventStartMillis) return false
        if (isAllDay != other.isAllDay) return false
        if (priority != other.priority) return false
        if (trashed != other.trashed) return false
        if (startDate != other.startDate) return false
        if (modificationDate != other.modificationDate) return false
        if (sortOrder != other.sortOrder) return false
        if (checklist != other.checklist) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type
        result = 31 * result + title.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + status
        result = 31 * result + start
        result = 31 * result + isTonight.hashCode()
        result = 31 * result + (stopDate?.hashCode() ?: 0)
        result = 31 * result + (dueDate?.hashCode() ?: 0)
        result = 31 * result + (areaId?.hashCode() ?: 0)
        result = 31 * result + (projectId?.hashCode() ?: 0)
        result = 31 * result + (headingId?.hashCode() ?: 0)
        result = 31 * result + cachedTags.hashCode()
        result = 31 * result + checklistItemsCount
        result = 31 * result + openChecklistItemsCount
        result = 31 * result + (googleTaskId?.hashCode() ?: 0)
        result = 31 * result + (googleTaskListId?.hashCode() ?: 0)
        result = 31 * result + creationDate.hashCode()
        result = 31 * result + (calendarColor ?: 0)
        result = 31 * result + (calendarDisplayName?.hashCode() ?: 0)
        result = 31 * result + (eventStartMillis?.hashCode() ?: 0)
        result = 31 * result + isAllDay.hashCode()
        result = 31 * result + priority
        result = 31 * result + trashed.hashCode()
        result = 31 * result + (startDate?.hashCode() ?: 0)
        result = 31 * result + modificationDate.hashCode()
        result = 31 * result + sortOrder
        result = 31 * result + checklist.hashCode()
        return result
    }
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
