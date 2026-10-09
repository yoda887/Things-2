package com.example.domain.edits

import com.example.data.model.Item
import java.util.UUID

/** Новый проект: пустое название, которое тут же правят на главном экране. */
object NewProjects {
    fun create(areaId: String? = null, id: String = UUID.randomUUID().toString(), now: Long = System.currentTimeMillis()): Item =
        Item(id = id, type = Item.TYPE_PROJECT, title = "", areaId = areaId, creationDate = now)
}
