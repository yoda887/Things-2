package com.example.domain.lists

import com.example.data.model.Item

/** Общие правила видимости в списках. */
object ListRules {

    /** Проект виден в списках: не удалён, не завершён и не отменён (закрытые — в Logbook), как в Things. */
    fun isOpenProject(project: Item): Boolean = !project.trashed && project.status == Item.STATUS_OPEN

    /** Задача закрыта — выполнена или отменена; её место в Logbook. */
    fun isClosed(item: Item): Boolean = item.isClosed
}
