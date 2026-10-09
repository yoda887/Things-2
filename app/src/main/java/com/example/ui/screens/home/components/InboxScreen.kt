package com.example.ui.screens.home.components

import com.example.data.model.Item

/**
 * «Входящие»: задачи без даты, срока, проекта и раздела «Когда» — ещё не разобранные — и не выполненные.
 * Закреплено тестами SimpleScreensTest.
 */
object InboxScreen {
    fun includes(item: Item): Boolean = item.isInbox && !item.isCompleted
}
