package com.example.ui.screens.home.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.TaskSection
import com.example.domain.edits.ProjectEdit
import com.example.ui.components.ThingsConfirmDialog
import com.example.ui.screens.home.inlineeditor.DeadlineDatePickerDialog
import com.example.ui.screens.home.subcomponents.startSection
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsMoveDialog
import com.example.ui.screens.home.inlineeditor.dialogs.ThingsWhenDialog
import com.example.ui.screens.home.subcomponents.BatchActionToolbar

/**
 * «Когда» для одного проекта — свайп вправо по проекту на главном экране, как у задач. Диалог
 * вырастает из строки ([growFromOffset]); выбор сохраняется при закрытии.
 */
@Composable
fun ProjectSwipeWhenDialog(
    project: Item,
    growFromOffset: Offset?,
    onEditProject: (Item, ProjectEdit) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember(project.id) { mutableStateOf(project.startDate) }
    var section by remember(project.id) { mutableStateOf(project.startSection) }
    var isTonight by remember(project.id) { mutableStateOf(project.isTonight) }
    ThingsWhenDialog(
        startDate = startDate,
        onStartDateChange = { startDate = it },
        section = section,
        onSectionChange = { section = it },
        isTonight = isTonight,
        onIsTonightChange = { isTonight = it },
        onShowCalendarHelperChange = { },
        growFromOffset = growFromOffset,
        onDismissRequest = {
            onEditProject(project, ProjectEdit.Start(section, startDate, isTonight))
            onDismiss()
        }
    )
}

private enum class ProjectBatchDialog { WHEN, MOVE, DELETE, COMPLETE, DEADLINE }

/**
 * Множественный выбор проектов на главном экране (свайп влево по проекту, как у задач): нижняя панель
 * действий и их диалоги — «Когда», «Переместить» (в область), «Удалить», «Завершить», «Срок»,
 * «Дублировать», «Поделиться». После действия режим выбора закрывается ([onDone]).
 */
@Composable
fun BoxScope.HomeProjectBatchLayer(
    isActive: Boolean,
    selected: List<Item>,
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    onEditProjects: (List<Item>, ProjectEdit) -> Unit,
    onDeleteProjects: (List<Item>) -> Unit,
    onCompleteProjects: (List<Item>) -> Unit,
    onDuplicateProjects: (List<Item>) -> Unit,
    onDone: () -> Unit
) {
    var dialog by remember { mutableStateOf<ProjectBatchDialog?>(null) }
    val context = LocalContext.current
    val close: () -> Unit = { dialog = null }
    val finish: () -> Unit = { dialog = null; onDone() }

    BatchActionToolbar(
        // Пока открыто окно «Переместить», панель скрыта — как у задач
        isVisible = isActive && dialog != ProjectBatchDialog.MOVE,
        selectedCount = selected.size,
        onWhenClick = { dialog = ProjectBatchDialog.WHEN },
        onMoveClick = { dialog = ProjectBatchDialog.MOVE },
        onDeleteClick = { dialog = ProjectBatchDialog.DELETE },
        onCompleteClick = { dialog = ProjectBatchDialog.COMPLETE },
        // Теги нескольким проектам сразу пока не назначаются
        onSetTagsClick = null,
        onSetDeadlineClick = { dialog = ProjectBatchDialog.DEADLINE },
        onDuplicateClick = { onDuplicateProjects(selected); onDone() },
        onShareClick = {
            val text = selected.joinToString("\n") { "• " + it.title }
            if (text.isNotEmpty()) {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(send, null))
            }
        },
        modifier = Modifier.align(Alignment.BottomCenter)
    )

    when (dialog) {
        ProjectBatchDialog.WHEN -> {
            // Начальное состояние — общее у выбранных проектов, иначе ничего не отмечено
            var startDate by remember { mutableStateOf(selected.map { it.startDate }.distinct().singleOrNull()) }
            var section by remember {
                mutableStateOf(selected.map { it.startSection }.distinct().singleOrNull() ?: TaskSection.ANYTIME)
            }
            var isTonight by remember { mutableStateOf(selected.isNotEmpty() && selected.all { it.isTonight }) }
            var picked by remember { mutableStateOf(false) }
            ThingsWhenDialog(
                startDate = startDate,
                onStartDateChange = { startDate = it; picked = true },
                section = section,
                onSectionChange = { section = it; picked = true },
                isTonight = isTonight,
                onIsTonightChange = { isTonight = it; picked = true },
                onShowCalendarHelperChange = { },
                onDismissRequest = {
                    if (picked) {
                        onEditProjects(selected, ProjectEdit.Start(section, startDate, isTonight))
                        finish()
                    } else {
                        close()
                    }
                }
            )
        }
        ProjectBatchDialog.MOVE -> ThingsMoveDialog(
            currentProjectId = null,
            currentAreaId = selected.map { it.areaId }.distinct().singleOrNull(),
            currentIsInbox = false,
            projects = projects,
            areas = areas.filter { !it.trashed },
            allTasks = allTasks,
            onMove = { _, areaId, _ ->
                onEditProjects(selected, ProjectEdit.MoveToArea(areaId))
                finish()
            },
            onDismissRequest = close,
            areasOnly = true
        )
        ProjectBatchDialog.DELETE -> ThingsConfirmDialog(
            title = stringResource(R.string.batch_delete_projects_title),
            message = pluralStringResource(R.plurals.batch_delete_projects_message, selected.size, selected.size),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = { onDeleteProjects(selected); finish() },
            onDismiss = close
        )
        ProjectBatchDialog.COMPLETE -> ThingsConfirmDialog(
            title = stringResource(R.string.batch_complete_projects_title),
            message = pluralStringResource(R.plurals.batch_complete_projects_message, selected.size, selected.size),
            confirmText = stringResource(R.string.project_complete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = { onCompleteProjects(selected); finish() },
            onDismiss = close,
            destructive = false
        )
        ProjectBatchDialog.DEADLINE -> DeadlineDatePickerDialog(
            initialSelectedDateMillis = selected.map { it.dueDate }.distinct().singleOrNull(),
            onDateSelected = { date -> onEditProjects(selected, ProjectEdit.Deadline(date)); finish() },
            onDismiss = close
        )
        null -> Unit
    }
}
