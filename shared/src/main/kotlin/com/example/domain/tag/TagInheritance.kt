package com.example.domain.tag

import com.example.data.model.Area
import com.example.data.model.Item

/**
 * Наследование тегов, как в Things: задача несёт свои теги, теги своего проекта и теги своей области (своей
 * или области проекта); проект — свои и теги своей области. Поэтому задача области с тегом «Дом» видна на экране
 * тега «Дом» и проходит фильтр по нему, хотя у самой задачи этого тега нет. Хранятся только свои теги
 * (`cachedTags`), унаследованные вычисляются здесь. Закреплено тестами TagInheritanceTest.
 */
object TagInheritance {

    /** Теги проектов и областей по id — один раз на весь список. */
    class Index(projects: List<Item>, areas: List<Area>) {
        private val projectsById = projects.associateBy { it.id }
        private val areasById = areas.associateBy { it.id }

        /** Свои и унаследованные теги — без повторов (без учёта регистра), свои первыми. */
        fun tagsOf(item: Item): List<String> {
            val project = item.projectId?.let { projectsById[it] }
            val area = (item.areaId ?: project?.areaId)?.let { areasById[it] }
            if (project == null && area == null) return item.tags
            return TagTitles.clean(item.tags + project?.tags.orEmpty() + area?.tags.orEmpty())
        }

        /**
         * Теги строки фильтра списка — как в Things, только те, что есть у его задач (свои и унаследованные):
         * по любому из них фильтр что-то оставит. Без повторов (без учёта регистра), в порядке появления.
         */
        fun filterTags(items: List<Item>): Set<String> =
            TagTitles.clean(items.flatMap { tagsOf(it) }).toCollection(LinkedHashSet())

        /** Есть ли у задачи или проекта тег [title] — свой или унаследованный. */
        fun has(item: Item, title: String): Boolean {
            val key = TagTitles.key(title)
            return tagsOf(item).any { TagTitles.key(it) == key }
        }
    }

    /** Без проектов и областей — только свои теги. */
    val NONE = Index(emptyList(), emptyList())
}
