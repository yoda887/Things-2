package com.example.domain.usecase.task

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.repeat.RepeatIds
import com.example.domain.repeat.RepeatPlan
import com.example.domain.repeat.RepeatProjects
import com.example.domain.repeat.RepeatRule
import com.example.domain.repository.ITaskRepository
import java.time.LocalDate
import java.time.ZoneId

/**
 * Записывает копию шаблона на день повтора: задачу с чек-листом и тегами ([RepeatPlan.copyOf]) или проект
 * с заголовками и задачами ([RepeatProjects.copyOf]). Вызывать внутри транзакции вместе с новым состоянием правила.
 */
object RepeatCopies {

    /**
     * @param appearOn другой день появления копии задачи — «Исключение» при переносе (срок сдвигается вместе с ним)
     * @param completedAt копия задачи сразу выполнена в это время — повторяющуюся задачу отметили заранее
     */
    suspend fun insert(
        repository: ITaskRepository,
        template: Item,
        rule: RepeatRule,
        occurrence: LocalDate,
        zone: ZoneId,
        appearOn: LocalDate? = null,
        completedAt: Long? = null
    ) {
        // Копия этого дня уже была — новые id, чтобы не затереть её ([RepeatIds.generator])
        val taken = repository.getTask(RepeatIds.of(template.id, occurrence)) != null
        val newId = RepeatIds.generator(template.id, occurrence, taken)
        if (template.type == Item.TYPE_PROJECT_TEMPLATE) {
            val inside = repository.getProjectItems(template.id)
            val parts = inside.filter { it.isTemplatePart && it.repeatTemplateId == template.id && !it.trashed }
                .map { ItemWithChecklist(it, repository.getChecklist(it.id)) }
            val copy = RepeatProjects.copyOf(template, inside.filter { it.isHeading }, parts, rule, occurrence, zone, newId = newId)
            repository.insertProject(copy.project)
            if (copy.project.tags.isNotEmpty()) repository.setItemTagsByTitles(copy.project.id, copy.project.tags)
            if (copy.headings.isNotEmpty()) repository.insertTasks(copy.headings)
            copy.tasks.forEach { (task, checklist) ->
                repository.insertTask(task, checklist)
                if (task.tags.isNotEmpty()) repository.setItemTagsByTitles(task.id, task.tags)
            }
        } else {
            val (planned, checklist) = RepeatPlan.copyOf(template, repository.getChecklist(template.id), rule, occurrence, zone, newId = newId)
            var copy = planned
            if (appearOn != null) copy = copy.copy(
                startDate = appearOn.atStartOfDay(zone).toInstant().toEpochMilli(),
                dueDate = rule.deadlineDaysBefore?.let { appearOn.plusDays(it.toLong()).atStartOfDay(zone).toInstant().toEpochMilli() }
            )
            if (completedAt != null) copy = copy.copy(status = Item.STATUS_COMPLETED, stopDate = completedAt)
            repository.insertTask(copy, checklist)
            if (copy.tags.isNotEmpty()) repository.setItemTagsByTitles(copy.id, copy.tags)
        }
    }
}
