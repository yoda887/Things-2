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
