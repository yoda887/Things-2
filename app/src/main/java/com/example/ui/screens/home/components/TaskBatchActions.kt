package com.example.ui.screens.home.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.TaskSection
import com.example.ui.screens.home.inlineeditor.DeadlineDatePickerDialog
import com.example.ui.screens.home.inlineeditor.dialogs.DeleteConfirmDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsMoveDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsTagDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog
import com.example.ui.screens.home.subcomponents.BatchActionToolbar
import com.example.ui.viewmodel.ThingsCategoryListState

/** Какие диалоги пакетных действий над выбранными задачами открыты (их открывает нижняя панель действий). */
@Stable
class TaskBatchDialogsState {
    var whenOpen by mutableStateOf(false)
    var moveOpen by mutableStateOf(false)
    var deleteOpen by mutableStateOf(false)
    var tagsOpen by mutableStateOf(false)
    var deadlineOpen by mutableStateOf(false)

    /** Открыт любой из диалогов. */
    val anyOpen: Boolean get() = whenOpen || moveOpen || deleteOpen || tagsOpen || deadlineOpen
}

/**
 * Диалоги пакетных действий над выбранными задачами: «Когда», «Переместить», подтверждение удаления, теги, срок.
 * Каждое действие уходит событием [onEvent], после него диалог закрывается.
 */
@Composable
fun TaskBatchDialogs(
    dialogs: TaskBatchDialogsState,
    state: ThingsCategoryListState,
    onEvent: (ThingsCategoryListEvent) -> Unit
) {
    if (dialogs.whenOpen) {
        // Начальное состояние — общее для выбранных задач: если все они уже в одной секции и с одной
        // датой, диалог показывает её отмеченной (и даёт кнопку «Clear»); если задачи разные —
        // не отмечено ничего. Пока пользователь ничего не выбрал, закрытие крестиком ничего не меняет.
        val batchSelected = remember(state.selectedTaskIds, state.allTasks) {
            state.allTasks.filter { state.selectedTaskIds.contains(it.item.id) }
        }
        var batchStartDate by remember {
            mutableStateOf(batchSelected.map { it.item.startDate }.distinct().singleOrNull())
        }
        var batchSection by remember {
            mutableStateOf(batchSelected.map { it.item.section }.distinct().singleOrNull() ?: TaskSection.ANYTIME)
        }
        var batchIsTonight by remember {
            mutableStateOf(batchSelected.isNotEmpty() && batchSelected.all { it.item.isTonight })
        }
        var batchPicked by remember { mutableStateOf(false) }

        ThingsWhenDialog(
            startDate = batchStartDate,
            onStartDateChange = { batchStartDate = it; batchPicked = true },
            section = batchSection,
            onSectionChange = { batchSection = it; batchPicked = true },
            isTonight = batchIsTonight,
            onIsTonightChange = { batchIsTonight = it; batchPicked = true },
            onShowCalendarHelperChange = { },
            onDismissRequest = {
                if (batchPicked) {
                    onEvent(ThingsCategoryListEvent.BatchScheduleTasks(batchStartDate, batchIsTonight, batchSection))
                }
                dialogs.whenOpen = false
            }
        )
    }

    if (dialogs.moveOpen) {
        // Среди выбранных есть подзаголовки — их можно перенести только в проект (не во «Входящие», не в область
        // и не под чужой подзаголовок)
        val withHeadings = state.allHeadings.any { it.id in state.selectedTaskIds }
        ThingsMoveDialog(
            projectsOnly = withHeadings,
            currentProjectId = null,
            currentAreaId = null,
            currentIsInbox = false,
            projects = state.projects,
            areas = state.areas,
            allTasks = state.allTasks,
            onMove = { projectId, areaId, moveToInbox ->
                onEvent(ThingsCategoryListEvent.BatchMoveTasks(projectId, areaId, moveToInbox))
                dialogs.moveOpen = false
            },
            onMoveToNewProject = { title ->
                onEvent(ThingsCategoryListEvent.BatchMoveTasksToNewProject(title))
                dialogs.moveOpen = false
            },
            headings = state.allHeadings,
            onMoveToHeading = if (withHeadings) null else { projectId, areaId, headingId ->
                onEvent(ThingsCategoryListEvent.BatchMoveTasks(projectId, areaId, false, headingId))
                dialogs.moveOpen = false
            },
            onDismissRequest = { dialogs.moveOpen = false }
        )
    }

    if (dialogs.deleteOpen) {
        DeleteConfirmDialog(
            onDismissRequest = { dialogs.deleteOpen = false },
            onConfirmDelete = {
                dialogs.deleteOpen = false
                onEvent(ThingsCategoryListEvent.BatchDeleteTasks)
            }
        )
    }

    if (dialogs.tagsOpen) {
        ThingsTagDialog(
            activeTags = emptyList(),
            allSavedTags = state.allSavedTags,
            allSavedTagObjects = state.allSavedTagObjects,
            onNewTagCreated = { name, parentId -> onEvent(ThingsCategoryListEvent.CreateTag(name, parentId)) },
            onDeleteTag = { tag -> onEvent(ThingsCategoryListEvent.DeleteTag(tag)) },
            onUpdateTag = { tag -> onEvent(ThingsCategoryListEvent.UpdateTag(tag)) },
            onUpdateTagsOrder = { tags -> onEvent(ThingsCategoryListEvent.UpdateTagsOrder(tags)) },
            onTagsSelected = { newTags ->
                onEvent(ThingsCategoryListEvent.BatchSetTags(newTags))
                dialogs.tagsOpen = false
            },
            onDismissRequest = { dialogs.tagsOpen = false }
        )
    }

    if (dialogs.deadlineOpen) {
        DeadlineDatePickerDialog(
            initialSelectedDateMillis = null,
            onDateSelected = { deadline ->
                onEvent(ThingsCategoryListEvent.BatchSetDeadline(deadline))
                dialogs.deadlineOpen = false
            },
            onDismiss = { dialogs.deadlineOpen = false }
        )
    }
}

