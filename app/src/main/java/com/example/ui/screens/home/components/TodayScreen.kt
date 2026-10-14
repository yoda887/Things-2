package com.example.ui.screens.home.components

import com.example.domain.lists.TodayList
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/** Строки экрана «Сегодня» (правила — в TodayList). Перенос между днём и вечером — в TaskDrops. */
object TodayScreen {

    /**
     * Проекты и задачи дня, затем заголовок «Вечер», вечерние проекты и задачи. Проекты — строками над задачами,
     * как в Things. Пока задачу несут, заголовок виден и без вечерних задач — на него можно перенести задачу в вечер.
     */
    fun rows(
        day: List<ItemWithChecklist>,
        evening: List<ItemWithChecklist>,
        isDragging: Boolean,
        projects: Pair<List<Item>, List<Item>> = emptyList<Item>() to emptyList()
    ): List<Any> = buildList {
        addAll(projects.first)
        addAll(day)
        if (evening.isNotEmpty() || projects.second.isNotEmpty() || isDragging) {
            add(TaskListKeys.EVENING_HEADER)
            addAll(projects.second)
            addAll(evening)
        }
    }

    fun isEmpty(
        day: List<ItemWithChecklist>,
        evening: List<ItemWithChecklist>,
        isDragging: Boolean,
        projects: Pair<List<Item>, List<Item>> = emptyList<Item>() to emptyList()
    ): Boolean = day.isEmpty() && evening.isEmpty() && projects.first.isEmpty() && projects.second.isEmpty() && !isDragging
}
