package com.example.data.model

data class ItemWithChecklist(
    val item: Item,
    val checklist: List<ChecklistItem> = emptyList()
)
