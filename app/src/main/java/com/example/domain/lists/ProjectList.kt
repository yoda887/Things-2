package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/** Проект: какие задачи, его открытые заголовки и порядок задач по заголовкам. Закреплено тестами ProjectScreenTest. */
object ProjectList {

    fun includes(item: Item, projectId: String?): Boolean =
        projectId != null && item.projectId == projectId && !item.isCompleted

    /** Открытые заголовки проекта по порядку. */
    fun headings(all: List<Item>, projectId: String?): List<Item> =
        all.filter { projectId != null && it.projectId == projectId && it.status == Item.STATUS_OPEN && !it.trashed }
            .sortedBy { it.sortOrder }

    /** Задачи в порядке экрана: сначала без заголовка, затем под каждым заголовком. */
    fun order(tasks: List<ItemWithChecklist>, headings: List<Item>): List<ItemWithChecklist> =
        if (headings.isEmpty()) tasks else HeadingOrder.orderByHeading(tasks, headings.map { it.id }) { it.item }
}
