package com.example.domain.tag

import com.example.data.model.Item

/**
 * Фильтр списка по тегам, как в Things («Filter by Tag»): выбрано несколько тегов — остаются задачи и проекты, у
 * которых есть **все** выбранные (свои или унаследованные от проекта и области, [TagInheritance]); «Без тега»
 * ([untagged]) — только те, у кого тегов нет вовсе. «Без тега» и теги вместе не выбираются: одно снимает другое
 * ([toggle], [toggleUntagged]). Закреплено тестами TagFilterTest.
 */
data class TagFilter(
    /** Выбранные теги — в порядке выбора */
    val tags: List<String> = emptyList(),
    val untagged: Boolean = false
) {
    /** Фильтр что-то отбирает */
    val isActive: Boolean get() = untagged || tags.isNotEmpty()

    /** Задача или проект проходит фильтр */
    fun matches(item: Item, inheritance: TagInheritance.Index = TagInheritance.NONE): Boolean = when {
        untagged -> inheritance.tagsOf(item).isEmpty()
        tags.isEmpty() -> true
        else -> tags.all { inheritance.has(item, it) }
    }

    /** Выбран ли тег [title] (без учёта регистра) */
    fun has(title: String): Boolean {
        val key = TagTitles.key(title)
        return tags.any { TagTitles.key(it) == key }
    }

    /** Тег [title] выбран или снят; выбор тега снимает «Без тега» */
    fun toggle(title: String): TagFilter {
        val key = TagTitles.key(title)
        return if (has(title)) copy(tags = tags.filterNot { TagTitles.key(it) == key })
        else TagFilter(tags = tags + title, untagged = false)
    }

    /** «Без тега» выбран или снят; выбор снимает теги */
    fun toggleUntagged(): TagFilter = if (untagged) NONE else TagFilter(untagged = true)

    companion object {
        /** Фильтра нет — видно всё */
        val NONE = TagFilter()
    }
}
