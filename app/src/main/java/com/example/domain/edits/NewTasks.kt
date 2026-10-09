package com.example.domain.edits

import com.example.data.model.Item
import com.example.domain.lists.AreaList
import java.util.Calendar
import java.util.UUID

/** Где создаётся новая задача — от этого зависят её поля. */
sealed interface NewTaskPlace {
    data object Inbox : NewTaskPlace
    data object Today : NewTaskPlace
    /** «Предстоящие»: дата старта; null — по правилу [NewTasks.defaultUpcomingDate] */
    data class Upcoming(val startDate: Long?) : NewTaskPlace
    data object Anytime : NewTaskPlace
    data object Someday : NewTaskPlace
    data class Project(val projectId: String) : NewTaskPlace
    data class Area(val areaId: String) : NewTaskPlace
    data class Tag(val title: String) : NewTaskPlace
}

/**
 * Куда в списке бросили «+»: подзаголовок проекта, вечер «Сегодня», дата раздела «Предстоящих»,
 * раздел области, группа «по месту» (проект или область). Всё, что известно, — уточняет поля задачи.
 */
data class DropSpot(
    val headingId: String? = null,
    val isTonight: Boolean = false,
    val startDate: Long? = null,
    val areaSection: AreaList.Section? = null,
    val groupProject: Item? = null,
    val groupAreaId: String? = null
)

/** Новая задача: правило одно — и для «+», и для «+», брошенного в список. Закреплено тестами EditsTest. */
object NewTasks {

    fun create(place: NewTaskPlace, id: String = UUID.randomUUID().toString(), now: Long = System.currentTimeMillis()): Item {
        val base = Item(id = id, type = Item.TYPE_TASK, title = "", creationDate = now)
        return when (place) {
            NewTaskPlace.Inbox -> base.copy(start = Item.START_INBOX)
            NewTaskPlace.Today -> base.copy(start = Item.START_TODAY, startDate = now)
            is NewTaskPlace.Upcoming -> base.copy(start = Item.START_ANYTIME, startDate = place.startDate)
            NewTaskPlace.Anytime -> base.copy(start = Item.START_ANYTIME)
            NewTaskPlace.Someday -> base.copy(start = Item.START_SOMEDAY)
            is NewTaskPlace.Project -> base.copy(start = Item.START_INBOX, projectId = place.projectId)
            is NewTaskPlace.Area -> base.copy(start = Item.START_ANYTIME, areaId = place.areaId)
            is NewTaskPlace.Tag -> base.copy(start = Item.START_ANYTIME, cachedTags = place.title)
        }
    }

    /** Задача, брошенная «+» в место [spot] списка. */
    fun atDrop(task: Item, spot: DropSpot, now: Long = System.currentTimeMillis()): Item {
        val placed = task.copy(
            headingId = spot.headingId,
            isTonight = spot.isTonight,
            startDate = spot.startDate ?: task.startDate
        )
        return when {
            // Экран области: раздел «Когда-нибудь» и «Планы» задаются свойствами задачи
            spot.areaSection == AreaList.Section.SOMEDAY -> placed.copy(start = Item.START_SOMEDAY)
            spot.areaSection == AreaList.Section.UPCOMING -> placed.copy(startDate = tomorrowNoon(now))
            // Группа «по месту»: задача встаёт в проект или область группы
            spot.groupProject != null -> placed.copy(projectId = spot.groupProject.id, areaId = spot.groupProject.areaId)
            spot.groupAreaId != null -> placed.copy(projectId = null, areaId = spot.groupAreaId)
            else -> placed
        }
    }

    /** «+» на «Предстоящих»: дата самой ранней запланированной задачи, а если таких нет — завтра. */
    fun defaultUpcomingDate(upcomingStartDates: List<Long>, now: Long = System.currentTimeMillis()): Long =
        upcomingStartDates.minOrNull() ?: Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Завтра в полдень — дата задачи, брошенной в «Планы» области. */
    fun tomorrowNoon(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
