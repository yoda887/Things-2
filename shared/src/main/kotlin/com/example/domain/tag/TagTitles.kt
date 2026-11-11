package com.example.domain.tag

import com.example.data.model.Tag
import java.util.UUID

/**
 * Правила работы с названиями тегов. Задача хранит теги строкой названий (`Item.cachedTags`),
 * справочник — отдельными записями [Tag]; связь между ними — только по названию. Поэтому
 * сравнение названий должно быть везде одинаковым: без учёта регистра и пробелов по краям.
 * Иначе один и тот же тег заводится повторно и расходится между задачами.
 */
object TagTitles {

    /** Разделитель в строке тегов задачи. */
    const val SEPARATOR = ", "

    /** Ключ сравнения названий: без регистра и пробелов по краям. */
    fun key(title: String): String = title.trim().lowercase()

    /**
     * Разбирает строку тегов задачи. Понимает и старый формат без пробела после запятой
     * (`"a,b"`), который когда-то сохранялся из раскрытого редактора.
     */
    fun parse(cachedTags: String): List<String> = clean(cachedTags.split(','))

    /** Склеивает названия в строку тегов задачи. */
    fun join(titles: List<String>): String = titles.joinToString(SEPARATOR)

    /** Обрезает пробелы, убирает пустые и повторы (без учёта регистра), сохраняя порядок. */
    fun clean(titles: List<String>): List<String> {
        val seen = HashSet<String>()
        val result = ArrayList<String>(titles.size)
        for (raw in titles) {
            val title = raw.trim()
            if (title.isEmpty()) continue
            if (seen.add(key(title))) result += title
        }
        return result
    }

    /** Тег справочника с таким же названием (без учёта регистра), кроме [exceptId]. */
    fun find(tags: List<Tag>, title: String, exceptId: String? = null): Tag? {
        val k = key(title)
        return tags.firstOrNull { it.id != exceptId && key(it.title) == k }
    }

    /**
     * Сопоставляет названия с тегами справочника.
     * @return теги в порядке [titles] и те из них, которых в справочнике ещё нет, — их нужно создать.
     *   Новым тегам даются порядковые номера после последнего существующего.
     */
    fun resolve(titles: List<String>, existing: List<Tag>): Resolution {
        val byKey = HashMap<String, Tag>()
        for (tag in existing) byKey.putIfAbsent(key(tag.title), tag)
        var nextSortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val resolved = ArrayList<Tag>()
        val created = ArrayList<Tag>()
        for (title in clean(titles)) {
            val k = key(title)
            val tag = byKey[k] ?: Tag(id = UUID.randomUUID().toString(), title = title, sortOrder = nextSortOrder++)
                .also {
                    byKey[k] = it
                    created += it
                }
            resolved += tag
        }
        return Resolution(resolved, created)
    }

    data class Resolution(val tags: List<Tag>, val created: List<Tag>)

    /**
     * Дубли в справочнике: для каждого лишнего тега — тег, который остаётся (с тем же названием,
     * первый по порядку сортировки). Группы (корневые теги) предпочитаются дочерним, чтобы не терять
     * вложенность.
     */
    fun duplicates(tags: List<Tag>): Map<Tag, Tag> {
        val result = LinkedHashMap<Tag, Tag>()
        tags.groupBy { key(it.title) }.values.forEach { group ->
            if (group.size < 2) return@forEach
            val keep = group.sortedWith(
                compareBy<Tag> { if (it.parentId == null) 0 else 1 }.thenBy { it.sortOrder }.thenBy { it.id }
            ).first()
            group.filter { it.id != keep.id }.forEach { result[it] = keep }
        }
        return result
    }

    /**
     * Заменяет в списке названий [oldTitle] на [newTitle] (без учёта регистра) и убирает повторы,
     * если новое название у задачи уже было.
     */
    fun rename(titles: List<String>, oldTitle: String, newTitle: String): List<String> {
        val oldKey = key(oldTitle)
        return clean(titles.map { if (key(it) == oldKey) newTitle else it })
    }

    /** Убирает из списка названий все названия из [removed] (без учёта регистра). */
    fun remove(titles: List<String>, removed: Collection<String>): List<String> {
        val keys = removed.map(::key).toSet()
        return titles.filter { key(it) !in keys }
    }
}
