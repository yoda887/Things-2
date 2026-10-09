package com.example.domain.edits

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.lists.AreaList
import com.example.domain.lists.HeadingOrder
import com.example.domain.lists.PlaceGroups
import java.util.Calendar

internal const val MS_PER_DAY = 24 * 3600 * 1000L

/** На что бросают перетаскиваемую задачу. */
sealed interface DropTarget {
    /** Другая задача списка */
    data class Task(val id: String) : DropTarget
    /** Заголовок экрана «Сегодня» */
    data object MainHeader : DropTarget
    /** Заголовок вечерней секции «Сегодня» */
    data object EveningHeader : DropTarget
    /** Заголовок дня «Предстоящих»; [dayStart] — начало суток */
    data class DayHeader(val dayStart: Long) : DropTarget
    /** Заголовок месяца (раздела) «Предстоящих»; [monthStart] — начало раздела */
    data class MonthHeader(val monthStart: Long) : DropTarget
    /** Подзаголовок проекта */
    data class Heading(val headingId: String) : DropTarget
}

/** Список, в котором перетаскивают, — со своими правилами переноса. */
sealed interface DropList {
    data object Today : DropList
    /** «Предстоящие»; [dayStarts] — начала суток дней-заголовков по порядку */
    data class Upcoming(val dayStarts: List<Long>) : DropList
    /** Проект; [headingIds] — его подзаголовки по порядку */
    data class Project(val headingIds: List<String>) : DropList
    /** Область: разделы задаются свойствами задачи по границам дня [bounds] */
    data class Area(val bounds: DayBounds) : DropList
    /** «В любое время» и «Когда-нибудь»: группы по проектам и областям */
    data object ByPlace : DropList
    /** «Журнал»: порядок по дате закрытия, переставлять нечего */
    data object Logbook : DropList
    /** Прочие списки: простой обмен */
    data object Plain : DropList
}

/**
 * Куда встанет перетаскиваемая задача — правила переноса для всех списков. Закреплено тестами
 * TaskDropPlacementTest и TaskDropsTest.
 *
 * Главное правило: секции экранов — фильтры и группировки поверх общего списка, упорядоченного
 * по sortOrder глобально, поэтому элементы одной секции лежат в списке не подряд, а вперемешку
 * с чужими. Место вставки поэтому всегда ищется по КРАЮ нужной секции ([findInsertionIndexForSection]),
 * а не по первому элементу с подходящей датой или признаком.
 */
object TaskDrops {

    /**
     * Новый порядок списка [list], если принять перенос задачи [draggedId] на [target], либо null,
     * если перенос принимать не нужно. [movingDown] — направление жеста (его знает только UI).
     */
    fun plan(
        list: List<ItemWithChecklist>,
        draggedId: String,
        target: DropTarget,
        dropList: DropList,
        movingDown: Boolean,
    ): List<ItemWithChecklist>? {
        val fromIndex = list.indexOfFirst { it.item.id == draggedId }
        if (fromIndex == -1) return null

        return when {
            target == DropTarget.MainHeader -> planMainHeaderDrop(list, fromIndex)
            // Проект: задачи сгруппированы по подзаголовкам, перенос меняет и подзаголовок задачи
            dropList is DropList.Project -> planProjectDrop(list, draggedId, target, movingDown, dropList.headingIds)
            target == DropTarget.EveningHeader -> planEveningDrop(list, fromIndex)
            target is DropTarget.DayHeader ->
                if (dropList is DropList.Upcoming) planDayDrop(list, fromIndex, target.dayStart, movingDown, dropList.dayStarts) else null
            target is DropTarget.MonthHeader ->
                if (dropList is DropList.Upcoming) planMonthDrop(list, fromIndex, target.monthStart, movingDown, dropList.dayStarts) else null
            target is DropTarget.Task -> planSwap(list, fromIndex, target.id, dropList)
            else -> null
        }
    }

    /**
     * Задача, перетаскиваемая группой вслед за ведущей [leader]: встаёт в её вечер и дату, а в проекте
     * ([inProject]) — и под её подзаголовок. На других экранах задачи группы могут быть из разных
     * проектов — их подзаголовки не трогаем.
     */
    fun follower(task: ItemWithChecklist, leader: Item, inProject: Boolean): ItemWithChecklist {
        var updated = task
        if (inProject && updated.item.headingId != leader.headingId) {
            updated = updated.copy(item = updated.item.copy(headingId = leader.headingId))
        }
        if (updated.item.isTonight != leader.isTonight) updated = updated.copyWithTonight(leader.isTonight)
        if (updated.item.startDate != leader.startDate) updated = updated.copyWithStartDate(leader.startDate)
        return updated
    }

