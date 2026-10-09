package com.example.ui.screens.home.components

import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Экран области — все его правила в одном месте: что на нём показывается, в каком порядке и на какие
 * разделы делится. Пользуются им ViewModel (порядок задач), список экрана (строки), проверка пустого
 * экрана и перетаскивание (раздел задачи). Функции чистые — закреплены тестами AreaScreenTest.
 *
 * Экран: открытые проекты области (завершённые и отменённые — в Logbook), затем её задачи без проекта
 * по разделам — текущие, «Планы», «Когда-нибудь».
 * «Планы» и «Когда-нибудь» прячутся переключателем «Скрыть более поздние».
 */
object AreaScreen {

    /** Раздел задачи на экране области. */
    enum class Section { CURRENT, UPCOMING, SOMEDAY }

    fun sectionOf(item: Item, bounds: DayBounds): Section = when {
        item.start == Item.START_SOMEDAY -> Section.SOMEDAY
        bounds.isUpcoming(item) -> Section.UPCOMING
        else -> Section.CURRENT
    }

    /** Задачи списка экрана: невыполненные задачи области (и её проектов — они идут после своих, см. [order]). */
    fun includes(item: Item, areaId: String?): Boolean =
        areaId != null && item.areaId == areaId && item.type == Item.TYPE_TASK && !item.isCompleted

    /** Задача стоит в области сама, а не в одном из её проектов. */
    fun isOwnTask(item: Item, areaId: String?): Boolean =
        areaId != null && item.areaId == areaId && item.projectId.isNullOrEmpty()

    /** Содержимое экрана, разложенное по разделам. */
    data class Content(
        val projects: List<Item>,
        val current: List<ItemWithChecklist>,
        val upcoming: List<ItemWithChecklist>,
        val someday: List<ItemWithChecklist>
    ) {
        val isEmpty: Boolean get() = projects.isEmpty() && current.isEmpty() && upcoming.isEmpty() && someday.isEmpty()
        val hasLater: Boolean get() = upcoming.isNotEmpty() || someday.isNotEmpty()
    }

    fun content(
        areaId: String?,
        tasks: List<ItemWithChecklist>,
        projects: List<Item>,
        bounds: DayBounds = DayBounds.now()
    ): Content {
        val own = tasks.filter { isOwnTask(it.item, areaId) }
        val bySection = own.groupBy { sectionOf(it.item, bounds) }
        return Content(
            projects = projects.filter { areaId != null && it.areaId == areaId && ListRules.isOpenProject(it) },
            current = bySection[Section.CURRENT].orEmpty(),
            upcoming = bySection[Section.UPCOMING].orEmpty(),
            someday = bySection[Section.SOMEDAY].orEmpty()
        )
    }

    /**
     * Задачи в порядке экрана: свои задачи области по разделам, за ними — задачи её проектов
     * (на экране их нет, но в списке экрана они остаются, как и раньше). Порядок внутри раздела не меняется.
     */
    fun order(tasks: List<ItemWithChecklist>, areaId: String?, bounds: DayBounds = DayBounds.now()): List<ItemWithChecklist> {
        val (own, rest) = tasks.partition { isOwnTask(it.item, areaId) }
        return own.sortedBy { sectionOf(it.item, bounds).ordinal } + rest
    }

    /** Строки списка экрана: проекты, отступ, текущие задачи, затем «Планы», «Когда-нибудь» и переключатель. */
    fun rows(content: Content, isLaterHidden: Boolean): List<Any> = buildList {
        addAll(content.projects)
        val hasTasks = content.current.isNotEmpty() || content.hasLater
        if (content.projects.isNotEmpty() && hasTasks) add(TaskListKeys.AREA_PROJECTS_SPACER)
        addAll(content.current)
        if (content.hasLater) {
            if (!isLaterHidden) {
                if (content.upcoming.isNotEmpty()) {
                    add(TaskListKeys.AREA_UPCOMING_HEADING)
                    addAll(content.upcoming)
                }
                if (content.someday.isNotEmpty()) {
                    add(TaskListKeys.AREA_SOMEDAY_HEADING)
                    addAll(content.someday)
                }
            }
            add(TaskListKeys.AREA_LATER_TOGGLE)
        }
    }
}
