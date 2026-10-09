package com.example.ui.screens.home.components

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Экран «Сегодня» — его правила в одном месте: какие задачи на нём, деление на день и вечер,
 * строки списка и признак пустого экрана. Перенос между днём и вечером — в TaskDropPlacement.
 * Закреплено тестами TodayScreenTest.
 */
object TodayScreen {

    fun includes(item: Item, bounds: DayBounds): Boolean = bounds.isToday(item)

    /** Задачи дня — не вечерние. */
    fun day(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.filter { !it.item.isTonight }

    /** Задачи «Сегодня вечером». */
    fun evening(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.filter { it.item.isTonight }

    /**
     * Строки экрана: задачи дня, затем заголовок «Вечер» и вечерние задачи. Пока задачу несут, заголовок
     * виден и без вечерних задач — на него можно перенести задачу в вечер.
     */
    fun rows(day: List<ItemWithChecklist>, evening: List<ItemWithChecklist>, isDragging: Boolean): List<Any> = buildList {
        addAll(day)
        if (evening.isNotEmpty() || isDragging) {
            add(TaskListKeys.EVENING_HEADER)
            addAll(evening)
        }
    }

    fun isEmpty(day: List<ItemWithChecklist>, evening: List<ItemWithChecklist>, isDragging: Boolean): Boolean =
        day.isEmpty() && evening.isEmpty() && !isDragging
}
