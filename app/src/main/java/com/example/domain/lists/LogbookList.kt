package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import java.util.Calendar

/**
 * «Журнал», как Logbook в Things: выполненные и отменённые задачи и завершённые и отменённые проекты,
 * свежие сверху — по дате закрытия, разделами «Сегодня», «Вчера» и по месяцам. Порядок задаётся датой,
 * поэтому вручную задачи здесь не переставляются. Закреплено тестами SimpleScreensTest и LogbookListTest.
 */
object LogbookList {

    /** Задача в «Журнале»: закрытая (выполненная или отменённая) и не удалённая. */
    fun includes(item: Item): Boolean = item.type == Item.TYPE_TASK && !item.trashed && ListRules.isClosed(item)

    /** Проект в «Журнале»: завершённый или отменённый и не удалённый. */
    fun includesProject(project: Item): Boolean =
        project.type == Item.TYPE_PROJECT && !project.trashed && ListRules.isClosed(project)

    /** Когда закрыт: дата выполнения или отмены, а если её нет — последнего изменения. */
    fun closedAt(item: Item): Long = item.stopDate ?: item.modificationDate

    /** Задачи в порядке экрана — свежие сверху. */
    fun order(tasks: List<ItemWithChecklist>): List<ItemWithChecklist> = tasks.sortedByDescending { closedAt(it.item) }

    /** Задачи ([ItemWithChecklist]) и проекты ([Item]) вместе, свежие сверху. */
    fun entries(tasks: List<ItemWithChecklist>, projects: List<Item>): List<Any> =
        (tasks.map { it to closedAt(it.item) } + projects.filter(::includesProject).map { it to closedAt(it) })
            .sortedByDescending { it.second }
            .map { it.first }

    /** Раздел «Журнала»: сегодня, вчера или месяц ([year], [month] — месяц по Calendar, с 0). */
    enum class Period { TODAY, YESTERDAY, MONTH }

    data class Section(val period: Period, val year: Int, val month: Int, val start: Long, val entries: List<Any>)

    /**
     * [entries] по разделам: закрытое сегодня, вчера, затем по месяцам — свежие сверху, пустых разделов нет.
     * Месяц, в который попали сегодня и вчера, содержит только то, что закрыто раньше.
     */
    fun sections(tasks: List<ItemWithChecklist>, projects: List<Item>, now: Calendar = Calendar.getInstance()): List<Section> {
        val todayStart = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterdayStart = (now.clone() as Calendar).apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val cal = Calendar.getInstance()
        fun closedAtOf(entry: Any): Long = when (entry) {
            is ItemWithChecklist -> closedAt(entry.item)
            is Item -> closedAt(entry)
            else -> 0L
        }
        return entries(tasks, projects)
            .groupBy { entry ->
                val at = closedAtOf(entry)
                when {
                    at >= todayStart -> Triple(Period.TODAY, 0, 0)
                    at >= yesterdayStart -> Triple(Period.YESTERDAY, 0, 0)
                    else -> {
                        cal.timeInMillis = at
                        Triple(Period.MONTH, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    }
                }
            }
            .map { (key, list) ->
                val start = when (key.first) {
                    Period.TODAY -> todayStart
                    Period.YESTERDAY -> yesterdayStart
                    Period.MONTH -> cal.apply { clear(); set(key.second, key.third, 1) }.timeInMillis
                }
                Section(key.first, key.second, key.third, start, list)
            }
    }
}
