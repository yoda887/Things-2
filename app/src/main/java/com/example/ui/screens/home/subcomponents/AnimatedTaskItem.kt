package com.example.ui.screens.home.subcomponents

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.TaskSection
import com.example.data.model.ChecklistItem
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.components.ThingsCategoryListEvent
import com.example.ui.screens.home.components.TaskDragDropState
import com.example.ui.screens.home.components.taskDragAndDrop
import com.example.ui.screens.home.components.UpcomingDay
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DELETE_ANIMATION_DELAY_MS = 300L

/**
 * Анимированный и интерактивный контейнер задачи.
 * Объединяет логику анимаций перетаскивания (Drag-and-Drop), затемнения (Dimming),
 * отображения встроенного редактора задачи (ThingsTaskInlineEditor) и стандартной строки.
 */
@Composable
fun AnimatedTaskItem(
    taskWrapper: ItemWithChecklist,
    dragDropState: TaskDragDropState,
    inlineExpandedTaskId: String?,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    highlightedTaskId: String?,
    projects: List<Item>,
    allSavedTags: List<String>,
    allSavedTagObjects: List<Tag>,
    deletedTaskIds: List<String>,
    onDeletedTaskIdAdd: (String) -> Unit,
    onEvent: (ThingsCategoryListEvent) -> Unit,
    coroutineScope: CoroutineScope,
    screen: ActiveScreen,
    upcomingDays: List<UpcomingDay>,
    localTasksList: List<ItemWithChecklist>,
    displayTasks: List<ItemWithChecklist>,
    onLocalTasksListChange: (List<ItemWithChecklist>) -> Unit,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    val task = taskWrapper.item
    val isDragTask = dragDropState.draggedTaskId == task.id
    val isExpanded = inlineExpandedTaskId == task.id
    val shouldDim = inlineExpandedTaskId != null && !isExpanded

    val dimAlpha by animateFloatAsState(
        targetValue = if (shouldDim) 0.3f else 1f,
        label = "dimAlpha_${task.id}"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragTask) 1.04f else 1.0f,
        label = "dragScale_${task.id}"
    )
    val dragElevation by animateDpAsState(
        targetValue = if (isDragTask) 8.dp else (if (isExpanded) 8.dp else 0.dp),
        label = "dragElev_${task.id}"
    )
    val zIndexValToUse = if (isDragTask) 100f else (if (isExpanded) 1f else 0f)
    val translationYVal = if (isDragTask) dragDropState.dragAccumulatedOffset else 0f

    val containerBgColor = if (isExpanded || isDragTask) MaterialTheme.colorScheme.background else Color.Transparent

    Column(
        modifier = modifier
            .zIndex(zIndexValToUse)
            .graphicsLayer {
                translationY = translationYVal
                scaleX = dragScale
                scaleY = dragScale
                alpha = dimAlpha
            }
            .layout { measurable, constraints ->
                val paddingPx = 4.dp.roundToPx()
                val extendedConstraints = constraints.copy(
                    minWidth = (constraints.minWidth + paddingPx * 2).coerceAtMost(constraints.maxWidth + paddingPx * 2),
                    maxWidth = (constraints.maxWidth + paddingPx * 2)
                )
                val placeable = measurable.measure(extendedConstraints)
                layout(placeable.width - paddingPx * 2, placeable.height) {
                    placeable.place(-paddingPx, 0)
                }
            }
            .shadow(dragElevation, RoundedCornerShape(8.dp))
            .background(containerBgColor, RoundedCornerShape(8.dp))
            .animateContentSize(animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ))
    ) {
        if (isExpanded) {
            ThingsTaskInlineEditor(
                task = taskWrapper,
                projects = projects,
                allSavedTags = allSavedTags,
                allSavedTagObjects = allSavedTagObjects,
                onNewTagCreated = { title, parentId -> onEvent(ThingsCategoryListEvent.CreateTag(title, parentId)) },
                onDeleteTag = { tag -> onEvent(ThingsCategoryListEvent.DeleteTag(tag)) },
                onUpdateTag = { tag -> onEvent(ThingsCategoryListEvent.UpdateTag(tag)) },
                onUpdateTagsOrder = { tags -> onEvent(ThingsCategoryListEvent.UpdateTagsOrder(tags)) },
                isDeletedExternally = { deletedTaskIds.contains(task.id) },
                onSave = { title, notes, section, isTonight, startDate, dueDate, tags, projectId, checklist, priority ->
                    onEvent(
                        ThingsCategoryListEvent.SaveTask(
                            taskWrapper = taskWrapper,
                            title = title,
                            notes = notes,
                            section = section,
                            isTonight = isTonight,
                            startDate = startDate,
                            dueDate = dueDate,
                            tags = tags,
                            projectId = projectId,
                            priority = priority,
                            checklist = checklist
                        )
                    )
                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                },
                onDelete = {
                    onDeletedTaskIdAdd(task.id)
                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                    coroutineScope.launch {
                        delay(DELETE_ANIMATION_DELAY_MS)
                        onEvent(ThingsCategoryListEvent.DeleteTask(taskWrapper))
                    }
                },
                onDone = {
                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                }
            )
        } else {
            TaskItemRow(
                modifier = Modifier,
                task = taskWrapper.item,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                dividerColor = dividerColor,
                onToggle = { onEvent(ThingsCategoryListEvent.ToggleTask(taskWrapper)) },
                onClick = {
                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(task.id))
                },
                projects = projects,
                showTodayIndicator = screen == ActiveScreen.TODAY && !task.isTonight,
                isDragging = false,
                dragOffsetY = 0f,
                dragModifier = Modifier.taskDragAndDrop(
                    state = dragDropState,
                    taskWrapper = taskWrapper,
                    lazyListState = lazyListState,
                    screen = screen,
                    upcomingDays = upcomingDays,
                    localTasksList = localTasksList,
                    filteredTasks = displayTasks,
                    onLocalTasksListChange = onLocalTasksListChange,
                    onTasksReordered = { items ->
                        onEvent(ThingsCategoryListEvent.ReorderTasks(items))
                    }
                ),
                isHighlighted = task.id == highlightedTaskId
            )
        }
    }
}
