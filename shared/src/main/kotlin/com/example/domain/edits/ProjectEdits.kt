package com.example.domain.edits

import com.example.domain.reminders.Reminders
import com.example.data.model.Item
import com.example.data.model.TaskSection
import com.example.data.model.toStartVal
import com.example.domain.tag.TagTitles

/** Правка проекта. */
sealed interface ProjectEdit {
    data class Rename(val title: String) : ProjectEdit
    data class Notes(val notes: String) : ProjectEdit
    /**
     * «Когда»: раздел, дата старта и вечер — как их выбирают в окне «Когда»; [reminder] — новое напоминание
     * (null — напоминание не меняется, например при выборе нескольких проектов)
     */
    data class Start(
        val section: TaskSection,
        val startDate: Long?,
        val isTonight: Boolean,
        val reminder: ReminderChange? = null
    ) : ProjectEdit
    data class Tags(val titles: List<String>) : ProjectEdit
    data class Deadline(val dueDate: Long?) : ProjectEdit
    data class MoveToArea(val areaId: String?) : ProjectEdit
}

/** Новое напоминание: время дня в минутах или null — убрать. */
data class ReminderChange(val time: Int?)

/** Правки проекта. Закреплено тестами EditsTest. */
object ProjectEdits {

    /** Проект после правки; если ничего не изменилось — тот же объект (сохранять нечего). */
    fun apply(project: Item, edit: ProjectEdit, now: Long = System.currentTimeMillis()): Item {
        val changed = when (edit) {
            is ProjectEdit.Rename -> project.copy(title = edit.title)
            is ProjectEdit.Notes -> project.copy(notes = edit.notes)
            // У проекта нет «Входящих»: без даты он «В любое время»
            is ProjectEdit.Start -> {
                val start = if (edit.section == TaskSection.INBOX) TaskSection.ANYTIME.toStartVal() else edit.section.toStartVal()
                project.copy(
                    start = start,
                    startDate = edit.startDate,
                    isTonight = edit.isTonight,
                    // Напоминание срабатывает в день проекта: без дня («Когда-нибудь», «В любое время») снимается
                    reminderTime = (if (edit.reminder != null) edit.reminder.time else project.reminderTime)
                        ?.takeIf { Reminders.hasDay(edit.startDate, start) }
                )
            }
            is ProjectEdit.Tags -> project.copy(cachedTags = TagTitles.join(edit.titles))
            is ProjectEdit.Deadline -> project.copy(dueDate = edit.dueDate)
            is ProjectEdit.MoveToArea -> project.copy(areaId = edit.areaId)
        }
        return if (changed == project) project else changed.copy(modificationDate = now)
    }
}
