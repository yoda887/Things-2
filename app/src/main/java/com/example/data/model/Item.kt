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

/**
 * Преобразует секцию UI/Домена во внутреннее значение `start` для Item.
 */
fun TaskSection.toStartVal(): Int {
    return when (this) {
        TaskSection.INBOX -> Item.START_INBOX
        TaskSection.TODAY -> Item.START_TODAY
        TaskSection.ANYTIME -> Item.START_ANYTIME
        TaskSection.UPCOMING -> Item.START_ANYTIME // Anytime и Upcoming делят один статус
        TaskSection.SOMEDAY -> Item.START_SOMEDAY
    }
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
    val type: Int = TYPE_TASK, // TYPE_*
    val title: String = "",
    val notes: String = "",
    val status: Int = STATUS_OPEN, // STATUS_*
    val start: Int = START_INBOX, // START_*
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
    val priority: Int = PRIORITY_NONE, // PRIORITY_*
    val trashed: Boolean = false,
    val startDate: Long? = null,
    val modificationDate: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
) {
    @get:Ignore
    val isCompleted: Boolean
        get() = status == Item.STATUS_COMPLETED

    /** Закрыта — выполнена или отменена: в открытых списках её нет, её место в «Журнале». */
    @get:Ignore
    val isClosed: Boolean
        get() = status == Item.STATUS_COMPLETED || status == Item.STATUS_CANCELLED

    /** Заголовок внутри проекта: группирует задачи проекта, сам задачей не является */
    @get:Ignore
    val isHeading: Boolean
        get() = type == TYPE_HEADING

    companion object {
        // type
        const val TYPE_TASK = 0
        const val TYPE_PROJECT = 1
        const val TYPE_HEADING = 2
        const val TYPE_TEMPLATE = 3

        // status
        const val STATUS_OPEN = 0
        const val STATUS_CANCELLED = 2
        const val STATUS_COMPLETED = 3

        // start — раздел задачи без даты
        const val START_INBOX = 0
        const val START_TODAY = 1
        const val START_ANYTIME = 2
        const val START_SOMEDAY = 3

        // priority
        const val PRIORITY_NONE = 0
        const val PRIORITY_LOW = 1
        const val PRIORITY_MEDIUM = 2
        const val PRIORITY_HIGH = 3
    }

    @get:Ignore
    val name: String
        get() = title

    /**
     * Секция задачи на момент обращения.
     * Для классификации больших списков используйте [DayBounds.sectionOf] — он не пересчитывает
     * границы дня на каждый элемент.
     */
    @get:Ignore
    val section: TaskSection
        get() = DayBounds.now().sectionOf(this)

    /**
     * Determines if the task belongs to Inbox category.
     */
    @get:Ignore
    val isInbox: Boolean
        get() = type == Item.TYPE_TASK && start == Item.START_INBOX && startDate == null && dueDate == null && projectId == null

    /**
     * Determines if the task belongs to Today category.
     * Для пакетной фильтрации используйте [DayBounds.isToday].
     */
    @get:Ignore
    val isToday: Boolean
        get() = DayBounds.now().isToday(this)

    /**
     * Determines if the task belongs to Upcoming category.
     * Для пакетной фильтрации используйте [DayBounds.isUpcoming].
     */
    @get:Ignore
    val isUpcoming: Boolean
        get() = DayBounds.now().isUpcoming(this)

    /**
     * Determines if the task belongs to Anytime category.
     * Для пакетной фильтрации используйте [DayBounds.isAnytime].
     */
    @get:Ignore
    val isAnytime: Boolean
        get() = DayBounds.now().isAnytime(this)

    /**
     * Determines if the task belongs to Someday category.
     * Для пакетной фильтрации используйте [DayBounds.isSomeday].
     */
    @get:Ignore
    val isSomeday: Boolean
        get() = DayBounds.now().isSomeday(this)

    @get:Ignore
    val tags: List<String>
        get() = com.example.domain.tag.TagTitles.parse(cachedTags)
}

/**
 * Границы текущего дня, вычисленные один раз для классификации целого списка задач.
 * Позволяет отфильтровать список без создания [java.util.Calendar] на каждый элемент.
 *
 * Экземпляр фиксирует момент создания, поэтому для актуальной классификации
 * создавайте новый через [DayBounds.now] на каждый пересчёт списка.
 */