/**
 * Плавающая нижняя панель пакетных действий над выбранными задачами. Действия с диалогом открывают его в [dialogs]
 * (их показывает [TaskBatchDialogs]); пока открыто окно «Переместить», панель скрыта.
 */
@Composable
fun BoxScope.TaskBatchToolbar(
    dialogs: TaskBatchDialogsState,
    state: ThingsCategoryListState,
    onEvent: (ThingsCategoryListEvent) -> Unit
) {
    val context = LocalContext.current
    BatchActionToolbar(
        isVisible = state.isSelectionMode && !dialogs.moveOpen,
        selectedCount = state.selectedTaskIds.size,
        onWhenClick = { dialogs.whenOpen = true },
        onMoveClick = { dialogs.moveOpen = true },
        onDeleteClick = { dialogs.deleteOpen = true },
        onCompleteClick = { onEvent(ThingsCategoryListEvent.BatchCompleteTasks(true)) },
        onSetTagsClick = { dialogs.tagsOpen = true },
        onSetDeadlineClick = { dialogs.deadlineOpen = true },
        onDuplicateClick = { onEvent(ThingsCategoryListEvent.BatchDuplicateTasks) },
        // «Когда», «Срок», «Теги», «Завершить» — только к задачам: выбраны одни подзаголовки — неактивны
        taskActionsEnabled = state.allTasks.any { state.selectedTaskIds.contains(it.item.id) },
        onShareClick = {
            // Подзаголовок — с его задачами, затем выбранные задачи
            val shareText = com.example.domain.edits.HeadingBatch.shareText(
                com.example.domain.edits.HeadingBatch.split(state.selectedTaskIds, state.allTasks.map { it.item }, state.allHeadings)
            )
            if (shareText.isNotEmpty()) {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
            }
        },
        modifier = Modifier.align(Alignment.BottomCenter),
        // Среди выбранных есть повторяющиеся задачи: пауза, возобновить и остановить повтор у всех сразу
        onRepeatAction = state.allTasks
            .filter { state.selectedTaskIds.contains(it.item.id) && it.item.isRepeatTemplate }
            .map { it.item.id }
            .takeIf { it.isNotEmpty() }
            ?.let { templateIds ->
                { action ->
                    onEvent(ThingsCategoryListEvent.BatchRepeatAction(templateIds, action))
                    onEvent(ThingsCategoryListEvent.ExitSelectionMode)
                }
            }
    )
}
