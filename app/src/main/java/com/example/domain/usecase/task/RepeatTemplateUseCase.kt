package com.example.domain.usecase.task

import com.example.data.model.Item
import com.example.data.model.RecurrenceRule
import com.example.domain.repeat.RepeatControl
import com.example.domain.repeat.RepeatProjects
import com.example.domain.repeat.RepeatRule
import com.example.domain.repeat.RepeatState
import com.example.domain.repository.ITaskRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Действия с шаблоном повтора ([RepeatControl]): правило, следующая копия, пауза, возобновить, остановить. */
class RepeatTemplateUseCase @Inject constructor(private val repository: ITaskRepository) {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val today: LocalDate get() = LocalDate.now(zone)

    suspend fun pause(templateId: String) = updateState(templateId) { RepeatControl.paused(it) }

    suspend fun resume(templateId: String) = updateState(templateId) { RepeatControl.resumed(it, today) }

    /** Новое правило и напоминание копий */
    suspend fun updateRule(templateId: String, rule: RepeatRule, reminderTime: Int?) {
        val template = repository.getTask(templateId) ?: return
        val hasOpenCopy = repository.getRepeatCopies(templateId).any { !it.isClosed && !it.trashed }
        repository.inTransaction {
            if (template.reminderTime != reminderTime) {
                repository.insertTask(template.copy(reminderTime = reminderTime, modificationDate = System.currentTimeMillis()))
            }
            updateState(templateId, encodeRule = rule) { RepeatControl.withRule(it, rule, today, hasOpenCopy) }
        }
    }

    /** Остановить: шаблон снова обычная задача (или проект со своими задачами), правило удаляется */
    suspend fun stop(templateId: String) {
        val template = repository.getTask(templateId) ?: return
        val projectItems = if (template.type == Item.TYPE_PROJECT_TEMPLATE) repository.getProjectItems(templateId) else emptyList()
        repository.inTransaction {
            if (template.type == Item.TYPE_PROJECT_TEMPLATE) {
                repository.insertTasks(RepeatProjects.stopped(template, projectItems))
            } else {
                repository.insertTask(RepeatControl.stopped(template))
            }
            repository.deleteRecurrenceRule(templateId)
        }
    }

    /**
     * Отметить повторяющуюся задачу заранее ([RepeatControl.completedEarly]): копия ближайшего повтора
     * сразу выполнена, следующая — по правилу. «После выполнения» с открытой копией — выполняется она.
     */
    suspend fun completeEarly(templateId: String) {
        val stored = repository.getRecurrenceRule(templateId) ?: return
        val state = stored.toState() ?: return
        val template = repository.getTask(templateId) ?: return
        val now = System.currentTimeMillis()
        if (state.rule.afterCompletion) {
            val open = repository.getRepeatCopies(templateId).filter { !it.isClosed && !it.trashed }.maxByOrNull { it.startDate ?: it.creationDate }
            if (open != null) {
                repository.insertTask(open.copy(status = Item.STATUS_COMPLETED, stopDate = now, modificationDate = now))
                return
            }
        }
        val (occurrence, next) = RepeatControl.completedEarly(state, today) ?: return
        repository.inTransaction {
            RepeatCopies.insert(repository, template, state.rule, occurrence, zone, completedAt = now)
            repository.saveRecurrenceRule(stored.withState(next))
        }
    }

    /**
     * Перенести ближайший повтор на день появления [appearOn], как в Things: «после выполнения» — просто
     * переносится; регулярное — «Исключение» ([updateRule] = false: копия этого повтора сразу, на новый день,
     * расписание не меняется) или «Обновить правило» ([RepeatControl.ruleMovedTo]: все повторы — по новому дню).
     * «После выполнения», который ждёт выполнения копии, переносит эту копию.
     */
    suspend fun move(templateId: String, appearOn: LocalDate, updateRule: Boolean) {
        val stored = repository.getRecurrenceRule(templateId) ?: return
        val state = stored.toState() ?: return
        val rule = state.rule
        // «После выполнения» ждёт выполнения копии — переносится сама открытая копия
        if (rule.afterCompletion && state.nextOccurrence == null) {
            val open = repository.getRepeatCopies(templateId).filter { !it.isClosed && !it.trashed }.maxByOrNull { it.startDate ?: it.creationDate } ?: return
            val day = appearOn.atStartOfDay(zone).toInstant().toEpochMilli()
            repository.insertTask(open.copy(
                start = Item.START_ANYTIME,
                startDate = day,
                dueDate = rule.deadlineDaysBefore?.let { appearOn.plusDays(it.toLong()).atStartOfDay(zone).toInstant().toEpochMilli() },
                modificationDate = System.currentTimeMillis()
            ))
            return
        }
        val old = state.nextOccurrence ?: return
        val occurrence = appearOn.plusDays((rule.deadlineDaysBefore ?: 0).toLong())
        when {
            rule.afterCompletion -> repository.saveRecurrenceRule(stored.withState(state.copy(nextOccurrence = occurrence)))
            updateRule -> {
                val moved = RepeatControl.ruleMovedTo(rule, old, occurrence)
                repository.saveRecurrenceRule(stored.copy(rrule = moved.encode()).withState(state.copy(rule = moved, nextOccurrence = occurrence)))
            }
            else -> {
                val template = repository.getTask(templateId) ?: return
                val (copyOccurrence, next) = RepeatControl.nextCopy(state, today) ?: return
                repository.inTransaction {
                    RepeatCopies.insert(repository, template, rule, copyOccurrence, zone, appearOn = appearOn)
                    repository.saveRecurrenceRule(stored.withState(next))
                }
            }
        }
    }

    /** Копия на следующий повтор — заранее; появится в свой день */
    suspend fun createNextCopy(templateId: String) {
        val stored = repository.getRecurrenceRule(templateId) ?: return
        val state = stored.toState() ?: return
        val template = repository.getTask(templateId) ?: return
        val (occurrence, next) = RepeatControl.nextCopy(state, today) ?: return
        repository.inTransaction {
            RepeatCopies.insert(repository, template, state.rule, occurrence, zone)
            repository.saveRecurrenceRule(stored.withState(next))
        }
    }

    private suspend fun updateState(templateId: String, encodeRule: RepeatRule? = null, change: (RepeatState) -> RepeatState) {
        val stored = repository.getRecurrenceRule(templateId) ?: return
        val state = stored.toState() ?: return
        val updated = stored.withState(change(state))
        repository.saveRecurrenceRule(if (encodeRule != null) updated.copy(rrule = encodeRule.encode()) else updated)
    }

    private fun RecurrenceRule.toState(): RepeatState? = RepeatRule.decode(rrule)?.let { rule ->
        RepeatState(
            rule = rule,
            nextOccurrence = nextGenerationDate.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() },
            generated = generatedCount,
            active = isActive
        )
    }

    private fun RecurrenceRule.withState(state: RepeatState) = copy(
        nextGenerationDate = state.nextOccurrence?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L,
        generatedCount = state.generated,
        isActive = state.active
    )
}
