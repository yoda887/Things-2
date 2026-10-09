package com.example.ui.screens.home.components

import com.example.data.model.Item

/** Заголовок внутри проекта в плоском списке экрана проекта */
data class ProjectHeadingItem(val heading: Item) {
    val key: String get() = TaskListKeys.HEADING_PREFIX + heading.id
}
