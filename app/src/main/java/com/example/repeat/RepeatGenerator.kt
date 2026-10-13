package com.example.repeat

import com.example.data.local.TaskDao
import com.example.data.model.Item
import com.example.data.model.RecurrenceRule
import com.example.domain.repeat.RepeatPlan
import com.example.domain.repeat.RepeatRule
import com.example.domain.repeat.RepeatState
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Создаёт копии повторяющихся задач по их правилам ([RepeatPlan]): копии, которые пора показать, появляются
 * в «Сегодня». Для «после выполнения» — назначает следующий повтор, когда текущую копию закрыли (выполнили,
 * отменили или удалили). Запуск повторный безопасен: состояние правила сохраняется вместе с копиями,
 * и уже созданное второй раз не создаётся.
 */
@Singleton
class RepeatGenerator @Inject constructor(
    private val dao: TaskDao,
    private val repository: ITaskRepository
) {
    private val mutex = Mutex()

    suspend fun run(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()) = mutex.withLock {
        for (stored in dao.getAllRecurrenceRules()) {
            val rule = RepeatRule.decode(stored.rrule) ?: continue
            val template = dao.getItemById(stored.templateId)?.takeIf { !it.trashed } ?: continue
            val initial = RepeatState(
                rule = rule,
                nextOccurrence = stored.nextGenerationDate.takeIf { it > 0 }?.let { dateOf(it, zone) },
                generated = stored.generatedCount,
                active = stored.isActive
            )
            var state = initial
            // «После выполнения»: открытых копий не осталось — следующий повтор от дня, когда закрыли последнюю
            if (rule.afterCompletion && state.active && state.nextOccurrence == null) {
                val copies = dao.getRepeatCopiesOf(template.id)
                val done = copies.filter { it.isClosed || it.trashed }
                if (copies.isNotEmpty() && copies.none { !it.isClosed && !it.trashed }) {
                    val last = done.maxOf { it.stopDate ?: it.modificationDate }
                    state = RepeatPlan.completed(state, dateOf(last, zone))
                }
            }
            val (occurrences, next) = RepeatPlan.due(state, today)
            if (occurrences.isEmpty() && next == initial) continue
            val checklist = dao.getChecklistItemsByItemId(template.id)
            repository.inTransaction {
                occurrences.forEach { occurrence ->
                    val (copy, copyChecklist) = RepeatPlan.copyOf(template, checklist, rule, occurrence, zone)
                    repository.insertTask(copy, copyChecklist)
                    if (copy.tags.isNotEmpty()) repository.setItemTagsByTitles(copy.id, copy.tags)
                }
                dao.upsertRecurrenceRule(stored.withState(next, zone))
            }
        }
    }

    private fun RecurrenceRule.withState(state: RepeatState, zone: ZoneId) = copy(
        nextGenerationDate = state.nextOccurrence?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L,
        generatedCount = state.generated,
        isActive = state.active
    )

    private fun dateOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
