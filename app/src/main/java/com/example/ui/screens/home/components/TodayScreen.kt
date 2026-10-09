package com.example.ui.screens.home.components

import com.example.domain.lists.TodayList
import com.example.data.model.ItemWithChecklist

/** Строки экрана «Сегодня» (правила — в TodayList). Перенос между днём и вечером — в TaskDropPlacement. */
object TodayScreen {

    /**
     * Задачи дня, затем заголовок «Вечер» и вечерние задачи. Пока задачу несут, заголовок виден
     * и без вечерних задач — на него можно перенести задачу в вечер.
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
