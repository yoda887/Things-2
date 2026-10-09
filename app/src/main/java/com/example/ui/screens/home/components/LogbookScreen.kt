package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * «Журнал», как Logbook в Things: выполненные и отменённые задачи и завершённые и отменённые проекты
 * (строкой проекта), свежие сверху — по дате закрытия. Порядок задаётся датой, поэтому вручную
 * задачи здесь не переставляются. Закреплено тестами SimpleScreensTest.
 */
object LogbookScreen {

    /** Задача в «Журнале»: закрытая (выполненная или отменённая) и не удалённая. */
    fun includes(item: Item): Boolean = item.type == Item.TYPE_TASK && !item.trashed && ListRules.isClosed(item)

    /** Проект в «Журнале»: завершённый или отменённый и не удалённый. */
    fun includesProject(project: Item): Boolean =
        project.type == Item.TYPE_PROJECT && !project.trashed && ListRules.isClosed(project)

    /** Когда закрыт: дата выполнения или отмены, а если её нет — последнего изменения. */
    fun closedAt(item: Item): Long = item.stopDate ?: item.modificationDate

    /** Задачи в порядке экрана — свежие сверху. */
    fun order(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.sortedByDescending { closedAt(it.item) }

    /** Строки экрана: задачи и проекты вместе, свежие сверху. */
    fun rows(tasks: List<ItemWithChecklist>, projects: List<Item>): List<Any> =
        (tasks.map { it to closedAt(it.item) } + projects.filter(::includesProject).map { it to closedAt(it) })
            .sortedByDescending { it.second }
            .map { it.first }
}
