package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/** «Сегодня»: какие задачи и деление на день и вечер. Закреплено тестами TodayScreenTest. */
object TodayList {

    fun includes(item: Item, bounds: DayBounds): Boolean = bounds.isToday(item)

    /** Задачи дня — не вечерние. */
    fun day(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.filter { !it.item.isTonight }

    /** Задачи «Сегодня вечером». */
    fun evening(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.filter { it.item.isTonight }

    /** События календаря, которые начинаются сегодня. */
    fun events(events: List<Item>, bounds: DayBounds = DayBounds.now()): List<Item> =
        events.filter { event -> event.eventStartMillis?.let { it in bounds.todayStart..bounds.endOfToday } == true }
}
