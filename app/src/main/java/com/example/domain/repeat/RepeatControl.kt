package com.example.domain.repeat

import com.example.data.model.Item
import java.time.LocalDate

/**
 * Действия с шаблоном повтора, как в Things: изменить правило, создать следующую копию заранее, пауза,
 * возобновить, остановить. Закреплено тестами RepeatTest.
 */
object RepeatControl {

    /** Пауза: копии не создаются, пока повтор не возобновят. */
    fun paused(state: RepeatState): RepeatState = state.copy(active = false)

    /**
     * Возобновить: пропущенные за паузу дни не копятся — регулярное правило продолжается с ближайшего дня
     * начиная с сегодня.
     */
    fun resumed(state: RepeatState, today: LocalDate): RepeatState {
        val next = state.nextOccurrence
        val skipMissed = !state.rule.afterCompletion && next != null && next.isBefore(today)
        return state.copy(
            active = true,
            nextOccurrence = if (skipMissed) RepeatDates.nextOccurrence(state.rule, today) else next
        )
    }

    /** Остановить: шаблон снова становится обычной задачей «В любое время», копии больше не создаются. */
    fun stopped(template: Item, now: Long = System.currentTimeMillis()): Item =
        template.copy(type = Item.TYPE_TASK, start = Item.START_ANYTIME, modificationDate = now)

    /**
     * «Создать следующую копию» — заранее, на день следующего повтора: регулярное правило переходит к
     * повтору после него; «после выполнения» без назначенного повтора — на интервал от сегодня, и снова
     * ждёт выполнения. Null — повторов больше нет (закончились).
     */
    fun nextCopy(state: RepeatState, today: LocalDate): Pair<LocalDate, RepeatState>? {
        val rule = state.rule
        if (rule.endCount != null && state.generated >= rule.endCount) return null
        val occurrence = state.nextOccurrence
            ?: if (rule.afterCompletion) RepeatDates.afterCompletion(rule, today) else return null
        val next = if (rule.afterCompletion) null else RepeatDates.nextOccurrence(rule, occurrence.plusDays(1))
        val generated = state.generated + 1
        val ended = rule.endCount != null && generated >= rule.endCount
        return occurrence to state.copy(nextOccurrence = if (ended) null else next, generated = generated)
    }

    /**
     * Новое правило: следующий повтор считается заново от сегодня ([RepeatPlan.start]), созданные копии
     * учитываются; «после выполнения» с ещё открытой копией ждёт её выполнения.
     */
    fun withRule(state: RepeatState, rule: RepeatRule, today: LocalDate, hasOpenCopy: Boolean): RepeatState {
        val started = RepeatPlan.start(rule, today)
        return state.copy(
            rule = rule,
            nextOccurrence = if (rule.afterCompletion && hasOpenCopy) null else started.nextOccurrence
        )
    }

    /**
     * Отметить повторяющуюся задачу заранее, как в Things: копия ближайшего повтора создаётся сразу
     * выполненной, следующая — по правилу. Регулярное — повтор после неё ([nextCopy]); «после выполнения» —
     * на интервал от сегодня. Null — повторов больше нет.
     */
    fun completedEarly(state: RepeatState, today: LocalDate): Pair<LocalDate, RepeatState>? {
        val rule = state.rule
        if (!rule.afterCompletion) return nextCopy(state, today)
        if (rule.endCount != null && state.generated >= rule.endCount) return null
        val occurrence = state.nextOccurrence ?: today
        val generated = state.generated + 1
        val ended = rule.endCount != null && generated >= rule.endCount
        return occurrence to state.copy(
            nextOccurrence = if (ended) null else RepeatDates.afterCompletion(rule, today),
            generated = generated
        )
    }

    /**
     * «Обновить правило» при переносе: расписание сдвигается так, чтобы повтор [old] стал [new] — день недели,
     * число или N-й день недели месяца (и месяц у ежегодных) берутся от новой даты, отсчёт «каждые N» — с неё.
     * Остальные дни правила не меняются: еженедельное «пн и чт», перенесённое с пн на вт, — «вт и чт».
     */
    fun ruleMovedTo(rule: RepeatRule, old: LocalDate, new: LocalDate): RepeatRule {
        val on = when (rule.unit) {
            RepeatUnit.DAY -> rule.on
            RepeatUnit.WEEK -> {
                val days = rule.on.mapNotNull { (it.day as? RepeatDay.Weekday)?.dayOfWeek }.ifEmpty { listOf(rule.start.dayOfWeek.value) }
                (days - old.dayOfWeek.value + new.dayOfWeek.value).distinct().sorted().map { RepeatOn(RepeatDay.Weekday(it)) }
            }
            RepeatUnit.MONTH, RepeatUnit.YEAR -> {
                val yearly = rule.unit == RepeatUnit.YEAR
                val on = rule.on.ifEmpty { listOf(RepeatOn(RepeatDay.OfMonth(rule.start.dayOfMonth), if (yearly) rule.start.monthValue else null)) }
                val index = on.indexOfFirst { entry ->
                    RepeatDates.dayMatches(entry.day, old) && (!yearly || (entry.month ?: rule.start.monthValue) == old.monthValue)
                }.coerceAtLeast(0)
                val day = when (val d = on[index].day) {
                    is RepeatDay.OfMonth ->
                        RepeatDay.OfMonth(if (d.day == RepeatDay.LAST && new.dayOfMonth == new.lengthOfMonth()) RepeatDay.LAST else new.dayOfMonth)
                    is RepeatDay.Weekday -> RepeatDay.Weekday(
                        new.dayOfWeek.value,
                        if (d.ordinal == RepeatDay.LAST && new.plusWeeks(1).month != new.month) RepeatDay.LAST else (new.dayOfMonth - 1) / 7 + 1
                    )
                }
                on.toMutableList().also { it[index] = RepeatOn(day, if (yearly) new.monthValue else on[index].month) }
            }
        }
        return rule.copy(start = new, on = on)
    }
}
