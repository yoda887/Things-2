package com.example.domain.edits

import com.example.data.model.Item

/** Куда перемещают задачу. */
sealed interface MoveTarget {
    /** Во «Входящие»: без проекта, области, даты и срока */
    data object Inbox : MoveTarget
    /** В проект и/или область (null и null — «без проекта») */
    data class Place(val projectId: String?, val areaId: String?) : MoveTarget
}

/** Перемещение задачи. Закреплено тестами EditsTest. */
object TaskMoves {

    fun moved(task: Item, target: MoveTarget, now: Long = System.currentTimeMillis()): Item = when (target) {
        MoveTarget.Inbox -> task.copy(
            projectId = null,
            areaId = null,
            start = Item.START_INBOX,
            startDate = null,
            dueDate = null,
            modificationDate = now
        )
        is MoveTarget.Place -> task.copy(
            projectId = target.projectId,
            areaId = target.areaId,
            // Задача из «Входящих», попавшая в проект, — уже разобрана
            start = if (task.start == Item.START_INBOX && target.projectId != null) Item.START_ANYTIME else task.start,
            modificationDate = now
        )
    }
}
