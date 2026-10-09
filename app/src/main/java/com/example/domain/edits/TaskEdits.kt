package com.example.domain.edits

import com.example.data.model.Item
import com.example.data.model.TaskSection
import com.example.data.model.toStartVal
import com.example.domain.tag.TagTitles

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
    val priority: Int
)

/**
 * Задача после правки в редакторе — одно правило и для сохранения, и для отметки выполненной
 * прямо из открытого редактора. Закреплено тестами EditsTest.
 */
object TaskEdits {

    fun apply(task: Item, fields: TaskEditorFields, untitledTitle: String, now: Long = System.currentTimeMillis()): Item =
        task.copy(
            title = fields.title.ifBlank { untitledTitle },
            notes = fields.notes,
            // Задача «Входящих», попавшая в проект, — уже разобрана
            start = if (fields.projectId != null && fields.section == TaskSection.INBOX) {
                TaskSection.ANYTIME.toStartVal()
            } else {
                fields.section.toStartVal()
            },
            isTonight = fields.isTonight,
            startDate = fields.startDate,
            dueDate = fields.dueDate,
            cachedTags = TagTitles.join(fields.tags),
            projectId = fields.projectId,
            priority = fields.priority,
            modificationDate = now
        )
}
