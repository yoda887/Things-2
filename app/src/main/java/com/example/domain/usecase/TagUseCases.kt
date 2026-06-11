package com.example.domain.usecase

import com.example.domain.usecase.tag.*
import javax.inject.Inject

/**
 * Фасад для сценариев использования, связанных с тегами.
 */
class TagUseCases @Inject constructor(
    val createGroup: CreateGroupUseCase,
    val createTagInGroup: CreateTagInGroupUseCase,
    val moveTagToGroup: MoveTagToGroupUseCase,
    val deleteTag: DeleteTagUseCase,
    val updateTag: UpdateTagUseCase,
    val updateTagsOrder: UpdateTagsOrderUseCase,
    val insertTag: InsertTagUseCase
)