    /**
     * Заголовок «Вечер» на экране «Сегодня»: задача меняет принадлежность секции на противоположную.
     * Направление берётся из самой задачи, а не из жеста: на этот заголовок сверху приходят только
     * дневные задачи, снизу — только вечерние.
     */
    private fun planEveningDrop(list: List<ItemWithChecklist>, fromIndex: Int): List<ItemWithChecklist> {
        val next = list.toMutableList()
        var moved = next.removeAt(fromIndex)
        val wasTonight = moved.item.isTonight

        moved = moved.copyWithTonight(!wasTonight)

        val insertAt = if (wasTonight) {
            // Вверх через заголовок: задача становится дневной и встаёт последней в дневной секции,
            // прямо над заголовком
            next.indexOfLast { !it.item.isTonight } + 1
        } else {
            // Вниз через заголовок: задача становится вечерней и встаёт первой в вечерней секции
            next.indexOfFirst { it.item.isTonight }.takeIf { it != -1 } ?: next.size
        }

        next.add(insertAt, moved)
        return next
    }

    /** Главный заголовок экрана «Сегодня»: вечерняя задача поднимается в самое начало дневной секции */
    private fun planMainHeaderDrop(list: List<ItemWithChecklist>, fromIndex: Int): List<ItemWithChecklist>? {
        val next = list.toMutableList()
        var moved = next.removeAt(fromIndex)
        if (!moved.item.isTonight) return null

        moved = moved.copyWithTonight(false)
        next.add(0, moved)
        return next
    }

    /**
     * Проект: перенос задачи [draggedId] на задачу или подзаголовок.
     *
     * - На задачу — встаёт на её место и в её группу.
     * - Вниз через подзаголовок — первой задачей этого подзаголовка.
     * - Вверх через подзаголовок своей группы — последней задачей предыдущей группы
     *   (предыдущего подзаголовка либо задач без подзаголовка).
     *
     * Список — в порядке экрана проекта ([HeadingOrder.orderByHeading]).
     */
    private fun planProjectDrop(
        list: List<ItemWithChecklist>,
        draggedId: String,
        target: DropTarget,
        movingDown: Boolean,
        headingIds: List<String>,
    ): List<ItemWithChecklist>? {
        val ordered = HeadingOrder.orderByHeading(list, headingIds) { it.item }
        val fromIndex = ordered.indexOfFirst { it.item.id == draggedId }
        if (fromIndex == -1) return null
        val next = ordered.toMutableList()
        var moved = next.removeAt(fromIndex)
        val movedGroup = HeadingOrder.groupOf(moved.item, headingIds)

        return when (target) {
            is DropTarget.Heading -> {
                val headingIndex = headingIds.indexOf(target.headingId)
                if (headingIndex == -1) return null
                if (movingDown && movedGroup >= headingIndex) return null
                if (!movingDown && movedGroup != headingIndex) return null
                val destGroup = if (movingDown) headingIndex else headingIndex - 1
                val insertAt = if (movingDown) {
                    next.count { HeadingOrder.groupOf(it.item, headingIds) < destGroup }
                } else {
                    next.count { HeadingOrder.groupOf(it.item, headingIds) <= destGroup }
                }
                moved = moved.withHeading(headingIds.getOrNull(destGroup))
                next.add(insertAt, moved)
                next
            }
            is DropTarget.Task -> {
                val toIndex = ordered.indexOfFirst { it.item.id == target.id }
                if (toIndex == -1 || toIndex == fromIndex) return null
                val hovered = ordered[toIndex].item
                moved = moved.withHeading(hovered.headingId?.takeIf { it in headingIds })
                next.add(toIndex, moved)
                next
            }
            else -> null
        }
    }

