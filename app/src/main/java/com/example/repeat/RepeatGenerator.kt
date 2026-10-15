package com.example.repeat

import com.example.data.local.TaskDao
import com.example.data.model.Item
import com.example.data.model.RecurrenceRule
import com.example.domain.repeat.RepeatPlan
import com.example.domain.repeat.RepeatProjects
import com.example.domain.usecase.task.RepeatCopies
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
 * Создаёт копии повторяющихся задач и проектов по их правилам ([RepeatPlan]): копии, которые пора показать, появляются
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
        // Задачи, добавленные в шаблон проекта или вынесенные из него, — частями шаблона или обычными задачами;
        // копии удалённого шаблона — обычными задачами и проектами
        val items = dao.getAllItemsSync()
        val partFixes = RepeatProjects.normalize(items)
        val fixedIds = partFixes.mapTo(HashSet()) { it.id }
        val fixes = partFixes + com.example.domain.repeat.RepeatControl.orphanCopies(items).filter { it.id !in fixedIds }
        if (fixes.isNotEmpty()) dao.insertItems(fixes)
        for (stored in dao.getAllRecurrenceRules()) {
            val rule = RepeatRule.decode(stored.rrule) ?: continue
            val found = dao.getItemById(stored.templateId)
            // Шаблон удалён насовсем — правило без него больше не нужно (из «Корзины» шаблон ещё вернут)
            if (found == null) {
                dao.deleteRecurrenceRulesOf(stored.templateId)
                continue
            }
            val template = found.takeIf { !it.trashed } ?: continue
            val initial = RepeatState(
                rule = rule,
                nextOccurrence = stored.nextGenerationDate.takeIf { it > 0 }?.let { dateOf(it, zone) },
                generated = stored.generatedCount,
                active = stored.isActive
            )
            var state = initial
            // «После выполнения»: открытых копий не осталось (выполнили, отменили или удалили) — следующий повтор
            if (rule.afterCompletion && state.active && state.nextOccurrence == null) {
                state = com.example.domain.repeat.RepeatControl.afterCopiesClosed(state, dao.getRepeatCopiesOf(template.id), today, zone)
            }
            val (occurrences, next) = RepeatPlan.due(state, today)
            if (occurrences.isEmpty() && next == initial) continue
            repository.inTransaction {
                occurrences.forEach { occurrence -> RepeatCopies.insert(repository, template, rule, occurrence, zone) }
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
