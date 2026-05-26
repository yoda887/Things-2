package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val googleTaskListId: String? = null,
    val creationDate: Long = System.currentTimeMillis()
)
