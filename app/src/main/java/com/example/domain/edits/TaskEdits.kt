package com.example.domain.edits

import com.example.data.model.Item
import com.example.data.model.TaskSection
import com.example.data.model.toStartVal
import com.example.domain.tag.TagTitles
import com.example.domain.reminders.Reminders

/** Поля задачи, как их правят в редакторе. */
data class TaskEditorFields(
    val title: String,
    val notes: String,
    val section: TaskSection,
    val isTonight: Boolean,
    val startDate: Long?,
    val dueDate: Long?,
    val tags: List<String>,
    val projectId: String?,
    val priority: Int,
    /** Напоминание — время дня в минутах; null — нет */
    val reminderTime: Int? = null
)

/**
 * Задача после правки в редакторе — одно правило и для сохранения, и для отметки выполненной
 * прямо из открытого редактора. Закреплено тестами EditsTest.
 */
object TaskEdits {

    fun apply(task: Item, fields: TaskEditorFields, untitledTitle: String, now: Long = System.currentTimeMillis()): Item {
        // Задача «Входящих», попавшая в проект, — уже разобрана
        val start = if (fields.projectId != null && fields.section == TaskSection.INBOX) {
            TaskSection.ANYTIME.toStartVal()
        } else {
            fields.section.toStartVal()
        }
        return task.copy(
            title = fields.title.ifBlank { untitledTitle },
            notes = fields.notes,
            start = start,
            isTonight = fields.isTonight,
            // У шаблона повтора своей даты нет — дни задаёт правило (в «Предстоящих» дата лишь подставлена)
            startDate = if (task.type == Item.TYPE_TEMPLATE) null else fields.startDate,
            dueDate = fields.dueDate,
            cachedTags = TagTitles.join(fields.tags),
            projectId = fields.projectId,
            priority = fields.priority,
            // Напоминание срабатывает в день задачи: без дня (дату убрали, «Когда-нибудь») оно снимается.
            // У шаблона повтора дня нет вовсе — его напоминание переходит в копии и сохраняется как есть
            reminderTime = if (task.type == Item.TYPE_TEMPLATE) fields.reminderTime
            else fields.reminderTime?.takeIf { Reminders.hasDay(fields.startDate, start) },
            modificationDate = now
        )
    }

    /**
     * Черновик открытого редактора, который записывается при уходе приложения в фон, чтобы набранное
     * не пропало, если система выгрузит процесс. Пишутся только содержимое и свойства, не меняющие
     * места задачи (название, заметки, теги, приоритет); раздел, даты, «Вечер» и проект остаются
     * сохранёнными: иначе задача ушла бы из текущего списка вместе с открытым редактором.
     * Остальное сохранится при сворачивании редактора, как обычно.
     */
    fun backgroundDraft(saved: TaskEditorFields, edited: TaskEditorFields): TaskEditorFields =
        edited.copy(
            section = saved.section,
            isTonight = saved.isTonight,
            startDate = saved.startDate,
            dueDate = saved.dueDate,
            projectId = saved.projectId,
            // Напоминание привязано к дню задачи — сохраняется вместе с ним, при сворачивании
            reminderTime = saved.reminderTime
        )
}
