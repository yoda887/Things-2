package com.example.domain.lists

import com.example.data.model.Item

/**
 * «Входящие»: открытые задачи без даты, срока, проекта и раздела «Когда» — ещё не разобранные.
 * Выполненные, отменённые и удалённые сюда не попадают. По этому же правилу считается число
 * у «Входящих» на главном экране. Закреплено тестами SimpleScreensTest.
 */
object InboxList {
    fun includes(item: Item): Boolean = item.isInbox && !item.trashed && !ListRules.isClosed(item)
}
