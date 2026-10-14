package com.example.domain.usecase.task

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
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

    suspend fun insert(repository: ITaskRepository, template: Item, rule: RepeatRule, occurrence: LocalDate, zone: ZoneId) {
        if (template.type == Item.TYPE_PROJECT_TEMPLATE) {
            val inside = repository.getProjectItems(template.id)
            val parts = inside.filter { it.isTemplatePart && it.repeatTemplateId == template.id && !it.trashed }
                .map { ItemWithChecklist(it, repository.getChecklist(it.id)) }
            val copy = RepeatProjects.copyOf(template, inside.filter { it.isHeading }, parts, rule, occurrence, zone)
            repository.insertProject(copy.project)
            if (copy.project.tags.isNotEmpty()) repository.setItemTagsByTitles(copy.project.id, copy.project.tags)
            if (copy.headings.isNotEmpty()) repository.insertTasks(copy.headings)
            copy.tasks.forEach { (task, checklist) ->
                repository.insertTask(task, checklist)
                if (task.tags.isNotEmpty()) repository.setItemTagsByTitles(task.id, task.tags)
            }
        } else {
            val (copy, checklist) = RepeatPlan.copyOf(template, repository.getChecklist(template.id), rule, occurrence, zone)
            repository.insertTask(copy, checklist)
            if (copy.tags.isNotEmpty()) repository.setItemTagsByTitles(copy.id, copy.tags)
        }
    }
}
