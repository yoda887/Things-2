package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Порядок задач проекта по заголовкам: сначала задачи без заголовка, затем каждый заголовок со своими
 * задачами. Группа задачи — позиция её заголовка в [headingIds]; -1 — без заголовка (в том числе если
 * её заголовок в архиве или удалён). Закреплено тестами ProjectHeadingsTest.
 */
object HeadingOrder {

    fun groupOf(task: Item, headingIds: List<String>): Int = headingIds.indexOf(task.headingId)

    /** Задачи в порядке экрана: по группам, внутри группы — в прежнем порядке. */
    fun <T> orderByHeading(tasks: List<T>, headingIds: List<String>, item: (T) -> Item): List<T> =
        tasks.sortedBy { groupOf(item(it), headingIds) }

    /** Задачи группы [headingId] (null — без заголовка) в порядке списка. */
    fun tasksOf(tasks: List<ItemWithChecklist>, headingId: String?, headingIds: List<String>): List<ItemWithChecklist> =
        if (headingId == null) tasks.filter { groupOf(it.item, headingIds) == -1 }
        else tasks.filter { it.item.headingId == headingId }
}
