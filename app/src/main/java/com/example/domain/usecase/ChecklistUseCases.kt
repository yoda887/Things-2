package com.example.domain.usecase

import com.example.domain.usecase.checklist.*
import javax.inject.Inject

/**
 * Фасад для сценариев использования, связанных с чек-листами.
 */
class ChecklistUseCases @Inject constructor(
    val addChecklistItem: AddChecklistItemUseCase,
    val deleteChecklistItem: DeleteChecklistItemUseCase,
    val toggleChecklistItem: ToggleChecklistItemUseCase,
    val updateChecklistItems: UpdateChecklistItemsUseCase
)
