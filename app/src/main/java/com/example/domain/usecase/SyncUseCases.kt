package com.example.domain.usecase

import com.example.domain.usecase.sync.*
import javax.inject.Inject

/**
 * Фасад для сценариев использования, связанных с синхронизацией.
 */
class SyncUseCases @Inject constructor(
    val syncGoogleTasks: SyncGoogleTasksUseCase,
    val fetchLocalCalendarEvents: FetchLocalCalendarEventsUseCase
)
