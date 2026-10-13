package com.example.domain.repeat

import com.example.data.model.ChecklistItem
import com.example.data.model.Item
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Состояние повтора шаблона: правило, дата следующего повтора ([nextOccurrence]; null — повторов пока нет:
 * «после выполнения» ждёт, когда отметят текущую копию, а у регулярного правила повторы закончились),
 * сколько копий уже создано и не на паузе ли он.
 */
data class RepeatState(
    val rule: RepeatRule,
    val nextOccurrence: LocalDate?,
    val generated: Int = 0,
    val active: Boolean = true
)

/** Какие копии создать и каким станет состояние повтора. Закреплено тестами RepeatTest. */
object RepeatPlan {

    /** Пропущенных повторов (приложение долго не запускали) создаётся не больше этого — остальные не копятся */
    const val MAX_CATCH_UP = 31

    /**
     * Новый повтор: «после выполнения» — первая копия сразу, сегодня (как в Things); регулярный — в
     * ближайший подходящий день начиная с сегодняшнего.
     */
    fun start(rule: RepeatRule, today: LocalDate): RepeatState = RepeatState(
        rule = rule,
        nextOccurrence = if (rule.afterCompletion) today else RepeatDates.nextOccurrence(rule, today)
    )

    /** День, когда копия повтора [occurrence] появляется: раньше на «срок заранее», если он задан. */
    fun appearsOn(rule: RepeatRule, occurrence: LocalDate): LocalDate =
        occurrence.minusDays((rule.deadlineDaysBefore ?: 0).toLong())

    /**
     * Повторы, копии которых пора создать на [today] (появляются не позже сегодня), и новое состояние.
     * Регулярное правило создаёт копии на все подошедшие даты — пропущенные дни копятся, как в Things
     * (не больше [MAX_CATCH_UP]); «после выполнения» — одну копию и ждёт её выполнения.
     */
    fun due(state: RepeatState, today: LocalDate): Pair<List<LocalDate>, RepeatState> {
        if (!state.active) return emptyList<LocalDate>() to state
        val rule = state.rule
        val result = mutableListOf<LocalDate>()
        var next = state.nextOccurrence
        var generated = state.generated
        while (next != null && !appearsOn(rule, next).isAfter(today) && result.size < MAX_CATCH_UP) {
            if (rule.endCount != null && generated >= rule.endCount) { next = null; break }
            if (rule.endDate != null && next.isAfter(rule.endDate)) { next = null; break }
            result += next
            generated++
            next = if (rule.afterCompletion) null else RepeatDates.nextOccurrence(rule, next.plusDays(1))
        }
        if (next != null && rule.endCount != null && generated >= rule.endCount) next = null
        return result to state.copy(nextOccurrence = next, generated = generated)
    }

    /** «После выполнения»: копию отметили [completedOn] — следующий повтор через интервал правила. */
    fun completed(state: RepeatState, completedOn: LocalDate): RepeatState {
        if (!state.rule.afterCompletion || state.nextOccurrence != null) return state
        val rule = state.rule
        if (rule.endCount != null && state.generated >= rule.endCount) return state
        return state.copy(nextOccurrence = RepeatDates.afterCompletion(rule, completedOn))
    }

    /**
     * Копия шаблона на день повтора [occurrence]: название, заметки, теги, место, приоритет и напоминание —
     * из шаблона; появляется в день [appearsOn] (в «Сегодня», когда он наступил), срок — в день повтора,
     * если правило его задаёт. Чек-лист — свежий, ничего не отмечено.
     */
    fun copyOf(
        template: Item,
        templateChecklist: List<ChecklistItem>,
        rule: RepeatRule,
        occurrence: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Long = System.currentTimeMillis(),
        newId: () -> String = { UUID.randomUUID().toString() }
    ): Pair<Item, List<ChecklistItem>> {
        val id = newId()
        val copy = template.copy(
            id = id,
            type = Item.TYPE_TASK,
            status = Item.STATUS_OPEN,
            start = Item.START_ANYTIME,
            isTonight = false,
            startDate = appearsOn(rule, occurrence).atStartOfDay(zone).toInstant().toEpochMilli(),
            dueDate = rule.deadlineDaysBefore?.let { occurrence.atStartOfDay(zone).toInstant().toEpochMilli() },
            stopDate = null,
            trashed = false,
            googleTaskId = null,
            googleTaskListId = null,
            creationDate = now,
            modificationDate = now,
            repeatTemplateId = template.id
        )
        val checklist = templateChecklist.map { it.copy(id = newId(), itemId = id, isCompleted = false) }
        return copy to checklist
    }
}
