package com.example.domain.lists

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/** Проект: какие задачи, его открытые заголовки и порядок задач по заголовкам. Закреплено тестами ProjectScreenTest. */
object ProjectList {

    fun includes(item: Item, projectId: String?): Boolean =
        projectId != null && item.projectId == projectId && !item.isClosed

    /** Открытые заголовки проекта по порядку. */
    fun headings(all: List<Item>, projectId: String?): List<Item> =
        all.filter { projectId != null && it.projectId == projectId && it.status == Item.STATUS_OPEN && !it.trashed }
            .sortedBy { it.sortOrder }

    /**
     * Задачи в порядке экрана: сначала без заголовка, затем под каждым заголовком. Внутри каждой части, как в
     * Things, более поздние ([isLater]) — в её конце: сначала запланированные, по дате старта, за ними «Когда-нибудь»;
     * остальные — в прежнем порядке.
     */
    fun order(tasks: List<ItemWithChecklist>, headings: List<Item>, bounds: DayBounds = DayBounds.now()): List<ItemWithChecklist> {
        val headingIds = headings.map { it.id }
        return tasks.sortedWith(
            compareBy<ItemWithChecklist>(
                { HeadingOrder.groupOf(it.item, headingIds) },
                { laterRank(it.item, bounds) },
                { if (laterRank(it.item, bounds) == LATER_SCHEDULED) it.item.startDate else null }
            )
        )
    }

    /**
     * Более поздняя задача проекта — её прячет переключатель «Скрыть более поздние»: запланирована на дату после
     * сегодня или отложена на «Когда-нибудь».
     */
    fun isLater(item: Item, bounds: DayBounds): Boolean = laterRank(item, bounds) != NOT_LATER

    private const val NOT_LATER = 0
    private const val LATER_SCHEDULED = 1
    private const val LATER_SOMEDAY = 2

    private fun laterRank(item: Item, bounds: DayBounds): Int = when {
        item.start == Item.START_SOMEDAY -> LATER_SOMEDAY
        bounds.isUpcoming(item) -> LATER_SCHEDULED
        else -> NOT_LATER
    }
}
