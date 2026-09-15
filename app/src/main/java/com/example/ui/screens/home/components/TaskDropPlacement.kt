package com.example.ui.screens.home.components

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.ui.screens.home.ActiveScreen
import java.util.Calendar

internal const val MS_PER_DAY = 24 * 3600 * 1000L

/**
 * Куда встанет перетаскиваемая задача, если сейчас принять перенос на цель [targetId].
 *
 * Функция чистая: зависит только от переданных данных, ничего не меняет и не знает ни про жест,
 * ни про раскладку списка. Всё, что приходит из UI, — это направление жеста [movingDown]
 * (его знает только геометрия LazyColumn) и состав экрана.
 *
 * Главное правило, которое здесь соблюдается: секции на экранах — это фильтры и группировки
 * поверх общего списка, упорядоченного по sortOrder глобально, поэтому элементы одной секции
 * лежат в списке не подряд, а вперемешку с чужими. Место вставки поэтому всегда ищется
 * по КРАЮ нужной секции ([findInsertionIndexForSection]), а не по первому элементу
 * с подходящей датой или признаком.
 *
 * @param list текущий порядок задач (общий плоский список экрана)
 * @param draggedId идентификатор перетаскиваемой задачи
 * @param targetId ключ цели: идентификатор задачи либо заголовок из [TaskListKeys]
 * @param movingDown жест идёт вниз по списку
 * @param upcomingDayStarts начала суток дней-заголовков экрана «Предстоящие», по порядку
 * @param bounds границы сегодняшнего дня для классификации секций экрана сферы
 * @return новый порядок списка либо null, если перенос принимать не нужно
 */
fun planTaskDrop(
    list: List<ItemWithChecklist>,
    draggedId: String,
    targetId: String,
    screen: ActiveScreen,
    movingDown: Boolean,
    upcomingDayStarts: List<Long>,
    bounds: DayBounds = DayBounds.now(),
): List<ItemWithChecklist>? {
    val fromIndex = list.indexOfFirst { it.item.id == draggedId }
    if (fromIndex == -1) return null

    return when {
        targetId == TaskListKeys.EVENING_HEADER -> planEveningDrop(list, fromIndex)

        targetId == TaskListKeys.MAIN_HEADER -> planMainHeaderDrop(list, fromIndex)

        screen == ActiveScreen.UPCOMING && targetId.startsWith(TaskListKeys.DAY_HEADER_PREFIX) ->
            planDayDrop(list, fromIndex, targetId, movingDown, upcomingDayStarts)

        screen == ActiveScreen.UPCOMING && targetId.startsWith(TaskListKeys.MONTH_HEADER_PREFIX) ->
            planMonthDrop(list, fromIndex, targetId, movingDown, upcomingDayStarts)

        else -> planSwap(list, fromIndex, targetId, screen, bounds)
    }
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

/** Заголовок дня на экране «Предстоящие»: задача переезжает в соседний день */
private fun planDayDrop(
    list: List<ItemWithChecklist>,
    fromIndex: Int,
    targetId: String,
    movingDown: Boolean,
    upcomingDayStarts: List<Long>,
): List<ItemWithChecklist>? {
    val timestamp = targetId.substringAfter(TaskListKeys.DAY_HEADER_PREFIX).toLongOrNull() ?: return null

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
    targetId: String,
    movingDown: Boolean,
    upcomingDayStarts: List<Long>,
): List<ItemWithChecklist>? {
    val monthTimestamp = targetId.substringAfter(TaskListKeys.MONTH_HEADER_PREFIX).toLongOrNull() ?: return null

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
    screen: ActiveScreen,
    bounds: DayBounds,
): List<ItemWithChecklist>? {
    val toIndex = list.indexOfFirst { it.item.id == targetId }
    if (toIndex == -1 || toIndex == fromIndex) return null

    val next = list.toMutableList()
    var moved = next.removeAt(fromIndex)
    val hoveredTask = list.firstOrNull { it.item.id == targetId } ?: return null

    // На экране сферы секции задаются свойствами самой задачи, и обмен их не меняет: перенос
    // через заголовок только переставил бы задачу внутри её же секции в произвольное место.
    if (screen == ActiveScreen.AREA_DETAIL &&
        hoveredTask.areaSection(bounds) != moved.areaSection(bounds)
    ) {
        return null
    }

    // Статус «Вечер» меняется только на экране «Сегодня», дата — только на «Предстоящих»
    if (screen == ActiveScreen.TODAY && hoveredTask.item.isTonight != moved.item.isTonight) {
        moved = moved.copyWithTonight(hoveredTask.item.isTonight)
    }
    if (screen == ActiveScreen.UPCOMING && hoveredTask.item.startDate != moved.item.startDate) {
        moved = moved.copyWithStartDate(hoveredTask.item.startDate)
    }

    next.add(toIndex, moved)
    return next
}

// ─── Private helpers ──────────────────────────────────────────────────────────

/**
 * Индекс вставки на краю целевой секции общего списка задач.
 *
 * Элементы секции лежат в списке не подряд, поэтому край ищется по самой секции, а не по
 * первому элементу с подходящей датой. Тот же приём, что и [findInsertionIndexForArea]
 * для проектов.
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

/** Секция задачи на экране сферы — та же классификация, что и в rememberFlattenedList */
private fun ItemWithChecklist.areaSection(bounds: DayBounds): Int = when {
    item.start == 3 -> 2
    bounds.isUpcoming(item) -> 1
    else -> 0
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

internal fun ItemWithChecklist.copyWithTonight(isTonight: Boolean) = ItemWithChecklist(
    item = item.copy(isTonight = isTonight),
    checklist = checklist,
)

internal fun ItemWithChecklist.copyWithStartDate(startDate: Long?) = ItemWithChecklist(
    item = item.copy(startDate = startDate),
    checklist = checklist,
)
