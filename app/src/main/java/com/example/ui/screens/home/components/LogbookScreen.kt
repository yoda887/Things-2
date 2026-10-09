package com.example.ui.screens.home.components

import com.example.data.model.Item

/** «Журнал»: выполненные элементы. Закреплено тестами SimpleScreensTest. */
object LogbookScreen {
    fun includes(item: Item): Boolean = item.isCompleted
}
