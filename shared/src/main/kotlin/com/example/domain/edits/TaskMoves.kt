package com.example.domain.edits

import com.example.data.model.Item

/** Куда перемещают задачу. */
sealed interface MoveTarget {
    /** Во «Входящие»: без проекта, области, даты и срока */
    data object Inbox : MoveTarget
    /**
     * В проект и/или область (null и null — «без проекта»); [headingId] — сразу под подзаголовок проекта
     * (найденный в поиске окна «Переместить»), null — без подзаголовка (в том же проекте — прежний остаётся)
     */
    data class Place(val projectId: String?, val areaId: String?, val headingId: String? = null) : MoveTarget
}

/** Перемещение задачи. Закреплено тестами EditsTest. */
object TaskMoves {

    fun moved(task: Item, target: MoveTarget, now: Long = System.currentTimeMillis()): Item = when (target) {
        MoveTarget.Inbox -> task.copy(
            projectId = null,
            areaId = null,
            headingId = null,
            start = Item.START_INBOX,
            startDate = null,
            dueDate = null,
            modificationDate = now
        )
        is MoveTarget.Place -> task.copy(
            projectId = target.projectId,
            areaId = target.areaId,
            // Подзаголовок старого проекта не идёт с задачей в другой: в базе удаление подзаголовка уносит его задачи
            // (ON DELETE CASCADE) — перенесённая задача пропала бы вместе со старым подзаголовком или проектом
            headingId = target.headingId ?: task.headingId.takeIf { target.projectId != null && target.projectId == task.projectId },
            // Задача из «Входящих», попавшая в проект или область, — уже разобрана
            start = if (task.start == Item.START_INBOX && (target.projectId != null || target.areaId != null)) Item.START_ANYTIME else task.start,
            modificationDate = now
        )
    }
}
