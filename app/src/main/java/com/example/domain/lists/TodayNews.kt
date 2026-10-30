package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Новое в «Сегодня», как в Things: задачи и проекты, что пришли туда сами — настал их день, появилась копия
 * повтора, — помечены жёлтой точкой, сверху плашка «У вас N нових завдань» с «OK». Что уже видели, помнится
 * между запусками ([known] — id). Что пользователь сам только что поставил на сегодня, новым не считается.
 * Закреплено тестами TodayNewsTest.
 */
object TodayNews {

    /** Изменение не старше этого — сделано только что, на этом устройстве: задачу поставили на сегодня сами */
    const val JUST_NOW_MS = 30_000L

    /** Задачи и проекты в «Сегодня» — по id */
    fun todayIds(items: List<ItemWithChecklist>, bounds: DayBounds): Set<String> =
        items.mapNotNullTo(HashSet()) { wrapper ->
            val item = wrapper.item
            wrapper.item.id.takeIf { !item.trashed && (TodayList.includes(item, bounds) || TodayList.includesProject(item, bounds)) }
        }

    /**
     * Виденное после обновления списка: из [known] остаётся то, что ещё в «Сегодня» (ушедшее и вернувшееся
     * потом — снова новое); добавляется то, что вошло в «Сегодня» с прошлого списка ([previous]) от правки
     * только что ([now]) — кроме свежей копии повтора: её создал повтор, она новая. [previous] — null после
     * запуска: что пришло, пока приложение было закрыто, — новое.
     */
    fun update(
        known: Set<String>,
        previous: Set<String>?,
        items: List<ItemWithChecklist>,
        bounds: DayBounds,
        now: Long
    ): Set<String> {
        val today = todayIds(items, bounds)
        val entered = if (previous == null) emptySet() else today - previous
        val byId = items.associateBy { it.item.id }
        val placed = entered.filter { id -> byId[id]?.item?.let { placedJustNow(it, now) } == true }
        return (known intersect today) + placed
    }

    /** Новое — то, что в «Сегодня», но ещё не видели */
    fun newIds(known: Set<String>, today: Set<String>): Set<String> = today - known

    /**
     * Новые задачи — наверх «Сегодня», один раз: тем, что ещё не поднимали ([raised]), — порядок выше всех задач
     * списка, в их прежнем порядке между собой. Возвращает задачи с новым порядком (сохранить); дальше их можно
     * переставлять, второй раз они не поднимаются.
     */
    fun raiseNew(items: List<ItemWithChecklist>, newIds: Set<String>, raised: Set<String>, bounds: DayBounds): List<Item> {
        val todayTasks = items.map { it.item }.filter { !it.trashed && it.type == Item.TYPE_TASK && TodayList.includes(it, bounds) }
        val toRaise = todayTasks.filter { it.id in newIds && it.id !in raised }.sortedBy { it.sortOrder }
        if (toRaise.isEmpty()) return emptyList()
        val raisingIds = toRaise.mapTo(HashSet()) { it.id }
        val top = todayTasks.filter { it.id !in raisingIds }.minOfOrNull { it.sortOrder } ?: 0
        return toRaise.mapIndexed { index, item -> item.copy(sortOrder = top - toRaise.size + index) }
    }

    private fun placedJustNow(item: Item, now: Long): Boolean {
        val changed = maxOf(item.modificationDate, item.updatedAt)
        if (changed < now - JUST_NOW_MS) return false
        val freshRepeatCopy = item.isRepeatCopy && item.creationDate >= now - JUST_NOW_MS
        return !freshRepeatCopy
    }
}
