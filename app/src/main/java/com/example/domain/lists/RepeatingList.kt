package com.example.domain.lists

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.RecurrenceRule

/**
 * «Повторяющиеся» — особый список шаблонов повтора, как в Things: шаблоны без проекта — вверху, остальные —
 * под своим проектом или областью; внутри — по ближайшему повтору, на паузе и закончившиеся — в конце.
 * Шаблоны повторяющихся проектов стоят строками проектов — без области вверху, с областью — под ней.
 * Под шаблоном задачи или проекта — его копии, которые ещё впереди (созданы заранее), по дате.
 * Закреплено тестами RepeatTest.
 */
object RepeatingList {

    /** Шаблон задачи — строкой списка (задачи внутри шаблона проекта — нет: они видны на его экране) */
    fun includes(item: Item): Boolean = item.type == Item.TYPE_TEMPLATE && item.repeatTemplateId == null && !item.trashed

    /** Копия повторяющейся задачи, которая ещё впереди: открыта и появится позже сегодняшнего дня */
    fun includesCopy(item: Item, endOfToday: Long): Boolean =
        item.type == Item.TYPE_TASK && item.repeatTemplateId != null && !item.isClosed && !item.trashed &&
            (item.startDate ?: 0L) > endOfToday

    /** Копия повторяющегося проекта, которая ещё впереди: открыта и начнётся позже сегодняшнего дня */
    fun includesProjectCopy(item: Item, endOfToday: Long): Boolean =
        item.type == Item.TYPE_PROJECT && item.repeatTemplateId != null && !item.isClosed && !item.trashed &&
            (item.startDate ?: 0L) > endOfToday

    /** Шаблон проекта — строкой проекта */
    fun includesProject(item: Item): Boolean = item.type == Item.TYPE_PROJECT_TEMPLATE && !item.trashed

    /**
     * Ближайший повтор — раньше; на паузе и без следующего повтора (закончились, ждут выполнения) — в конце.
     * Копии ([includesCopy]) — сразу под своим шаблоном, по дате; копии без шаблона в списке — в конце.
     */
    fun order(items: List<ItemWithChecklist>, rules: Map<String, RecurrenceRule>): List<ItemWithChecklist> {
        val (copies, templates) = items.partition { it.item.type == Item.TYPE_TASK }
        val byTemplate = copies.sortedBy { it.item.startDate ?: 0L }.groupBy { it.item.repeatTemplateId }
        val sorted = templates.sortedWith(
            compareBy<ItemWithChecklist> { waits(rules[it.item.id]) }
                .thenBy { rules[it.item.id]?.nextGenerationDate ?: Long.MAX_VALUE }
                .thenBy { it.item.sortOrder }
        )
        val templateIds = sorted.mapTo(HashSet()) { it.item.id }
        return buildList {
            sorted.forEach { template ->
                add(template)
                addAll(byTemplate[template.item.id].orEmpty())
            }
            addAll(copies.filter { it.item.repeatTemplateId !in templateIds }.sortedBy { it.item.startDate ?: 0L })
        }
    }

    /**
     * @param items все записи: из них берутся шаблоны проектов и копии проектов, которые ещё впереди
     * ([includesProjectCopy] — для этого нужен [endOfToday]); они стоят строками проектов
     */
    fun groups(
        templates: List<ItemWithChecklist>,
        rules: Map<String, RecurrenceRule>,
        projects: List<Item>,
        areas: List<Area>,
        items: List<Item> = emptyList(),
        endOfToday: Long = Long.MAX_VALUE
    ): List<PlaceGroups.Group> = PlaceGroups.group(
        order(templates, rules),
        projects,
        areas,
        projectRows = projectRows(items, rules, endOfToday),
        areaHeaderOnlyWithOwnItems = true
    )

    /** Шаблоны проектов по ближайшему повтору, под каждым — его копии, которые ещё впереди, по дате */
    fun projectRows(items: List<Item>, rules: Map<String, RecurrenceRule>, endOfToday: Long): List<Item> {
        val copies = items.filter { includesProjectCopy(it, endOfToday) }.sortedBy { it.startDate ?: 0L }.groupBy { it.repeatTemplateId }
        val templates = order(items.filter(::includesProject).map { ItemWithChecklist(it) }, rules).map { it.item }
        val templateIds = templates.mapTo(HashSet()) { it.id }
        return templates.flatMap { listOf(it) + copies[it.id].orEmpty() } +
            copies.filterKeys { it !in templateIds }.values.flatten()
    }

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