class DayBounds private constructor(
    /** Начало сегодняшнего дня (00:00:00.000) */
    val todayStart: Long,
    /** Последняя миллисекунда сегодняшнего дня */
    val endOfToday: Long,
    /** Последняя миллисекунда дня «сегодня + [DUE_SOON_DAYS]» */
    private val dueSoonCutoff: Long
) {

    /**
     * Дедлайн считается «горящим», если до него осталось не больше [DUE_SOON_DAYS] дней.
     * Просроченные дедлайны тоже попадают под это правило.
     */
    fun isDueSoon(dueDate: Long?): Boolean = dueDate != null && dueDate <= dueSoonCutoff

    /**
     * Задача относится к категории «Сегодня»: помещена туда вручную,
     * имеет горящий дедлайн или дату старта не позже конца сегодняшнего дня.
     */
    fun isToday(item: Item): Boolean {
        if (item.type != Item.TYPE_TASK || item.isClosed) return false
        if (item.start == Item.START_TODAY) return true
        if (isDueSoon(item.dueDate)) return true
        val startDate = item.startDate ?: return false
        return startDate <= endOfToday
    }

    /**
     * Задача относится к категории «Предстоящие»: запланирована на будущее
     * и при этом не имеет горящего дедлайна.
     */
    fun isUpcoming(item: Item): Boolean {
        if (item.type != Item.TYPE_TASK || item.isClosed) return false
        if (isToday(item)) return false
        val startDate = item.startDate ?: return false
        return startDate > endOfToday
    }

    /**
     * Задача относится к категории «Когда-нибудь» (Anytime): её можно выполнять сейчас,
     * но она не в Inbox, не в Someday и не запланирована на будущее.
     */
    fun isAnytime(item: Item): Boolean {
        if (item.type != Item.TYPE_TASK || item.isClosed) return false
        if (isToday(item)) return false
        if (item.start == Item.START_INBOX && item.startDate == null && item.projectId == null) return false
        if (item.start == Item.START_SOMEDAY) return false

        val startDate = item.startDate
        if (startDate != null) return startDate <= endOfToday
        return item.start == Item.START_ANYTIME || item.start == Item.START_INBOX || item.projectId != null
    }

    /**
     * Задача отложена в «Когда-нибудь потом» (Someday).
     */
    fun isSomeday(item: Item): Boolean =
        item.type == Item.TYPE_TASK && !item.isClosed && !isToday(item) && item.start == Item.START_SOMEDAY

    /**
     * Секция, в которую попадает задача с учётом дедлайна и даты старта.
     */
    fun sectionOf(item: Item): TaskSection {
        // Горящий дедлайн перекрывает всё остальное и поднимает задачу в «Сегодня»
        if (isDueSoon(item.dueDate)) return TaskSection.TODAY

        val startDate = item.startDate
        if (startDate != null) {
            return if (startDate > endOfToday) TaskSection.UPCOMING else TaskSection.TODAY
        }
        return when (item.start) {
            Item.START_INBOX -> TaskSection.INBOX
            Item.START_TODAY -> TaskSection.TODAY
            Item.START_ANYTIME -> TaskSection.ANYTIME
            Item.START_SOMEDAY -> TaskSection.SOMEDAY
            else -> TaskSection.INBOX
        }
    }

    companion object {
        /** Горизонт «горящего» дедлайна в днях */
        const val DUE_SOON_DAYS = 3

        /**
         * Вычисляет границы дня для текущего момента. Создаёт ровно один [java.util.Calendar].
         */
        fun now(): DayBounds {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis

            // Конец дня считаем как «следующая полночь минус миллисекунда»,
            // чтобы не промахнуться в сутках с переводом часов
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val endOfToday = cal.timeInMillis - 1

            cal.add(java.util.Calendar.DAY_OF_YEAR, DUE_SOON_DAYS)
            val dueSoonCutoff = cal.timeInMillis - 1

            return DayBounds(todayStart, endOfToday, dueSoonCutoff)
        }
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
