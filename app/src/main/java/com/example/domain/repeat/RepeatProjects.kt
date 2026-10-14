package com.example.domain.repeat

import com.example.data.model.ChecklistItem
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Повторяющийся проект, как в Things: проект становится шаблоном ([Item.TYPE_PROJECT_TEMPLATE]), его открытые
 * задачи — частями шаблона ([Item.isTemplatePart]), заголовки остаются при нём. В день повтора появляется
 * копия проекта с копиями заголовков и задач. Шаблон и его части в обычных списках не видны — только
 * в «Повторяющихся», «Предстоящих» (на дне следующей копии) и на экране самого шаблона.
 * Закреплено тестами RepeatTest.
 */
object RepeatProjects {

    /** Сделать повторяющимся можно открытый проект, который сам не копия повторяющегося. */
    fun canRepeat(project: Item): Boolean =
        project.type == Item.TYPE_PROJECT && project.repeatTemplateId == null && !project.isClosed && !project.trashed

    /**
     * Проект и его открытые задачи — шаблоном: «Когда» и сроки снимаются (их задаёт правило), напоминание —
     * из окна «Повторять». Закрытые и удалённые задачи остаются как есть, в «Журнале» и «Корзине».
     * Возвращает изменённые записи.
     */
    fun template(project: Item, projectItems: List<Item>, reminderTime: Int?, now: Long = System.currentTimeMillis()): List<Item> {
        val template = project.copy(
            type = Item.TYPE_PROJECT_TEMPLATE,
            start = Item.START_ANYTIME,
            startDate = null,
            dueDate = null,
            isTonight = false,
            reminderTime = reminderTime,
            modificationDate = now
        )
        val parts = projectItems
            .filter { it.projectId == project.id && it.type == Item.TYPE_TASK && !it.isClosed && !it.trashed }
            .map { it.asPartOf(project.id, now) }
        return listOf(template) + parts
    }

    /** Копия проекта на день повтора: сам проект, его открытые заголовки и задачи-части с чек-листами. */
    data class Copy(val project: Item, val headings: List<Item>, val tasks: List<Pair<Item, List<ChecklistItem>>>)

    /**
     * Копия появляется в день [RepeatPlan.appearsOn] (срок — в день повтора, если правило его задаёт),
     * задачи — открытыми, без своих дат, под копиями своих заголовков; чек-листы свежие.
     */
    fun copyOf(
        template: Item,
        headings: List<Item>,
        parts: List<ItemWithChecklist>,
        rule: RepeatRule,
        occurrence: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Long = System.currentTimeMillis(),
        newId: () -> String = { UUID.randomUUID().toString() }
    ): Copy {
        val (taskCopy, _) = RepeatPlan.copyOf(template, emptyList(), rule, occurrence, zone, now, newId)
        val project = taskCopy.copy(type = Item.TYPE_PROJECT)
        val liveHeadings = headings.filter { it.isHeading && it.projectId == template.id && it.status == Item.STATUS_OPEN && !it.trashed }
        val headingIds = liveHeadings.associate { it.id to newId() }
        val headingCopies = liveHeadings.map { heading ->
            heading.copy(
                id = headingIds.getValue(heading.id),
                projectId = project.id,
                googleTaskId = null,
                googleTaskListId = null,
                creationDate = now,
                modificationDate = now
            )
        }
        // Порядок задач как в шаблоне: при равном sortOrder список ставит новые выше, поэтому время создания
        // копий убывает в порядке шаблона
        val ordered = parts
            .filter { it.item.isTemplatePart && it.item.repeatTemplateId == template.id && !it.item.trashed }
            .sortedWith(compareBy<ItemWithChecklist> { it.item.sortOrder }.thenByDescending { it.item.creationDate })
        val tasks = ordered.mapIndexed { index, part ->
            val id = newId()
            val task = part.item.copy(
                id = id,
                type = Item.TYPE_TASK,
                status = Item.STATUS_OPEN,
                projectId = project.id,
                headingId = part.item.headingId?.let { headingIds[it] },
                repeatTemplateId = null,
                start = Item.START_ANYTIME,
                startDate = null,
                dueDate = null,
                isTonight = false,
                stopDate = null,
                googleTaskId = null,
                googleTaskListId = null,
                creationDate = now - index,
                modificationDate = now
            )
            task to part.checklist.map { it.copy(id = newId(), itemId = id, isCompleted = false) }
        }
        return Copy(project, headingCopies, tasks)
    }

    /** Остановить: шаблон снова обычный проект «В любое время», части — его обычные задачи. */
    fun stopped(template: Item, projectItems: List<Item>, now: Long = System.currentTimeMillis()): List<Item> =
        listOf(template.copy(type = Item.TYPE_PROJECT, start = Item.START_ANYTIME, modificationDate = now)) +
            projectItems.filter { it.isTemplatePart && it.repeatTemplateId == template.id }.map { it.asTask(now) }

    /**
     * Порядок после правок: задача, добавленная или перенесённая в шаблон проекта, становится его частью;
     * часть, перенесённая из шаблона в другое место, — обычной задачей. Возвращает исправленные записи.
     */
    fun normalize(items: List<Item>, now: Long = System.currentTimeMillis()): List<Item> {
        val templateIds = items.filter { it.type == Item.TYPE_PROJECT_TEMPLATE && !it.trashed }.mapTo(HashSet()) { it.id }
        return items.mapNotNull { item ->
            when {
                item.type == Item.TYPE_TASK && item.projectId in templateIds && !item.isClosed && !item.trashed ->
                    item.asPartOf(item.projectId!!, now)
                item.isTemplatePart && item.projectId != item.repeatTemplateId -> item.asTask(now)
                else -> null
            }
        }
    }

    private fun Item.asPartOf(templateId: String, now: Long) = copy(
        type = Item.TYPE_TEMPLATE,
        repeatTemplateId = templateId,
        start = Item.START_ANYTIME,
        startDate = null,
        dueDate = null,
        isTonight = false,
        modificationDate = now
    )

    private fun Item.asTask(now: Long) = copy(type = Item.TYPE_TASK, repeatTemplateId = null, modificationDate = now)
}
