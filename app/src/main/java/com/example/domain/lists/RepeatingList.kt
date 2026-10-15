package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.RecurrenceRule

/**
 * «Повторяющиеся» — особый список, как в Things: повторяющиеся задачи и проекты (шаблоны) и их открытые копии.
 * - Группы: без проекта и области — вверху, остальные — под своим проектом или областью.
 * - Внутри группы — сначала задачи, потом проекты; каждые по дню появления, ближайшие раньше (у шаблона — день
 *   его ближайшей копии, у копии — её день старта); затем копии без даты («В любое время», «Когда-нибудь»);
 *   в конце — шаблоны, которые ждут (после выполнения), на паузе или закончились.
 * Закреплено тестами RepeatTest.
 */
object RepeatingList {

    /** Шаблон задачи — строкой списка (задачи внутри шаблона проекта — нет: они видны на его экране) */
    fun includes(item: Item): Boolean = item.type == Item.TYPE_TEMPLATE && item.repeatTemplateId == null && !item.trashed

    /** Открытая копия повторяющейся задачи — с любой датой: сегодня, вечером, в любое время, когда-нибудь, позже */
    fun includesCopy(item: Item): Boolean =
        item.type == Item.TYPE_TASK && item.repeatTemplateId != null && !item.isClosed && !item.trashed

    /** Открытая копия повторяющегося проекта — с любой датой */
    fun includesProjectCopy(item: Item): Boolean =
        item.type == Item.TYPE_PROJECT && item.repeatTemplateId != null && !item.isClosed && !item.trashed

    /** Шаблон проекта — строкой проекта */
    fun includesProject(item: Item): Boolean = item.type == Item.TYPE_PROJECT_TEMPLATE && !item.trashed

    /**
     * День появления: у шаблона — день его ближайшей копии (раньше дня повтора на «срок заранее»), у копии —
     * её день старта; null — даты нет (шаблон ждёт, на паузе, закончился; копия без даты).
     */
    fun dayOf(item: Item, rules: Map<String, RecurrenceRule>, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long? {
        if (!item.isRepeatTemplate) return item.startDate
        val stored = rules[item.id]?.takeIf { it.isActive && it.nextGenerationDate > 0 } ?: return null
        val rule = com.example.domain.repeat.RepeatRule.decode(stored.rrule) ?: return stored.nextGenerationDate
        val occurrence = java.time.Instant.ofEpochMilli(stored.nextGenerationDate).atZone(zone).toLocalDate()
        return com.example.domain.repeat.RepeatPlan.appearsOn(rule, occurrence).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /**
     * Порядок: с датой — по дате; копии без даты; шаблоны без следующего повтора — в конце. Всё на сегодня и
     * раньше (просроченные копии) — один день «сегодня»: в нём «Сегодня» выше «Сегодня вечером».
     */
    fun comparator(
        rules: Map<String, RecurrenceRule>,
        todayStart: Long = com.example.data.model.DayBounds.now().todayStart,
        endOfToday: Long = com.example.data.model.DayBounds.now().endOfToday
    ): Comparator<Item> = compareBy<Item>(
        { item ->
            when {
                dayOf(item, rules) != null -> 0
                !item.isRepeatTemplate -> 1
                else -> 2
            }
        },
        { dayOf(it, rules)?.let { day -> if (day <= endOfToday) todayStart else day } ?: Long.MAX_VALUE },
        // В тот же день «Сегодня» — выше «Сегодня вечером»
        { if (it.isTonight) 1 else 0 },
        { it.sortOrder }
    )

    fun order(
        items: List<ItemWithChecklist>,
        rules: Map<String, RecurrenceRule>,
        todayStart: Long = com.example.data.model.DayBounds.now().todayStart,
        endOfToday: Long = com.example.data.model.DayBounds.now().endOfToday
    ): List<ItemWithChecklist> {
        val byItem = comparator(rules, todayStart, endOfToday)
        return items.sortedWith { a, b -> byItem.compare(a.item, b.item) }
    }

    /**
     * Группы «по месту». Строками проектов стоят шаблоны проектов и открытые копии проектов ([items] — все записи);
     * [rows] сливает их с задачами группы в общем порядке.
     */
    fun groups(
        templates: List<ItemWithChecklist>,
        rules: Map<String, RecurrenceRule>,
        projects: List<Item>,
        areas: List<Area>,
        items: List<Item> = emptyList()
    ): List<PlaceGroups.Group> = PlaceGroups.group(
        order(templates, rules),
        projects,
        areas,
        projectRows = items.filter { includesProject(it) || includesProjectCopy(it) }.sortedWith(comparator(rules)),
        areaHeaderOnlyWithOwnItems = true
    )

    /**
     * Строки группы: сначала задачи, потом проекты — отдельно, как в Things; каждые — в порядке [comparator]
     * (задачи без проекта и области — вверху списка)
     */
    fun rows(
        group: PlaceGroups.Group,
        rules: Map<String, RecurrenceRule>,
        todayStart: Long = com.example.data.model.DayBounds.now().todayStart,
        endOfToday: Long = com.example.data.model.DayBounds.now().endOfToday
    ): List<Any> = order(group.tasks, rules, todayStart, endOfToday) + projectRowsOf(group, rules, todayStart, endOfToday)

    /** Проекты группы в порядке [comparator] */
    fun projectRowsOf(
        group: PlaceGroups.Group,
        rules: Map<String, RecurrenceRule>,
        todayStart: Long = com.example.data.model.DayBounds.now().todayStart,
        endOfToday: Long = com.example.data.model.DayBounds.now().endOfToday
    ): List<Item> = group.projectRows.sortedWith(comparator(rules, todayStart, endOfToday))

    /**
     * Шаблоны в «Предстоящих», как в Things: действующий шаблон стоит на день, когда появится его следующая
     * копия (раньше дня повтора на «срок заранее»), если этот день позже сегодняшнего. Дата подставляется
     * только для показа — сам шаблон не меняется.
     */
    fun upcomingPreviews(
        templates: List<ItemWithChecklist>,
        rules: Map<String, RecurrenceRule>,
        endOfToday: Long,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): List<ItemWithChecklist> = templates.mapNotNull { wrapper ->
        if (!includes(wrapper.item) && !includesProject(wrapper.item)) return@mapNotNull null
        val stored = rules[wrapper.item.id]?.takeIf { it.isActive && it.nextGenerationDate > 0 } ?: return@mapNotNull null
        val rule = com.example.domain.repeat.RepeatRule.decode(stored.rrule) ?: return@mapNotNull null
        val occurrence = java.time.Instant.ofEpochMilli(stored.nextGenerationDate).atZone(zone).toLocalDate()
        val appears = com.example.domain.repeat.RepeatPlan.appearsOn(rule, occurrence).atStartOfDay(zone).toInstant().toEpochMilli()
        if (appears <= endOfToday) null else wrapper.copy(item = wrapper.item.copy(startDate = appears))
    }

    /** Шаблон ждёт: на паузе, без правила или без следующего повтора */
    private fun waits(rule: RecurrenceRule?): Boolean = rule == null || !rule.isActive || rule.nextGenerationDate <= 0L
}