    /** Заголовок дня на экране «Предстоящие»: задача переезжает в соседний день */
    private fun planDayDrop(
        list: List<ItemWithChecklist>,
        fromIndex: Int,
        timestamp: Long,
        movingDown: Boolean,
        upcomingDayStarts: List<Long>,
    ): List<ItemWithChecklist>? {
        val next = list.toMutableList()
        var moved = next.removeAt(fromIndex)

        val tomorrowStart = upcomingDayStarts.firstOrNull() ?: 0L
        val oldDayStart = moved.item.startDate?.dayStart() ?: tomorrowStart

        // Целевой день зависит от направления: вниз — день заголовка, вверх — предыдущий день
        val targetTimestamp = if (movingDown) {
            timestamp
        } else {
            val currentDayIdx = upcomingDayStarts.indexOfFirst { it.dayStart() == timestamp.dayStart() }
            if (currentDayIdx > 0) upcomingDayStarts[currentDayIdx - 1] else timestamp - MS_PER_DAY
        }

        val clipped = maxOf(targetTimestamp, tomorrowStart)
        val targetDayStart = clipped.dayStart()

        if (oldDayStart == targetDayStart) return null

        // Расположение задачи внутри блока целевого дня:
        // при движении вниз — в начало списка задач дня, при движении вверх — в конец
        val nextDayStart = targetDayStart.plusDays(1)
        val insertAt = findInsertionIndexForSection(
            list = next,
            atStart = movingDown,
            sectionBoundaryMillis = targetDayStart
        ) { item ->
            item.startDate?.let { it >= targetDayStart && it < nextDayStart } == true
        }

        // Копируем точное время соседа, чтобы время не прыгало хаотично
        val referenceTask = if (movingDown) next.getOrNull(insertAt) else next.getOrNull(insertAt - 1)
        val finalStartDate = if (referenceTask != null && referenceTask.item.startDate?.dayStart() == targetDayStart) {
            referenceTask.item.startDate
        } else {
            clipped.atNoon()
        }

        // Временный sortOrder, чтобы список не дёргался при равном startDate
        moved = moved.copyWithStartDate(finalStartDate)
        moved = moved.copy(item = moved.item.copy(sortOrder = if (movingDown) -1 else 99999))

        next.add(insertAt, moved)
        return next
    }

    /** Заголовок месяца на экране «Предстоящие»: задача переезжает в соседний раздел */
    private fun planMonthDrop(
        list: List<ItemWithChecklist>,
        fromIndex: Int,
        monthTimestamp: Long,
        movingDown: Boolean,
        upcomingDayStarts: List<Long>,
    ): List<ItemWithChecklist>? {
        val next = list.toMutableList()
        var moved = next.removeAt(fromIndex)

        // Защита от no-op: если задача уже лежит в разделе этого заголовка, перенос не нужен.
        // Сравнения по (год, месяц) мало: первый раздел после 14-дневного горизонта — не целый
        // месяц, а его остаток, и задача тех же суток, но ещё внутри горизонта, принадлежит
        // разделу «дни». Только для движения вниз: вверх через свой заголовок задача как раз
        // уходит в предыдущий раздел.
        val horizonEndMillis = upcomingDayStarts.lastOrNull()?.plus(MS_PER_DAY) ?: 0L
        val oldStart = moved.item.startDate
        if (movingDown && oldStart != null && oldStart >= horizonEndMillis &&
            oldStart.sameMonthAs(monthTimestamp)
        ) {
            return null
        }

        val targetDateMillis = if (movingDown) {
            // Вниз — в начало месяца или периода (после событий календаря)
            monthTimestamp.atNoon()
        } else {
            // Вверх через заголовок месяца — задача переходит в предшествующий раздел
            val prevDay = Calendar.getInstance().apply {
                timeInMillis = monthTimestamp
                add(Calendar.DAY_OF_MONTH, -1)
            }.timeInMillis.atNoon()
            maxOf(prevDay, upcomingDayStarts.firstOrNull() ?: 0L)
        }

        // Раздел, в который задача попадает по новой дате. Вверх из первого месячного раздела
        // (остаток текущего месяца, его monthTimestamp — конец 14-дневного горизонта) задача
        // уходит внутрь горизонта, то есть в ДЕНЬ, а не в месяц, и край ищется по дневному блоку.
        val insertAt = if (!movingDown && targetDateMillis < horizonEndMillis) {
            val destDayStart = targetDateMillis.dayStart()
            val destNextDayStart = destDayStart.plusDays(1)
            findInsertionIndexForSection(
                list = next,
                atStart = false,
                sectionBoundaryMillis = destDayStart
            ) { item ->
                item.startDate?.let { it >= destDayStart && it < destNextDayStart } == true
            }
        } else {
            findInsertionIndexForSection(
                list = next,
                atStart = movingDown,
                sectionBoundaryMillis = monthTimestamp
            ) { item ->
                val start = item.startDate
                // Задачи того же месяца, но ещё внутри горизонта, принадлежат разделу «дни»
                start != null && start >= horizonEndMillis && start.sameMonthAs(targetDateMillis)
            }
        }

        moved = moved.copyWithStartDate(targetDateMillis)
        moved = moved.copy(item = moved.item.copy(sortOrder = if (movingDown) -1 else 99999))

        next.add(insertAt, moved)
        return next
    }

