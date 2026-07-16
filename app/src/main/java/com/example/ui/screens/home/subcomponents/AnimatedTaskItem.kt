package com.example.ui.screens.home.subcomponents

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.draggedTaskId
import com.example.ui.screens.home.components.taskDragAndDrop
import com.example.ui.screens.home.components.UpcomingDay
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.AppIcons
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted

private const val DELETE_ANIMATION_DELAY_MS = 300L

/**
 * Анимированный и интерактивный контейнер задачи.
 * Объединяет логику анимаций перетаскивания (Drag-and-Drop), затемнения (Dimming),
 * отображения встроенного редактора задачи (ThingsTaskInlineEditor) и стандартной строки.
 */
@Composable
fun AnimatedTaskItem(
    taskWrapper: ItemWithChecklist,
    dragDropState: GenericDragDropState,
    inlineExpandedTaskId: String?,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    highlightedTaskId: String?,
    projects: List<Item>,
    areas: List<com.example.data.model.Area> = emptyList(),
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
    onWhenDialogVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val task = taskWrapper.item
    val currentProject = remember(task.projectId, projects) {
        projects.firstOrNull { it.id == task.projectId }
    }
    val currentArea = remember(task.areaId, areas) {
        areas.firstOrNull { it.id == task.areaId }
    }
    val isDragTask = dragDropState.draggedTaskId == task.id
    val isExpanded = inlineExpandedTaskId == task.id
    val shouldDim = inlineExpandedTaskId != null && !isExpanded

    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "expansionProgress_${task.id}"
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    var prevPaddingPx by remember(task.id) { mutableStateOf(0f) }

    LaunchedEffect(expansionProgress) {
        val currentPaddingPx = with(density) { (32.dp * expansionProgress).toPx() }
        val delta = currentPaddingPx - prevPaddingPx
        if (delta != 0f) {
            lazyListState.dispatchRawDelta(delta)
        }
        prevPaddingPx = currentPaddingPx
    }

    val dimAlpha by animateFloatAsState(
        targetValue = if (shouldDim) 0.3f else 1f,
        label = "dimAlpha_${task.id}"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragTask) 1.04f else 1.0f,
        label = "dragScale_${task.id}"
    )
    val dragElevation by animateDpAsState(
        targetValue = if (isDragTask) 8.dp else (if (isExpanded || expansionProgress > 0f) 8.dp else 0.dp),
        label = "dragElev_${task.id}"
    )
    val zIndexValToUse = if (isDragTask) 100f else (if (isExpanded || expansionProgress > 0f) 1f else 0f)
    // Извлечение значения из Animatable
    val translationYVal = if (isDragTask) dragDropState.dragAccumulatedOffset.value else 0f
    val translationXVal = if (isDragTask) dragDropState.dragAccumulatedOffsetHorizontal.value else 0f

    val containerBgColor = if (isExpanded || expansionProgress > 0f || isDragTask || dragElevation > 0.dp) MaterialTheme.colorScheme.background else Color.Transparent

    val cornerRadiusValue = (8 * (1f - expansionProgress).coerceAtLeast(0f)).dp
    val currCornerShape = RoundedCornerShape(cornerRadiusValue)

    val extraPaddingDp = 4.dp + (16.dp * expansionProgress)

    val verticalGapPadding = (32 * expansionProgress).dp

    // Добавляем светло-серую подложку (плейсхолдер) на физическое место задачи во время перетаскивания (landing slot)
    Column(
        modifier = modifier
            .zIndex(zIndexValToUse)
            .graphicsLayer {
                translationX = translationXVal
                translationY = translationYVal
                scaleX = dragScale
                scaleY = dragScale
                alpha = dimAlpha
            }
            .padding(top = verticalGapPadding, bottom = verticalGapPadding)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isDragTask) {
                val isDark = isSystemInDarkTheme()
                val placeholderBgColor = if (isDark) Color(0xFF2C2D32) else Color(0xFFE5E6EB)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.5f) // Полупрозрачный
                        .zIndex(-1f) // Уровнем ниже всех задач в списке (в рамках контекста элемента)
                        .background(placeholderBgColor, currCornerShape)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val extraPaddingPx = extraPaddingDp.roundToPx()
                        val extendedConstraints = constraints.copy(
                            minWidth = (constraints.minWidth + extraPaddingPx * 2).coerceAtMost(constraints.maxWidth + extraPaddingPx * 2),
                            maxWidth = (constraints.maxWidth + extraPaddingPx * 2)
                        )
                        val placeable = measurable.measure(extendedConstraints)
                        layout(placeable.width - extraPaddingPx * 2, placeable.height) {
                            placeable.place(-extraPaddingPx, 0)
                        }
                    }
                    .shadow(dragElevation, currCornerShape)
                    .background(containerBgColor, currCornerShape)
                    .animateContentSize(animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ))
            ) {
                if (isExpanded || expansionProgress > 0f) {
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
                        expansionProgress = expansionProgress,
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
                        },
                        onWhenDialogVisibilityChange = onWhenDialogVisibilityChange,
                        areas = areas,
                        onNavigateToProject = { project ->
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            onEvent(ThingsCategoryListEvent.ClickProject(project))
                        },
                        onNavigateToArea = { area ->
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            onEvent(ThingsCategoryListEvent.ClickArea(area))
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
                        areas = areas,
                        showTodayIndicator = screen == ActiveScreen.TODAY && !task.isTonight,
                        isDragging = false,
                        dragOffsetY = 0f,
                        dragModifier = Modifier.taskDragAndDrop(
                            state = dragDropState,
                            taskWrapper = taskWrapper,
                            screen = screen,
                            upcomingDays = upcomingDays,
                            localTasksList = localTasksList,
                            filteredTasks = displayTasks,
                            onLocalTasksListChange = onLocalTasksListChange,
                            onTasksReordered = { items ->
                                onEvent(ThingsCategoryListEvent.ReorderTasks(items))
                            }
                        ),
                        isHighlighted = task.id == highlightedTaskId,
                        screen = screen
                    )
                }
            }
        }

        // Project/Area indicator row (drawn under/outside the Card)
        if (isExpanded || expansionProgress > 0f) {
            if (currentProject != null || currentArea != null) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isExpanded,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .clickable {
                                    // Save task automatically and navigate
                                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                                    if (currentProject != null) {
                                        onEvent(ThingsCategoryListEvent.ClickProject(currentProject))
                                    } else if (currentArea != null) {
                                        onEvent(ThingsCategoryListEvent.ClickArea(currentArea))
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentProject != null) {
                                    androidx.compose.material.icons.Icons.Outlined.FormatListBulleted
                                } else {
                                    AppIcons.Area
                                },
                                contentDescription = null,
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentProject?.title ?: currentArea?.title ?: "",
                                style = TextStyle(
                                    fontSize = androidx.compose.material3.MaterialTheme.typography.bodyMedium.fontSize,
                                    color = Color(0xFF8E8E93),
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = ">",
                                style = TextStyle(
                                    fontSize = androidx.compose.material3.MaterialTheme.typography.bodyMedium.fontSize,
                                    color = Color(0xFFC7C7CC),
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
