package com.example.domain.edits

import com.example.data.model.Item
import java.util.UUID

/**
 * Новый проект: с главного экрана — с пустым названием, которое тут же правят; из окна «Переместить» —
 * сразу с названием из поиска ([title]).
 */
object NewProjects {
    fun create(
        areaId: String? = null,
        id: String = UUID.randomUUID().toString(),
        now: Long = System.currentTimeMillis(),
        title: String = ""
    ): Item = Item(id = id, type = Item.TYPE_PROJECT, title = title.trim(), areaId = areaId, creationDate = now)
}
