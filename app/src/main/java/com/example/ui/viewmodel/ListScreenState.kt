package com.example.ui.viewmodel

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import kotlinx.serialization.Serializable

/** Экран приложения: главный, умные списки, проект, область, тег, поиск. */
@Serializable
enum class ActiveScreen {
    HOME, INBOX, TODAY, UPCOMING, ANYTIME, SOMEDAY, LOGBOOK, PROJECT_DETAIL, AREA_DETAIL, SEARCH, TAG_DETAIL
}

/** Прогресс проекта: сколько задач выполнено из скольких. */
data class ProjectProgress(
    val completed: Int = 0,
    val total: Int = 0
)

/** Состояние экрана списка, которое ViewModel отдаёт экрану. */
data class ThingsCategoryListState(
    val screen: ActiveScreen = ActiveScreen.INBOX,
    val project: Item? = null,
    val area: Area? = null,
    val tag: Tag? = null,
    val inlineExpandedTaskId: String? = null,
    val selectedTagFilter: String? = null,
    val allTags: Set<String> = emptySet(),
    val displayTasks: List<ItemWithChecklist> = emptyList(),
    val calendarEvents: List<Item> = emptyList(),
    val allSavedTags: List<String> = emptyList(),
    val allSavedTagObjects: List<Tag> = emptyList(),
    val areas: List<Area> = emptyList(),
    val projects: List<Item> = emptyList(),
    val highlightedTaskId: String? = null,
    val allTasks: List<ItemWithChecklist> = emptyList(),
    val projectProgressMap: Map<String, ProjectProgress> = emptyMap(),
    val isSelectionMode: Boolean = false,
    val selectedTaskIds: Set<String> = emptySet(),
    // Текст запроса экрана поиска
    val searchQuery: String = "",
    // Активные заголовки проекта по порядку (экран проекта)
    val headings: List<Item> = emptyList()
)
