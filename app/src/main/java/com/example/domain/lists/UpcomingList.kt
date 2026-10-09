package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item

/** «Предстоящие»: какие задачи. Расписание по дням и месяцам — в UpcomingScreen. */
object UpcomingList {
    fun includes(item: Item, bounds: DayBounds): Boolean = bounds.isUpcoming(item)
}
