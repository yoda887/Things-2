package com.example.domain.usecase.task

import com.example.data.model.RecurrenceRule
import com.example.domain.repeat.RepeatPlan
import com.example.domain.repeat.RepeatRule
import com.example.domain.repeat.RepeatSetup
import com.example.domain.repository.ITaskRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Делает задачу повторяющейся: задача становится шаблоном ([RepeatSetup.template]), рядом сохраняется
 * правило с первым повтором ([RepeatPlan.start]); копии создаёт RepeatGenerator — он срабатывает на
 * изменение задач. Берётся задача из базы, а не из редактора: её правки уже сохранены при сворачивании.
 */
class MakeTaskRepeatingUseCase @Inject constructor(private val repository: ITaskRepository) {

    suspend operator fun invoke(taskId: String, rule: RepeatRule, reminderTime: Int?) {
        val task = repository.getTask(taskId) ?: return
        if (!RepeatSetup.canRepeat(task)) return
        val zone = ZoneId.systemDefault()
        val state = RepeatPlan.start(rule, LocalDate.now(zone))
        repository.inTransaction {
            repository.insertTask(RepeatSetup.template(task, reminderTime))
            repository.saveRecurrenceRule(
                RecurrenceRule(
                    templateId = task.id,
                    rrule = rule.encode(),
                    nextGenerationDate = state.nextOccurrence?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L
                )
            )
        }
    }
}
