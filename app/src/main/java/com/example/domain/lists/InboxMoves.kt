package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Задачи, ушедшие из «Входящих», как в Things: получили дату, проект, область, «Когда-нибудь» — и во «Входящих»
 * не стоят. Они остаются там бледными строками под плашкой «N завдань переміщено з «Вхідних»», уже в новом
 * виде, пока не нажмут «OK». Выполненные и удалённые задачи переехавшими не считаются, а вернувшаяся во
 * «Входящие» задача снова стоит в списке как обычная.
 */
object InboxMoves {

    /** Задачи во «Входящих» — по id */
    fun inboxIds(tasks: List<ItemWithChecklist>): Set<String> =
        tasks.filter { InboxList.includes(it.item) }.mapTo(HashSet()) { it.item.id }

    /**
     * Новый набор переехавших: к [moved] добавляются задачи, что были во «Входящих» ([previousInbox]), а в [tasks]
     * уже не там; выбывают вернувшиеся во «Входящие», удалённые, выполненные и исчезнувшие.
     */
    fun update(moved: Set<String>, previousInbox: Set<String>, tasks: List<ItemWithChecklist>): Set<String> {
        val byId = tasks.associateBy { it.item.id }
        val inbox = inboxIds(tasks)
        return (moved + (previousInbox - inbox)).filterTo(LinkedHashSet()) { id ->
            id !in inbox && byId[id]?.item?.let { stillOpen(it) } == true
        }
    }

    /** Переехавшие задачи для показа во «Входящих» — кроме тех, что уже стоят в списке (раскрытая в редакторе) */
    fun ghosts(moved: Set<String>, tasks: List<ItemWithChecklist>, shown: List<ItemWithChecklist>): List<ItemWithChecklist> {
        if (moved.isEmpty()) return emptyList()
        val shownIds = shown.mapTo(HashSet()) { it.item.id }
        return tasks.filter { it.item.id in moved && it.item.id !in shownIds && stillOpen(it.item) }
            .sortedBy { it.item.sortOrder }
    }

    private fun stillOpen(item: Item): Boolean = !item.trashed && !ListRules.isClosed(item)
}
