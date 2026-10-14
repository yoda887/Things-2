package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/** «Сегодня»: какие задачи и проекты и деление на день и вечер. Закреплено тестами TodayScreenTest. */
object TodayList {

    fun includes(item: Item, bounds: DayBounds): Boolean = bounds.isToday(item)

    /**
     * Проект на сегодня, как в Things: открытый, не «Когда-нибудь», начат сегодня или раньше (в том числе
     * «Сегодня» без даты) или с горящим дедлайном. Стоит строкой проекта над задачами своей части дня.
     */
    fun includesProject(project: Item, bounds: DayBounds): Boolean {
        if (project.type != Item.TYPE_PROJECT || !ListRules.isOpenProject(project) || project.start == Item.START_SOMEDAY) return false
        if (project.start == Item.START_TODAY || bounds.isDueSoon(project.dueDate)) return true
        val startDate = project.startDate ?: return false
        return startDate <= bounds.endOfToday
    }

    /** Проекты на сегодня: дневные и вечерние ([Item.isTonight]), в порядке списка проектов. */
    fun projects(projects: List<Item>, bounds: DayBounds = DayBounds.now()): Pair<List<Item>, List<Item>> =
        projects.filter { includesProject(it, bounds) }.partition { !it.isTonight }

    /** Задачи дня — не вечерние. */
    fun day(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.filter { !it.item.isTonight }

    /** Задачи «Сегодня вечером». */
    fun evening(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.filter { it.item.isTonight }

    /** События календаря, которые начинаются сегодня. */
    fun events(events: List<Item>, bounds: DayBounds = DayBounds.now()): List<Item> =
        events.filter { event -> event.eventStartMillis?.let { it in bounds.todayStart..bounds.endOfToday } == true }
}
