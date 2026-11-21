package com.example.domain.edits

import com.example.data.model.Area
import java.util.UUID

/** Новая область из меню «+» главного экрана: с пустым названием, которое тут же правят. */
object NewAreas {
    /** Как в Things, встаёт первой среди областей — перед самой верхней. */
    fun create(existing: List<Area>, id: String = UUID.randomUUID().toString()): Area =
        Area(id = id, title = "", sortOrder = (existing.minOfOrNull { it.sortOrder } ?: 1) - 1)
}