    /** Обычный обмен двух задач */
    private fun planSwap(
        list: List<ItemWithChecklist>,
        fromIndex: Int,
        targetId: String,
        dropList: DropList,
    ): List<ItemWithChecklist>? {
        val toIndex = list.indexOfFirst { it.item.id == targetId }
        if (toIndex == -1 || toIndex == fromIndex) return null
        // «Журнал» упорядочен по дате закрытия — переставлять там нечего
        if (dropList == DropList.Logbook) return null

        val next = list.toMutableList()
        var moved = next.removeAt(fromIndex)
        val hoveredTask = list[toIndex]

        // В области разделы задаются свойствами самой задачи, и обмен их не меняет: перенос
        // через заголовок только переставил бы задачу внутри её же раздела в произвольное место.
        if (dropList is DropList.Area &&
            AreaList.sectionOf(hoveredTask.item, dropList.bounds) != AreaList.sectionOf(moved.item, dropList.bounds)
        ) {
            return null
        }

        // «В любое время» и «Когда-нибудь» сгруппированы по проектам и областям, а обмен их не меняет: задача
        // перескочила бы в чужую группу и тут же вернулась в свою
        if (dropList == DropList.ByPlace && placeKey(hoveredTask.item) != placeKey(moved.item)) {
            return null
        }

        // Статус «Вечер» меняется только на экране «Сегодня», дата — только на «Предстоящих»
        if (dropList == DropList.Today && hoveredTask.item.isTonight != moved.item.isTonight) {
            moved = moved.copyWithTonight(hoveredTask.item.isTonight)
        }
        if (dropList is DropList.Upcoming && hoveredTask.item.startDate != moved.item.startDate) {
            moved = moved.copyWithStartDate(hoveredTask.item.startDate)
        }

        next.add(toIndex, moved)
        return next
    }

    /** Группа задачи «по месту»: проект, иначе область (см. [PlaceGroups]). */
    private fun placeKey(item: Item): String? =
        item.projectId?.takeIf { it.isNotEmpty() }?.let { "project:$it" }
            ?: item.areaId?.takeIf { it.isNotEmpty() }?.let { "area:$it" }

    /**
     * Индекс вставки на краю целевой секции общего списка задач.
     *
     * Элементы секции лежат в списке не подряд, поэтому край ищется по самой секции, а не по
     * первому элементу с подходящей датой.
     *
     * @param atStart начало секции (перенос вниз) или место сразу за её последним элементом (вверх)
     * @param sectionBoundaryMillis ориентир на случай, когда в целевой секции ещё нет задач
     */
    private fun findInsertionIndexForSection(
        list: List<ItemWithChecklist>,
        atStart: Boolean,
        sectionBoundaryMillis: Long,
        inSection: (Item) -> Boolean,
    ): Int {
        val first = list.indexOfFirst { inSection(it.item) }
        if (first != -1) return if (atStart) first else list.indexOfLast { inSection(it.item) } + 1
        return list.indexOfFirst { wrapper -> wrapper.item.startDate?.let { it >= sectionBoundaryMillis } == true }
            .takeIf { it != -1 } ?: list.size
    }

    private fun Long.dayStart(): Long = Calendar.getInstance().run {
        timeInMillis = this@dayStart
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun Long.plusDays(days: Int): Long = Calendar.getInstance().run {
        timeInMillis = this@plusDays
        add(Calendar.DAY_OF_YEAR, days)
        timeInMillis
    }

    private fun Long.atNoon(): Long = Calendar.getInstance().run {
        timeInMillis = this@atNoon
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun Long.sameMonthAs(other: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = this
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        cal.timeInMillis = other
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }

    private fun ItemWithChecklist.copyWithTonight(isTonight: Boolean) = copy(item = item.copy(isTonight = isTonight))

    private fun ItemWithChecklist.copyWithStartDate(startDate: Long?) = copy(item = item.copy(startDate = startDate))

    private fun ItemWithChecklist.withHeading(headingId: String?): ItemWithChecklist =
        if (item.headingId == headingId) this else copy(item = item.copy(headingId = headingId))
}
