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
}
