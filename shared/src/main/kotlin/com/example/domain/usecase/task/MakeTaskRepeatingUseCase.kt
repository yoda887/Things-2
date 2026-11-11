package com.example.domain.usecase.task

import com.example.data.model.RecurrenceRule
import com.example.domain.repeat.RepeatPlan
import com.example.domain.repeat.RepeatProjects
import com.example.domain.repeat.RepeatRule
import com.example.domain.repeat.RepeatSetup
import com.example.domain.repository.ITaskRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Делает задачу или проект повторяющимися: задача становится шаблоном ([RepeatSetup.template]), проект —
 * шаблоном вместе с открытыми задачами ([RepeatProjects.template]), рядом сохраняется
 * правило с первым повтором ([RepeatPlan.start]); копии создаёт RepeatGenerator — он срабатывает на
 * изменение задач. Берётся задача из базы, а не из редактора: её правки уже сохранены при сворачивании.
 */
class MakeTaskRepeatingUseCase @Inject constructor(private val repository: ITaskRepository) {

    suspend operator fun invoke(taskId: String, rule: RepeatRule, reminderTime: Int?) {
        val task = repository.getTask(taskId) ?: return
        val isProject = RepeatProjects.canRepeat(task)
        if (!isProject && !RepeatSetup.canRepeat(task)) return
        val zone = ZoneId.systemDefault()
        val state = RepeatPlan.start(rule, LocalDate.now(zone))
        val projectItems = if (isProject) repository.getProjectItems(task.id) else emptyList()
        repository.inTransaction {
            if (isProject) {
                repository.insertTasks(RepeatProjects.template(task, projectItems, reminderTime))
            } else {
                repository.insertTask(RepeatSetup.template(task, reminderTime))
            }
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
