package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.RecurrenceRule

/**
 * «Повторяющиеся» — особый список шаблонов повтора, как в Things: шаблоны без проекта — вверху, остальные —
 * под своим проектом или областью; внутри — по ближайшему повтору, на паузе и закончившиеся — в конце.
 * Шаблоны повторяющихся проектов стоят строками проектов — без области вверху, с областью — под ней.
 * Закреплено тестами RepeatTest.
 */
object RepeatingList {

    /** Шаблон задачи — строкой списка (задачи внутри шаблона проекта — нет: они видны на его экране) */
    fun includes(item: Item): Boolean = item.type == Item.TYPE_TEMPLATE && item.repeatTemplateId == null && !item.trashed

    /** Шаблон проекта — строкой проекта */
    fun includesProject(item: Item): Boolean = item.type == Item.TYPE_PROJECT_TEMPLATE && !item.trashed

    /** Ближайший повтор — раньше; на паузе и без следующего повтора (закончились, ждут выполнения) — в конце. */
    fun order(templates: List<ItemWithChecklist>, rules: Map<String, RecurrenceRule>): List<ItemWithChecklist> =
        templates.sortedWith(
            compareBy<ItemWithChecklist> { waits(rules[it.item.id]) }
                .thenBy { rules[it.item.id]?.nextGenerationDate ?: Long.MAX_VALUE }
                .thenBy { it.item.sortOrder }
        )

    fun groups(
        templates: List<ItemWithChecklist>,
        rules: Map<String, RecurrenceRule>,
        projects: List<Item>,
        areas: List<Area>,
        projectTemplates: List<Item> = emptyList()
    ): List<PlaceGroups.Group> = PlaceGroups.group(
        order(templates, rules),
        projects,
        areas,
        projectRows = order(projectTemplates.filter(::includesProject).map { ItemWithChecklist(it) }, rules).map { it.item },
        areaHeaderOnlyWithOwnItems = true
    )

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
