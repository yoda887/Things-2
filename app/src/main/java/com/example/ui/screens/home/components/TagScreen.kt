package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Экран тега — все его правила в одном месте: открытые проекты с тегом, открытые задачи с тегом,
 * затем закрытые задачи с тегом (Logbook, свежие сверху). Пользуются им ViewModel (какие задачи
 * в списке экрана), список экрана (строки) и проверка пустого экрана. Закреплены тестами TagScreenTest.
 */
object TagScreen {

    fun hasTag(item: Item, tag: String): Boolean = item.tags.contains(tag)

    /** Задача списка экрана: открытая, не удалённая задача с тегом. */
    fun isActiveTask(item: Item, tag: String): Boolean =
        item.type == Item.TYPE_TASK && !item.trashed && !ListRules.isClosed(item) && hasTag(item, tag)

    data class Content(
        val projects: List<Item>,
        val active: List<ItemWithChecklist>,
        val logbook: List<ItemWithChecklist>
    ) {
        val isEmpty: Boolean get() = projects.isEmpty() && active.isEmpty() && logbook.isEmpty()
    }

    /**
     * @param tasks задачи списка экрана (из ViewModel). Тег у них не перепроверяется: раскрытая
     * в редакторе задача остаётся на месте, даже если тег с неё только что сняли
     * @param allTasks все задачи — из них берётся Logbook тега
     */
    fun content(tag: String?, tasks: List<ItemWithChecklist>, allTasks: List<ItemWithChecklist>, projects: List<Item>): Content {
        val title = tag.orEmpty()
        return Content(
            projects = projects.filter { ListRules.isOpenProject(it) && hasTag(it, title) },
            active = tasks.filter { !it.item.trashed && !ListRules.isClosed(it.item) },
            logbook = allTasks
                .filter { !it.item.trashed && ListRules.isClosed(it.item) && hasTag(it.item, title) }
                .sortedByDescending { it.item.stopDate ?: it.item.modificationDate }
        )
    }

    /** Строки списка экрана: проекты, отступ, открытые задачи, затем Logbook. */
    fun rows(content: Content): List<Any> = buildList {
        addAll(content.projects)
        if (content.projects.isNotEmpty() && content.active.isNotEmpty()) add(TaskListKeys.AREA_PROJECTS_SPACER)
        addAll(content.active)
        addAll(content.logbook)
    }
}
