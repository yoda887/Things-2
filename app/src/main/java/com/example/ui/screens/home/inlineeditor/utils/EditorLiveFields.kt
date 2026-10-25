package com.example.ui.screens.home.inlineeditor.utils

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.Item

/**
 * Несохранённые дата и напоминание открытого редактора — для окна поверх него («Повторять»). Редактор пишет
 * их в базу только при сворачивании (черновик при уходе в фон даты не трогает), а окно должно начинаться
 * с дня, выбранного в редакторе.
 */
@Stable
class EditorLiveFields {
    private var taskId by mutableStateOf<String?>(null)
    private var startDate by mutableStateOf<Long?>(null)
    private var reminderTime by mutableStateOf<Int?>(null)

    /** Редактор задачи [id] сейчас показывает эти дату и напоминание */
    fun update(id: String, startDate: Long?, reminderTime: Int?) {
        taskId = id
        this.startDate = startDate
        this.reminderTime = reminderTime
    }

    /** Задача [item] с датой и напоминанием из её открытого редактора; без редактора — как есть */
    fun applyTo(item: Item): Item =
        if (item.id == taskId) item.copy(startDate = startDate, reminderTime = reminderTime) else item
}
